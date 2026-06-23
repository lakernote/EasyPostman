package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ResponseStatusBarTest extends AbstractSwingUiTest {

    @Test
    public void sizeLabelWithoutEventInfoShouldNotAppearClickable() {
        ResponseStatusBar statusBar = new ResponseStatusBar();

        statusBar.setResponseSize(492, null);

        JLabel sizeLabel = findLabel(statusBar, "492 B");
        assertNotNull(sizeLabel);
        assertEquals(sizeLabel.getCursor().getType(), Cursor.DEFAULT_CURSOR);
        assertEquals(sizeLabel.getToolTipText(), I18nUtil.getMessage("response.size.body.tooltip"));
    }

    private JLabel findLabel(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (child instanceof Container nested) {
                JLabel found = findLabel(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
