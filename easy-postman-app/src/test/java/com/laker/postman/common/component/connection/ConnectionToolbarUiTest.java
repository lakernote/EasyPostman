package com.laker.postman.common.component.connection;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import com.laker.postman.common.component.EasyComboBox;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;

import org.testng.annotations.Test;

public class ConnectionToolbarUiTest {

    private enum TestMode {
        SHORT,
        LONG
    }

    @Test
    public void compactButtonShouldNotShrinkBelowNaturalTextWidth() {
        JButton button = new JButton("Reload Metadata");
        button.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
        Dimension naturalSize = button.getPreferredSize();

        ConnectionToolbarUi.compactButton(button, 88);

        assertTrue(
                button.getPreferredSize().width >= naturalSize.width,
                "button preferred width " + button.getPreferredSize().width
                        + " is smaller than natural text width " + naturalSize.width
        );
    }

    @Test
    public void comboBoxShouldUseFixedMaxEasyComboBox() {
        JComboBox<TestMode> comboBox = ConnectionToolbarUi.comboBox(TestMode.values(),
                value -> value == TestMode.LONG ? "Very Long Display Name" : "No");

        assertTrue(comboBox instanceof EasyComboBox<?>,
                "connection toolbar combo boxes should use EasyComboBox fixed-max sizing");

        comboBox.setSelectedItem(TestMode.SHORT);
        int compactSelectionWidth = comboBox.getPreferredSize().width;
        comboBox.setSelectedItem(TestMode.LONG);
        int longestSelectionWidth = comboBox.getPreferredSize().width;

        assertEquals(compactSelectionWidth, longestSelectionWidth,
                "fixed-max combo should not resize when selecting shorter display names");
    }
}
