package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.DefaultTokenPainterFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenPainter;
import org.fife.ui.rsyntaxtextarea.TokenPainterFactory;

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
 *
 * <p>注意：选区绘制不能走这里的长 token 分块逻辑。RSyntaxTextArea 的
 * {@code SyntaxView.drawTokenWithSelection} 会对同一个 token 多次设置不同 clip，
 * 分别绘制选中段、未选中前缀和未选中后缀；这些 clip 边界同时决定用户看到的选区和
 * 复制时使用的 model offset。自定义 painter 如果再叠加分块裁剪，在多屏缩放或
 * HiDPI 场景下更容易让视觉选区和真实 selection range 产生偏差。</p>
 */
public class ViewportClippedTokenPainter implements TokenPainter {

    // 使用 RSTA 自带工厂而不是直接 new DefaultTokenPainter，保留“显示空白字符”模式下的默认行为。
    private static final TokenPainterFactory DEFAULT_TOKEN_PAINTER_FACTORY = new DefaultTokenPainterFactory();

    // 普通 token 继续走直接绘制路径，避免给常规文本增加分块开销。
    private static final int LONG_TOKEN_THRESHOLD = 512;

    // 长 token 的绘制粒度。块越小，跳过视口外内容越精细；块越大，测量次数越少。
    private static final int LONG_TOKEN_CHUNK_SIZE = 256;

    @Override
    public float nextX(Token token, int charCount, float x, RSyntaxTextArea host, TabExpander e) {
        if (token == null || charCount <= 0) {
            return x;
        }

        FontMetrics fm = host.getFontMetricsForToken(token);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = Math.min(start + charCount, start + token.length());
        return measureRange(text, start, end, fm, e, x);
    }

    @Override
    public float paint(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host, TabExpander e) {
        // 选区相交时交回 RSTA 默认 painter，保持 selection clip、modelToView 和复制范围一致。
        if (selectionIntersectsToken(token, host)) {
            return defaultTokenPainter(host).paint(token, g, x, y, host, e);
        }
        return paint(token, g, x, y, host, e, 0);
    }

    @Override
    public float paint(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                       TabExpander e, float clipStart) {
        // 不能只在 paintSelected() 里兜底：RSTA 还会用普通 paint() 绘制选中 token 的未选中片段。
        if (selectionIntersectsToken(token, host)) {
            return defaultTokenPainter(host).paint(token, g, x, y, host, e, clipStart);
        }
        return paintImpl(token, g, x, y, host, e, clipStart, true, false);
    }

    @Override
    public float paint(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                       TabExpander e, float clipStart, boolean paintBG) {
        // 同上，选区相关 token 必须完整遵循 RSTA 默认 painter 的坐标和 clip 语义。
        if (selectionIntersectsToken(token, host)) {
            return defaultTokenPainter(host).paint(token, g, x, y, host, e, clipStart, paintBG);
        }
        return paintImpl(token, g, x, y, host, e, clipStart, paintBG, false);
    }

    @Override
    public float paintSelected(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                               TabExpander e, boolean useSTC) {
        // 选中段由 RSTA 默认 painter 绘制，避免自定义分块和 RSTA 选区分段重复裁剪。
        return defaultTokenPainter(host).paintSelected(token, g, x, y, host, e, useSTC);
    }

    @Override
    public float paintSelected(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                               TabExpander e, float clipStart, boolean useSTC) {
        return defaultTokenPainter(host).paintSelected(token, g, x, y, host, e, clipStart, useSTC);
    }

    private TokenPainter defaultTokenPainter(RSyntaxTextArea host) {
        return DEFAULT_TOKEN_PAINTER_FACTORY.getTokenPainter(host);
    }

    private boolean selectionIntersectsToken(Token token, RSyntaxTextArea host) {
        if (token == null || host == null) {
            return false;
        }
        int selectionStart = host.getSelectionStart();
        int selectionEnd = host.getSelectionEnd();
        return selectionStart < selectionEnd
                && selectionStart < token.getEndOffset()
                && selectionEnd > token.getOffset();
    }

