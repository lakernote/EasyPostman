package com.laker.postman.common.animation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 窗口级快照淡出过渡。
 * 先对旧内容截图，再把截图挂到 layeredPane 上做短暂淡出。
 * 这里刻意不用 glassPane，避免覆盖层参与鼠标命中，影响底层组件的 hover / cursor / drag。
 */
public final class WindowSnapshotTransition {

    public static final int DEFAULT_DURATION_MS = 150;
    public static final Easing DEFAULT_EASING = Easing.EASE_IN_OUT_CUBIC;
    private static final int DEFAULT_FRAME_DELAY_MS = 16;

    private final RootPaneContainer container;
    private Timer timer;
    private SnapshotOverlay overlay;

    public WindowSnapshotTransition(RootPaneContainer container) {
        this.container = container;
    }

    public void start(JComponent snapshotSource) {
        start(snapshotSource, DEFAULT_DURATION_MS, DEFAULT_EASING);
    }

    public void start(JComponent snapshotSource, int durationMs) {
        start(snapshotSource, durationMs, DEFAULT_EASING);
    }

    public void start(JComponent snapshotSource, int durationMs, Easing easing) {
        if (container == null || snapshotSource == null || durationMs <= 0) {
            return;
        }
        CapturedSnapshot capturedSnapshot = capture(snapshotSource);
        if (capturedSnapshot == null) {
            return;
        }
        start(capturedSnapshot, durationMs, easing);
    }

    public void start(CapturedSnapshot capturedSnapshot, int durationMs, Easing easing) {
        if (container == null || capturedSnapshot == null || capturedSnapshot.image == null || durationMs <= 0) {
            return;
        }
        JLayeredPane layeredPane = container.getLayeredPane();
        if (layeredPane == null) {
            return;
        }
        stop();

        overlay = new SnapshotOverlay(capturedSnapshot.image, capturedSnapshot.bounds, durationMs,
                easing == null ? DEFAULT_EASING : easing);
        layeredPane.add(overlay, Integer.valueOf(JLayeredPane.DRAG_LAYER + 1));
        overlay.setVisible(true);
        overlay.repaint();

        timer = new Timer(DEFAULT_FRAME_DELAY_MS, e -> {
            if (overlay == null) {
                stop();
                return;
            }
            overlay.advance();
            if (overlay.isFinished()) {
                stop();
                return;
            }
            overlay.repaint();
        });
        timer.start();
    }

    public void stop() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        timer = null;
        if (overlay != null) {
            Container parent = overlay.getParent();
            if (parent != null) {
                parent.remove(overlay);
                parent.repaint(overlay.getX(), overlay.getY(), overlay.getWidth(), overlay.getHeight());
            }
            overlay = null;
        }
    }

    public CapturedSnapshot captureSnapshot(JComponent source) {
        return capture(source);
    }

    public void start(CapturedSnapshot capturedSnapshot) {
        start(capturedSnapshot, DEFAULT_DURATION_MS, DEFAULT_EASING);
    }

    private CapturedSnapshot capture(JComponent source) {
        JLayeredPane layeredPane = container != null ? container.getLayeredPane() : null;
        if (layeredPane == null) {
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        BufferedImage snapshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = snapshot.createGraphics();
        try {
            source.printAll(g2);
        } finally {
            g2.dispose();
        }
        Rectangle bounds = resolveBounds(source, layeredPane);
        return new CapturedSnapshot(snapshot, bounds);
    }

    private Rectangle resolveBounds(JComponent source, JLayeredPane layeredPane) {
        Container parent = source.getParent();
        if (parent == null) {
            return new Rectangle(0, 0, source.getWidth(), source.getHeight());
        }
        try {
            return SwingUtilities.convertRectangle(parent, source.getBounds(), layeredPane);
        } catch (IllegalComponentStateException ex) {
            return new Rectangle(0, 0, source.getWidth(), source.getHeight());
        }
    }

    public enum Easing {
        LINEAR,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC
    }

    public static final class CapturedSnapshot {
        private final BufferedImage image;
        private final Rectangle bounds;

        private CapturedSnapshot(BufferedImage image, Rectangle bounds) {
            this.image = image;
            this.bounds = bounds != null ? new Rectangle(bounds) : new Rectangle();
        }
    }

    private static final class SnapshotOverlay extends JComponent {
        private final BufferedImage snapshot;
        private final Easing easing;
        private final long startNanos;
        private final long durationNanos;
        private float alpha = 1f;

        private SnapshotOverlay(BufferedImage snapshot, Rectangle bounds, int durationMs, Easing easing) {
            this.snapshot = snapshot;
            this.easing = easing;
            this.durationNanos = durationMs * 1_000_000L;
            this.startNanos = System.nanoTime();
            Rectangle safeBounds = bounds != null ? bounds : new Rectangle();
            setBounds(safeBounds);
            setOpaque(false);
            setFocusable(false);
            setEnabled(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (snapshot == null || alpha <= 0f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.drawImage(snapshot, 0, 0, null);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public boolean contains(int x, int y) {
            // 过渡层只负责“画旧图”，不允许成为鼠标命中目标。
            // 否则底层 JSplitPane、Tab 拖拽等交互会拿不到正常的 hover / cursor 事件。
            return false;
        }

        private void advance() {
            long elapsed = System.nanoTime() - startNanos;
            float progress = Math.min(1f, (float) elapsed / durationNanos);
            float eased = applyEasing(progress);
            alpha = 1f - eased;
        }

        private boolean isFinished() {
            return alpha <= 0f;
        }

        private float applyEasing(float progress) {
            if (easing == Easing.LINEAR) {
                return progress;
            }
            if (easing == Easing.EASE_IN_OUT_CUBIC) {
                return easeInOutCubic(progress);
            }
            return easeOutCubic(progress);
        }

        private static float easeOutCubic(float t) {
            float inv = 1f - t;
            return 1f - inv * inv * inv;
        }

        private static float easeInOutCubic(float t) {
            if (t < 0.5f) {
                return 4f * t * t * t;
            }
            float value = -2f * t + 2f;
            return 1f - (value * value * value) / 2f;
        }
    }
}
