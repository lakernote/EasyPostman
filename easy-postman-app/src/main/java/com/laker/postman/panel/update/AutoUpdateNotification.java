package com.laker.postman.panel.update;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * 自动更新通知弹窗 - 位于主窗口右下角，自动淡入淡出
 * <p>支持两种模式：</p>
 * <ul>
 *   <li>NORMAL  - 蓝色左侧条，info 图标，「查看详情」→ ModernUpdateDialog</li>
 *   <li>NO_ASSET - 橙色左侧条，warning 图标，「前往 GitHub」→ NoAssetDialog</li>
 * </ul>
 */
@Slf4j
public class AutoUpdateNotification {

    /** 通知变体 */
    public enum Variant { NORMAL, NO_ASSET }

    private static final int NOTIFICATION_WIDTH = 400;
    private static final int MARGIN = 20;
    private static final int FADE_DURATION = 300;
    private static final int DISPLAY_DURATION = 6000;
    private static final float FADE_STEP = 0.05f;
    private static final int FADE_TIMER_DELAY = 10;
    private static final int INDICATOR_WIDTH = 4;
    /** 底部预留空间（避免遮住状态栏/Dock） */
    private static final int BOTTOM_CLEARANCE = 40;

    private final JDialog dialog;
    private final JFrame parent;
    private Timer fadeTimer;
    private Timer autoCloseTimer;
    private float opacity = 0f;
    private ComponentAdapter parentMoveListener;
    private int hoverAlpha = 0;

    private AutoUpdateNotification(JFrame parent, UpdateInfo updateInfo,
                                   Consumer<UpdateInfo> onAction, Variant variant) {
        this.parent = parent;
        dialog = new JDialog(parent, false);
        dialog.setUndecorated(true);
        dialog.setFocusableWindowState(false);
        dialog.setType(Window.Type.UTILITY);
        dialog.setOpacity(0f);

        JPanel contentPanel = createNotificationPanel(updateInfo, onAction, variant);
        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setSize(NOTIFICATION_WIDTH, dialog.getHeight());

        positionDialog();
        registerParentMoveListener();
    }

    // ─── Public factory methods ───────────────────────────────────────────────

    /** 普通更新通知（蓝色） */
    public static void show(JFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        showInternal(parent, updateInfo, onViewDetails, Variant.NORMAL);
    }

    /** 有新版本但无安装包通知（橙色） */
    public static void showNoAsset(JFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onAction) {
        showInternal(parent, updateInfo, onAction, Variant.NO_ASSET);
    }

    private static void showInternal(JFrame parent, UpdateInfo updateInfo,
                                     Consumer<UpdateInfo> onAction, Variant variant) {
        SwingUtilities.invokeLater(() -> {
            if (parent == null || !parent.isVisible()) {
                log.debug("Parent window is not visible, skip showing notification");
                return;
            }
            if (!parent.isFocused() && !parent.isActive()) {
                log.debug("Parent window is not active, skip showing notification");
                return;
            }
            new AutoUpdateNotification(parent, updateInfo, onAction, variant).display();
        });
    }

    private void display() {
        dialog.setVisible(true);
        fadeIn();
        scheduleAutoClose();
    }

