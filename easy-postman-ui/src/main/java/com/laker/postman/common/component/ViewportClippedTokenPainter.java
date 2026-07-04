package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenPainter;

import javax.swing.text.TabExpander;
import java.awt.*;

/**
 * 针对响应体查看器的长 token 横向滚动优化。
 *
 * <p>RSyntaxTextArea 默认的裁剪优化停在 token 级别：如果一个 JSON 字符串 token 很长，
 * 横向拖动时仍可能整段测量和绘制这个 token。响应体里常见的模型输出、base64、压缩 JSON
 * 都容易形成这种长 token。</p>
 *
 * <p>这里不改变文档内容、不自动换行、不截断显示，只在绘制阶段把长 token 按小块处理：
 * 先测量每块的屏幕范围，跳过视口外的块，只绘制和当前 clip 相交的块。这样保留原有
 * RSyntaxTextArea 的 token 样式语义，同时降低水平滚动时每一帧的绘制成本。</p>
 */
public class ViewportClippedTokenPainter implements TokenPainter {

    // 普通 token 继续走直接绘制路径，避免给常规文本增加分块开销。
    private static final int LONG_TOKEN_THRESHOLD = 512;

    // 长 token 的绘制粒度。块越小，跳过视口外内容越精细；块越大，测量次数越少。
    private static final int LONG_TOKEN_CHUNK_SIZE = 256;

    @Override
    public float nextX(Token token, int charCount, float x, RSyntaxTextArea host, TabExpander e) {
        if (token == null || charCount <= 0) {
            return x;
        }

        Font primaryFont = host.getFontForToken(token);
        Font fallbackFont = resolveFallbackFont(host, primaryFont);
        FontMetrics fm = host.getFontMetricsForToken(token);
        FontMetrics fallbackFm = fallbackFont == null ? null : host.getFontMetrics(fallbackFont);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = Math.min(start + charCount, start + token.length());
        return measureRange(text, start, end, primaryFont, fm, fallbackFont, fallbackFm, e, x);
    }

    @Override
    public float paint(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host, TabExpander e) {
        return paint(token, g, x, y, host, e, 0);
    }

    @Override
    public float paint(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                       TabExpander e, float clipStart) {
        return paintImpl(token, g, x, y, host, e, clipStart, true, false);
    }

    @Override
    public float paint(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                       TabExpander e, float clipStart, boolean paintBG) {
        return paintImpl(token, g, x, y, host, e, clipStart, paintBG, false);
    }

    @Override
    public float paintSelected(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                               TabExpander e, boolean useSTC) {
        return paintSelected(token, g, x, y, host, e, 0, useSTC);
    }

    @Override
    public float paintSelected(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                               TabExpander e, float clipStart, boolean useSTC) {
        // 选中态也保持分块绘制。这里优先保证长 token 横向拖动性能；
        // 不再回退到 RSTA 默认 painter，避免选区拖动时整段长 token 绘制导致卡顿。
        return paintImpl(token, g, x, y, host, e, clipStart, false, useSTC);
    }

    private float paintImpl(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                            TabExpander e, float clipStart, boolean paintTokenBackground,
                            boolean useSelectedTextColor) {
        if (token == null || !token.isPaintable()) {
            return x;
        }

        Font primaryFont = host.getFontForToken(token);
        Font fallbackFont = resolveFallbackFont(host, primaryFont);
        FontMetrics fm = host.getFontMetricsForToken(token);
        FontMetrics fallbackFm = fallbackFont == null ? null : host.getFontMetrics(fallbackFont);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = start + token.length();
        Color fg = useSelectedTextColor ? host.getSelectedTextColor() : host.getForegroundForToken(token);
        Color bg = paintTokenBackground ? host.getBackgroundForToken(token) : null;
        boolean underline = host.getUnderlineForToken(token);

        g.setFont(primaryFont);
        g.setColor(fg);

        ClipRange clipRange = resolveClipRange(g, clipStart);
        Shape originalClip = null;
        if (clipRange.narrowsActualClip()) {
            originalClip = g.getClip();
            constrainActualClip(g, clipRange);
        }
        try {
            if (token.length() > LONG_TOKEN_THRESHOLD) {
                return paintLongToken(text, start, end, g, x, y, primaryFont, fm, fallbackFont, fallbackFm,
                        fg, bg, underline, host, e, clipRange);
            }

            return paintRange(text, start, end, g, x, y, primaryFont, fm, fallbackFont, fallbackFm,
                    fg, bg, underline, host, e, clipRange);
        } finally {
            if (originalClip != null) {
                g.setClip(originalClip);
            }
        }
    }

