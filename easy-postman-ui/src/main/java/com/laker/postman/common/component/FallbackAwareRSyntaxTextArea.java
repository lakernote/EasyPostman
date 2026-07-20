package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Arrays;

/**
 * RSyntaxTextArea that resolves one layout font for each token.
 *
 * <p>If the token's syntax font cannot display its complete text, the configured editor fallback
 * font is used for the entire token. Resolving at the text-area level is important: native token
 * painting, caret placement, mouse hit-testing, wrapping, and horizontal scrolling all consume the
 * same token font metrics.</p>
 */
public class FallbackAwareRSyntaxTextArea extends RSyntaxTextArea {

    private Font cachedFallbackBase;
    private float cachedFallbackSize = Float.NaN;
    private final Font[] derivedFallbackFonts = new Font[4];

    public FallbackAwareRSyntaxTextArea() {
        super();
        installFallbackFontListener();
    }

    public FallbackAwareRSyntaxTextArea(int rows, int columns) {
        super(rows, columns);
        installFallbackFontListener();
    }

    @Override
    public Font getFontForToken(Token token) {
        if (token == null) {
            return getFont();
        }
        if (token instanceof ResolvedFontToken resolvedToken && resolvedToken.resolvedFont() != null) {
            return resolvedToken.resolvedFont();
        }

        Font primaryFont = super.getFontForToken(token);
        if (canDisplayToken(primaryFont, token)) {
            return primaryFont;
        }

        Font fallbackFont = resolveFallbackFont(primaryFont);
        return fallbackFont != null && canDisplayToken(fallbackFont, token)
                ? fallbackFont
                : primaryFont;
    }

    @Override
    public FontMetrics getFontMetricsForToken(Token token) {
        return getFontMetrics(getFontForToken(token));
    }

    @Override
    public int getLineHeight() {
        FontMetrics fallbackMetrics = editorFallbackMetrics();
        return fallbackMetrics == null
                ? super.getLineHeight()
                : Math.max(super.getLineHeight(), fallbackMetrics.getHeight());
    }

    @Override
    public int getMaxAscent() {
        FontMetrics fallbackMetrics = editorFallbackMetrics();
        return fallbackMetrics == null
                ? super.getMaxAscent()
                : Math.max(super.getMaxAscent(), fallbackMetrics.getMaxAscent());
    }

    private void installFallbackFontListener() {
        addPropertyChangeListener(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY, ignored -> {
            revalidate();
            repaint();
        });
    }

    private FontMetrics editorFallbackMetrics() {
        Font fallbackFont = resolveFallbackFont(getFont());
        return fallbackFont == null ? null : getFontMetrics(fallbackFont);
    }

    private boolean canDisplayToken(Font font, Token token) {
        if (font == null || token.length() <= 0) {
            return true;
        }
        int start = token.getTextOffset();
        try {
            return font.canDisplayUpTo(token.getTextArray(), start, start + token.length()) == -1;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private synchronized Font resolveFallbackFont(Font primaryFont) {
        Object value = getClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY);
        if (!(value instanceof Font fallbackBase) || primaryFont == null) {
            return null;
        }

        float size = primaryFont.getSize2D();
        if (!fallbackBase.equals(cachedFallbackBase) || Float.compare(size, cachedFallbackSize) != 0) {
            cachedFallbackBase = fallbackBase;
            cachedFallbackSize = size;
            Arrays.fill(derivedFallbackFonts, null);
        }

        int style = primaryFont.getStyle();
        Font fallbackFont = derivedFallbackFonts[style];
        if (fallbackFont == null) {
            fallbackFont = fallbackBase.getStyle() == style
                    && Float.compare(fallbackBase.getSize2D(), size) == 0
                    ? fallbackBase
                    : fallbackBase.deriveFont(style, size);
            derivedFallbackFonts[style] = fallbackFont;
        }

        return samePhysicalFont(primaryFont, fallbackFont) ? null : fallbackFont;
    }

    private boolean samePhysicalFont(Font first, Font second) {
        return first.getFamily().equals(second.getFamily())
                && first.getStyle() == second.getStyle()
                && Float.compare(first.getSize2D(), second.getSize2D()) == 0;
    }

    /** Token slice whose font was already resolved from its complete source token. */
    interface ResolvedFontToken {
        Font resolvedFont();
    }
}
