package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Measures and paints a token text segment with the configured editor fallback font.
 *
 * <p>This is deliberately below the {@code TokenPainter} level: viewport policies can reuse the
 * exact same font-run measurement and drawing without wrapping or invoking another painter.</p>
 */
final class FallbackFontTextRenderer {

    FontContext resolveFontContext(RSyntaxTextArea host, Token token) {
        Font primaryFont = host.getFontForToken(token);
        Font fallbackFont = resolveFallbackFont(host, primaryFont);
        FontMetrics primaryMetrics = host.getFontMetricsForToken(token);
        FontMetrics fallbackMetrics = fallbackFont == null ? null : host.getFontMetrics(fallbackFont);
        return new FontContext(primaryFont, primaryMetrics, fallbackFont, fallbackMetrics);
    }

    float measure(char[] text, int start, int end, FontContext context) {
        if (start >= end) {
            return 0f;
        }
        if (!context.hasFallback() || canDisplayRange(context.primaryFont(), text, start, end)) {
            return context.primaryMetrics().charsWidth(text, start, end - start);
        }

        float width = 0f;
        int runStart = start;
        FontMetrics runMetrics = selectMetrics(text, start, end, context);
        for (int i = start; i < end; ) {
            FontMetrics metrics = selectMetrics(text, i, end, context);
            if (metrics != runMetrics) {
                width += runMetrics.charsWidth(text, runStart, i - runStart);
                runStart = i;
                runMetrics = metrics;
            }
            i += codePointCharCount(text, i, end);
        }
        return width + runMetrics.charsWidth(text, runStart, end - runStart);
    }

    void paint(char[] text, int start, int end, Graphics2D graphics, float x, float y, FontContext context) {
        if (start >= end) {
            return;
        }
        if (!context.hasFallback() || canDisplayRange(context.primaryFont(), text, start, end)) {
            graphics.setFont(context.primaryFont());
            graphics.drawChars(text, start, end - start, (int) x, (int) y);
            return;
        }

        float currentX = x;
        int runStart = start;
        Font runFont = selectFont(text, start, end, context);
        FontMetrics runMetrics = metricsFor(runFont, context);
        for (int i = start; i < end; ) {
            Font font = selectFont(text, i, end, context);
            if (font != runFont) {
                graphics.setFont(runFont);
                graphics.drawChars(text, runStart, i - runStart, (int) currentX, (int) y);
                currentX += runMetrics.charsWidth(text, runStart, i - runStart);
                runStart = i;
                runFont = font;
                runMetrics = metricsFor(runFont, context);
            }
            i += codePointCharCount(text, i, end);
        }
        graphics.setFont(runFont);
        graphics.drawChars(text, runStart, end - runStart, (int) currentX, (int) y);
        graphics.setFont(context.primaryFont());
    }

    private FontMetrics selectMetrics(char[] text, int index, int end, FontContext context) {
        return metricsFor(selectFont(text, index, end, context), context);
    }

    private FontMetrics metricsFor(Font font, FontContext context) {
        return font == context.fallbackFont() ? context.fallbackMetrics() : context.primaryMetrics();
    }

    private Font selectFont(char[] text, int index, int end, FontContext context) {
        if (!context.hasFallback() || canDisplay(context.primaryFont(), text, index, end)) {
            return context.primaryFont();
        }
        if (canDisplay(context.fallbackFont(), text, index, end)) {
            return context.fallbackFont();
        }
        return context.primaryFont();
    }

    private Font resolveFallbackFont(RSyntaxTextArea host, Font primaryFont) {
        Object value = host.getClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY);
        if (!(value instanceof Font fallbackFont) || primaryFont == null) {
            return null;
        }
        Font derivedFallback = fallbackFont.deriveFont(primaryFont.getStyle(), primaryFont.getSize2D());
        if (samePhysicalFont(primaryFont, derivedFallback)) {
            return null;
        }
        return derivedFallback;
    }

    private boolean samePhysicalFont(Font first, Font second) {
        return first.getFamily().equals(second.getFamily())
                && first.getStyle() == second.getStyle()
                && first.getSize() == second.getSize();
    }

    private boolean canDisplay(Font font, char[] text, int index, int end) {
        if (font == null || index >= end) {
            return false;
        }
        try {
            char ch = text[index];
            if (Character.isHighSurrogate(ch) && index + 1 < end && Character.isLowSurrogate(text[index + 1])) {
                return font.canDisplay(Character.toCodePoint(ch, text[index + 1]));
            }
            return font.canDisplay(ch);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean canDisplayRange(Font font, char[] text, int start, int end) {
        if (font == null || start >= end) {
            return false;
        }
        try {
            return font.canDisplayUpTo(text, start, end) == -1;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private int codePointCharCount(char[] text, int index, int end) {
        if (index + 1 < end && Character.isHighSurrogate(text[index]) && Character.isLowSurrogate(text[index + 1])) {
            return 2;
        }
        return 1;
    }

    record FontContext(Font primaryFont, FontMetrics primaryMetrics,
                       Font fallbackFont, FontMetrics fallbackMetrics) {
        boolean hasFallback() {
            return fallbackFont != null && fallbackMetrics != null;
        }
    }
}