    private float paintImpl(Token token, Graphics2D g, float x, float y, RSyntaxTextArea host,
                            TabExpander e, float clipStart, boolean paintTokenBackground,
                            boolean useSelectedTextColor) {
        if (token == null || !token.isPaintable()) {
            return x;
        }

        FontMetrics fm = host.getFontMetricsForToken(token);
        char[] text = token.getTextArray();
        int start = token.getTextOffset();
        int end = start + token.length();
        Color fg = useSelectedTextColor ? host.getSelectedTextColor() : host.getForegroundForToken(token);
        Color bg = paintTokenBackground ? host.getBackgroundForToken(token) : null;
        boolean underline = host.getUnderlineForToken(token);

        g.setFont(host.getFontForToken(token));
        g.setColor(fg);

        ClipRange clipRange = resolveClipRange(g, clipStart);
        Shape originalClip = null;
        if (clipRange.narrowsActualClip()) {
            originalClip = g.getClip();
            constrainActualClip(g, clipRange);
        }
        try {
            if (token.length() > LONG_TOKEN_THRESHOLD) {
                return paintLongToken(text, start, end, g, x, y, fm, fg, bg, underline, host, e, clipRange);
            }

            return paintRange(text, start, end, g, x, y, fm, fg, bg, underline, host, e, clipRange);
        } finally {
            if (originalClip != null) {
                g.setClip(originalClip);
            }
        }
    }

    private float paintLongToken(char[] text, int start, int end, Graphics2D g, float x, float y,
                                 FontMetrics fm, Color fg, Color bg, boolean underline,
                                 RSyntaxTextArea host, TabExpander e, ClipRange clipRange) {
        float currentX = x;
        for (int chunkStart = start; chunkStart < end; chunkStart += LONG_TOKEN_CHUNK_SIZE) {
            int chunkEnd = Math.min(chunkStart + LONG_TOKEN_CHUNK_SIZE, end);
            float nextX = measureRange(text, chunkStart, chunkEnd, fm, e, currentX);

            // 后续块已经在视口右侧之外，后面只需要测量剩余宽度并返回 token 结束坐标。
            // 调用方依赖返回值继续绘制下一个 token，因此不能直接提前返回 currentX。
            if (currentX > clipRange.right) {
                return measureRange(text, chunkStart, end, fm, e, currentX);
            }

            // 只绘制和当前 clip 相交的块；完全在左侧或右侧之外的块不调用 drawChars。
            if (isRangeVisible(currentX, nextX, clipRange)) {
                paintRange(text, chunkStart, chunkEnd, g, currentX, y, fm, fg, bg, underline, host, e, clipRange);
            }

            currentX = nextX;
        }
        return currentX;
    }

    private float paintRange(char[] text, int start, int end, Graphics2D g, float x, float y,
                             FontMetrics fm, Color fg, Color bg, boolean underline,
                             RSyntaxTextArea host, TabExpander e, ClipRange clipRange) {
        float currentX = x;
        int flushStart = start;
        int flushLen = 0;

        for (int i = start; i < end; i++) {
            if (text[i] == '\t') {
                float flushEndX = currentX + fm.charsWidth(text, flushStart, flushLen);
                float tabEndX = e.nextTabStop(flushEndX, 0);
                paintSegment(text, flushStart, flushLen, g, currentX, flushEndX, y, fm,
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

        float nextX = currentX + fm.charsWidth(text, flushStart, flushLen);
        paintSegment(text, flushStart, flushLen, g, currentX, nextX, y, fm,
                fg, bg, underline, host, clipRange);
        return nextX;
    }

    private void paintSegment(char[] text, int start, int length, Graphics2D g,
                              float x, float nextX, float y, FontMetrics fm,
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
            g.drawChars(text, start, length, (int) x, (int) y);
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

    private float measureRange(char[] text, int start, int end, FontMetrics fm, TabExpander e, float x) {
        float currentX = x;
        int flushStart = start;
        int flushLen = 0;

        // 与 RSyntaxTextArea 默认 painter 的宽度计算保持一致：普通字符批量 charsWidth，
        // tab 交给 TabExpander 处理，保证滚动条宽度、caret 定位和后续 token 起点不漂移。
        for (int i = start; i < end; i++) {
            if (text[i] == '\t') {
                currentX += fm.charsWidth(text, flushStart, flushLen);
                currentX = e.nextTabStop(currentX, 0);
                flushStart = i + 1;
                flushLen = 0;
            } else {
                flushLen++;
            }
        }

        return currentX + fm.charsWidth(text, flushStart, flushLen);
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
