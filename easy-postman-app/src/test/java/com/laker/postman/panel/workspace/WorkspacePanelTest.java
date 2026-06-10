package com.laker.postman.panel.workspace;

import com.laker.postman.common.component.AppToolWindowChrome;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class WorkspacePanelTest {

    @Test
    public void defaultDetailDividerShouldGiveDetailAreaReadableSpaceInTallWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                1000,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 448);
    }

    @Test
    public void defaultDetailDividerShouldKeepDetailAreaReadableInMediumWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                700,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 380);
    }

    @Test
    public void defaultDetailDividerShouldPreserveLogAreaWhenWindowIsShort() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                520,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 335);
    }
}
