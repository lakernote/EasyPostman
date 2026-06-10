package com.laker.postman.panel.workspace;

import com.laker.postman.common.component.AppToolWindowChrome;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class WorkspacePanelTest {

    @Test
    public void defaultDetailDividerShouldNotOverstretchDetailAreaInTallWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                1400,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 400);
    }

    @Test
    public void defaultDetailDividerShouldGiveDetailAreaReadableSpaceInLargeWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                1000,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 360);
    }

    @Test
    public void defaultDetailDividerShouldKeepDetailAreaReadableInMediumWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                700,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 360);
    }

    @Test
    public void defaultDetailDividerShouldPreserveLogAreaWhenWindowIsShort() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                520,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 335);
    }
}
