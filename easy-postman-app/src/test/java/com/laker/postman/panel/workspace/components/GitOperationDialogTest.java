package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.model.GitOperation;
import org.testng.annotations.Test;

import javax.swing.JButton;
import java.awt.Color;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class GitOperationDialogTest {

    @Test
    public void executeButtonShouldUseOperationColoredPrimaryHoverStates() {
        JButton button = GitOperationDialog.createExecuteButton(GitOperation.COMMIT);
        Color baseColor = GitOperationPresentation.getColor(GitOperation.COMMIT);

        assertTrue(button instanceof PrimaryButton);
        assertEquals(button.getClientProperty("baseColor"), baseColor);
        assertNotEquals(button.getClientProperty("hoverColor"), baseColor);
        assertNotEquals(button.getClientProperty("pressColor"), baseColor);
        assertEquals(button.isContentAreaFilled(), false);
        assertEquals(button.isFocusPainted(), false);
    }
}
