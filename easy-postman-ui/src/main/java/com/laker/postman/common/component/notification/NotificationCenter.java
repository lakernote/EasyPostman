package com.laker.postman.common.component.notification;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.NotificationPosition;
import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * IntelliJ IDEA 风格的全局 Toast 通知入口。
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationCenter {
    private static final int MAX_ACTIVE_TOASTS = 5;
    private static final NotificationCenter DEFAULT = new NotificationCenter(new ToastStack(MAX_ACTIVE_TOASTS));

    private NotificationPosition defaultPosition = NotificationPosition.BOTTOM_RIGHT;
    private final ToastStack toastStack;

    private Window getMainFrame() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Window activeWindow = focusManager.getActiveWindow();
        if (activeWindow != null && activeWindow.isShowing()) {
            return resolveNotificationAnchor(activeWindow);
        }

        Window focusedWindow = focusManager.getFocusedWindow();
        if (focusedWindow != null && focusedWindow.isShowing()) {
            return resolveNotificationAnchor(focusedWindow);
        }

        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.isActive()) {
                return resolveNotificationAnchor(window);
            }
        }

        return JOptionPane.getRootFrame();
    }

    /**
     * 通知默认挂到最外层可见 owner 上，避免在模态对话框里触发时贴在子窗口边缘。
     */
    private static Window resolveNotificationAnchor(Window window) {
        Window anchor = window;
        Window owner = window.getOwner();
        while (owner != null) {
            if (owner.isShowing()) {
                anchor = owner;
            }
            owner = owner.getOwner();
        }
        return anchor;
    }

    // ==================== 通知类型 ====================

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum NotificationType {
        SUCCESS(ModernColors::getSuccess, "✓", CommonMessageKeys.GENERAL_SUCCESS),
        INFO(ModernColors::getInfo, "i", CommonMessageKeys.GENERAL_INFO),
        WARNING(ModernColors::getWarning, "!", CommonMessageKeys.GENERAL_WARNING),
        ERROR(ModernColors::getError, "✕", CommonMessageKeys.GENERAL_ERROR);

        private final Supplier<Color> colorSupplier;
        private final String icon;
        private final String titleKey;

        public Color getColor() {
            return colorSupplier.get();
        }

        public String getDefaultTitle() {
            return CommonI18n.get(titleKey);
        }
    }

    // ==================== 公开 API ====================

    public static void setDefaultPosition(NotificationPosition position) {
        DEFAULT.defaultPosition = position != null ? position : NotificationPosition.BOTTOM_RIGHT;
    }

    public static void showSuccess(String message) {
        DEFAULT.show(message, NotificationType.SUCCESS, 2, DEFAULT.defaultPosition, null);
    }

    public static void showInfo(String message) {
        DEFAULT.show(message, NotificationType.INFO, 2, DEFAULT.defaultPosition, null);
    }

    public static void showWarning(String message) {
        DEFAULT.show(message, NotificationType.WARNING, 3, DEFAULT.defaultPosition, null);
    }

    public static void showError(String message) {
        DEFAULT.show(message, NotificationType.ERROR, 3, DEFAULT.defaultPosition, null);
    }

    public static void showCloseable(String message, NotificationType type, int seconds) {
        DEFAULT.show(message, type, seconds, DEFAULT.defaultPosition, null);
    }

    public static void showLongMessage(String message, NotificationType type) {
        DEFAULT.show(message, type, 5, DEFAULT.defaultPosition, null);
    }

    public static void showToast(String message, NotificationType type, int seconds) {
        DEFAULT.show(message, type, seconds, DEFAULT.defaultPosition, null);
    }

    public static void showToast(String message, NotificationType type, int seconds, NotificationPosition position) {
        DEFAULT.show(message, type, seconds, position, null);
    }

    public static void showToast(String message, NotificationType type, int seconds, String title) {
        DEFAULT.show(message, type, seconds, DEFAULT.defaultPosition, title);
    }

    // ==================== 内部实现 ====================

    private void show(String message, NotificationType type, int seconds,
                      NotificationPosition position, String title) {
        SwingUtilities.invokeLater(() -> {
            Window mainFrame = getMainFrame();
            ToastWindow toast = new ToastWindow(
                    mainFrame,
                    message,
                    title,
                    type != null ? type : NotificationType.INFO,
                    seconds,
                    position != null ? position : defaultPosition,
                    toastStack::remove,
                    toastStack::refreshPositions
            );
            toastStack.add(toast);
            toast.startShow();
        });
    }

    static Color toastTitleColor(NotificationType type) {
        return ToastStyle.titleColor(type);
    }

    static Color toastHeaderBackgroundColor(NotificationType type) {
        return ToastStyle.headerBackgroundColor(type);
    }
}
