package com.laker.postman.common.component;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class EasyComboBoxTest {

    @Test
    public void shouldUseCustomPreferredWidth() {
        EasyComboBox<String> comboBox = new EasyComboBox<>(new String[]{"GET", "POST"}, 180);

        assertEquals(comboBox.getPreferredSize().width, 180);
    }
}
