package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SyntaxEditorScrollPaneTest {
    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void rememberLookAndFeel() {
        previousLookAndFeel = UIManager.getLookAndFeel();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (previousLookAndFeel != null) {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    public void constructorShouldApplyEditorChrome() {
        FlatDarkLaf.setup();

        SyntaxEditorScrollPane scrollPane = new SyntaxEditorScrollPane(new RSyntaxTextArea());

        assertEquals(scrollPane.getViewport().getBackground(), new Color(0x3A, 0x3D, 0x3F));
        assertEquals(scrollPane.getVerticalScrollBarPolicy(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        assertEquals(scrollPane.getHorizontalScrollBarPolicy(), ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
    }
}
