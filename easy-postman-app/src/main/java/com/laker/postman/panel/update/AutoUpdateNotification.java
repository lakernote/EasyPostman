package com.laker.postman.panel.update;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
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

    private static final int DISPLAY_DURATION = 6000;

    private final UpdateFloatingNotificationWindow window;

    private AutoUpdateNotification(JFrame parent, UpdateInfo updateInfo,
                                   Consumer<UpdateInfo> onAction, Variant variant) {
        this.window = new UpdateFloatingNotificationWindow(parent, DISPLAY_DURATION);
        JPanel contentPanel = createNotificationPanel(updateInfo, onAction, variant);
        window.installContent(contentPanel, UpdateNotificationStyle.NOTIFICATION_WIDTH);
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
                log.info("Parent window is not visible, skip showing update notification");
                return;
            }
            if (!parent.isFocused() && !parent.isActive()) {
                log.info("Parent window is not active, skip showing update notification");
                return;
            }
            new AutoUpdateNotification(parent, updateInfo, onAction, variant).display();
        });
    }

    private void display() {
        window.display();
    }

    private JPanel createNotificationPanel(UpdateInfo updateInfo,
                                            Consumer<UpdateInfo> onAction, Variant variant) {
        boolean isNoAsset = variant == Variant.NO_ASSET;
        Color indicatorColor = isNoAsset ? ModernColors.getWarning() : ModernColors.getPrimary();

        JPanel root = UpdateNotificationStyle.createRootPanel(indicatorColor);

        root.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                window.pauseAutoClose();
                UpdateNotificationStyle.setHoverAlpha(root, 30);
            }
            @Override public void mouseExited(MouseEvent e) {
                Component dest = SwingUtilities.getDeepestComponentAt(root, e.getX(), e.getY());
                if (dest != null && SwingUtilities.isDescendingFrom(dest, root)) return;
                UpdateNotificationStyle.setHoverAlpha(root, 0);
                window.resumeAutoClose();
            }
        });

        String iconPath = isNoAsset ? "icons/warning.svg" : "icons/info.svg";
        root.add(UpdateNotificationStyle.createIconLabel(iconPath, indicatorColor), BorderLayout.WEST);

        JPanel contentPanel = UpdateNotificationStyle.createContentPanel();

        String titleText = isNoAsset
                ? I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TITLE, updateInfo.getLatestVersion())
                : I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE);
        JLabel titleLabel = UpdateNotificationStyle.createTitleLabel(titleText);
        JLabel versionLabel = UpdateNotificationStyle.createVersionLabel(
                UpdateTextFormatter.versionTransition(updateInfo.getCurrentVersion(), updateInfo.getLatestVersion())
        );
        String description = isNoAsset
                ? I18nUtil.getMessage(MessageKeys.UPDATE_NOTIFICATION_NO_ASSET_DESCRIPTION)
                : UpdateTextFormatter.notificationDescription(updateInfo);
        JLabel descLabel = UpdateNotificationStyle.createDescriptionLabel(description);

        JButton actionButton = isNoAsset
                ? createNoAssetActionButton(updateInfo, onAction)
                : createViewDetailsButton(updateInfo, onAction);
        actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(actionButton);

        root.add(contentPanel, BorderLayout.CENTER);

        JButton closeButton = UpdateNotificationStyle.createCloseButton();
        closeButton.addActionListener(e -> window.fadeOut());
        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setOpaque(false);
        topRight.add(closeButton, BorderLayout.NORTH);
        root.add(topRight, BorderLayout.EAST);

        return root;
    }

    private JButton createNoAssetActionButton(UpdateInfo updateInfo, Consumer<UpdateInfo> onAction) {
        JButton button = UpdateNotificationStyle.createLinkButton(
                I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_GO_GITHUB),
                ModernColors.getWarning()
        );
        button.addActionListener(e -> {
            window.fadeOutThen(() -> onAction.accept(updateInfo));
        });
        return button;
    }


    private JButton createViewDetailsButton(UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        JButton button = UpdateNotificationStyle.createLinkButton(
                I18nUtil.getMessage(MessageKeys.UPDATE_VIEW_DETAILS),
                ModernColors.getPrimary()
        );
        button.addActionListener(e -> {
            window.fadeOutThen(() -> onViewDetails.accept(updateInfo));
        });
        return button;
    }
}
