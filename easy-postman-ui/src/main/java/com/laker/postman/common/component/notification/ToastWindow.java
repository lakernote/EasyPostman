package com.laker.postman.common.component.notification;

import com.laker.postman.model.NotificationPosition;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

class ToastWindow extends JWindow {
    private final Window parentWindow;
    private final NotificationCenter.NotificationType type;
    private final NotificationPosition position;
    private final String fullMessage;
    private final Consumer<ToastWindow> onClosed;
    private final Runnable onLayoutChanged;

    private int stackOffset = 0;
    private boolean hovered = false;
    private boolean expanded = false;
    private boolean closed = false;
    private int slideStep = 0;
    private Point targetPosition;

    private Timer autoCloseTimer;
    private Timer slideTimer;
    private Timer fadeTimer;
    private JTextArea bodyLabel;
    private JPanel rootPanel;
    private JButton closeButton;

    private long pausedTime = 0;
    private long startTime = 0;
    private int totalDuration = 0;

    ToastWindow(Window parentWindow,
                String message,
                String customTitle,
                NotificationCenter.NotificationType type,
                int seconds,
                NotificationPosition position,
                Consumer<ToastWindow> onClosed,
                Runnable onLayoutChanged) {
        super(parentWindow);
        this.parentWindow = parentWindow;
        this.type = type;
        this.position = position;
        this.fullMessage = message;
        this.onClosed = onClosed;
        this.onLayoutChanged = onLayoutChanged;

        configureWindow();
        String title = customTitle != null && !customTitle.isBlank() ? customTitle : type.getDefaultTitle();
        rootPanel = buildRootPanel(message, title, seconds);
        setContentPane(rootPanel);
        setSize(ToastStyle.MAX_WIDTH, 1);
        bodyLabel.setSize(ToastStyle.bodyWidth(), 1);
        setSize(ToastStyle.MAX_WIDTH, rootPanel.getPreferredSize().height);
        targetPosition = calculatePosition(stackOffset);
    }

    void startShow() {
        Point startPosition = ToastPlacement.slideStart(targetPosition, getSize(), position);
        setLocation(startPosition);
        setVisible(true);

        slideStep = 0;
        slideTimer = new Timer(ToastStyle.SLIDE_INTERVAL, e -> {
            slideStep++;
            double progress = (double) slideStep / ToastStyle.SLIDE_STEPS;
            double eased = 1 - Math.pow(1 - progress, 3);
            int x = startPosition.x + (int) ((targetPosition.x - startPosition.x) * eased);
            int y = startPosition.y + (int) ((targetPosition.y - startPosition.y) * eased);
            setLocation(x, y);
            if (slideStep >= ToastStyle.SLIDE_STEPS) {
                ((Timer) e.getSource()).stop();
                setLocation(targetPosition);
                if (totalDuration > 0) {
                    startAutoCloseTimer();
                }
            }
        });
        slideTimer.start();
    }

    void closeQuietly() {
        stopTimers();
        dispose();
        notifyClosed();
    }

    void updateStackOffset(int offset) {
        stackOffset = offset;
        targetPosition = calculatePosition(offset);
        setLocation(targetPosition);
    }

