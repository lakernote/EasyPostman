package com.laker.postman.plugin.kafka.connection.ui;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.laker.postman.common.component.EasyComboBox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
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
