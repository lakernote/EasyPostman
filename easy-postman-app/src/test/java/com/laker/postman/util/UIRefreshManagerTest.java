package com.laker.postman.util;

import com.laker.postman.common.IRefreshable;
import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;

import static org.testng.Assert.assertEquals;
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

    @Test
    public void languageRefreshShouldNotReinstallComponentUiDelegates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("language-refresh-test");
            CountingRefreshablePanel panel = new CountingRefreshablePanel();

            try {
                frame.setContentPane(panel);
                frame.pack();
                panel.resetCounters();

                UIRefreshManager.refreshLanguage();

                assertEquals(panel.getRefreshCount(), 1);
                assertEquals(panel.getUpdateUiCount(), 0);
            } finally {
                frame.dispose();
            }
        });
    }

    private static class CountingRefreshablePanel extends JPanel implements IRefreshable {
        private int refreshCount;
        private int updateUiCount;

        @Override
        public void refresh() {
            refreshCount++;
        }

        @Override
        public void updateUI() {
            super.updateUI();
            updateUiCount++;
        }

        void resetCounters() {
            refreshCount = 0;
            updateUiCount = 0;
        }

        int getRefreshCount() {
            return refreshCount;
        }

        int getUpdateUiCount() {
            return updateUiCount;
        }
    }
}
