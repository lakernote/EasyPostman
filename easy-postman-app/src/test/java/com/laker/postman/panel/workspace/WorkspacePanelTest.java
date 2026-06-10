package com.laker.postman.panel.workspace;

import com.laker.postman.common.component.ToolWindowChrome;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class WorkspacePanelTest {

    @Test
    public void defaultDetailDividerShouldGiveDetailAreaReadableSpaceInTallWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                1000,
                ToolWindowChrome.INNER_DIVIDER_SIZE
        ), 446);
    }

    @Test
    public void defaultDetailDividerShouldKeepDetailAreaReadableInMediumWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                700,
                ToolWindowChrome.INNER_DIVIDER_SIZE
        ), 380);
    }

    @Test
    public void defaultDetailDividerShouldPreserveLogAreaWhenWindowIsShort() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                520,
                ToolWindowChrome.INNER_DIVIDER_SIZE
        ), 332);
    }
}
