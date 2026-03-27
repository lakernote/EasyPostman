package com.laker.postman.common.animation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 组件级快照淡出过渡。
 * 在根窗口的 layeredPane 上仅覆盖目标组件所在区域，
 * 适合 request 编辑器这类局部骨架 -> 真实内容的平滑切换。
 */
public final class ComponentSnapshotTransition {

    public static final int DEFAULT_DURATION_MS = 120;
    public static final WindowSnapshotTransition.Easing DEFAULT_EASING = WindowSnapshotTransition.Easing.EASE_OUT_CUBIC;
    private static final int DEFAULT_FRAME_DELAY_MS = 16;

    private final JComponent owner;
    private Timer timer;
    private SnapshotOverlay overlay;

    public ComponentSnapshotTransition(JComponent owner) {
        this.owner = owner;
    }

    public CapturedSnapshot captureSnapshot(JComponent source) {
        if (owner == null || source == null) {
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        JRootPane rootPane = SwingUtilities.getRootPane(owner);
        if (rootPane == null) {
            return null;
        }
        JLayeredPane layeredPane = rootPane.getLayeredPane();
        if (layeredPane == null) {
            return null;
        }
        BufferedImage snapshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = snapshot.createGraphics();
        try {
            source.printAll(g2);
        } finally {
            g2.dispose();
        }
        Rectangle bounds = SwingUtilities.convertRectangle(source.getParent(), source.getBounds(), layeredPane);
        return new CapturedSnapshot(snapshot, bounds);
    }

    public void start(CapturedSnapshot capturedSnapshot) {
        start(capturedSnapshot, DEFAULT_DURATION_MS, DEFAULT_EASING);
    }

    public void start(CapturedSnapshot capturedSnapshot, int durationMs, WindowSnapshotTransition.Easing easing) {
        if (owner == null || capturedSnapshot == null || capturedSnapshot.image == null || durationMs <= 0) {
            return;
        }
        JRootPane rootPane = SwingUtilities.getRootPane(owner);
        if (rootPane == null || rootPane.getLayeredPane() == null) {
            return;
        }
        stop();

        JLayeredPane layeredPane = rootPane.getLayeredPane();
        overlay = new SnapshotOverlay(capturedSnapshot.image, capturedSnapshot.bounds, durationMs,
                easing == null ? DEFAULT_EASING : easing);
        layeredPane.add(overlay, JLayeredPane.DRAG_LAYER);
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
        private final WindowSnapshotTransition.Easing easing;
        private final long startNanos;
        private final long durationNanos;
        private float alpha = 1f;

        private SnapshotOverlay(BufferedImage snapshot, Rectangle bounds, int durationMs,
                                WindowSnapshotTransition.Easing easing) {
            this.snapshot = snapshot;
            this.easing = easing;
            this.durationNanos = durationMs * 1_000_000L;
            this.startNanos = System.nanoTime();
            Rectangle safeBounds = bounds != null ? bounds : new Rectangle();
            setBounds(safeBounds);
            setOpaque(false);
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
            if (easing == WindowSnapshotTransition.Easing.LINEAR) {
                return progress;
            }
            if (easing == WindowSnapshotTransition.Easing.EASE_IN_OUT_CUBIC) {
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