    private float paintLongToken(char[] text, int start, int end, Graphics2D g, float x, float y,
                                 Font primaryFont, FontMetrics fm, Font fallbackFont, FontMetrics fallbackFm,
                                 Color fg, Color bg, boolean underline,
                                 RSyntaxTextArea host, TabExpander e, ClipRange clipRange) {
        float currentX = x;
        for (int chunkStart = start; chunkStart < end; chunkStart += LONG_TOKEN_CHUNK_SIZE) {
            int chunkEnd = Math.min(chunkStart + LONG_TOKEN_CHUNK_SIZE, end);
            float nextX = measureRange(text, chunkStart, chunkEnd, primaryFont, fm, fallbackFont, fallbackFm,
                    e, currentX);

            // 后续块已经在视口右侧之外，后面只需要测量剩余宽度并返回 token 结束坐标。
            // 调用方依赖返回值继续绘制下一个 token，因此不能直接提前返回 currentX。
            if (currentX > clipRange.right) {
                return measureRange(text, chunkStart, end, primaryFont, fm, fallbackFont, fallbackFm, e, currentX);
            }

            // 只绘制和当前 clip 相交的块；完全在左侧或右侧之外的块不调用 drawChars。
            if (isRangeVisible(currentX, nextX, clipRange)) {
                paintRange(text, chunkStart, chunkEnd, g, currentX, y, primaryFont, fm, fallbackFont, fallbackFm,
                        fg, bg, underline, host, e, clipRange);
            }

            currentX = nextX;
        }
        return currentX;
    }

    private float paintRange(char[] text, int start, int end, Graphics2D g, float x, float y,
                             Font primaryFont, FontMetrics fm, Font fallbackFont, FontMetrics fallbackFm,
                             Color fg, Color bg, boolean underline,
                             RSyntaxTextArea host, TabExpander e, ClipRange clipRange) {
        float currentX = x;
        int flushStart = start;
        int flushLen = 0;

        for (int i = start; i < end; i++) {
            if (text[i] == '\t') {
                float flushEndX = currentX + measureDisplaySegment(text, flushStart, flushStart + flushLen,
                        primaryFont, fm, fallbackFont, fallbackFm);
                float tabEndX = e.nextTabStop(flushEndX, 0);
                paintSegment(text, flushStart, flushLen, g, currentX, flushEndX, y,
                        primaryFont, fm, fallbackFont, fallbackFm,
                        fg, bg, underline, host, clipRange);
                if (isRangeVisible(flushEndX, tabEndX, clipRange)) {
                    if (bg != null) {
                        paintBackground(g, flushEndX, tabEndX, y, fm, host, bg);
                    }
                    if (underline) {
                        paintUnderline(g, flushEndX, tabEndX, y, fg);
                    }
                }
                currentX = tabEndX;
                flushStart = i + 1;
                flushLen = 0;
            } else {
                flushLen++;
            }
        }

        float nextX = currentX + measureDisplaySegment(text, flushStart, flushStart + flushLen,
                primaryFont, fm, fallbackFont, fallbackFm);
        paintSegment(text, flushStart, flushLen, g, currentX, nextX, y,
                primaryFont, fm, fallbackFont, fallbackFm,
                fg, bg, underline, host, clipRange);
        return nextX;
    }

    private void paintSegment(char[] text, int start, int length, Graphics2D g,
                              float x, float nextX, float y,
                              Font primaryFont, FontMetrics fm, Font fallbackFont, FontMetrics fallbackFm,
                              Color fg, Color bg, boolean underline, RSyntaxTextArea host,
                              ClipRange clipRange) {
        // 短 token 也必须做 clip 判断。压缩 JSON 往往由大量短 token 组成；
        // 横向滚动到很右侧时，如果这些左侧 token 仍调用 drawChars，会比默认 painter 更慢。
        if (!isRangeVisible(x, nextX, clipRange)) {
            return;
        }
        if (bg != null) {
            paintBackground(g, x, nextX, y, fm, host, bg);
        }
        if (length > 0) {
            g.setColor(fg);
            paintTextRuns(text, start, start + length, g, x, y, primaryFont, fm, fallbackFont, fallbackFm);
        }
        if (underline) {
            paintUnderline(g, x, nextX, y, fg);
        }
    }

    private void paintBackground(Graphics2D g, float x, float nextX, float y,
                                 FontMetrics fm, RSyntaxTextArea host, Color bg) {
        g.setColor(bg);
        g.fillRect((int) x, (int) (y - fm.getAscent()),
                Math.max(0, (int) (nextX - x)), host.getLineHeight());
    }

    private void paintUnderline(Graphics2D g, float x, float nextX, float y, Color fg) {
        g.setColor(fg);
        int underlineY = (int) y + 1;
        g.drawLine((int) x, underlineY, (int) nextX, underlineY);
    }

