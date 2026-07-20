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

    boolean requiresFallback(char[] text, int start, int end, FontContext context) {
        return start < end && context.hasFallback()
                && !canDisplayRange(context.primaryFont(), text, start, end);
    }

    float measure(char[] text, int start, int end, FontContext context) {
        if (start >= end) {
            return 0f;
        }
        if (!requiresFallback(text, start, end, context)) {
            return context.primaryMetrics().charsWidth(text, start, end - start);
        }

        float width = 0f;
        int runStart = start;
        int firstClusterEnd = nextClusterEnd(text, start, end);
        FontMetrics runMetrics = selectMetrics(text, start, firstClusterEnd, context);
        for (int i = start; i < end; ) {
            int clusterEnd = nextClusterEnd(text, i, end);
            FontMetrics metrics = selectMetrics(text, i, clusterEnd, context);
            if (metrics != runMetrics) {
                width += runMetrics.charsWidth(text, runStart, i - runStart);
                runStart = i;
                runMetrics = metrics;
            }
            i = clusterEnd;
        }
        return width + runMetrics.charsWidth(text, runStart, end - runStart);
    }

    void paint(char[] text, int start, int end, Graphics2D graphics, float x, float y, FontContext context) {
        if (start >= end) {
            return;
        }
        if (!requiresFallback(text, start, end, context)) {
            graphics.setFont(context.primaryFont());
            graphics.drawChars(text, start, end - start, (int) x, (int) y);
            return;
        }

        float currentX = x;
        int runStart = start;
        int firstClusterEnd = nextClusterEnd(text, start, end);
        Font runFont = selectFont(text, start, firstClusterEnd, context);
        FontMetrics runMetrics = metricsFor(runFont, context);
        for (int i = start; i < end; ) {
            int clusterEnd = nextClusterEnd(text, i, end);
            Font font = selectFont(text, i, clusterEnd, context);
            if (font != runFont) {
                graphics.setFont(runFont);
                graphics.drawChars(text, runStart, i - runStart, (int) currentX, (int) y);
                currentX += runMetrics.charsWidth(text, runStart, i - runStart);
                runStart = i;
                runFont = font;
                runMetrics = metricsFor(runFont, context);
            }
            i = clusterEnd;
        }
        graphics.setFont(runFont);
        graphics.drawChars(text, runStart, end - runStart, (int) currentX, (int) y);
        graphics.setFont(context.primaryFont());
    }

    private FontMetrics selectMetrics(char[] text, int start, int end, FontContext context) {
        return metricsFor(selectFont(text, start, end, context), context);
    }

    private FontMetrics metricsFor(Font font, FontContext context) {
        return font == context.fallbackFont() ? context.fallbackMetrics() : context.primaryMetrics();
    }

    private Font selectFont(char[] text, int start, int end, FontContext context) {
        if (!context.hasFallback() || canDisplayCluster(context.primaryFont(), text, start, end)) {
            return context.primaryFont();
        }
        if (canDisplayCluster(context.fallbackFont(), text, start, end)) {
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

    private boolean canDisplayCluster(Font font, char[] text, int start, int end) {
        if (font == null || start >= end) {
            return false;
        }
        for (int i = start; i < end; ) {
            int codePoint = codePointAt(text, i, end);
            if (!isFontNeutral(codePoint) && !canDisplay(font, codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    private boolean canDisplay(Font font, int codePoint) {
        try {
            return font.canDisplay(codePoint);
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

    private static int codePointAt(char[] text, int index, int end) {
        if (index + 1 < end && Character.isHighSurrogate(text[index]) && Character.isLowSurrogate(text[index + 1])) {
            return Character.toCodePoint(text[index], text[index + 1]);
        }
        return text[index];
    }

    static int nextClusterEnd(char[] text, int start, int end) {
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
            if (isClusterExtender(codePoint)) {
                index += Character.charCount(codePoint);
                continue;
            }
            if (codePoint == 0x200D) {
                index += Character.charCount(codePoint);
                if (index < end) {
                    int joinedCodePoint = codePointAt(text, index, end);
                    index += Character.charCount(joinedCodePoint);
                }
                continue;
            }
            break;
        }
        return index;
    }

    private static boolean isClusterExtender(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || codePoint == 0x200C
                || isEmojiModifier(codePoint)
                || isTagCharacter(codePoint);
    }

    private static boolean isFontNeutral(int codePoint) {
        return Character.getType(codePoint) == Character.FORMAT || isVariationSelector(codePoint);
    }

    private static boolean isVariationSelector(int codePoint) {
        return (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                || (codePoint >= 0xE0100 && codePoint <= 0xE01EF);
    }

    private static boolean isEmojiModifier(int codePoint) {
        return codePoint >= 0x1F3FB && codePoint <= 0x1F3FF;
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    private static boolean isTagCharacter(int codePoint) {
        return codePoint >= 0xE0020 && codePoint <= 0xE007F;
    }

    record FontContext(Font primaryFont, FontMetrics primaryMetrics,
                       Font fallbackFont, FontMetrics fallbackMetrics) {
        boolean hasFallback() {
            return fallbackFont != null && fallbackMetrics != null;
        }
    }
}
