package com.laker.postman.panel.performance;

import com.laker.postman.common.UiSingletonPanel;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformancePanelReportRefreshModeTest {

    @Test
    public void shouldOnlyAllowManualReportSnapshotDuringRunWhenRealtimeReportIsEnabled() throws Exception {
        PerformancePanel panel = newPanelWithoutInit();

        setField(panel, "running", true);
        setField(panel, "reportRealtimeEnabled", false);
        assertFalse(panel.shouldRefreshReportSnapshotNow());

        setField(panel, "reportRealtimeEnabled", true);
        assertTrue(panel.shouldRefreshReportSnapshotNow());

        setField(panel, "running", false);
        setField(panel, "reportRealtimeEnabled", false);
        assertTrue(panel.shouldRefreshReportSnapshotNow());
    }

    private static PerformancePanel newPanelWithoutInit() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new PerformancePanel();
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
