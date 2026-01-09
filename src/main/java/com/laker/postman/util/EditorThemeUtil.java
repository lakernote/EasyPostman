package com.laker.postman.util;

import com.formdev.flatlaf.FlatLaf;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.io.IOException;
import java.io.InputStream;

/**
 * 编辑器主题工具类
 * 为 RSyntaxTextArea 提供主题适配支持，根据 FlatLaf 主题自动选择亮色或暗色编辑器主题
 */
@Slf4j
public class EditorThemeUtil {

    private EditorThemeUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 加载并应用编辑器主题 - 支持亮色和暗色主题自适应
     *
     * @param area RSyntaxTextArea 编辑器实例
     */
    public static void loadTheme(RSyntaxTextArea area) {
        // 根据 FlatLaf 主题选择对应的 RSyntaxTextArea 主题
        // 暗色主题：使用 dark.xml
        // 亮色主题：使用 vs.xml (Visual Studio 风格)
        String themeFile = FlatLaf.isLafDark() ? "dark.xml" : "vs.xml";

        try (InputStream in = EditorThemeUtil.class.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + themeFile)) {
            if (in != null) {
                Theme theme = Theme.load(in);
                theme.apply(area);
                log.debug("Loaded RSyntaxTextArea theme: {}", themeFile);
            } else {
                log.warn("Theme file not found: {}", themeFile);
            }
        } catch (IOException e) {
            log.error("Failed to load editor theme: {}", themeFile, e);
        }
    }
}
