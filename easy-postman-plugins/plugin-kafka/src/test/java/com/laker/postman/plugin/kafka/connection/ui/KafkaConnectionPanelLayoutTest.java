package com.laker.postman.plugin.kafka.connection.ui;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static com.laker.postman.plugin.kafka.KafkaI18n.t;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.connection.ConnectionToolbarUi;
import com.laker.postman.plugin.kafka.MessageKeys;
import com.laker.postman.util.I18nUtil;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.testng.annotations.Test;

public class KafkaConnectionPanelLayoutTest {

    @Test
    public void securityProtocolComboShouldUseFixedMaxEasyComboBox() {
        KafkaConnectionPanel panel = new KafkaConnectionPanel(() -> {
        }, () -> {
        });

        assertTrue(panel.securityProtocolCombo instanceof EasyComboBox<?>,
                "security protocol selector should use EasyComboBox to preserve longest option width");
        assertFixedMaxWidth(panel.securityProtocolCombo);
    }

    @Test
    public void saslMechanismComboShouldUseFixedMaxEasyComboBox() {
        KafkaConnectionPanel panel = new KafkaConnectionPanel(() -> {
        }, () -> {
        });

        assertTrue(panel.saslMechanismCombo instanceof EasyComboBox<?>,
                "SASL mechanism selector should use EasyComboBox to preserve longest option width");
        assertFixedMaxWidth(panel.saslMechanismCombo);
    }

    @Test
    public void optionsRowShouldStayNearContentWidthInWideContainer() {
        KafkaConnectionPanel panel = new KafkaConnectionPanel(() -> {
        }, () -> {
        });
        panel.setSize(new Dimension(1800, 90));
        layoutRecursively(panel);

        assertTrue(panel.optionsRow.getWidth() > 0, "options row should be laid out");
        assertTrue(panel.optionsRow.getWidth() <= panel.optionsRow.getPreferredSize().width + 16,
                "options row width " + panel.optionsRow.getWidth()
                        + " should stay near preferred width " + panel.optionsRow.getPreferredSize().width);
    }

