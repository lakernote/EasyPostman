package com.laker.postman.plugin.capture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class WebSocketFramePreviewDecoder {
    private static final int FRAME_PREVIEW_LIMIT = 512;

    private byte[] pendingBytes = new byte[0];
    private Integer fragmentedOpcode;
    private ByteArrayOutputStream fragmentedPayload;

    List<String> decode(byte[] incomingBytes) {
        if (incomingBytes == null || incomingBytes.length == 0) {
            return List.of();
        }
        pendingBytes = appendBytes(pendingBytes, incomingBytes);
        List<String> events = new ArrayList<>();
        int offset = 0;
        while (true) {
            FrameParseResult result = tryParseFrame(pendingBytes, offset);
            if (result == null) {
                break;
            }
            offset = result.nextOffset();
            String event = handleFrame(result.frame());
            if (event != null && !event.isBlank()) {
                events.add(event);
            }
        }
        if (offset > 0) {
            byte[] remaining = new byte[pendingBytes.length - offset];
            System.arraycopy(pendingBytes, offset, remaining, 0, remaining.length);
            pendingBytes = remaining;
        }
        return events;
    }

    private String handleFrame(Frame frame) {
        int opcode = frame.opcode();
        if (opcode == 0x0) {
            return handleContinuation(frame);
        }
        if (opcode == 0x8) {
            return formatCloseFrame(frame.payload());
        }
        if (opcode == 0x9) {
            return formatPayload("PING", frame.payload(), true);
        }
        if (opcode == 0xA) {
            return formatPayload("PONG", frame.payload(), true);
        }
        if (opcode == 0x1 || opcode == 0x2) {
            if (frame.fin()) {
                return formatDataFrame(opcode, frame.payload());
            }
            fragmentedOpcode = opcode;
            fragmentedPayload = new ByteArrayOutputStream();
            fragmentedPayload.writeBytes(frame.payload());
            return "FRAGMENT START " + opcodeName(opcode) + " len=" + frame.payload().length;
        }
        return "FRAME opcode=0x" + Integer.toHexString(opcode).toUpperCase() + " len=" + frame.payload().length;
    }

    private String handleContinuation(Frame frame) {
        if (fragmentedPayload == null || fragmentedOpcode == null) {
            return formatPayload("CONTINUATION", frame.payload(), false);
        }
        fragmentedPayload.writeBytes(frame.payload());
        if (!frame.fin()) {
            return "FRAGMENT CONT len=" + frame.payload().length;
        }
        byte[] payload = fragmentedPayload.toByteArray();
        int opcode = fragmentedOpcode;
        fragmentedPayload = null;
        fragmentedOpcode = null;
        return "FRAGMENT END " + formatDataFrame(opcode, payload);
    }

    private String formatDataFrame(int opcode, byte[] payload) {
        String prefix = opcode == 0x1 ? "TEXT" : "BINARY";
        return formatPayload(prefix, payload, opcode == 0x1);
    }

    private String formatCloseFrame(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "CLOSE";
        }
        if (payload.length < 2) {
            return "CLOSE len=" + payload.length;
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int code = buffer.getShort() & 0xFFFF;
        byte[] reasonBytes = new byte[buffer.remaining()];
        buffer.get(reasonBytes);
        String reason = new String(reasonBytes, StandardCharsets.UTF_8);
        return reason.isBlank() ? "CLOSE code=" + code : "CLOSE code=" + code + " reason=" + reason;
    }

    private String formatPayload(String prefix, byte[] payload, boolean preferText) {
        int length = payload == null ? 0 : payload.length;
        if (length == 0) {
            return prefix;
        }
        String preview = preferText ? toTextPreview(payload) : null;
        if (preview == null || preview.isBlank()) {
            preview = toHexPreview(payload);
        }
        return prefix + " len=" + length + "\n" + preview;
    }

    private String toTextPreview(byte[] payload) {
        String text = new String(payload, StandardCharsets.UTF_8);
        int printable = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch) || !Character.isISOControl(ch)) {
                printable++;
            }
        }
        if (!text.isEmpty() && printable * 100 / text.length() < 90) {
            return null;
        }
        if (text.length() <= FRAME_PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, FRAME_PREVIEW_LIMIT) + "\n... truncated ...";
    }

    private String toHexPreview(byte[] payload) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(payload.length, 128);
        for (int i = 0; i < limit; i++) {
            if (i > 0 && i % 16 == 0) {
                builder.append('\n');
            } else if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", payload[i]));
        }
        if (payload.length > limit) {
            builder.append('\n').append("... truncated ...");
        }
        return builder.toString();
    }

    private static FrameParseResult tryParseFrame(byte[] bytes, int offset) {
        if (bytes.length - offset < 2) {
            return null;
        }
        int b0 = bytes[offset] & 0xFF;
        int b1 = bytes[offset + 1] & 0xFF;
        boolean fin = (b0 & 0x80) != 0;
        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        long payloadLength = b1 & 0x7F;
        int headerLength = 2;

        if (payloadLength == 126) {
            if (bytes.length - offset < 4) {
                return null;
            }
            payloadLength = ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
            headerLength = 4;
        } else if (payloadLength == 127) {
            if (bytes.length - offset < 10) {
                return null;
            }
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | (bytes[offset + 2 + i] & 0xFFL);
            }
            headerLength = 10;
        }

        int maskOffset = offset + headerLength;
        int payloadOffset = maskOffset + (masked ? 4 : 0);
        long totalLength = (long) payloadOffset + payloadLength;
        if (totalLength > bytes.length) {
            return null;
        }
        if (payloadLength > Integer.MAX_VALUE) {
            return null;
        }

        byte[] payload = new byte[(int) payloadLength];
        System.arraycopy(bytes, payloadOffset, payload, 0, payload.length);
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ bytes[maskOffset + (i % 4)]);
            }
        }
        return new FrameParseResult(new Frame(fin, opcode, payload), payloadOffset + payload.length);
    }

    private static byte[] appendBytes(byte[] existing, byte[] appended) {
        if (existing.length == 0) {
            return appended.clone();
        }
        byte[] merged = new byte[existing.length + appended.length];
        System.arraycopy(existing, 0, merged, 0, existing.length);
        System.arraycopy(appended, 0, merged, existing.length, appended.length);
        return merged;
    }

    private static String opcodeName(int opcode) {
        return opcode == 0x1 ? "TEXT" : opcode == 0x2 ? "BINARY" : "opcode=0x" + Integer.toHexString(opcode).toUpperCase();
    }

    private record Frame(boolean fin, int opcode, byte[] payload) {
    }

    private record FrameParseResult(Frame frame, int nextOffset) {
    }
}