    /** 启动自动关闭倒计时 */
    private void scheduleAutoClose() {
        stopAutoCloseTimer();
        autoCloseTimer = new Timer(DISPLAY_DURATION, e -> fadeOut());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    private void fadeIn() {
        stopFadeTimer();
        fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            opacity = Math.min(opacity + FADE_STEP, 1.0f);
            dialog.setOpacity(opacity);
            if (opacity >= 1.0f) stopFadeTimer();
        });
        fadeTimer.start();
    }

    private void fadeOut() {
        stopFadeTimer();
        stopAutoCloseTimer();
        fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            opacity = Math.max(opacity - FADE_STEP, 0f);
            dialog.setOpacity(opacity);
            if (opacity <= 0f) {
                stopFadeTimer();
                cleanupAndClose();
            }
        });
        fadeTimer.start();
    }

    private void stopFadeTimer() {
        if (fadeTimer != null) { fadeTimer.stop(); fadeTimer = null; }
    }

    private void stopAutoCloseTimer() {
        if (autoCloseTimer != null) { autoCloseTimer.stop(); autoCloseTimer = null; }
    }

    private void cleanupAndClose() {
        stopFadeTimer();
        stopAutoCloseTimer();
        // 移除父窗口移动监听器，防止内存泄漏
        if (parentMoveListener != null) {
            parent.removeComponentListener(parentMoveListener);
            parentMoveListener = null;
        }
        dialog.dispose();
    }

    /** 跟随父窗口移动/缩放重新定位 */
    private void registerParentMoveListener() {
        parentMoveListener = new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e)  { positionDialog(); }
            @Override public void componentResized(ComponentEvent e) { positionDialog(); }
        };
        parent.addComponentListener(parentMoveListener);
    }

    private void positionDialog() {
        Rectangle pb = parent.getBounds();
        int x = pb.x + pb.width  - NOTIFICATION_WIDTH  - MARGIN;
        int y = pb.y + pb.height - dialog.getHeight() - MARGIN - BOTTOM_CLEARANCE;

        GraphicsConfiguration gc = parent.getGraphicsConfiguration();
        if (gc != null) {
            Rectangle screen = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            x = Math.min(x, screen.x + screen.width  - insets.right  - NOTIFICATION_WIDTH  - MARGIN);
            y = Math.min(y, screen.y + screen.height - insets.bottom - dialog.getHeight() - MARGIN);
            x = Math.max(x, screen.x + insets.left + MARGIN);
            y = Math.max(y, screen.y + insets.top  + MARGIN);
        }
        dialog.setLocation(x, y);
    }

    private JPanel createNotificationPanel(UpdateInfo updateInfo,
                                            Consumer<UpdateInfo> onAction, Variant variant) {
        boolean isNoAsset = variant == Variant.NO_ASSET;
        Color indicatorColor = isNoAsset ? ModernColors.WARNING : ModernColors.INFO;

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIManager.getColor("Panel.background"));
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (hoverAlpha > 0) {
                    Color sel = UIManager.getColor("List.selectionBackground");
                    if (sel != null) {
                        g2.setColor(new Color(sel.getRed(), sel.getGreen(), sel.getBlue(), hoverAlpha));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
                g2.setColor(indicatorColor);
                g2.fillRect(0, 0, INDICATOR_WIDTH, getHeight());
                g2.setColor(ModernColors.getBorderLightColor());
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14 + INDICATOR_WIDTH, 14, 14));

        root.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                stopAutoCloseTimer();
                hoverAlpha = 30;
                root.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                Component dest = SwingUtilities.getDeepestComponentAt(root, e.getX(), e.getY());
                if (dest != null && SwingUtilities.isDescendingFrom(dest, root)) return;
                hoverAlpha = 0;
                root.repaint();
                scheduleAutoClose();
            }
        });

        // 左侧图标
        String iconPath = isNoAsset ? "icons/warning.svg" : "icons/info.svg";
        JLabel iconLabel = new JLabel(IconUtil.createThemed(iconPath, 32, 32));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        root.add(iconLabel, BorderLayout.WEST);

        // 中心内容
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 标题
        String titleText = isNoAsset
                ? I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TITLE, updateInfo.getLatestVersion())
                : I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE);
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        if (isNoAsset) titleLabel.setForeground(ModernColors.WARNING);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 版本行
        String currentV = updateInfo.getCurrentVersion() != null ? updateInfo.getCurrentVersion() : "-";
        JLabel versionLabel = new JLabel(currentV + "  →  " + updateInfo.getLatestVersion());
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        versionLabel.setForeground(ModernColors.getTextSecondary());
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 描述行
        String description = isNoAsset
                ? (I18nUtil.isChinese()
                    ? "安装包尚未上传，请前往 GitHub 手动下载"
                    : "Installer not yet available, please download from GitHub")
                : extractDescription(updateInfo);
        JLabel descLabel = new JLabel("<html><body style='width:220px'>" + description + "</body></html>");
        descLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        descLabel.setForeground(ModernColors.getTextHint());
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 操作按钮
        JButton actionButton = isNoAsset
                ? createNoAssetActionButton(updateInfo, onAction)
                : createViewDetailsButton(updateInfo, onAction);
        actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(actionButton);

        root.add(contentPanel, BorderLayout.CENTER);

        // 右上角关闭按钮
        JButton closeButton = createCloseButton();
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRight.setOpaque(false);
        topRight.add(closeButton);
        root.add(topRight, BorderLayout.EAST);

        return root;
    }

    /** 「前往 GitHub 下载」链接按钮（橙色，用于 NO_ASSET 变体） */
    private JButton createNoAssetActionButton(UpdateInfo updateInfo, Consumer<UpdateInfo> onAction) {
        JButton button = new JButton(
                "<html><u>" + I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_GO_GITHUB) + "</u></html>");
        button.setForeground(ModernColors.WARNING);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(e -> {
            fadeOut();
            Timer delayTimer = new Timer(FADE_DURATION, evt -> onAction.accept(updateInfo));
            delayTimer.setRepeats(false);
            delayTimer.start();
        });
        return button;
    }


    private JButton createCloseButton() {
        JButton button = new JButton("×");
        button.setFont(new Font("Arial", Font.PLAIN, 18));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(22, 22));
        button.setMaximumSize(new Dimension(22, 22));
        button.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addActionListener(e -> fadeOut());
        return button;
    }

    private JButton createViewDetailsButton(UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        // 链接风格：无边框、蓝色文字、下划线 HTML，同 IDEA 通知底部 "View" 链接
        JButton button = new JButton(
                "<html><u>" + I18nUtil.getMessage(MessageKeys.UPDATE_VIEW_DETAILS) + "</u></html>");
        button.setForeground(ModernColors.INFO);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(e -> {
            fadeOut();
            Timer delayTimer = new Timer(FADE_DURATION, evt -> onViewDetails.accept(updateInfo));
            delayTimer.setRepeats(false);
            delayTimer.start();
        });
        return button;
    }

    private String extractDescription(UpdateInfo updateInfo) {
        if (updateInfo.getReleaseInfo() == null) {
            return I18nUtil.isChinese() ? "点击查看更新详情" : "Click to view details";
        }
        String body = updateInfo.getReleaseInfo().getStr("body", "");
        if (body.isEmpty()) {
            return I18nUtil.isChinese() ? "包含新功能和改进" : "New features and improvements";
        }
        String cleaned = body.trim()
                .replaceAll("^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)]\\([^)]+\\)", "$1")
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        int maxLength = 60;
        if (cleaned.length() > maxLength) {
            int lastSpace = cleaned.lastIndexOf(' ', maxLength);
            cleaned = (lastSpace > maxLength * 0.7)
                    ? cleaned.substring(0, lastSpace) + "..."
                    : cleaned.substring(0, maxLength) + "...";
        }
        if (cleaned.isEmpty()) {
            return I18nUtil.isChinese() ? "包含新功能和改进" : "New features and improvements";
        }
        return cleaned;
    }
}