    private void configureWindow() {
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setBackground(new Color(0, 0, 0, 0));
        getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);
        getRootPane().setOpaque(false);
        getRootPane().setBackground(new Color(0, 0, 0, 0));
        getLayeredPane().setOpaque(false);
    }

    private JPanel buildRootPanel(String message, String title, int seconds) {
        JPanel wrapper = ToastStyle.createCardPanel();

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);

        JPanel header = buildHeaderPanel(title);
        JPanel body = buildBodyPanel(message);
        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        wrapper.add(card, BorderLayout.CENTER);

        for (JPanel panel : new JPanel[]{wrapper, card, header, body}) {
            installHoverListener(panel);
        }

        totalDuration = seconds * 1000;
        return wrapper;
    }

    private JPanel buildHeaderPanel(String title) {
        JPanel header = ToastStyle.createHeaderPanel(type);
        JLabel iconLabel = ToastStyle.createTypeIcon(type);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(ToastStyle.titleColor(type));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));

        closeButton = ToastStyle.createCloseButton(this::startFadeOut);
        closeButton.setOpaque(false);

        header.add(iconLabel, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBodyPanel(String message) {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(
                ToastStyle.VERTICAL_PADDING,
                ToastStyle.HORIZONTAL_PADDING,
                ToastStyle.VERTICAL_PADDING,
                ToastStyle.HORIZONTAL_PADDING
        ));

        bodyLabel = ToastStyle.createBodyTextArea(
                ToastTextFormatter.displayText(message, false)
        );
        body.add(bodyLabel, BorderLayout.CENTER);

        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    handleBodySingleClick();
                } else if (e.getClickCount() == 2) {
                    copyMessageAndFlash();
                }
            }
        };
        body.addMouseListener(clickListener);
        bodyLabel.addMouseListener(clickListener);
        return body;
    }

    private void handleBodySingleClick() {
        if (ToastTextFormatter.isFoldable(fullMessage)) {
            toggleExpand();
        } else {
            copyMessageAndFlash();
        }
    }

    private void installHoverListener(JPanel panel) {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                onHoverEnter();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(),
                        ToastWindow.this);
                if (!new Rectangle(getSize()).contains(point)) {
                    onHoverExit();
                }
            }
        });
    }

    private void onHoverEnter() {
        if (hovered) {
            return;
        }
        hovered = true;
        pauseAutoClose();
        ToastStyle.showCloseButton(closeButton);
    }

    private void onHoverExit() {
        if (!hovered) {
            return;
        }
        hovered = false;
        resumeAutoClose();
        ToastStyle.hideCloseButton(closeButton);
    }

    private void startAutoCloseTimer() {
        startTime = System.currentTimeMillis();
        autoCloseTimer = new Timer(totalDuration, e -> startFadeOut());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    private void pauseAutoClose() {
        pausedTime = System.currentTimeMillis();
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
        }
    }

    private void resumeAutoClose() {
        if (pausedTime <= 0) {
            return;
        }
        long paused = System.currentTimeMillis() - pausedTime;
        startTime += paused;
        pausedTime = 0;
        long remaining = totalDuration - (System.currentTimeMillis() - startTime);
        if (remaining > 0) {
            autoCloseTimer = new Timer((int) remaining, e -> startFadeOut());
            autoCloseTimer.setRepeats(false);
            autoCloseTimer.start();
        } else {
            startFadeOut();
        }
    }

    private void startFadeOut() {
        if (closed) {
            return;
        }
        stopTimers();
        final int[] step = {0};
        fadeTimer = new Timer(ToastStyle.FADE_INTERVAL, e -> {
            step[0]++;
            float alpha = Math.max(0f, 1f - (float) step[0] / ToastStyle.FADE_STEPS);
            setOpacity(alpha);
            if (step[0] >= ToastStyle.FADE_STEPS) {
                ((Timer) e.getSource()).stop();
                dispose();
                notifyClosed();
            }
        });
        fadeTimer.start();
    }

    private void stopTimers() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            autoCloseTimer = null;
        }
        if (slideTimer != null) {
            slideTimer.stop();
            slideTimer = null;
        }
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
    }

    private void toggleExpand() {
        expanded = !expanded;
        bodyLabel.setText(ToastTextFormatter.displayText(fullMessage, expanded));
        bodyLabel.setSize(ToastStyle.bodyWidth(), 1);
        setSize(ToastStyle.MAX_WIDTH, rootPanel.getPreferredSize().height);
        targetPosition = calculatePosition(stackOffset);
        setLocation(targetPosition);
        onLayoutChanged.run();
    }

    private Point calculatePosition(int offset) {
        return ToastPlacement.calculate(parentWindow, getSize(), position, offset);
    }

    private void copyMessageAndFlash() {
        copyToClipboard(fullMessage);
        flashFeedback();
    }

    private void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (Exception ignored) {
        }
    }

    private void flashFeedback() {
        Color original = bodyLabel.getForeground();
        bodyLabel.setForeground(type.getColor());
        Timer timer = new Timer(200, e -> {
            bodyLabel.setForeground(original);
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void notifyClosed() {
        if (closed) {
            return;
        }
        closed = true;
        onClosed.accept(this);
    }
}
