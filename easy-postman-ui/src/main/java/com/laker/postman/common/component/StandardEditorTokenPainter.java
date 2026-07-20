package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.DefaultTokenPainterFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenPainter;

import javax.swing.text.TabExpander;
import java.awt.Graphics2D;

/**
 * Native RSyntaxTextArea token painting with token-aware selection measurement.
 *
 * <p>All actual drawing stays in RSyntaxTextArea's default painter. The only adapted operation is
 * {@link #nextX(Token, int, float, RSyntaxTextArea, TabExpander)}, because the library implementation
 * measures by token type while {@link FallbackAwareRSyntaxTextArea} resolves a font per token.</p>
 */
public class StandardEditorTokenPainter implements TokenPainter {

    private TokenPainter nativePainter;
    private boolean nativePainterShowsWhitespace;

    @Override
    public float nextX(Token token, int charCount, float x, RSyntaxTextArea host, TabExpander e) {
        if (token == null || charCount <= 0) {
            return x;
        }
        int measuredCharCount = Math.min(charCount, token.length());
        return x + token.getWidthUpTo(measuredCharCount, host, e, x);
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e) {
        return isPaintable(token) ? nativePainter(host).paint(token, graphics, x, y, host, e) : x;
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart) {
        return isPaintable(token)
                ? nativePainter(host).paint(token, graphics, x, y, host, e, clipStart)
                : x;
    }

    @Override
    public float paint(Token token, Graphics2D graphics, float x, float y,
                       RSyntaxTextArea host, TabExpander e, float clipStart, boolean paintBG) {
        return isPaintable(token)
                ? nativePainter(host).paint(token, graphics, x, y, host, e, clipStart, paintBG)
                : x;
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, boolean useSTC) {
        return isPaintable(token)
                ? nativePainter(host).paintSelected(token, graphics, x, y, host, e, useSTC)
                : x;
    }

    @Override
    public float paintSelected(Token token, Graphics2D graphics, float x, float y,
                               RSyntaxTextArea host, TabExpander e, float clipStart, boolean useSTC) {
        return isPaintable(token)
                ? nativePainter(host).paintSelected(token, graphics, x, y, host, e, clipStart, useSTC)
                : x;
    }

    protected final TokenPainter nativePainter(RSyntaxTextArea host) {
        boolean showWhitespace = host.isWhitespaceVisible();
        if (nativePainter == null || nativePainterShowsWhitespace != showWhitespace) {
            nativePainter = new DefaultTokenPainterFactory().getTokenPainter(host);
            nativePainterShowsWhitespace = showWhitespace;
        }
        return nativePainter;
    }

    private boolean isPaintable(Token token) {
        return token != null && token.isPaintable();
    }
}
