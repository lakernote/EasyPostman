package com.laker.postman.panel.performance;

import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.panel.performance.control.PerformanceRunUiController;
import com.laker.postman.panel.performance.control.PerformanceStatisticsCoordinator;
import com.laker.postman.panel.performance.control.PerformanceTimerManager;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.PerformanceStatsCollectorListener;
import com.laker.postman.panel.performance.model.PerformanceTrendWindowCollector;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceResultTableVisualizer;
import com.laker.postman.panel.performance.runtime.PerformanceExecutionEngine;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;

public class PerformanceRunControlSupportTest extends AbstractSwingUiTest {

    @Test
    public void startRunShouldKeepCurrentResultTabOnTrendView() throws Exception {
        AtomicBoolean running = new AtomicBoolean(false);
        AtomicLong startTime = new AtomicLong();
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        PerformanceTrendWindowCollector trendWindowCollector = new PerformanceTrendWindowCollector();
        PerformanceResultTablePanel resultTablePanel = new PerformanceResultTablePanel();
        PerformanceReportPanel reportPanel = new PerformanceReportPanel();
        JTabbedPane resultTabbedPane = createResultTabbedPane(resultTablePanel, reportPanel);
        PerformanceTimerManager timerManager = new PerformanceTimerManager(running::get);
        PerformanceStatisticsCoordinator statisticsCoordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                trendWindowCollector,
                reportPanel,
                null,
                resultTabbedPane,
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                () -> false,
                now -> PerformanceRealtimeMetrics.Sample.empty(),
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );

        try {
            PerformanceRunControlSupport runControlSupport = new PerformanceRunControlSupport(
                    new JPanel(),
                    running::get,
                    running::set,
                    startTime::get,
                    startTime::set,
                    newNoSelectionPropertyPanelSupport(),
                    new PerformanceExecutionEngine(
                            new JPanel(),
                            running::get,
                            () -> false,
                            () -> 64,
                            null,
                            new PerformanceResultCollector(List.of(
                                    new PerformanceStatsCollectorListener(statsCollector),
                                    trendWindowCollector,
                                    new PerformanceResultTableVisualizer(resultTablePanel, () -> 500)
                            ))
                    ),
                    statisticsCoordinator,
                    timerManager,
                    new PerformanceRunUiController(new StartButton(), new StopButton(), new RefreshButton()),
                    new JCheckBox(),
                    resultTabbedPane,
                    resultTablePanel,
                    reportPanel,
                    null,
                    statsCollector,
                    () -> {
                    }
            );

            Thread runThread = runControlSupport.startRun(
                    new DefaultMutableTreeNode(new JMeterTreeNode("测试计划", NodeType.ROOT)),
                    new JLabel(),
                    false,
                    ignored -> {
                    }
            );

            assertEquals(resultTabbedPane.getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TREND);
            if (runThread != null) {
                runThread.join(1000);
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                timerManager.stopAll();
                timerManager.dispose();
                statisticsCoordinator.dispose();
                resultTablePanel.dispose();
            });
        }
    }

    private static JTabbedPane createResultTabbedPane(PerformanceResultTablePanel resultTablePanel,
                                                     PerformanceReportPanel reportPanel) {
        JTabbedPane resultTabbedPane = new JTabbedPane();
        resultTabbedPane.addTab("趋势", new JPanel());
        resultTabbedPane.addTab("报表", reportPanel);
        resultTabbedPane.addTab("结果表", resultTablePanel);
        resultTabbedPane.setSelectedIndex(PerformancePanelViewFactory.RESULT_TAB_TREND);
        return resultTabbedPane;
    }

    private static PerformancePropertyPanelSupport newNoSelectionPropertyPanelSupport() {
        return new PerformancePropertyPanelSupport(
                new JTree(),
                null,
                null,
                null,
                new ExtractorPropertyPanel(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                () -> null,
                () -> null,
                ignored -> {
                },
                null,
                (node, treeNode) -> {
                }
        );
    }
}
