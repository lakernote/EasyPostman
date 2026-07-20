package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenPainter;

import javax.swing.text.TabExpander;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Standard token painter for EasyPostman code editors.
 *
 * <p>It preserves RSyntaxTextArea token styling while delegating text-run measurement and drawing
 * to a fallback-aware renderer. Viewport-specific long-token chunking is provided separately by
 * {@link ViewportClippedTokenPainter}.</p>
 */
public class StandardEditorTokenPainter implements TokenPainter {

    private final FallbackFontTextRenderer textRenderer = new FallbackFontTextRenderer();

    @Override
    public float nextX(Token token, int charCount, float x, RSyntaxTextArea host, TabExpander e) {
        if (token == null || charCount <= 0) {
            return x;
        }

        FallbackFontTextRenderer.FontContext fontContext = textRenderer.resolveFontContext(host, token);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = Math.min(start + charCount, start + token.length());
        return measureRange(text, start, end, fontContext, e, x);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e) {
        return paint(token, graphics, x, y, host, e, 0);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart) {
        return paintImpl(token, graphics, x, y, host, e, clipStart, true, false);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart, boolean paintBG) {
        return paintImpl(token, graphics, x, y, host, e, clipStart, paintBG, false);
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, boolean useSTC) {
        return paintSelected(token, graphics, x, y, host, e, 0, useSTC);
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, float clipStart, boolean useSTC) {
        return paintImpl(token, graphics, x, y, host, e, clipStart, false, useSTC);
    }

    private float paintImpl(Token token, Graphics2D graphics, float x, float y,
                            RSyntaxTextArea host, TabExpander e, float clipStart,
                            boolean paintTokenBackground, boolean useSelectedTextColor) {
        if (token == null || !token.isPaintable()) {
            return x;
        }

        FallbackFontTextRenderer.FontContext fontContext = textRenderer.resolveFontContext(host, token);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = start + token.length();
        Color foreground = useSelectedTextColor ? host.getSelectedTextColor() : host.getForegroundForToken(token);
        Color background = paintTokenBackground ? host.getBackgroundForToken(token) : null;
        boolean underline = host.getUnderlineForToken(token);

        graphics.setFont(fontContext.primaryFont());
        graphics.setColor(foreground);

        ClipRange clipRange = resolveClipRange(graphics, clipStart);
        Shape originalClip = null;
        if (clipRange.narrowsActualClip()) {
            originalClip = graphics.getClip();
            constrainActualClip(graphics, clipRange);
        }

        TokenPaintContext context = new TokenPaintContext(
                text, start, end, graphics, x, y, fontContext,
                foreground, background, underline, host, e, clipRange
        );
        try {
            return paintToken(context);
        } finally {
            if (originalClip != null) {
                graphics.setClip(originalClip);
            }
        }
    }

    float paintToken(TokenPaintContext context) {
        return paintRange(context, context.start(), context.end(), context.x());
    }

    final float paintRange(TokenPaintContext context, int start, int end, float x) {
        float currentX = x;
        int flushStart = start;
        int flushLength = 0;

        for (int i = start; i < end; i++) {
            if (context.text()[i] == '\t') {
                float flushEndX = currentX + textRenderer.measure(
                        context.text(), flushStart, flushStart + flushLength, context.fontContext());
                float tabEndX = context.tabExpander().nextTabStop(flushEndX, 0);
                paintSegment(context, flushStart, flushLength, currentX, flushEndX);
                if (isRangeVisible(flushEndX, tabEndX, context.clipRange())) {
                    if (context.background() != null) {
                        paintBackground(context, flushEndX, tabEndX);
                    }
                    if (context.underline()) {
                        paintUnderline(context.graphics(), flushEndX, tabEndX,
                                context.y(), context.foreground());
                    }
                }
                currentX = tabEndX;
                flushStart = i + 1;
                flushLength = 0;
            } else {
                flushLength++;
            }
        }

        float nextX = currentX + textRenderer.measure(
                context.text(), flushStart, flushStart + flushLength, context.fontContext());
        paintSegment(context, flushStart, flushLength, currentX, nextX);
        return nextX;
    }

