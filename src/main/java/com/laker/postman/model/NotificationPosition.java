package com.laker.postman.model;

import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知位置枚举
 * 包含位置的顺序索引和国际化键
 */
@Getter
@RequiredArgsConstructor
public enum NotificationPosition {
    TOP_RIGHT(0, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOP_RIGHT),
    TOP_CENTER(1, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOP_CENTER),
    TOP_LEFT(2, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOP_LEFT),
    BOTTOM_RIGHT(3, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_BOTTOM_RIGHT),
    BOTTOM_CENTER(4, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_BOTTOM_CENTER),
    BOTTOM_LEFT(5, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_BOTTOM_LEFT),
    CENTER(6, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_CENTER);

    private final int index;
    private final String i18nKey;

    /**
     * 根据索引获取位置
     */
    public static NotificationPosition fromIndex(int index) {
        for (NotificationPosition position : values()) {
            if (position.index == index) {
                return position;
            }
        }
        return BOTTOM_RIGHT; // 默认右下角
    }

    /**
     * 根据名称获取位置
     */
    public static NotificationPosition fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return BOTTOM_RIGHT; // 默认右下角
        }
    }
}