    @Test
    public void connectionActionsShouldUseIconOnlyToolbarButtonsWithTooltips() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertConnectionActionsUseIconOnlyButtons(Locale.CHINESE);
            assertConnectionActionsUseIconOnlyButtons(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void connectionRowsShouldAlignAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertConnectionRowsAlign(Locale.CHINESE);
            assertConnectionRowsAlign(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void securityLabelsShouldUseShortTextWithFullTooltipsAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertSecurityLabelsUseShortTextWithFullTooltips(Locale.CHINESE, "协议:", "SASL:");
            assertSecurityLabelsUseShortTextWithFullTooltips(Locale.ENGLISH, "Proto:", "SASL:");
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static void assertConnectionActionsUseIconOnlyButtons(Locale locale) {
        I18nUtil.setLocale(locale);
        KafkaConnectionPanel panel = new KafkaConnectionPanel(() -> {
        }, () -> {
        });
        panel.setSize(new Dimension(1320, 90));
        layoutRecursively(panel);

        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_KAFKA_CONNECT));
        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT));
    }

    private static void assertConnectionRowsAlign(Locale locale) {
        I18nUtil.setLocale(locale);
        KafkaConnectionPanel panel = new KafkaConnectionPanel(() -> {
        }, () -> {
        });
        panel.setSize(new Dimension(1800, 90));
        layoutRecursively(panel);

        assertEquals(panel.bootstrapField.getX(), panel.clientIdField.getX(),
                locale + " Host and Client ID fields should start at the same x position");
        assertEquals(panel.bootstrapField.getWidth(), panel.clientIdField.getWidth(),
                locale + " Host and Client ID fields should use the same width");
        assertEquals(panel.securityProtocolCombo.getX(), panel.saslMechanismCombo.getX(),
                locale + " Protocol and SASL fields should start at the same x position");
        assertEquals(panel.securityProtocolCombo.getWidth(), panel.saslMechanismCombo.getWidth(),
                locale + " Protocol and SASL fields should use the same width");
        assertTrue(panel.usernameField.getX() > panel.saslMechanismCombo.getX() + panel.saslMechanismCombo.getWidth(),
                locale + " User field should remain to the right of the SASL selector");
        assertTrue(panel.passwordField.getX() > panel.usernameField.getX() + panel.usernameField.getWidth(),
                locale + " Password field should remain to the right of the User field");
    }

    private static void assertSecurityLabelsUseShortTextWithFullTooltips(Locale locale,
                                                                         String protocolText,
                                                                         String saslText) {
        I18nUtil.setLocale(locale);
        KafkaConnectionPanel panel = new KafkaConnectionPanel(() -> {
        }, () -> {
        });
        panel.setSize(new Dimension(1320, 90));
        layoutRecursively(panel);

        assertLabelWithTooltip(panel, protocolText, t(MessageKeys.TOOLBOX_KAFKA_SECURITY_PROTOCOL));
        assertLabelWithTooltip(panel, saslText, t(MessageKeys.TOOLBOX_KAFKA_SASL_MECHANISM));
    }

    private static void assertLabelWithTooltip(Component component, String text, String tooltip) {
        JLabel label = findLabelByText(component, text);
        assertNotNull(label, "label not found: " + text);
        assertEquals(label.getToolTipText(), tooltip, text + " should keep the full label in tooltip");
    }

    private static JLabel findLabelByText(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabelByText(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void assertIconOnlyToolbarButton(KafkaConnectionPanel panel, String tooltip) {
        JButton button = findButtonByTooltip(panel, tooltip);
        assertNotNull(button, "button not found: " + tooltip);
        assertTrue(button.getText() == null || button.getText().isBlank(),
                tooltip + " should use icon-only text");
        assertNotNull(button.getIcon(), tooltip + " should expose an SVG icon");
        assertTrue(FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON.equals(
                        button.getClientProperty(FlatClientProperties.BUTTON_TYPE)),
                tooltip + " should reuse the shared toolbar button style");
        Dimension expectedSize = new Dimension(ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE,
                ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE);
        assertEquals(button.getPreferredSize(), expectedSize,
                tooltip + " should use the shared toolbar button preferred size");
        assertEquals(button.getMinimumSize(), expectedSize,
                tooltip + " should use the shared toolbar button minimum size");
        assertEquals(button.getMaximumSize(), expectedSize,
                tooltip + " should use the shared toolbar button maximum size");
    }

    private static JButton findButtonByTooltip(Component component, String tooltip) {
        if (component instanceof JButton button && tooltip.equals(button.getToolTipText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButtonByTooltip(child, tooltip);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void assertFixedMaxWidth(JComboBox<String> comboBox) {
        int shortestIndex = 0;
        int longestIndex = 0;
        int shortestWidth = Integer.MAX_VALUE;
        int longestWidth = -1;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            int width = renderedWidth(comboBox, comboBox.getItemAt(i), i);
            if (width < shortestWidth) {
                shortestWidth = width;
                shortestIndex = i;
            }
            if (width > longestWidth) {
                longestWidth = width;
                longestIndex = i;
            }
        }

        comboBox.setSelectedIndex(shortestIndex);
        int compactSelectionWidth = comboBox.getPreferredSize().width;
        comboBox.setSelectedIndex(longestIndex);
        int longestSelectionWidth = comboBox.getPreferredSize().width;

        assertEquals(compactSelectionWidth, longestSelectionWidth,
                "fixed-max combo should not resize when selecting shorter localized values");
    }

    private static int renderedWidth(JComboBox<String> comboBox, String value, int index) {
        @SuppressWarnings("unchecked")
        ListCellRenderer<? super String> renderer = (ListCellRenderer<? super String>) comboBox.getRenderer();
        Component component = renderer.getListCellRendererComponent(new JList<>(), value, index, false, false);
        if (component instanceof JLabel label) {
            return label.getPreferredSize().width;
        }
        return component.getPreferredSize().width;
    }

    private static void layoutRecursively(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
