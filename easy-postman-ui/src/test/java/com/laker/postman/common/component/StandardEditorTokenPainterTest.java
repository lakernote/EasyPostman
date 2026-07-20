package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.testng.annotations.Test;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Covers the shared editor painter's fallback-font behavior and viewport specialization.
 */
public class StandardEditorTokenPainterTest {

    @Test
    public void shouldSkipShortTokenThatEndsBeforeViewport() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("abc");
        RecordingGraphics2D graphics = graphicsWithClip(100, 0, 60, 40);

        float nextX = new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 100f, true);

        assertTrue(nextX > 0f);
        assertEquals(graphics.drawCharsCalls, 0);
    }

    @Test
    public void shouldPaintShortTokenThatIntersectsViewport() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("abc");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 60, 40);

        float nextX = new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(nextX > 0f);
        assertEquals(graphics.drawCharsCalls, 1);
    }

    @Test
    public void shouldConstrainActualClipToLogicalClipStart() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("a".repeat(600));
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 160, 40);

        new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 80f, true);

        assertTrue(graphics.drawCharsCalls > 0);
        assertTrue(graphics.leftMostClipAtDraw >= 80,
                "Text drawing must be clipped to clipStart so horizontally scrolled text cannot bleed into the gutter");
    }

    @Test
    public void shouldKeepChunkedPaintingForLongTokenWhenNoSelectionIntersects() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("a".repeat(2_048));
        RecordingGraphics2D graphics = graphicsWithClip(2_500, 0, 120, 40);

        new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.drawCharsCalls > 0);
        assertTrue(graphics.longestDrawCharsLength <= 256,
                "Long-token rendering without selection must stay chunked so horizontal scrolling remains responsive");
    }

    @Test
    public void standardPainterShouldNotChunkLongTokens() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("a".repeat(2_048));
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 120, 40);

        new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(graphics.drawCharsCalls, 1);
        assertEquals(graphics.longestDrawCharsLength, 2_048,
                "Standard painting should remain independent from long-token chunking");
    }

    @Test
    public void shouldKeepChunkedPaintingWhenSelectionIntersectsLongToken() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        host.setText("a".repeat(600));
        host.select(10, 20);
        Token token = token("a".repeat(600));
        RecordingGraphics2D graphics = graphicsWithClip(120, 0, 80, 40);

        new ViewportClippedTokenPainter()
                .paintSelected(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.drawCharsCalls > 0);
        assertTrue(graphics.longestDrawCharsLength <= 256,
                "Selected long-token rendering must stay chunked so horizontal dragging remains responsive");
    }

    @Test
    public void viewportChunksShouldNotSplitEmojiSurrogatePairs() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("a".repeat(255) + "😀" + "b".repeat(300));
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 5_000, 40);

        new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.drawCharsCalls > 1);
        assertFalse(graphics.splitSurrogatePairAtDraw,
                "Viewport chunk boundaries must keep an emoji surrogate pair in the same draw call");
        assertTrue(graphics.longestDrawCharsLength <= 256);
    }

    @Test
    public void shouldConstrainActualClipWhenSelectionIntersectsLongToken() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        host.setText("a".repeat(600));
        host.select(10, 20);
        Token token = token("a".repeat(600));
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 160, 40);

        new ViewportClippedTokenPainter()
                .paintSelected(token, graphics, 0f, 20f, host, fixedTabExpander(), 80f, true);

        assertTrue(graphics.drawCharsCalls > 0);
        assertTrue(graphics.leftMostClipAtDraw >= 80,
                "Selected-token rendering must still constrain the real Graphics clip to clipStart");
    }

    @Test
    public void shouldUseConfiguredFallbackFontForUnsupportedCharacters() {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new Font(Font.SERIF, Font.PLAIN, 13);
        RSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY,
                new AllGlyphFont(fallbackFont.getName(), fallbackFont.getStyle(), fallbackFont.getSize()));
        Token token = token("ab汉cd");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 200, 40);

        new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(graphics.drawCharsCalls, 1,
                "A token must use one font so painting and editor layout share identical metrics");
        assertEquals(graphics.fontsAtDraw.get(0).getFamily(), fallbackFont.getFamily(),
                "A token unsupported by its syntax font should use the configured fallback font");
    }

    @Test
    public void shouldPaintPrimarySupportedTokenAsSingleRunWhenFallbackIsConfigured() {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new Font(Font.SERIF, Font.PLAIN, 13);
        RSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY, fallbackFont);
        Token token = token("abc123");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 200, 40);

        new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(graphics.drawCharsCalls, 1,
                "A token fully covered by the primary font should use the bulk drawing fast path");
        assertEquals(graphics.fontsAtDraw.get(0), primaryFont);
    }

    @Test
    public void standardPainterShouldPreserveNativeTabLines() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        host.setPaintTabLines(true);
        Token token = token("        ");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 200, 40);

        new StandardEditorTokenPainter().paint(
                token, graphics, host.getMargin().left, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.drawLineCalls > 0,
                "Tokens supported by the primary font must retain RSyntaxTextArea indentation guides");
    }

    @Test
    public void viewportPainterShouldPreserveAllNativeTabLinesForLongLeadingWhitespace() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        host.setPaintTabLines(true);
        Token token = token(" ".repeat(600) + "x");
        RecordingGraphics2D standardGraphics = graphicsWithClip(0, 0, 10_000, 40);
        RecordingGraphics2D viewportGraphics = graphicsWithClip(0, 0, 10_000, 40);

        new StandardEditorTokenPainter().paint(
                token, standardGraphics, host.getMargin().left, 20f, host, fixedTabExpander(), 0f, true);
        new ViewportClippedTokenPainter().paint(
                token, viewportGraphics, host.getMargin().left, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(viewportGraphics.drawLineCalls, standardGraphics.drawLineCalls,
                "Long-token optimization must not truncate native indentation guides");
    }

    @Test
    public void standardPainterShouldPreserveNativeVisibleWhitespace() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        host.setWhitespaceVisible(true);
        Token token = token("a b");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 200, 40);

        new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.drawLineCalls > 0,
                "Primary-font tokens must retain native visible-space markers");
    }

    @Test
    public void fallbackPaintingShouldPreserveVisibleWhitespace() {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new Font(Font.SERIF, Font.PLAIN, 13);
        RSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY,
                new AllGlyphFont(fallbackFont.getName(), fallbackFont.getStyle(), fallbackFont.getSize()));
        host.setWhitespaceVisible(true);
        Token token = token("汉 字");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 200, 40);

        new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(graphics.fontsAtDraw.get(0).getFamily(), fallbackFont.getFamily());
        assertTrue(graphics.drawLineCalls > 0,
                "Fallback-font tokens must still render visible-space markers");
    }

    @Test
    public void fallbackPaintingShouldPreserveTabLines() {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new Font(Font.SERIF, Font.PLAIN, 13);
        RSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY,
                new AllGlyphFont(fallbackFont.getName(), fallbackFont.getStyle(), fallbackFont.getSize()));
        host.setPaintTabLines(true);
        Token token = token("        汉");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 200, 40);

        new StandardEditorTokenPainter().paint(
                token, graphics, host.getMargin().left, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(graphics.fontsAtDraw.get(0).getFamily(), fallbackFont.getFamily());
        assertTrue(graphics.drawLineCalls > 0,
                "Fallback-font tokens must retain indentation guides for leading whitespace");
    }

    @Test
    public void shouldKeepEmojiZwjSequenceInOneFallbackFontRun() {
        Font primaryFont = new FormattingAwareAsciiFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new AllGlyphFont(Font.SERIF, Font.PLAIN, 13);
        RSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY, fallbackFont);
        String emojiSequence = "\uD83D\uDC68\uFE0F\u200D\uD83D\uDC69";
        Token token = token("A" + emojiSequence + "B");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 300, 40);

        new StandardEditorTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertEquals(graphics.textsAtDraw, List.of("A" + emojiSequence + "B"),
                "The whole token should be painted with the resolved fallback font in one native draw");
        assertEquals(graphics.fontsAtDraw, List.of(fallbackFont));
    }

    @Test
    public void fallbackMetricsShouldMatchCaretGeometry() throws Exception {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new AllGlyphFont(Font.SERIF, Font.PLAIN, 13);
        FallbackAwareRSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY, fallbackFont);
        host.setText("汉".repeat(100));
        host.setSize(3_000, 80);

        Token token = ((RSyntaxDocument) host.getDocument()).getTokenListForLine(0);
        Rectangle2D start = host.modelToView2D(0);
        Rectangle2D end = host.modelToView2D(host.getDocument().getLength());
        assertNotNull(start);
        assertNotNull(end);

        float paintedWidth = new StandardEditorTokenPainter()
                .nextX(token, token.length(), 0f, host, fixedTabExpander());
        assertEquals(paintedWidth, end.getX() - start.getX(), 0.01,
                "Painted text and caret placement must use the same fallback metrics");
    }

    @Test
    public void lineMetricsShouldAccommodateTheConfiguredFallbackFont() {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new AllGlyphFont(Font.SERIF, Font.PLAIN, 13);
        FallbackAwareRSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY, fallbackFont);
        FontMetrics fallbackMetrics = host.getFontMetrics(host.getFontForToken(token("汉")));

        assertTrue(host.getLineHeight() >= fallbackMetrics.getHeight());
        assertTrue(host.getMaxAscent() >= fallbackMetrics.getMaxAscent());
    }

    @Test
    public void viewportChunksShouldKeepTheCompleteTokensResolvedFallbackFont() {
        Font primaryFont = new AsciiOnlyFont(Font.MONOSPACED, Font.PLAIN, 13);
        Font fallbackFont = new AllGlyphFont(Font.SERIF, Font.PLAIN, 13);
        FallbackAwareRSyntaxTextArea host = new FallbackAwareRSyntaxTextArea();
        host.setFont(primaryFont);
        host.putClientProperty(EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY, fallbackFont);
        Token token = token("a".repeat(600) + "汉");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 10_000, 40);

        float paintedEnd = new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.drawCharsCalls > 1);
        assertTrue(graphics.fontsAtDraw.stream().allMatch(fallbackFont::equals),
                "Every slice must inherit the font resolved from the complete source token");
        assertEquals(paintedEnd, token.getWidth(host, fixedTabExpander(), 0f), 0.01,
                "Chunked painting and unsliced token layout must end at the same x coordinate");
    }

    @Test
    public void viewportChunksShouldNotSplitEmojiZwjSequences() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        String emojiSequence = "\uD83D\uDC68\uFE0F\u200D\uD83D\uDC69";
        Token token = token("a".repeat(253) + emojiSequence + "b".repeat(300));
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 5_000, 40);

        new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(graphics.textsAtDraw.stream().anyMatch(text -> text.contains(emojiSequence)),
                "Viewport chunk boundaries must keep an entire ZWJ emoji sequence together");
    }

    @Test
    public void viewportPainterShouldSpecializeStandardPainter() {
        assertTrue(new ViewportClippedTokenPainter() instanceof StandardEditorTokenPainter);
    }

    private Token token(String text) {
        char[] chars = text.toCharArray();
        return new TokenImpl(chars, 0, chars.length - 1, 0, TokenTypes.IDENTIFIER, 0);
    }

    private RecordingGraphics2D graphicsWithClip(int x, int y, int width, int height) {
        BufferedImage image = new BufferedImage(Math.max(200, x + width + 10), 60, BufferedImage.TYPE_INT_ARGB);
        RecordingGraphics2D graphics = new RecordingGraphics2D(image.createGraphics());
        graphics.setClip(x, y, width, height);
        return graphics;
    }

    private TabExpander fixedTabExpander() {
        return (x, tabOffset) -> x + 40;
    }

    private static final class AsciiOnlyFont extends Font {
        private AsciiOnlyFont(String name, int style, int size) {
            super(name, style, size);
        }

        @Override
        public boolean canDisplay(int codePoint) {
            return codePoint >= 0 && codePoint < 128;
        }

        @Override
        public boolean canDisplay(char c) {
            return c < 128;
        }

        @Override
        public int canDisplayUpTo(char[] text, int start, int limit) {
            for (int i = start; i < limit; i++) {
                if (!canDisplay(text[i])) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static final class FormattingAwareAsciiFont extends Font {
        private FormattingAwareAsciiFont(String name, int style, int size) {
            super(name, style, size);
        }

        @Override
        public boolean canDisplay(int codePoint) {
            return codePoint >= 0 && codePoint < 128
                    || codePoint == 0x200D
                    || codePoint >= 0xFE00 && codePoint <= 0xFE0F;
        }

        @Override
        public boolean canDisplay(char c) {
            return canDisplay((int) c);
        }

        @Override
        public int canDisplayUpTo(char[] text, int start, int limit) {
            for (int i = start; i < limit; ) {
                int codePoint = Character.codePointAt(text, i, limit);
                if (!canDisplay(codePoint)) {
                    return i;
                }
                i += Character.charCount(codePoint);
            }
            return -1;
        }
    }

    private static final class AllGlyphFont extends Font {
        private AllGlyphFont(String name, int style, int size) {
            super(name, style, size);
        }

        @Override
        public boolean canDisplay(int codePoint) {
            return Character.isValidCodePoint(codePoint);
        }

        @Override
        public boolean canDisplay(char c) {
            return true;
        }

        @Override
        public int canDisplayUpTo(char[] text, int start, int limit) {
            return -1;
        }

        @Override
        public Font deriveFont(int style, float size) {
            return new AllGlyphFont(getName(), style, Math.round(size));
        }
    }

    private static class RecordingGraphics2D extends Graphics2D {
        private final Graphics2D delegate;
        private int drawCharsCalls;
        private int leftMostClipAtDraw = Integer.MAX_VALUE;
        private int longestDrawCharsLength;
        private int drawLineCalls;
        private boolean splitSurrogatePairAtDraw;
        private final List<Font> fontsAtDraw = new ArrayList<>();
        private final List<String> textsAtDraw = new ArrayList<>();

        private RecordingGraphics2D(Graphics2D delegate) {
            this.delegate = delegate;
        }

        @Override
        public void drawChars(char[] data, int offset, int length, int x, int y) {
            drawCharsCalls++;
            longestDrawCharsLength = Math.max(longestDrawCharsLength, length);
            if (length > 0) {
                splitSurrogatePairAtDraw |= Character.isLowSurrogate(data[offset])
                        || Character.isHighSurrogate(data[offset + length - 1]);
            }
            fontsAtDraw.add(delegate.getFont());
            textsAtDraw.add(new String(data, offset, length));
            Rectangle clip = delegate.getClipBounds();
            if (clip != null) {
                leftMostClipAtDraw = Math.min(leftMostClipAtDraw, clip.x);
            }
            delegate.drawChars(data, offset, length, x, y);
        }

        @Override
        public void draw(Shape s) {
            delegate.draw(s);
        }

        @Override
        public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
            return delegate.drawImage(img, xform, obs);
        }

        @Override
        public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
            delegate.drawImage(img, op, x, y);
        }

        @Override
        public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
            delegate.drawRenderedImage(img, xform);
        }

        @Override
        public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
            delegate.drawRenderableImage(img, xform);
        }

        @Override
        public void drawString(String str, int x, int y) {
            delegate.drawString(str, x, y);
        }

        @Override
        public void drawString(String str, float x, float y) {
            delegate.drawString(str, x, y);
        }

        @Override
        public void drawString(AttributedCharacterIterator iterator, int x, int y) {
            delegate.drawString(iterator, x, y);
        }

        @Override
        public void drawString(AttributedCharacterIterator iterator, float x, float y) {
            delegate.drawString(iterator, x, y);
        }

        @Override
        public void drawGlyphVector(GlyphVector g, float x, float y) {
            delegate.drawGlyphVector(g, x, y);
        }

        @Override
        public void fill(Shape s) {
            delegate.fill(s);
        }

        @Override
        public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
            return delegate.hit(rect, s, onStroke);
        }

        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return delegate.getDeviceConfiguration();
        }

        @Override
        public void setComposite(Composite comp) {
            delegate.setComposite(comp);
        }

        @Override
        public void setPaint(Paint paint) {
            delegate.setPaint(paint);
        }

        @Override
        public void setStroke(Stroke s) {
            delegate.setStroke(s);
        }

        @Override
        public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
            delegate.setRenderingHint(hintKey, hintValue);
        }

        @Override
        public Object getRenderingHint(RenderingHints.Key hintKey) {
            return delegate.getRenderingHint(hintKey);
        }

        @Override
        public void setRenderingHints(Map<?, ?> hints) {
            delegate.setRenderingHints(hints);
        }

        @Override
        public void addRenderingHints(Map<?, ?> hints) {
            delegate.addRenderingHints(hints);
        }

        @Override
        public RenderingHints getRenderingHints() {
            return delegate.getRenderingHints();
        }

        @Override
        public void translate(int x, int y) {
            delegate.translate(x, y);
        }

        @Override
        public void translate(double tx, double ty) {
            delegate.translate(tx, ty);
        }

        @Override
        public void rotate(double theta) {
            delegate.rotate(theta);
        }

        @Override
        public void rotate(double theta, double x, double y) {
            delegate.rotate(theta, x, y);
        }

        @Override
        public void scale(double sx, double sy) {
            delegate.scale(sx, sy);
        }

        @Override
        public void shear(double shx, double shy) {
            delegate.shear(shx, shy);
        }

        @Override
        public void transform(AffineTransform Tx) {
            delegate.transform(Tx);
        }

        @Override
        public void setTransform(AffineTransform Tx) {
            delegate.setTransform(Tx);
        }

        @Override
        public AffineTransform getTransform() {
            return delegate.getTransform();
        }

        @Override
        public Paint getPaint() {
            return delegate.getPaint();
        }

        @Override
        public Composite getComposite() {
            return delegate.getComposite();
        }

        @Override
        public void setBackground(Color color) {
            delegate.setBackground(color);
        }

        @Override
        public Color getBackground() {
            return delegate.getBackground();
        }

        @Override
        public Stroke getStroke() {
            return delegate.getStroke();
        }

        @Override
        public void clip(Shape s) {
            delegate.clip(s);
        }

        @Override
        public FontRenderContext getFontRenderContext() {
            return delegate.getFontRenderContext();
        }

        @Override
        public Graphics create() {
            return new RecordingGraphics2D((Graphics2D) delegate.create());
        }

        @Override
        public Color getColor() {
            return delegate.getColor();
        }

        @Override
        public void setColor(Color c) {
            delegate.setColor(c);
        }

        @Override
        public void setPaintMode() {
            delegate.setPaintMode();
        }

        @Override
        public void setXORMode(Color c1) {
            delegate.setXORMode(c1);
        }

        @Override
        public Font getFont() {
            return delegate.getFont();
        }

        @Override
        public void setFont(Font font) {
            delegate.setFont(font);
        }

        @Override
        public FontMetrics getFontMetrics(Font f) {
            return delegate.getFontMetrics(f);
        }

        @Override
        public Rectangle getClipBounds() {
            return delegate.getClipBounds();
        }

        @Override
        public void clipRect(int x, int y, int width, int height) {
            delegate.clipRect(x, y, width, height);
        }

        @Override
        public void setClip(int x, int y, int width, int height) {
            delegate.setClip(x, y, width, height);
        }

        @Override
        public Shape getClip() {
            return delegate.getClip();
        }

        @Override
        public void setClip(Shape clip) {
            delegate.setClip(clip);
        }

        @Override
        public void copyArea(int x, int y, int width, int height, int dx, int dy) {
            delegate.copyArea(x, y, width, height, dx, dy);
        }

        @Override
        public void drawLine(int x1, int y1, int x2, int y2) {
            drawLineCalls++;
            delegate.drawLine(x1, y1, x2, y2);
        }

        @Override
        public void fillRect(int x, int y, int width, int height) {
            delegate.fillRect(x, y, width, height);
        }

        @Override
        public void clearRect(int x, int y, int width, int height) {
            delegate.clearRect(x, y, width, height);
        }

        @Override
        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }

        @Override
        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }

        @Override
        public void drawOval(int x, int y, int width, int height) {
            delegate.drawOval(x, y, width, height);
        }

        @Override
        public void fillOval(int x, int y, int width, int height) {
            delegate.fillOval(x, y, width, height);
        }

        @Override
        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.drawArc(x, y, width, height, startAngle, arcAngle);
        }

        @Override
        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.fillArc(x, y, width, height, startAngle, arcAngle);
        }

        @Override
        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolyline(xPoints, yPoints, nPoints);
        }

        @Override
        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolygon(xPoints, yPoints, nPoints);
        }

        @Override
        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.fillPolygon(xPoints, yPoints, nPoints);
        }

        @Override
        public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
            return delegate.drawImage(img, x, y, observer);
        }

        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, observer);
        }

        @Override
        public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
            return delegate.drawImage(img, x, y, bgcolor, observer);
        }

        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                                 ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
        }

        @Override
        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                                 int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
            return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        }

        @Override
        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                                 int sx1, int sy1, int sx2, int sy2, Color bgcolor,
                                 ImageObserver observer) {
            return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }
    }
}
