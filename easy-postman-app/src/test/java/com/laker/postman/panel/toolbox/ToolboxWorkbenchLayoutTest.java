package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowChrome;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JSplitPane;

import static org.testng.Assert.assertEquals;

public class ToolboxWorkbenchLayoutTest {

    @Test
    public void horizontalEditorSplitShouldHideDividerLineButKeepFourPixelDragTarget() {
        JSplitPane splitPane = ToolboxWorkbench.horizontalSplit(
                new JLabel("left"),
                new JLabel("right"),
                360
        );

        assertEquals(splitPane.getDividerLocation(), 360);
        assertEquals(splitPane.getDividerSize(), ToolWindowChrome.DIVIDER_SIZE);
        assertEquals(splitPane.getResizeWeight(), 0.5);
    }

    @Test
    public void verticalEditorSplitShouldKeepAppDragGapChrome() {
        JSplitPane splitPane = ToolboxWorkbench.editorSplit(
                new JLabel("top"),
                new JLabel("bottom"),
                250
        );

        assertEquals(splitPane.getDividerLocation(), 250);
        assertEquals(splitPane.getDividerSize(), AppToolWindowChrome.DIVIDER_SIZE);
        assertEquals(splitPane.getResizeWeight(), 0.5);
    }
}
