package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.component.EasyPasswordField;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ProxySettingsPanelModernTest extends AbstractSwingUiTest {

    @Test
    public void shouldUseRevealablePasswordFieldAndOnlyShowSystemPreviewInSystemMode() throws Exception {
        boolean oldProxyEnabled = SettingManager.isProxyEnabled();
        String oldProxyMode = SettingManager.getProxyMode();

        try {
            SettingManager.setProxyEnabled(true);
            SettingManager.setProxyMode(SettingManager.PROXY_MODE_MANUAL);

            AtomicReference<ProxySettingsPanelModern> panelRef = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                ProxySettingsPanelModern panel = new ProxySettingsPanelModern();
                panel.getPreferredSize();
                panelRef.set(panel);
            });

            ProxySettingsPanelModern panel = panelRef.get();
            assertNotNull(findFirstComponent(panel, EasyPasswordField.class));

            JLabel previewLabel = findLabel(
                    panel,
                    I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PREVIEW_TARGET)
            );
            assertNotNull(previewLabel);
            assertFalse(isEffectivelyVisible(previewLabel, panel));

            JComboBox<?> modeComboBox = findComboBoxContaining(
                    panel,
                    I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_MODE_SYSTEM)
            );
            assertNotNull(modeComboBox);
            SwingUtilities.invokeAndWait(() -> modeComboBox.setSelectedItem(
                    I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_MODE_SYSTEM)
            ));

            assertTrue(isEffectivelyVisible(previewLabel, panel));
        } finally {
            SettingManager.setProxyEnabled(oldProxyEnabled);
            SettingManager.setProxyMode(oldProxyMode);
        }
    }

    private static <T extends JComponent> T findFirstComponent(Container container, Class<T> componentType) {
        for (Component component : container.getComponents()) {
            if (componentType.isInstance(component)) {
                return componentType.cast(component);
            }
            if (component instanceof Container child) {
                T found = findFirstComponent(child, componentType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel found = findLabel(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JComboBox<?> findComboBoxContaining(Container container, String itemText) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox<?> comboBox && containsItem(comboBox, itemText)) {
                return comboBox;
            }
            if (component instanceof Container child) {
                JComboBox<?> found = findComboBoxContaining(child, itemText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean containsItem(JComboBox<?> comboBox, String itemText) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Object item = comboBox.getItemAt(i);
            if (itemText.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEffectivelyVisible(Component component, Component stopAt) {
        Component current = component;
        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }
            if (current == stopAt) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
