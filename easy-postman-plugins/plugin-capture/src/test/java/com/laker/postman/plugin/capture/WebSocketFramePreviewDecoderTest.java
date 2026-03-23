package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WebSocketFramePreviewDecoderTest {

    @Test
    public void shouldDecodeMaskedTextFrame() {
        WebSocketFramePreviewDecoder decoder = new WebSocketFramePreviewDecoder();

        List<String> events = decoder.decode(buildFrame(true, 0x1, true, "hello".getBytes(StandardCharsets.UTF_8)));

        assertEquals(events.size(), 1);
        assertTrue(events.get(0).contains("TEXT len=5"));
        assertTrue(events.get(0).contains("hello"));
    }

    @Test
    public void shouldAssembleFragmentedBinaryFrames() {
        WebSocketFramePreviewDecoder decoder = new WebSocketFramePreviewDecoder();

        List<String> first = decoder.decode(buildFrame(false, 0x2, false, new byte[]{0x01, 0x02}));
        List<String> second = decoder.decode(buildFrame(true, 0x0, false, new byte[]{0x03, 0x04}));

        assertEquals(first.size(), 1);
        assertTrue(first.get(0).contains("FRAGMENT START BINARY"));
        assertEquals(second.size(), 1);
        assertTrue(second.get(0).contains("FRAGMENT END BINARY len=4"));
        assertTrue(second.get(0).contains("01 02 03 04"));
    }

    @Test
    public void shouldDecodeCloseFrameReason() {
        WebSocketFramePreviewDecoder decoder = new WebSocketFramePreviewDecoder();
        ByteBuffer payload = ByteBuffer.allocate(2 + "done".length());
        payload.putShort((short) 1000);
        payload.put("done".getBytes(StandardCharsets.UTF_8));

        List<String> events = decoder.decode(buildFrame(true, 0x8, false, payload.array()));

        assertEquals(events, List.of("CLOSE code=1000 reason=done"));
    }

    private static byte[] buildFrame(boolean fin, int opcode, boolean masked, byte[] payload) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write((fin ? 0x80 : 0x00) | (opcode & 0x0F));
        int payloadLength = payload.length;
        if (payloadLength <= 125) {
            output.write((masked ? 0x80 : 0x00) | payloadLength);
        } else if (payloadLength <= 0xFFFF) {
            output.write((masked ? 0x80 : 0x00) | 126);
            output.write((payloadLength >>> 8) & 0xFF);
            output.write(payloadLength & 0xFF);
        } else {
            output.write((masked ? 0x80 : 0x00) | 127);
            for (int shift = 56; shift >= 0; shift -= 8) {
                output.write((payloadLength >>> shift) & 0xFF);
            }
        }
        if (masked) {
            byte[] mask = new byte[]{0x11, 0x22, 0x33, 0x44};
            output.writeBytes(mask);
            for (int i = 0; i < payload.length; i++) {
                output.write(payload[i] ^ mask[i % 4]);
            }
        } else {
            output.writeBytes(payload);
        }
        return output.toByteArray();
    }
}
