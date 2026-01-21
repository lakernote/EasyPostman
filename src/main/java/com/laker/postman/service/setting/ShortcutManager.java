package com.laker.postman.service.setting;

import com.laker.postman.util.SystemUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 快捷键管理器
 * 管理应用程序的所有快捷键配置，支持用户自定义
 */
public class ShortcutManager {
    private static final String SHORTCUT_CONFIG_FILE = SystemUtil.getUserHomeEasyPostmanPath() + "shortcuts.properties";
    private static final Properties props = new Properties();
    private static final Map<String, ShortcutConfig> shortcuts = new HashMap<>();

    // 快捷键ID常量
    public static final String SEND_REQUEST = "send_request";
    public static final String NEW_REQUEST = "new_request";
    public static final String SAVE_REQUEST = "save_request";
    public static final String CLOSE_CURRENT_TAB = "close_current_tab";
    public static final String CLOSE_OTHER_TABS = "close_other_tabs";
    public static final String CLOSE_ALL_TABS = "close_all_tabs";
    public static final String EXIT_APP = "exit_app";

    // 私有构造函数
    private ShortcutManager() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static {
        initDefaultShortcuts();
        load();
    }

    /**
     * 初始化默认快捷键
     */
    private static void initDefaultShortcuts() {
        int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        shortcuts.put(SEND_REQUEST, new ShortcutConfig(
            SEND_REQUEST, "send_request",
            KeyEvent.VK_ENTER, cmdMask, "Ctrl+Enter", "Cmd+Enter"
        ));

        shortcuts.put(NEW_REQUEST, new ShortcutConfig(
            NEW_REQUEST, "new_request",
            KeyEvent.VK_N, cmdMask, "Ctrl+N", "Cmd+N"
        ));

        shortcuts.put(SAVE_REQUEST, new ShortcutConfig(
            SAVE_REQUEST, "save_request",
            KeyEvent.VK_S, cmdMask, "Ctrl+S", "Cmd+S"
        ));

        shortcuts.put(CLOSE_CURRENT_TAB, new ShortcutConfig(
            CLOSE_CURRENT_TAB, "close_current_tab",
            KeyEvent.VK_W, cmdMask, "Ctrl+W", "Cmd+W"
        ));

        shortcuts.put(CLOSE_OTHER_TABS, new ShortcutConfig(
            CLOSE_OTHER_TABS, "close_other_tabs",
            KeyEvent.VK_W, cmdMask | InputEvent.ALT_DOWN_MASK, "Ctrl+Alt+W", "Cmd+Option+W"
        ));

        shortcuts.put(CLOSE_ALL_TABS, new ShortcutConfig(
            CLOSE_ALL_TABS, "close_all_tabs",
            KeyEvent.VK_W, cmdMask | InputEvent.SHIFT_DOWN_MASK, "Ctrl+Shift+W", "Cmd+Shift+W"
        ));

        shortcuts.put(EXIT_APP, new ShortcutConfig(
            EXIT_APP, "exit_app",
            KeyEvent.VK_Q, cmdMask, "Ctrl+Q", "Cmd+Q"
        ));
    }

    /**
     * 从文件加载快捷键配置
     */
    public static void load() {
        File file = new File(SHORTCUT_CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                // 应用加载的配置
                for (String key : shortcuts.keySet()) {
                    String keyCodeStr = props.getProperty(key + ".keyCode");
                    String modifiersStr = props.getProperty(key + ".modifiers");
                    if (keyCodeStr != null && modifiersStr != null) {
                        try {
                            int keyCode = Integer.parseInt(keyCodeStr);
                            int modifiers = Integer.parseInt(modifiersStr);
                            shortcuts.get(key).setKeyCode(keyCode);
                            shortcuts.get(key).setModifiers(modifiers);
                        } catch (NumberFormatException e) {
                            // 忽略解析错误，使用默认值
                        }
                    }
                }
            } catch (IOException e) {
                // 忽略，使用默认配置
            }
        }
    }

    /**
     * 保存快捷键配置到文件
     */
    public static void save() {
        for (Map.Entry<String, ShortcutConfig> entry : shortcuts.entrySet()) {
            String key = entry.getKey();
            ShortcutConfig config = entry.getValue();
            props.setProperty(key + ".keyCode", String.valueOf(config.getKeyCode()));
            props.setProperty(key + ".modifiers", String.valueOf(config.getModifiers()));
        }

        try (FileOutputStream fos = new FileOutputStream(SHORTCUT_CONFIG_FILE)) {
            props.store(fos, "EasyPostman Keyboard Shortcuts");
        } catch (IOException e) {
            // 忽略保存错误
        }
    }

    /**
     * 获取快捷键配置
     */
    public static ShortcutConfig getShortcut(String id) {
        return shortcuts.get(id);
    }

    /**
     * 获取 KeyStroke
     */
    public static KeyStroke getKeyStroke(String id) {
        ShortcutConfig config = shortcuts.get(id);
        if (config != null) {
            return KeyStroke.getKeyStroke(config.getKeyCode(), config.getModifiers());
        }
        return null;
    }

    /**
     * 设置快捷键
     */
    public static void setShortcut(String id, int keyCode, int modifiers) {
        ShortcutConfig config = shortcuts.get(id);
        if (config != null) {
            config.setKeyCode(keyCode);
            config.setModifiers(modifiers);
        }
    }

    /**
     * 获取快捷键的显示文本（根据操作系统）
     */
    public static String getShortcutText(String id) {
        ShortcutConfig config = shortcuts.get(id);
        if (config != null) {
            return config.getDisplayText();
        }
        return "";
    }

    /**
     * 重置为默认快捷键
     */
    public static void resetToDefaults() {
        shortcuts.clear();
        initDefaultShortcuts();
        save();
    }

    /**
     * 检查快捷键是否冲突
     */
    public static String checkConflict(String excludeId, int keyCode, int modifiers) {
        for (Map.Entry<String, ShortcutConfig> entry : shortcuts.entrySet()) {
            if (!entry.getKey().equals(excludeId)) {
                ShortcutConfig config = entry.getValue();
                if (config.getKeyCode() == keyCode && config.getModifiers() == modifiers) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 快捷键配置类
     */
    @Getter
    public static class ShortcutConfig {
        private final String id;
        private final String nameKey;
        private int keyCode;
        private int modifiers;
        private final String windowsText;
        private final String macText;

        public ShortcutConfig(String id, String nameKey, int keyCode, int modifiers,
                            String windowsText, String macText) {
            this.id = id;
            this.nameKey = nameKey;
            this.keyCode = keyCode;
            this.modifiers = modifiers;
            this.windowsText = windowsText;
            this.macText = macText;
        }

        public void setKeyCode(int keyCode) {
            this.keyCode = keyCode;
        }

        public void setModifiers(int modifiers) {
            this.modifiers = modifiers;
        }

        /**
         * 获取显示文本（根据操作系统）
         */
        public String getDisplayText() {
            boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
            StringBuilder sb = new StringBuilder();

            int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

            if ((modifiers & cmdMask) != 0) {
                sb.append(isMac ? "Cmd+" : "Ctrl+");
            }
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
                sb.append("Shift+");
            }
            if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
                sb.append(isMac ? "Option+" : "Alt+");
            }
            if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0 && (modifiers & cmdMask) == 0) {
                sb.append("Ctrl+");
            }

            sb.append(KeyEvent.getKeyText(keyCode));

            return sb.toString();
        }
    }
}

