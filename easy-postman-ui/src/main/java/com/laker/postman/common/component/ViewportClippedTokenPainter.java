package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;

import javax.swing.text.TabExpander;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Native token painter specialization that divides very long tokens into viewport-sized slices.
 *
 * <p>Font fallback is intentionally not implemented here. A resolved font is inherited from the
 * complete source token so every slice uses the same metrics as caret placement, wrapping, and
 * horizontal scrolling.</p>
 */
public class ViewportClippedTokenPainter extends StandardEditorTokenPainter {

    private static final int LONG_TOKEN_THRESHOLD = 512;
    private static final int LONG_TOKEN_CHUNK_SIZE = 256;
    private static final Token NON_TERMINAL_TOKEN = createNonTerminalToken();

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e) {
        return shouldChunk(token, host, x)
                ? paintLongToken(token, graphics, x, y, host, e, 0,
                (slice, sliceX) -> nativePainter(host).paint(slice, graphics, sliceX, y, host, e))
                : super.paint(token, graphics, x, y, host, e);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart) {
        return shouldChunk(token, host, x)
                ? paintLongToken(token, graphics, x, y, host, e, clipStart,
                (slice, sliceX) -> nativePainter(host).paint(slice, graphics, sliceX, y, host, e, clipStart))
                : super.paint(token, graphics, x, y, host, e, clipStart);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart, boolean paintBG) {
        return shouldChunk(token, host, x)
                ? paintLongToken(token, graphics, x, y, host, e, clipStart,
                (slice, sliceX) -> nativePainter(host).paint(
                        slice, graphics, sliceX, y, host, e, clipStart, paintBG))
                : super.paint(token, graphics, x, y, host, e, clipStart, paintBG);
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, boolean useSTC) {
        return shouldChunk(token, host, x)
                ? paintLongToken(token, graphics, x, y, host, e, 0,
                (slice, sliceX) -> nativePainter(host).paintSelected(
                        slice, graphics, sliceX, y, host, e, useSTC))
                : super.paintSelected(token, graphics, x, y, host, e, useSTC);
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, float clipStart, boolean useSTC) {
        return shouldChunk(token, host, x)
                ? paintLongToken(token, graphics, x, y, host, e, clipStart,
                (slice, sliceX) -> nativePainter(host).paintSelected(
                        slice, graphics, sliceX, y, host, e, clipStart, useSTC))
                : super.paintSelected(token, graphics, x, y, host, e, clipStart, useSTC);
    }

    private float paintLongToken(Token token, Graphics2D graphics, float x, float y,
                                 RSyntaxTextArea host, TabExpander e, float clipStart,
                                 SlicePainter slicePainter) {
        Font resolvedFont = host.getFontForToken(token);
        ClipRange clipRange = resolveClipRange(graphics, clipStart);
        Shape originalClip = constrainClip(graphics, clipRange);
        float currentX = x;
        int tokenStart = token.getTextOffset();
        int tokenEnd = tokenStart + token.length();

        try {
            for (int chunkStart = tokenStart; chunkStart < tokenEnd; ) {
                int chunkEnd = resolveChunkEnd(token.getTextArray(), chunkStart, tokenEnd);
                ResolvedTokenSlice slice = new ResolvedTokenSlice(token, chunkStart, chunkEnd, resolvedFont);
                slice.setNextToken(chunkEnd < tokenEnd ? NON_TERMINAL_TOKEN : token.getNextToken());
                float nextX = currentX + slice.getWidth(host, e, currentX);

                if (nextX >= clipRange.left() && currentX <= clipRange.right()) {
                    slicePainter.paint(slice, currentX);
                }
                currentX = nextX;
                chunkStart = chunkEnd;
            }
            return currentX;
        } finally {
            if (originalClip != null) {
                graphics.setClip(originalClip);
            }
        }
    }

    private boolean isLongToken(Token token) {
        return token != null && token.isPaintable() && token.length() > LONG_TOKEN_THRESHOLD;
    }

    private boolean shouldChunk(Token token, RSyntaxTextArea host, float x) {
        return isLongToken(token) && !mustPreserveNativeTabLines(token, host, x);
    }

    private boolean mustPreserveNativeTabLines(Token token, RSyntaxTextArea host, float x) {
        if (!host.getPaintTabLines() || (int) x != host.getMargin().left) {
            return false;
        }
        if (token.getType() == Token.WHITESPACE) {
            return true;
        }
        return token.length() >= 2
                && RSyntaxUtilities.isWhitespace(token.charAt(0))
                && RSyntaxUtilities.isWhitespace(token.charAt(1));
    }

    private ClipRange resolveClipRange(Graphics2D graphics, float clipStart) {
        Rectangle clip = graphics.getClipBounds();
        return clip == null
                ? new ClipRange(clipStart, Float.POSITIVE_INFINITY, null)
                : new ClipRange(Math.max(clip.x, clipStart), clip.x + clip.width, clip);
    }

    private Shape constrainClip(Graphics2D graphics, ClipRange clipRange) {
        Rectangle clip = clipRange.clipBounds();
        if (clip == null || clipRange.left() <= clip.x) {
            return null;
        }
        Shape originalClip = graphics.getClip();
        int left = (int) Math.floor(clipRange.left());
        int right = (int) Math.ceil(clipRange.right());
        graphics.clipRect(left, clip.y, Math.max(0, right - left), clip.height);
        return originalClip;
    }

    private int resolveChunkEnd(char[] text, int chunkStart, int tokenEnd) {
        int preferredEnd = Math.min(chunkStart + LONG_TOKEN_CHUNK_SIZE, tokenEnd);
        if (preferredEnd == tokenEnd) {
            return tokenEnd;
        }

        int sequenceStart = chunkStart;
        while (sequenceStart < preferredEnd) {
            int sequenceEnd = nextDisplaySequenceEnd(text, sequenceStart, tokenEnd);
            if (sequenceEnd > preferredEnd) {
                break;
            }
            sequenceStart = sequenceEnd;
        }
        return sequenceStart > chunkStart
                ? sequenceStart
                : nextDisplaySequenceEnd(text, chunkStart, tokenEnd);
    }

    /** Keeps common combining, emoji, and Hangul sequences intact at chunk boundaries. */
    private int nextDisplaySequenceEnd(char[] text, int start, int end) {
        int firstCodePoint = codePointAt(text, start, end);
        int index = start + Character.charCount(firstCodePoint);

        if (isRegionalIndicator(firstCodePoint) && index < end) {
            int nextCodePoint = codePointAt(text, index, end);
            if (isRegionalIndicator(nextCodePoint)) {
                index += Character.charCount(nextCodePoint);
            }
        }

        while (index < end) {
            int codePoint = codePointAt(text, index, end);
            if (isSequenceExtender(codePoint) || joinsHangul(firstCodePoint, codePoint)) {
                firstCodePoint = codePoint;
                index += Character.charCount(codePoint);
                continue;
            }
            if (codePoint == 0x200D) {
                index += Character.charCount(codePoint);
                if (index < end) {
                    firstCodePoint = codePointAt(text, index, end);
                    index += Character.charCount(firstCodePoint);
                }
                continue;
            }
            break;
        }
        return index;
    }

    private int codePointAt(char[] text, int index, int end) {
        return index + 1 < end && Character.isHighSurrogate(text[index]) && Character.isLowSurrogate(text[index + 1])
                ? Character.toCodePoint(text[index], text[index + 1])
                : text[index];
    }

    private boolean isSequenceExtender(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || codePoint == 0x200C
                || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF)
                || (codePoint >= 0xE0020 && codePoint <= 0xE007F);
    }

    private boolean joinsHangul(int previous, int current) {
        return (isHangulL(previous) && (isHangulL(current) || isHangulV(current)
                || isHangulLv(current) || isHangulLvt(current)))
                || ((isHangulLv(previous) || isHangulV(previous))
                && (isHangulV(current) || isHangulT(current)))
                || ((isHangulLvt(previous) || isHangulT(previous)) && isHangulT(current));
    }

    private boolean isHangulL(int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x115F)
                || (codePoint >= 0xA960 && codePoint <= 0xA97C);
    }

    private boolean isHangulV(int codePoint) {
        return (codePoint >= 0x1160 && codePoint <= 0x11A7)
                || (codePoint >= 0xD7B0 && codePoint <= 0xD7C6);
    }

    private boolean isHangulT(int codePoint) {
        return (codePoint >= 0x11A8 && codePoint <= 0x11FF)
                || (codePoint >= 0xD7CB && codePoint <= 0xD7FB);
    }

    private boolean isHangulLv(int codePoint) {
        return codePoint >= 0xAC00 && codePoint <= 0xD7A3 && (codePoint - 0xAC00) % 28 == 0;
    }

    private boolean isHangulLvt(int codePoint) {
        return codePoint >= 0xAC00 && codePoint <= 0xD7A3 && (codePoint - 0xAC00) % 28 != 0;
    }

    private boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    private static Token createNonTerminalToken() {
        return new TokenImpl(new char[]{'x'}, 0, 0, 0, Token.IDENTIFIER, 0);
    }

    private record ClipRange(float left, float right, Rectangle clipBounds) {
    }

    @FunctionalInterface
    private interface SlicePainter {
        float paint(Token token, float x);
    }

    private static final class ResolvedTokenSlice extends TokenImpl
            implements FallbackAwareRSyntaxTextArea.ResolvedFontToken {
        private final Font resolvedFont;

        private ResolvedTokenSlice(Token source, int start, int end, Font resolvedFont) {
            super(source.getTextArray(), start, end - 1,
                    source.getOffset() + start - source.getTextOffset(),
                    source.getType(), source.getLanguageIndex());
            setHyperlink(source.isHyperlink());
            this.resolvedFont = resolvedFont;
        }

        @Override
        public Font resolvedFont() {
            return resolvedFont;
        }
    }
}
