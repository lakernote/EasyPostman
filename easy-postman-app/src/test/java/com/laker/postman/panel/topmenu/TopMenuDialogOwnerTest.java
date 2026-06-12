package com.laker.postman.panel.topmenu;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static org.testng.Assert.assertSame;

public class TopMenuDialogOwnerTest extends AbstractSwingUiTest {

    @Test
    public void shouldResolveTopLevelWindowForMenuChildComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("owner-test");
            try {
                JPanel child = new JPanel();
                frame.add(child);
                frame.pack();

                assertSame(TopMenuDialogOwner.resolve(child), frame);
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void shouldKeepUnattachedComponentAsFallbackOwner() {
        JPanel panel = new JPanel();

        assertSame(TopMenuDialogOwner.resolve(panel), panel);
    }
}
