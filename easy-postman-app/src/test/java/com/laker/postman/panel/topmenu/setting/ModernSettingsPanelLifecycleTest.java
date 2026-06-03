package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ModernSettingsPanelLifecycleTest {

    @Test(description = "ModernSettingsPanel 不应在父类构造阶段调用子类 buildContent")
    public void testSubclassFieldsAreAvailableWhenBuildContentRuns() {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();

        assertTrue(panel.buildContentCalled);
        assertTrue(panel.registerListenersCalled);
        assertEquals(panel.fieldCountAtBuildContent, 1);
    }

    @Test(description = "设置对话框默认高度应能容纳压力测试页新增字段")
    public void settingsDialogPreferredHeightShouldFitPerformanceSettings() throws Exception {
        Field heightField = ModernSettingsDialog.class.getDeclaredField("PREFERRED_HEIGHT");
        heightField.setAccessible(true);

        assertTrue(heightField.getInt(null) >= 640);
    }

    @Test(description = "设置对话框固定尺寸，避免用户拖拽后内容被裁切")
    public void settingsDialogShouldDisableResize() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("setResizable(false)"));
        assertFalse(source.contains("setResizable(true)"));
    }

    @Test(description = "设置页初始化和切换 tab 后应回到内容顶部")
    public void settingsPanelShouldResetMainScrollPaneToTop() throws Exception {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();
        JScrollPane scrollPane = findScrollPane(panel);

        scrollPane.getViewport().setViewPosition(new Point(0, 120));
        panel.resetScrollPositionToTop();
        SwingUtilities.invokeAndWait(() -> {
        });

        assertEquals(scrollPane.getViewport().getViewPosition().y, 0);
    }

    @Test(description = "设置页主内容应跟随 viewport 宽度，避免右侧内容被裁切")
    public void settingsPanelContentShouldTrackViewportWidth() {
        SampleSettingsPanel panel = new SampleSettingsPanel();
        panel.getPreferredSize();
        JScrollPane scrollPane = findScrollPane(panel);

        assertTrue(scrollPane.getViewport().getView() instanceof Scrollable);
        Scrollable view = (Scrollable) scrollPane.getViewport().getView();
        assertTrue(view.getScrollableTracksViewportWidth());
    }

    private static JScrollPane findScrollPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
            if (component instanceof Container childContainer) {
                try {
                    return findScrollPane(childContainer);
                } catch (IllegalStateException ignored) {
                    // Keep scanning sibling components.
                }
            }
        }
        throw new IllegalStateException("No JScrollPane found");
    }

    private static final class SampleSettingsPanel extends ModernSettingsPanel {
        private final List<String> values = List.of("ready");
        private boolean buildContentCalled;
        private boolean registerListenersCalled;
        private int fieldCountAtBuildContent;

        @Override
        protected void buildContent(JPanel contentPanel) {
            buildContentCalled = true;
            fieldCountAtBuildContent = values.size();
            contentPanel.add(Box.createVerticalStrut(400));
        }

        @Override
        protected void registerListeners() {
            registerListenersCalled = true;
        }
    }
}
