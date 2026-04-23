package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import javax.swing.*;
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
