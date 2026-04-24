package com.laker.postman.panel.performance;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.PerformancePersistenceService;
import org.testng.annotations.Test;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
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
import static org.testng.Assert.assertSame;

public class PerformancePanelSaveTest {

    @Test(description = "显式保存性能配置时应先提交仍在编辑中的 spinner 文本")
    public void shouldCommitEditedSpinnerTextBeforeExplicitSave() throws Exception {
        runOnEdtAndWait(() -> {
            PerformancePanel panel = newPanelWithoutInit();
            ThreadGroupData threadGroupData = new ThreadGroupData();
            threadGroupData.numThreads = 1;
            JMeterTreeNode threadGroupTreeNode = new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP, threadGroupData);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
            DefaultMutableTreeNode threadGroupNode = new DefaultMutableTreeNode(threadGroupTreeNode);
            root.add(threadGroupNode);
            DefaultTreeModel treeModel = new DefaultTreeModel(root);
            JTree jmeterTree = new JTree(treeModel);
            jmeterTree.setSelectionPath(new TreePath(threadGroupNode.getPath()));

            ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
            threadGroupPanel.setThreadGroupData(threadGroupTreeNode);
            EasyJSpinner numThreadsSpinner = getSpinner(threadGroupPanel, "fixedNumThreadsSpinner");
            setEditorText(numThreadsSpinner, "7");
            assertEquals(numThreadsSpinner.getValue(), 1);

            RecordingPerformancePersistenceService persistenceService = new RecordingPerformancePersistenceService();
            setField(panel, "treeModel", treeModel);
            setField(panel, "persistenceService", persistenceService);
            setField(panel, "propertyPanelSupport", createPropertyPanelSupport(jmeterTree, treeModel, threadGroupPanel));

            panel.save();

            assertEquals(threadGroupData.numThreads, 7);
            assertEquals(persistenceService.saveCount.get(), 1);
            assertSame(persistenceService.savedRoot, root);
        });
    }

    private static PerformancePanel newPanelWithoutInit() {
        SingletonBasePanel.setCreatingAllowed(true);
        try {
            return new PerformancePanel();
        } finally {
            SingletonBasePanel.setCreatingAllowed(false);
        }
    }

    private static PerformancePropertyPanelSupport createPropertyPanelSupport(JTree jmeterTree,
                                                                              DefaultTreeModel treeModel,
                                                                              ThreadGroupPropertyPanel threadGroupPanel) throws Exception {
        return new PerformancePropertyPanelSupport(
                jmeterTree,
                threadGroupPanel,
                new AssertionPropertyPanel(),
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
        setField(panel, "awaitTimeoutSpinner", spinner());
        setField(panel, "holdConnectionSpinner", spinner());
        setField(panel, "targetMessageCountSpinner", spinner());
        return panel;
    }

    private static WebSocketStagePropertyPanel headlessWebSocketStagePanel() throws Exception {
        WebSocketStagePropertyPanel panel = allocateWithoutConstructor(WebSocketStagePropertyPanel.class);
        setField(panel, "connectTimeoutSpinner", spinner());
        setField(panel, "sendCountSpinner", spinner());
        setField(panel, "sendIntervalSpinner", spinner());
        setField(panel, "awaitTimeoutSpinner", spinner());
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

    private static void setEditorText(EasyJSpinner spinner, String text) {
        JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        textField.setText(text);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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

        @Override
        public void save(DefaultMutableTreeNode rootNode, boolean efficientMode, CsvDataPanel.CsvState csvState) {
            saveCount.incrementAndGet();
            savedRoot = rootNode;
        }
    }
}
