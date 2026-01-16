package com.laker.postman.util;

import com.laker.postman.common.IRefreshable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * UI 刷新管理器
 * <p>
 * 统一管理所有 UI 组件的刷新逻辑，支持以下场景：
 * <ul>
 *   <li>主题切换 - 更新所有组件的外观样式</li>
 *   <li>字体切换 - 更新所有组件的字体</li>
 *   <li>语言切换 - 更新所有文本内容</li>
 * </ul>
 *
 * @author laker
 */
@Slf4j
@UtilityClass
public class UIRefreshManager {

    /**
     * 刷新所有窗口（主题或字体切换时调用）
     * <p>
     * 此方法会：
     * <ul>
     *   <li>递归更新所有组件的 UI（应用新的 Look and Feel）</li>
     *   <li>重新验证布局</li>
     *   <li>重绘所有组件</li>
     * </ul>
     * <p>
     * 适用场景：主题切换、字体切换
     */
    public static void refreshAllWindows() {
        log.info("Refreshing all windows UI...");

        try {
            Window[] windows = Window.getWindows();
            int refreshedCount = 0;

            for (Window window : windows) {
                if (window.isDisplayable()) {
                    // 更新组件树 UI，应用新的 Look and Feel
                    SwingUtilities.updateComponentTreeUI(window);

                    // 重新验证布局
                    window.validate();

                    // 重绘窗口
                    window.repaint();

                    refreshedCount++;
                    log.debug("Refreshed window: {}", window.getClass().getSimpleName());
                }
            }

            log.info("Successfully refreshed {} window(s)", refreshedCount);
        } catch (Exception e) {
            log.error("Failed to refresh windows", e);
        }
    }

    /**
     * 刷新语言（语言切换时调用）
     * <p>
     * 此方法会：
     * <ul>
     *   <li>递归刷新实现了 IRefreshable 接口的组件（包括窗口标题更新）</li>
     *   <li>更新组件树 UI</li>
     *   <li>重新验证布局和重绘</li>
     * </ul>
     * <p>
     * 适用场景：语言切换
     */
    public static void refreshLanguage() {
        log.info("Refreshing UI for language change...");

        try {
            Window[] windows = Window.getWindows();
            int refreshedCount = 0;

            for (Window window : windows) {
                if (window.isDisplayable()) {
                    // 1. 递归刷新所有实现了 IRefreshable 的组件
                    refreshComponentRecursively(window);

                    // 2. 更新组件树 UI（确保 UI 样式正确）
                    SwingUtilities.updateComponentTreeUI(window);

                    // 3. 重新验证布局
                    window.validate();

                    // 4. 重绘窗口
                    window.repaint();

                    refreshedCount++;
                    log.debug("Refreshed window for language: {}", window.getClass().getSimpleName());
                }
            }

            log.info("Successfully refreshed {} window(s) for language change", refreshedCount);
        } catch (Exception e) {
            log.error("Failed to refresh UI for language change", e);
        }
    }


    /**
     * 递归刷新组件
     * <p>
     * 遍历组件树，对实现了 IRefreshable 接口的组件调用 refresh() 方法
     *
     * @param component 要刷新的组件
     */
    private static void refreshComponentRecursively(Component component) {
        if (component == null) {
            return;
        }

        // 如果组件实现了 IRefreshable 接口，调用其 refresh 方法
        if (component instanceof IRefreshable refreshable) {
            try {
                refreshable.refresh();
                log.debug("Refreshed component: {}", component.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Failed to refresh component: {}", component.getClass().getSimpleName(), e);
            }
        }

        // 递归处理子组件
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshComponentRecursively(child);
            }
        }

        // 处理菜单栏（JFrame 的菜单栏不在 contentPane 中）
        if (component instanceof JFrame frame) {
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar != null) {
                refreshComponentRecursively(menuBar);
            }
        }
    }

}