    final float measureRange(TokenPaintContext context, int start, int end, float x) {
        return measureRange(context.text(), start, end, context.fontContext(), context.tabExpander(), x);
    }

    final boolean isRangeVisible(float x, float nextX, ClipRange clipRange) {
        return nextX >= clipRange.left() && x <= clipRange.right();
    }

    private void paintSegment(TokenPaintContext context, int start, int length, float x, float nextX) {
        if (!isRangeVisible(x, nextX, context.clipRange())) {
            return;
        }
        if (context.background() != null) {
            paintBackground(context, x, nextX);
        }
        if (length > 0) {
            context.graphics().setColor(context.foreground());
            textRenderer.paint(context.text(), start, start + length,
                    context.graphics(), x, context.y(), context.fontContext());
        }
        if (context.underline()) {
            paintUnderline(context.graphics(), x, nextX, context.y(), context.foreground());
        }
    }

    private void paintBackground(TokenPaintContext context, float x, float nextX) {
        FontMetrics metrics = context.fontContext().primaryMetrics();
        context.graphics().setColor(context.background());
        context.graphics().fillRect((int) x, (int) (context.y() - metrics.getAscent()),
                Math.max(0, (int) (nextX - x)), context.host().getLineHeight());
    }

    private void paintUnderline(Graphics2D graphics, float x, float nextX, float y, Color foreground) {
        graphics.setColor(foreground);
        int underlineY = (int) y + 1;
        graphics.drawLine((int) x, underlineY, (int) nextX, underlineY);
    }

    private float measureRange(char[] text, int start, int end,
                               FallbackFontTextRenderer.FontContext fontContext,
                               TabExpander tabExpander, float x) {
        float currentX = x;
        int flushStart = start;
        int flushLength = 0;

        for (int i = start; i < end; i++) {
            if (text[i] == '\t') {
                currentX += textRenderer.measure(text, flushStart, flushStart + flushLength, fontContext);
                currentX = tabExpander.nextTabStop(currentX, 0);
                flushStart = i + 1;
                flushLength = 0;
            } else {
                flushLength++;
            }
        }

        return currentX + textRenderer.measure(text, flushStart, flushStart + flushLength, fontContext);
    }

    private ClipRange resolveClipRange(Graphics2D graphics, float clipStart) {
        Rectangle clip = graphics.getClipBounds();
        if (clip == null) {
            return new ClipRange(clipStart, Float.POSITIVE_INFINITY, null);
        }
        float left = Math.max(clip.x, clipStart);
        float right = clip.x + clip.width;
        return new ClipRange(left, right, clip);
    }

    private void constrainActualClip(Graphics2D graphics, ClipRange clipRange) {
        Rectangle clip = clipRange.clipBounds();
        int left = (int) Math.floor(clipRange.left());
        int right = (int) Math.ceil(clipRange.right());
        graphics.clipRect(left, clip.y, Math.max(0, right - left), clip.height);
    }

    record TokenPaintContext(char[] text, int start, int end,
                             Graphics2D graphics, float x, float y,
                             FallbackFontTextRenderer.FontContext fontContext,
                             Color foreground, Color background, boolean underline,
                             RSyntaxTextArea host, TabExpander tabExpander,
                             ClipRange clipRange) {
    }

    record ClipRange(float left, float right, Rectangle clipBounds) {
        boolean narrowsActualClip() {
            if (clipBounds == null || !Float.isFinite(right)) {
                return false;
            }
            int actualLeft = (int) Math.floor(left);
            int actualRight = (int) Math.ceil(right);
            int clipRight = clipBounds.x + clipBounds.width;
            return actualLeft > clipBounds.x || actualRight < clipRight;
        }
    }
}
