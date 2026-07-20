package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.DefaultTokenPainterFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
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
 * <p>Tokens fully covered by the editor font are delegated to RSyntaxTextArea's native painter, so
 * native whitespace markers, indentation guides, and bracket painting stay intact. Only tokens
 * requiring the configured fallback font use the custom font-run renderer. Viewport-specific
 * long-token chunking is provided separately by {@link ViewportClippedTokenPainter}.</p>
 */
public class StandardEditorTokenPainter implements TokenPainter {

    private static char[] tabBuffer;

    private final FallbackFontTextRenderer textRenderer = new FallbackFontTextRenderer();
    private TokenPainter defaultPainter;
    private boolean defaultPainterShowsWhitespace;

    @Override
    public float nextX(Token token, int charCount, float x, RSyntaxTextArea host, TabExpander e) {
        if (token == null || charCount <= 0) {
            return x;
        }

        FallbackFontTextRenderer.FontContext fontContext = textRenderer.resolveFontContext(host, token);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = Math.min(start + charCount, start + token.length());
        if (!textRenderer.requiresFallback(text, start, end, fontContext)) {
            return defaultPainter(host).nextX(token, end - start, x, host, e);
        }
        return measureRange(text, start, end, fontContext, e, x);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e) {
        if (!isPaintable(token)) {
            return x;
        }
        if (!requiresCustomPainting(token, host)) {
            return paintWithDefaultPainter(graphics, 0,
                    () -> defaultPainter(host).paint(token, graphics, x, y, host, e));
        }
        return paintImpl(token, graphics, x, y, host, e, 0, true, false);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart) {
        if (!isPaintable(token)) {
            return x;
        }
        if (!requiresCustomPainting(token, host)) {
            return paintWithDefaultPainter(graphics, clipStart,
                    () -> defaultPainter(host).paint(token, graphics, x, y, host, e, clipStart));
        }
        return paintImpl(token, graphics, x, y, host, e, clipStart, true, false);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart, boolean paintBG) {
        if (!isPaintable(token)) {
            return x;
        }
        if (!requiresCustomPainting(token, host)) {
            return paintWithDefaultPainter(graphics, clipStart,
                    () -> defaultPainter(host).paint(token, graphics, x, y, host, e, clipStart, paintBG));
        }
        return paintImpl(token, graphics, x, y, host, e, clipStart, paintBG, false);
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, boolean useSTC) {
        if (!isPaintable(token)) {
            return x;
        }
        if (!requiresCustomPainting(token, host)) {
            return paintWithDefaultPainter(graphics, 0,
                    () -> defaultPainter(host).paintSelected(token, graphics, x, y, host, e, useSTC));
        }
        return paintImpl(token, graphics, x, y, host, e, 0, false, useSTC);
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, float clipStart, boolean useSTC) {
        if (!isPaintable(token)) {
            return x;
        }
        if (!requiresCustomPainting(token, host)) {
            return paintWithDefaultPainter(graphics, clipStart,
                    () -> defaultPainter(host).paintSelected(token, graphics, x, y, host, e, clipStart, useSTC));
        }
        return paintImpl(token, graphics, x, y, host, e, clipStart, false, useSTC);
    }

    boolean requiresCustomPainting(Token token, RSyntaxTextArea host) {
        FallbackFontTextRenderer.FontContext fontContext = textRenderer.resolveFontContext(host, token);
        int start = token.getTextOffset();
        return textRenderer.requiresFallback(token.getTextArray(), start, start + token.length(), fontContext);
    }

