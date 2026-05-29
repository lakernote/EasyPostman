package com.laker.postman.util;

import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;

import static org.testng.Assert.assertNotEquals;

public class UIRefreshManagerTest extends AbstractSwingUiTest {

    @Test
    public void shouldRefreshEditorThemesInComponentTree() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel root = new JPanel(new BorderLayout());
            RSyntaxTextArea area = new RSyntaxTextArea();
            area.setBackground(Color.MAGENTA);
            root.add(new JScrollPane(area), BorderLayout.CENTER);

            UIRefreshManager.refreshEditorThemes(root);

            assertNotEquals(area.getBackground(), Color.MAGENTA);
        });
    }
}
