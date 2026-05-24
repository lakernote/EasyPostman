package com.laker.postman.panel.performance.result;

import com.laker.postman.common.UiSingletonFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class PerformanceTrendPanelRetentionTest {

    @Test
    public void shouldLimitTrendSeriesLengthForLongRunningTests() throws Exception {
        runOnEdtAndWait(() -> {
            PerformanceTrendPanel panel = UiSingletonFactory.getInstance(PerformanceTrendPanel.class);
            panel.clearTrendDataset();
            ChartPanel chartPanel = findFirst(panel, ChartPanel.class);
            assertNotNull(chartPanel);
            TimeSeriesCollection dataset = (TimeSeriesCollection) chartPanel.getChart().getXYPlot().getDataset();
            TimeSeries series = dataset.getSeries(0);

            long base = System.currentTimeMillis();
            for (int i = 0; i < 3_605; i++) {
                series.add(new Millisecond(new Date(base + i)), i);
            }

            assertEquals(series.getMaximumItemCount(), 3_600);
            assertEquals(series.getItemCount(), 3_600);
        });
    }

    private static <T extends Component> T findFirst(Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = findFirst(child, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void runOnEdtAndWait(ThrowingRunnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
