package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertEquals;
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

    private static final class SampleSettingsPanel extends ModernSettingsPanel {
        private final List<String> values = List.of("ready");
        private boolean buildContentCalled;
        private boolean registerListenersCalled;
        private int fieldCountAtBuildContent;

        @Override
        protected void buildContent(JPanel contentPanel) {
            buildContentCalled = true;
            fieldCountAtBuildContent = values.size();
        }

        @Override
        protected void registerListeners() {
            registerListenersCalled = true;
        }
    }
}