    private float measureRange(char[] text, int start, int end,
                               Font primaryFont, FontMetrics fm, Font fallbackFont, FontMetrics fallbackFm,
                               TabExpander e, float x) {
        float currentX = x;
        int flushStart = start;
        int flushLen = 0;

        // 与 RSyntaxTextArea 默认 painter 的宽度计算保持一致：普通字符批量 charsWidth，
        // tab 交给 TabExpander 处理，保证滚动条宽度、caret 定位和后续 token 起点不漂移。
        for (int i = start; i < end; i++) {
            if (text[i] == '\t') {
                currentX += measureDisplaySegment(text, flushStart, flushStart + flushLen,
                        primaryFont, fm, fallbackFont, fallbackFm);
                currentX = e.nextTabStop(currentX, 0);
                flushStart = i + 1;
                flushLen = 0;
            } else {
                flushLen++;
            }
        }

        return currentX + measureDisplaySegment(text, flushStart, flushStart + flushLen,
                primaryFont, fm, fallbackFont, fallbackFm);
    }

    private float measureDisplaySegment(char[] text, int start, int end,
                                        Font primaryFont, FontMetrics primaryFm,
                                        Font fallbackFont, FontMetrics fallbackFm) {
        if (start >= end) {
            return 0f;
        }
        if (fallbackFont == null || fallbackFm == null) {
            return primaryFm.charsWidth(text, start, end - start);
        }

        float width = 0f;
        int runStart = start;
        FontMetrics runMetrics = selectMetrics(text, start, end, primaryFont, primaryFm, fallbackFont, fallbackFm);
        for (int i = start; i < end; ) {
            int charCount = codePointCharCount(text, i, end);
            FontMetrics metrics = selectMetrics(text, i, end, primaryFont, primaryFm, fallbackFont, fallbackFm);
            if (metrics != runMetrics) {
                width += runMetrics.charsWidth(text, runStart, i - runStart);
                runStart = i;
                runMetrics = metrics;
            }
            i += charCount;
        }
        return width + runMetrics.charsWidth(text, runStart, end - runStart);
    }

    private void paintTextRuns(char[] text, int start, int end, Graphics2D g, float x, float y,
                               Font primaryFont, FontMetrics primaryFm,
                               Font fallbackFont, FontMetrics fallbackFm) {
        if (start >= end) {
            return;
        }
        if (fallbackFont == null || fallbackFm == null) {
            g.setFont(primaryFont);
            g.drawChars(text, start, end - start, (int) x, (int) y);
            return;
        }

        float currentX = x;
        int runStart = start;
        Font runFont = selectFont(text, start, end, primaryFont, fallbackFont);
        FontMetrics runMetrics = runFont == fallbackFont ? fallbackFm : primaryFm;
        for (int i = start; i < end; ) {
            int charCount = codePointCharCount(text, i, end);
            Font font = selectFont(text, i, end, primaryFont, fallbackFont);
            if (font != runFont) {
                g.setFont(runFont);
                g.drawChars(text, runStart, i - runStart, (int) currentX, (int) y);
                currentX += runMetrics.charsWidth(text, runStart, i - runStart);
                runStart = i;
                runFont = font;
                runMetrics = runFont == fallbackFont ? fallbackFm : primaryFm;
            }
            i += charCount;
        }
        g.setFont(runFont);
        g.drawChars(text, runStart, end - runStart, (int) currentX, (int) y);
        g.setFont(primaryFont);
    }

    private FontMetrics selectMetrics(char[] text, int index, int end,
                                      Font primaryFont, FontMetrics primaryFm,
                                      Font fallbackFont, FontMetrics fallbackFm) {
        Font font = selectFont(text, index, end, primaryFont, fallbackFont);
        return font == fallbackFont ? fallbackFm : primaryFm;
    }

    private Font selectFont(char[] text, int index, int end, Font primaryFont, Font fallbackFont) {
        if (fallbackFont == null || canDisplay(primaryFont, text, index, end)) {
            return primaryFont;
        }
        if (canDisplay(fallbackFont, text, index, end)) {
            return fallbackFont;
        }
        return primaryFont;
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

    private int codePointCharCount(char[] text, int index, int end) {
        if (index + 1 < end && Character.isHighSurrogate(text[index]) && Character.isLowSurrogate(text[index + 1])) {
            return 2;
        }
        return 1;
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

    private ClipRange resolveClipRange(Graphics2D g, float clipStart) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            return new ClipRange(clipStart, Float.POSITIVE_INFINITY, null);
        }

        float left = Math.max(clip.x, clipStart);
        float right = clip.x + clip.width;
        return new ClipRange(left, right, clip);
    }

    private void constrainActualClip(Graphics2D g, ClipRange clipRange) {
        Rectangle clip = clipRange.clipBounds;
        int left = (int) Math.floor(clipRange.left);
        int right = (int) Math.ceil(clipRange.right);
        g.clipRect(left, clip.y, Math.max(0, right - left), clip.height);
    }

    private boolean isRangeVisible(float x, float nextX, ClipRange clipRange) {
        return nextX >= clipRange.left && x <= clipRange.right;
    }

    private static class ClipRange {
        private final float left;
        private final float right;
        private final Rectangle clipBounds;

        private ClipRange(float left, float right, Rectangle clipBounds) {
            this.left = left;
            this.right = right;
            this.clipBounds = clipBounds;
        }

        private boolean narrowsActualClip() {
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
