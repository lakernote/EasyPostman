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

    public static final int DEFAULT_DURATION_MS = 150;
    public static final Easing DEFAULT_EASING = Easing.EASE_IN_OUT_CUBIC;
    private static final int DEFAULT_FRAME_DELAY_MS = 16;

    private final RootPaneContainer container;
    private Timer timer;
    private Component previousGlassPane;

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
        stop();

        previousGlassPane = container.getGlassPane();
        SnapshotGlassPane transitionPane = new SnapshotGlassPane(capturedSnapshot.origin, capturedSnapshot.image, durationMs,
                easing == null ? DEFAULT_EASING : easing);
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

    public CapturedSnapshot captureSnapshot(JComponent source) {
        return capture(source);
    }

    public void start(CapturedSnapshot capturedSnapshot) {
        start(capturedSnapshot, DEFAULT_DURATION_MS, DEFAULT_EASING);
    }

    private CapturedSnapshot capture(JComponent source) {
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
        Point origin = resolveOrigin(source);
        return new CapturedSnapshot(snapshot, origin);
    }

    private Point resolveOrigin(JComponent source) {
        if (!(container instanceof Component containerComponent)) {
            return new Point(0, 0);
        }
        try {
            Point containerOnScreen = containerComponent.getLocationOnScreen();
            Point sourceOnScreen = source.getLocationOnScreen();
            return new Point(sourceOnScreen.x - containerOnScreen.x, sourceOnScreen.y - containerOnScreen.y);
        } catch (IllegalComponentStateException ex) {
            return new Point(0, 0);
        }
    }

    public enum Easing {
        LINEAR,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC
    }

    public static final class CapturedSnapshot {
        private final BufferedImage image;
        private final Point origin;

        private CapturedSnapshot(BufferedImage image, Point origin) {
            this.image = image;
            this.origin = origin != null ? origin : new Point(0, 0);
        }
    }

    private static final class SnapshotGlassPane extends JComponent {
        private final Point origin;
        private final BufferedImage snapshot;
        private final Easing easing;
        private final long startNanos;
        private final long durationNanos;
        private float alpha = 1f;

        private SnapshotGlassPane(Point origin, BufferedImage snapshot, int durationMs, Easing easing) {
            this.origin = origin != null ? origin : new Point(0, 0);
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