    private float paintImpl(Token token, Graphics2D graphics, float x, float y,
                            RSyntaxTextArea host, TabExpander e, float clipStart,
                            boolean paintTokenBackground, boolean useSelectedTextColor) {
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
                token, text, start, end, graphics, x, y, fontContext,
                foreground, background, underline, host, e, clipRange
        );
        try {
            float nextX = paintToken(context);
            paintTabLines(context, (int) x, (int) nextX);
            return nextX;
        } finally {
            if (originalClip != null) {
                graphics.setClip(originalClip);
            }
        }
    }

    float paintToken(TokenPaintContext context) {
        return paintRange(context, context.start(), context.end(), context.x());
    }

    private boolean isPaintable(Token token) {
        return token != null && token.isPaintable();
    }

    private TokenPainter defaultPainter(RSyntaxTextArea host) {
        boolean showWhitespace = host.isWhitespaceVisible();
        if (defaultPainter == null || defaultPainterShowsWhitespace != showWhitespace) {
            defaultPainter = new DefaultTokenPainterFactory().getTokenPainter(host);
            defaultPainterShowsWhitespace = showWhitespace;
        }
        return defaultPainter;
    }

    private float paintWithDefaultPainter(Graphics2D graphics, float clipStart, PaintOperation operation) {
        ClipRange clipRange = resolveClipRange(graphics, clipStart);
        Shape originalClip = null;
        if (clipRange.narrowsActualClip()) {
            originalClip = graphics.getClip();
            constrainActualClip(graphics, clipRange);
        }
        try {
            return operation.paint();
        } finally {
            if (originalClip != null) {
                graphics.setClip(originalClip);
            }
        }
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
                paintWhitespace(context, flushEndX, tabEndX, true);
                currentX = tabEndX;
                flushStart = i + 1;
                flushLength = 0;
            } else if (context.host().isWhitespaceVisible() && context.text()[i] == ' ') {
                float flushEndX = currentX + textRenderer.measure(
                        context.text(), flushStart, flushStart + flushLength, context.fontContext());
                float spaceEndX = flushEndX + textRenderer.measure(
                        context.text(), i, i + 1, context.fontContext());
                paintSegment(context, flushStart, flushLength, currentX, flushEndX);
                paintWhitespace(context, flushEndX, spaceEndX, false);
                currentX = spaceEndX;
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

    private void paintWhitespace(TokenPaintContext context, float x, float nextX, boolean tab) {
        if (!isRangeVisible(x, nextX, context.clipRange())) {
            return;
        }
        if (context.background() != null) {
            paintBackground(context, x, nextX);
        }
        if (context.host().isWhitespaceVisible()) {
            context.graphics().setColor(context.foreground());
            if (tab) {
                paintVisibleTab(context, x, nextX);
            } else {
                paintVisibleSpace(context, x, nextX);
            }
        }
        if (context.underline()) {
            paintUnderline(context.graphics(), x, nextX, context.y(), context.foreground());
        }
    }

    private void paintVisibleSpace(TokenPaintContext context, float x, float nextX) {
        FontMetrics metrics = context.fontContext().primaryMetrics();
        int width = Math.max(1, (int) (nextX - x));
        int dotX = (int) (nextX - width / 2f);
        int dotY = (int) (context.y() - metrics.getAscent() + metrics.getHeight() / 2f);
        context.graphics().drawLine(dotX, dotY, dotX, dotY);
    }

    private void paintVisibleTab(TokenPaintContext context, float x, float nextX) {
        FontMetrics metrics = context.fontContext().primaryMetrics();
        int lineEndX = (int) nextX - 3;
        if (lineEndX >= (int) x) {
            int middleY = (int) context.y() - metrics.getAscent() + metrics.getHeight() / 2;
            context.graphics().drawLine((int) x, middleY, lineEndX, middleY);
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

    private void paintTabLines(TokenPaintContext context, int startX, int endX) {
        if (!context.host().getPaintTabLines() || startX != context.host().getMargin().left) {
            return;
        }

        if (context.token().getType() != Token.WHITESPACE) {
            int leadingWhitespaceLength = 0;
            while (leadingWhitespaceLength < context.token().length()
                    && RSyntaxUtilities.isWhitespace(context.token().charAt(leadingWhitespaceLength))) {
                leadingWhitespaceLength++;
            }
            if (leadingWhitespaceLength < 2) {
                return;
            }
            endX = (int) measureRange(
                    context, context.start(), context.start() + leadingWhitespaceLength, context.x());
        }

        int tabSize = context.host().getTabSize();
        if (tabBuffer == null || tabBuffer.length < tabSize) {
            tabBuffer = new char[tabSize];
            java.util.Arrays.fill(tabBuffer, ' ');
        }
        int tabWidth = context.fontContext().primaryMetrics().charsWidth(tabBuffer, 0, tabSize);
        if (tabWidth <= 0) {
            return;
        }

        context.graphics().setColor(context.host().getTabLineColor());
        int lineX = startX + tabWidth;
        int lineY = (int) context.y() - context.fontContext().primaryMetrics().getAscent();
        if ((lineY & 1) > 0) {
            lineY++;
        }

        Token next = context.token().getNextToken();
        if (next == null || !next.isPaintable()) {
            endX++;
        }
        while (lineX < endX) {
            int bottomY = lineY + context.host().getLineHeight();
            for (int y = lineY; y < bottomY; y += 2) {
                context.graphics().drawLine(lineX, y, lineX, y);
            }
            lineX += tabWidth;
        }
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

    record TokenPaintContext(Token token, char[] text, int start, int end,
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

    @FunctionalInterface
    private interface PaintOperation {
        float paint();
    }
}
