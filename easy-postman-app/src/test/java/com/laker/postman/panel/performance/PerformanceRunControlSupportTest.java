package com.laker.postman.panel.performance;

import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.panel.performance.control.PerformanceRunUiController;
import com.laker.postman.panel.performance.control.PerformanceStatisticsCoordinator;
import com.laker.postman.panel.performance.control.PerformanceTimerManager;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.WhilePropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.model.PerformanceStatsCollectorListener;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;
import com.laker.postman.performance.model.PerformanceTrendWindowCollectorListener;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.performance.result.PerformanceResultCollector;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceResultTableVisualizer;
import com.laker.postman.performance.runtime.PerformanceExecutionEngine;
import com.laker.postman.performance.runtime.PerformanceRunSession;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

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
            PerformanceExecutionEngine executionEngine = new PerformanceExecutionEngine(
                    running::get,
                    () -> false,
                    () -> 64,
                    new PerformanceResultCollector(List.of(
                            new PerformanceStatsCollectorListener(statsCollector),
                            new PerformanceTrendWindowCollectorListener(trendWindowCollector),
                            new PerformanceResultTableVisualizer(resultTablePanel, () -> 500)
                    ))
            );
            PerformanceRunControlSupport runControlSupport = new PerformanceRunControlSupport(
                    new JPanel(),
                    running::get,
                    running::set,
                    startTime::get,
                    startTime::set,
                    newNoSelectionPropertyPanelSupport(),
                    executionEngine,
                    new PerformanceRunSession(running::get, running::set, executionEngine),
                    statisticsCoordinator,
                    timerManager,
                    new PerformanceRunUiController(new StartButton(), new StopButton(), new RefreshButton()),
                    new JCheckBox(),
                    resultTablePanel,
                    statsCollector,
                    () -> {
                    }
            );

            Thread runThread = runControlSupport.startRun(
                    new DefaultMutableTreeNode(new PerformanceTreeNode("测试计划", NodeType.ROOT)),
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

    @Test
    public void runControlSupportShouldNotMutateGlobalOkHttpClientManager() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/performance/PerformanceRunControlSupport.java"
        ));

        assertFalse(source.contains("OkHttpClientManager.set"));
    }

    @Test
    public void finishRunShouldRefreshRequestLimitStatusWithFinalStats() throws Exception {
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
            PerformanceExecutionEngine executionEngine = new PerformanceExecutionEngine(
                    running::get,
                    () -> true,
                    () -> 64,
                    new PerformanceResultCollector(List.of(
                            new PerformanceStatsCollectorListener(statsCollector),
                            new PerformanceTrendWindowCollectorListener(trendWindowCollector),
                            new PerformanceResultTableVisualizer(resultTablePanel, () -> 500)
                    ))
            );
            PerformanceRunControlSupport runControlSupport = new PerformanceRunControlSupport(
                    new JPanel(),
                    running::get,
                    running::set,
                    startTime::get,
                    startTime::set,
                    newNoSelectionPropertyPanelSupport(),
                    executionEngine,
                    new PerformanceRunSession(running::get, running::set, executionEngine),
                    statisticsCoordinator,
                    timerManager,
                    new PerformanceRunUiController(new StartButton(), new StopButton(), new RefreshButton()),
                    new JCheckBox(),
                    resultTablePanel,
                    statsCollector,
                    () -> {
                    }
            );
            JLabel limitLabel = new JLabel();

            Thread runThread = runControlSupport.startRun(
                    rootWithSingleFailingRequest(),
                    new JLabel(),
                    limitLabel,
                    true,
                    ignored -> {
                    }
            );

            if (runThread != null) {
                runThread.join(5000);
            }
            SwingUtilities.invokeAndWait(() -> {
            });

            assertEquals(limitLabel.getText(), "1/1");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                timerManager.stopAll();
                timerManager.dispose();
                statisticsCoordinator.dispose();
                resultTablePanel.dispose();
            });
        }
    }

    @Test
    public void finishRunShouldShowDynamicRequestLimitForConditionPlan() throws Exception {
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
            PerformanceExecutionEngine executionEngine = new PerformanceExecutionEngine(
                    running::get,
                    () -> true,
                    () -> 64,
                    new PerformanceResultCollector(List.of(
                            new PerformanceStatsCollectorListener(statsCollector),
                            new PerformanceTrendWindowCollectorListener(trendWindowCollector),
                            new PerformanceResultTableVisualizer(resultTablePanel, () -> 500)
                    ))
            );
            PerformanceRunControlSupport runControlSupport = new PerformanceRunControlSupport(
                    new JPanel(),
                    running::get,
                    running::set,
                    startTime::get,
                    startTime::set,
                    newNoSelectionPropertyPanelSupport(),
                    executionEngine,
                    new PerformanceRunSession(running::get, running::set, executionEngine),
                    statisticsCoordinator,
                    timerManager,
                    new PerformanceRunUiController(new StartButton(), new StopButton(), new RefreshButton()),
                    new JCheckBox(),
                    resultTablePanel,
                    statsCollector,
                    () -> {
                    }
            );
            JLabel limitLabel = new JLabel();

            Thread runThread = runControlSupport.startRun(
                    rootWithFalseConditionRequest(),
                    new JLabel(),
                    limitLabel,
                    true,
                    ignored -> {
                    }
            );

            if (runThread != null) {
                runThread.join(5000);
            }
            SwingUtilities.invokeAndWait(() -> {
            });

            assertEquals(
                    limitLabel.getText(),
                    "0/" + I18nUtil.getMessage(MessageKeys.PERFORMANCE_RUN_STATUS_DYNAMIC)
            );
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                timerManager.stopAll();
                timerManager.dispose();
                statisticsCoordinator.dispose();
                resultTablePanel.dispose();
            });
        }
    }

    @Test
    public void stopRunShouldKeepUiInStoppingStateUntilRunCompletionCallback() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/performance/PerformanceRunControlSupport.java"
        ));
        String stopRunBody = source.substring(source.indexOf("void stopRun()"), source.indexOf("private void showRunError"));

        assertFalse(stopRunBody.contains("markIdle()"));
    }

    @Test
    public void formatRunDurationShouldUseCompactClockText() {
        assertEquals(PerformanceRunControlSupport.formatRunDuration(0), "00:00");
        assertEquals(PerformanceRunControlSupport.formatRunDuration(999), "00:00");
        assertEquals(PerformanceRunControlSupport.formatRunDuration(23_000), "00:23");
        assertEquals(PerformanceRunControlSupport.formatRunDuration(60_000), "01:00");
        assertEquals(PerformanceRunControlSupport.formatRunDuration(3_600_000), "01:00:00");
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
                new CsvDataSetPropertyPanel(),
                null,
                null,
                new WhilePropertyPanel(),
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

    private static DefaultMutableTreeNode rootWithSingleFailingRequest() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("测试计划", NodeType.ROOT));
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new PerformanceTreeNode("用户组", NodeType.THREAD_GROUP, threadGroupData)
        );
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-1");
        request.setName("request");
        request.setMethod("GET");
        request.setUrl("http://127.0.0.1:1");
        request.setProtocol(RequestItemProtocolEnum.HTTP);
        group.add(new DefaultMutableTreeNode(new PerformanceTreeNode("request", NodeType.REQUEST, request)));
        root.add(group);
        return root;
    }

    private static DefaultMutableTreeNode rootWithFalseConditionRequest() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("测试计划", NodeType.ROOT));
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new PerformanceTreeNode("用户组", NodeType.THREAD_GROUP, threadGroupData)
        );
        ConditionData conditionData = new ConditionData();
        conditionData.expression = "false";
        DefaultMutableTreeNode condition = new DefaultMutableTreeNode(
                new PerformanceTreeNode("Condition", NodeType.CONDITION, conditionData)
        );
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-1");
        request.setName("request");
        request.setMethod("GET");
        request.setUrl("http://127.0.0.1:1");
        request.setProtocol(RequestItemProtocolEnum.HTTP);
        condition.add(new DefaultMutableTreeNode(new PerformanceTreeNode("request", NodeType.REQUEST, request)));
        group.add(condition);
        root.add(group);
        return root;
    }
}
