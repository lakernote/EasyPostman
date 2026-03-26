package com.laker.postman.common.animation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 窗口级快照淡出过渡。
 * 先对旧内容截图，再把截图盖在新内容之上，通过 glass pane 做短暂淡出。
 * 适用于启动壳切换、局部大面板替换等“旧画面 -> 新画面”的平滑过渡。
 */
public final class WindowSnapshotTransition {

    public static final int DEFAULT_DURATION_MS = 120;
    private static final int DEFAULT_FRAME_DELAY_MS = 16;

    private final RootPaneContainer container;
    private Timer timer;
    private Component previousGlassPane;

    public WindowSnapshotTransition(RootPaneContainer container) {
        this.container = container;
    }

    public void start(JComponent snapshotSource) {
        start(snapshotSource, DEFAULT_DURATION_MS, Easing.EASE_OUT_CUBIC);
    }

    public void start(JComponent snapshotSource, int durationMs) {
        start(snapshotSource, durationMs, Easing.EASE_OUT_CUBIC);
    }

    public void start(JComponent snapshotSource, int durationMs, Easing easing) {
        if (container == null || snapshotSource == null || durationMs <= 0) {
            return;
        }
        BufferedImage snapshot = capture(snapshotSource);
        if (snapshot == null) {
            return;
        }
        stop();

        previousGlassPane = container.getGlassPane();
        SnapshotGlassPane transitionPane = new SnapshotGlassPane(snapshotSource, snapshot, durationMs,
                easing == null ? Easing.EASE_OUT_CUBIC : easing);
        container.setGlassPane(transitionPane);
        transitionPane.setVisible(true);

        timer = new Timer(DEFAULT_FRAME_DELAY_MS, e -> {
            transitionPane.advance();
            if (transitionPane.isFinished()) {
                stop();
                return;
            }
            transitionPane.repaint();
        });
        timer.start();
    }

    public void stop() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        timer = null;
        if (container != null && previousGlassPane != null) {
            previousGlassPane.setVisible(false);
            container.setGlassPane(previousGlassPane);
            previousGlassPane = null;
        }
    }

    private BufferedImage capture(JComponent source) {
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
        return snapshot;
    }

    public enum Easing {
        LINEAR,
        EASE_OUT_CUBIC
    }

    private static final class SnapshotGlassPane extends JComponent {
        private final JComponent snapshotSource;
        private final BufferedImage snapshot;
        private final Easing easing;
        private final long startNanos;
        private final long durationNanos;
        private float alpha = 1f;

        private SnapshotGlassPane(JComponent snapshotSource, BufferedImage snapshot, int durationMs, Easing easing) {
            this.snapshotSource = snapshotSource;
            this.snapshot = snapshot;
            this.easing = easing;
            this.durationNanos = durationMs * 1_000_000L;
            this.startNanos = System.nanoTime();
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
                Point origin = SwingUtilities.convertPoint(snapshotSource, 0, 0, this);
                g2.drawImage(snapshot, origin.x, origin.y, null);
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
            if (easing == Easing.LINEAR) {
                return progress;
            }
            return easeOutCubic(progress);
        }

        private static float easeOutCubic(float t) {
            float inv = 1f - t;
            return 1f - inv * inv * inv;
        }
    }
}
