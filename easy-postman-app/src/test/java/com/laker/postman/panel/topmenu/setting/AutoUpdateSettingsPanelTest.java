package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AutoUpdateSettingsPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldRenderIndependentAppAndPluginUpdateControls() throws Exception {
        AtomicReference<AutoUpdateSettingsPanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            AutoUpdateSettingsPanel panel = new AutoUpdateSettingsPanel();
            panel.getPreferredSize();
            panelRef.set(panel);
        });

        List<String> texts = collectTexts(panelRef.get());

        assertTrue(texts.contains(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_ENABLED_CHECKBOX)));
        assertTrue(texts.contains(I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_ENABLED_CHECKBOX)));
        assertTrue(texts.contains(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_FREQUENCY)));
        assertTrue(texts.contains(I18nUtil.getMessage(MessageKeys.SETTINGS_PLUGIN_UPDATE_FREQUENCY)));
        assertEquals(countComponents(panelRef.get(), JComboBox.class), 3);
    }

    @Test
    public void shouldReadPluginUpdateStateThroughUnifiedUpdateCenter() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/AutoUpdateSettingsPanel.java"
        ));

        assertTrue(source.contains("UpdateCenter"));
        assertTrue(!source.contains("PluginUpdateManager"));
    }

    private static List<String> collectTexts(Container container) {
        List<String> texts = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText() != null) {
                texts.add(label.getText());
            } else if (component instanceof AbstractButton button && button.getText() != null) {
                texts.add(button.getText());
            }
            if (component instanceof Container child) {
                texts.addAll(collectTexts(child));
            }
        }
        return texts;
    }

    private static int countComponents(Container container, Class<? extends JComponent> componentType) {
        int count = 0;
        for (Component component : container.getComponents()) {
            if (componentType.isInstance(component)) {
                count++;
            }
            if (component instanceof Container child) {
                count += countComponents(child, componentType);
            }
        }
        return count;
    }
}
