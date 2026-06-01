package com.laker.postman.panel.performance;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.plan.PerformancePlanWorkspace;
import com.laker.postman.performance.plan.PerformanceRemoteWorkerSettings;
import com.laker.postman.panel.performance.tree.PerformanceSwingTreePlanAdapter;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.PerformancePersistenceService;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.Timer;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformancePanelSaveTest extends AbstractSwingUiTest {

    @Test(description = "显式保存性能配置时应先提交仍在编辑中的 spinner 文本")
    public void shouldCommitEditedSpinnerTextBeforeExplicitSave() throws Exception {
        runOnEdtAndWait(() -> {
            PerformancePanel panel = newPanelWithoutInit();
            ThreadGroupData threadGroupData = new ThreadGroupData();
            threadGroupData.numThreads = 1;
            PerformanceTreeNode threadGroupTreeNode = new PerformanceTreeNode("Thread Group", NodeType.THREAD_GROUP, threadGroupData);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
            DefaultMutableTreeNode threadGroupNode = new DefaultMutableTreeNode(threadGroupTreeNode);
            root.add(threadGroupNode);
            DefaultTreeModel treeModel = new DefaultTreeModel(root);
            JTree performanceTree = new JTree(treeModel);
            performanceTree.setSelectionPath(new TreePath(threadGroupNode.getPath()));

            ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
            threadGroupPanel.setThreadGroupData(threadGroupTreeNode);
            EasyJSpinner numThreadsSpinner = getSpinner(threadGroupPanel, "fixedNumThreadsSpinner");
            setEditorText(numThreadsSpinner, "7");
            assertEquals(numThreadsSpinner.getValue(), 1);

            RecordingPerformancePersistenceService persistenceService = new RecordingPerformancePersistenceService();
            setField(panel, "treeModel", treeModel);
            setField(panel, "persistenceService", persistenceService);
            setField(panel, "propertyPanelSupport", createPropertyPanelSupport(performanceTree, treeModel, threadGroupPanel));

            panel.save();

            assertEquals(threadGroupData.numThreads, 7);
            assertEquals(persistenceService.saveCount.get(), 1);
            DefaultMutableTreeNode savedGroup = (DefaultMutableTreeNode) persistenceService.savedRoot.getChildAt(0);
            ThreadGroupData savedGroupData = ((PerformanceTreeNode) savedGroup.getUserObject()).threadGroupData;
            assertEquals(savedGroupData.numThreads, 7);
            assertFalse(persistenceService.savedRemoteWorkerSettings.isEnabled());
        });
    }

    @Test(description = "启动压测前应先提交仍在编辑中的线程组 spinner 文本")
    public void saveThreadGroupDataShouldCommitEditedSpinnerText() throws Exception {
        runOnEdtAndWait(() -> {
            ThreadGroupData threadGroupData = new ThreadGroupData();
            threadGroupData.numThreads = 1;
            PerformanceTreeNode threadGroupTreeNode = new PerformanceTreeNode("Thread Group", NodeType.THREAD_GROUP, threadGroupData);

            ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
            threadGroupPanel.setThreadGroupData(threadGroupTreeNode);
            EasyJSpinner numThreadsSpinner = getSpinner(threadGroupPanel, "fixedNumThreadsSpinner");
            setEditorText(numThreadsSpinner, "7");
            assertEquals(numThreadsSpinner.getValue(), 1);

            threadGroupPanel.saveThreadGroupData();

            assertEquals(threadGroupData.numThreads, 7);
        });
    }

    @Test(description = "线程组固定用户数不应被固定上限截断")
    public void threadGroupFixedUsersShouldAcceptLargeUserInput() throws Exception {
        runOnEdtAndWait(() -> {
            ThreadGroupData threadGroupData = new ThreadGroupData();
            threadGroupData.numThreads = 1;
            PerformanceTreeNode threadGroupTreeNode = new PerformanceTreeNode("Thread Group", NodeType.THREAD_GROUP, threadGroupData);

            ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
            threadGroupPanel.setThreadGroupData(threadGroupTreeNode);
            EasyJSpinner numThreadsSpinner = getSpinner(threadGroupPanel, "fixedNumThreadsSpinner");
            setEditorText(numThreadsSpinner, "123456");

            threadGroupPanel.forceCommitAllSpinners();
            threadGroupPanel.saveThreadGroupData();

            assertEquals(threadGroupData.numThreads, 123456);
        });
    }

    @Test(description = "阶梯预览应使用正在编辑但尚未提交的 spinner 文本")
    public void stairsPreviewShouldUseEditedSpinnerTextBeforeCommit() throws Exception {
        runOnEdtAndWait(() -> {
            ThreadGroupData threadGroupData = new ThreadGroupData();
            threadGroupData.threadMode = ThreadGroupData.ThreadMode.STAIRS;
            threadGroupData.stairsStartThreads = 200;
            threadGroupData.stairsEndThreads = 2_000;
            threadGroupData.stairsStep = 5;
            threadGroupData.stairsHoldTime = 5;
            threadGroupData.stairsDuration = 60;
            PerformanceTreeNode threadGroupTreeNode = new PerformanceTreeNode("Thread Group", NodeType.THREAD_GROUP, threadGroupData);

            ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
            threadGroupPanel.setThreadGroupData(threadGroupTreeNode);
            EasyJSpinner stairsStepSpinner = getSpinner(threadGroupPanel, "stairsStepSpinner");
            setEditorText(stairsStepSpinner, "200");
            assertEquals(stairsStepSpinner.getValue(), 5);

            invokeUpdatePreview(threadGroupPanel);

            Object previewData = getPreviewData(threadGroupPanel);
            assertEquals((Integer) getField(previewData, "stairsStep"), 200);
        });
    }

    @Test(description = "PerformancePanel cleanup 应释放结果表的 Swing Timer")
    public void cleanupShouldDisposeResultTablePanelTimer() throws Exception {
        runOnEdtAndWait(() -> {
            PerformancePanel panel = newPanelWithoutInit();
            PerformanceResultTablePanel resultTablePanel = new PerformanceResultTablePanel();
            setField(panel, "performanceResultTablePanel", resultTablePanel);

            panel.cleanup();

            assertFalse(getTimer(resultTablePanel, "uiFrameTimer").isRunning());
        });
    }

    private static PerformancePanel newPanelWithoutInit() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new PerformancePanel();
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static PerformancePropertyPanelSupport createPropertyPanelSupport(JTree performanceTree,
                                                                              DefaultTreeModel treeModel,
                                                                              ThreadGroupPropertyPanel threadGroupPanel) throws Exception {
        return new PerformancePropertyPanelSupport(
                performanceTree,
                threadGroupPanel,
                new CsvDataSetPropertyPanel(),
                headlessLoopPanel(),
                new AssertionPropertyPanel(),
                new ExtractorPropertyPanel(),
                new TimerPropertyPanel(),
                headlessSseStagePanel(),
                headlessSseStagePanel(),
                headlessWebSocketStagePanel(),
                headlessWebSocketStagePanel(),
                headlessWebSocketStagePanel(),
                headlessWebSocketStagePanel(),
                () -> null,
                () -> null,
                node -> {
                },
                new PerformanceTreeSupport(treeModel),
                (node, data) -> {
                }
        );
    }

    private static SseStagePropertyPanel headlessSseStagePanel() throws Exception {
        SseStagePropertyPanel panel = allocateWithoutConstructor(SseStagePropertyPanel.class);
        setField(panel, "connectTimeoutSpinner", spinner());
        setField(panel, "readTimeoutSpinner", spinner());
        setField(panel, "holdConnectionSpinner", spinner());
        setField(panel, "targetMessageCountSpinner", spinner());
        return panel;
    }

    private static LoopPropertyPanel headlessLoopPanel() throws Exception {
        LoopPropertyPanel panel = allocateWithoutConstructor(LoopPropertyPanel.class);
        setField(panel, "iterationsSpinner", spinner());
        return panel;
    }

    private static WebSocketStagePropertyPanel headlessWebSocketStagePanel() throws Exception {
        WebSocketStagePropertyPanel panel = allocateWithoutConstructor(WebSocketStagePropertyPanel.class);
        setField(panel, "connectTimeoutSpinner", spinner());
        setField(panel, "sendCountSpinner", spinner());
        setField(panel, "sendIntervalSpinner", spinner());
        setField(panel, "readTimeoutSpinner", spinner());
        setField(panel, "holdConnectionSpinner", spinner());
        setField(panel, "targetMessageCountSpinner", spinner());
        return panel;
    }

    private static EasyJSpinner spinner() {
        return new EasyJSpinner(new SpinnerNumberModel(1, 0, 1_000_000, 1));
    }

    private static <T> T allocateWithoutConstructor(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return type.cast(allocateInstance.invoke(unsafe, type));
    }

    private static EasyJSpinner getSpinner(ThreadGroupPropertyPanel panel, String fieldName) throws Exception {
        Field field = ThreadGroupPropertyPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (EasyJSpinner) field.get(panel);
    }

    private static void invokeUpdatePreview(ThreadGroupPropertyPanel panel) throws Exception {
        Method method = ThreadGroupPropertyPanel.class.getDeclaredMethod("updatePreview");
        method.setAccessible(true);
        method.invoke(panel);
    }

    private static Object getPreviewData(ThreadGroupPropertyPanel panel) throws Exception {
        Object previewPanel = getField(panel, "previewPanel");
        return getField(previewPanel, "previewData");
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setEditorText(EasyJSpinner spinner, String text) {
        JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        textField.setText(text);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Timer getTimer(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Timer) field.get(target);
    }

    private static void runOnEdtAndWait(ThrowingRunnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        Throwable throwable = failure.get();
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable != null) {
            throw new AssertionError(throwable);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingPerformancePersistenceService extends PerformancePersistenceService {
        private final AtomicInteger saveCount = new AtomicInteger();
        private DefaultMutableTreeNode savedRoot;
        private PerformanceRemoteWorkerSettings savedRemoteWorkerSettings;

        @Override
        public void saveWorkspace(PerformancePlanWorkspace workspace) {
            saveCount.incrementAndGet();
            savedRoot = PerformanceSwingTreePlanAdapter.toTree(workspace.getActiveConfiguration().getPlanDocument(), "Plan");
            savedRemoteWorkerSettings = workspace.getActiveConfiguration().getRemoteWorkerSettings();
        }
    }
}
