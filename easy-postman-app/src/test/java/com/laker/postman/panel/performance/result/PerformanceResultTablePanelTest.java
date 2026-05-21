package com.laker.postman.panel.performance.result;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.Container;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;

public class PerformanceResultTablePanelTest extends AbstractSwingUiTest {

    @Test
    public void flushPendingResultsShouldImmediatelyPublishRowsToTable() throws Exception {
        AtomicReference<PerformanceResultTablePanel> panelRef = new AtomicReference<>();
        AtomicInteger rowCount = new AtomicInteger(-1);

        SwingUtilities.invokeAndWait(() -> {
            PerformanceResultTablePanel panel = new PerformanceResultTablePanel();
            panelRef.set(panel);
            panel.addResult(sampleResult());
            panel.flushPendingResults();
            rowCount.set(findTable(panel).getRowCount());
        });

        try {
            assertEquals(rowCount.get(), 1);
        } finally {
            PerformanceResultTablePanel panel = panelRef.get();
            if (panel != null) {
                SwingUtilities.invokeAndWait(panel::dispose);
            }
        }
    }

    private static ResultNodeInfo sampleResult() {
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.costMs = 12;
        return new ResultNodeInfo(
                "Sample",
                "",
                new PreparedRequest(),
                response,
                List.of(),
                false
        );
    }

    private static JTable findTable(Container root) {
        try {
            java.lang.reflect.Field tableField = PerformanceResultTablePanel.class.getDeclaredField("table");
            tableField.setAccessible(true);
            return (JTable) tableField.get(root);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("JTable not found", e);
        }
    }
}
