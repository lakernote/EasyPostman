package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.testng.annotations.Test;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ViewportClippedTokenPainterTest {

    @Test
    public void shouldSkipShortTokenThatEndsBeforeViewport() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("abc");
        RecordingGraphics2D graphics = graphicsWithClip(100, 0, 60, 40);

        float nextX = new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 100f, true);

        assertTrue(nextX > 0f);
        assertEquals(graphics.drawCharsCalls, 0);
    }

    @Test
    public void shouldPaintShortTokenThatIntersectsViewport() {
        RSyntaxTextArea host = new RSyntaxTextArea();
        Token token = token("abc");
        RecordingGraphics2D graphics = graphicsWithClip(0, 0, 60, 40);

        float nextX = new ViewportClippedTokenPainter()
                .paint(token, graphics, 0f, 20f, host, fixedTabExpander(), 0f, true);

        assertTrue(nextX > 0f);
        assertEquals(graphics.drawCharsCalls, 1);
    }

    private Token token(String text) {
        char[] chars = text.toCharArray();
        return new TokenImpl(chars, 0, chars.length - 1, 0, TokenTypes.IDENTIFIER, 0);
    }

    private RecordingGraphics2D graphicsWithClip(int x, int y, int width, int height) {
        BufferedImage image = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
        RecordingGraphics2D graphics = new RecordingGraphics2D(image.createGraphics());
        graphics.setClip(x, y, width, height);
        return graphics;
    }

    private TabExpander fixedTabExpander() {
        return (x, tabOffset) -> x + 40;
    }

    private static class RecordingGraphics2D extends Graphics2D {
        private final Graphics2D delegate;
        private int drawCharsCalls;

        private RecordingGraphics2D(Graphics2D delegate) {
            this.delegate = delegate;
        }

        @Override
        public void drawChars(char[] data, int offset, int length, int x, int y) {
            drawCharsCalls++;
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
