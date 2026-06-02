package com.laker.postman.panel.topmenu;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;

import static org.testng.Assert.assertEquals;

public class TopMenuBarGitOperationTest {

    @Test(description = "打开 Git 操作对话框前应先保存已加载的工作区级面板")
    public void shouldSaveWorkspaceScopedPanelsBeforeOpeningGitOperationDialog() throws Exception {
        if (!GraphicsEnvironment.isHeadless()) {
            throw new SkipException("This ordering test relies on headless mode to stop before showing the dialog");
        }

        Map<Class<?>, Object> singletonMap = singletonMap();
        Map<Class<?>, Object> snapshot = new LinkedHashMap<>(singletonMap);
        RecordingFunctionalPanel functionalPanel = newRecordingFunctionalPanel();
        RecordingPerformancePanel performancePanel = newRecordingPerformancePanel();
        TopMenuWorkspaceControls controls = new TopMenuWorkspaceControls(new JPanel(), () -> {
        });
        try {
            singletonMap.put(FunctionalPanel.class, functionalPanel);
            singletonMap.put(PerformancePanel.class, performancePanel);

            invokePerformGitOperation(controls);

            assertEquals(functionalPanel.saveCount.get(), 1);
            assertEquals(performancePanel.saveCount.get(), 1);
        } finally {
            singletonMap.clear();
            singletonMap.putAll(snapshot);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Object> singletonMap() throws Exception {
        Field field = UiSingletonFactory.class.getDeclaredField("INSTANCE_MAP");
        field.setAccessible(true);
        return (Map<Class<?>, Object>) field.get(null);
    }

    private static RecordingFunctionalPanel newRecordingFunctionalPanel() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new RecordingFunctionalPanel();
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static RecordingPerformancePanel newRecordingPerformancePanel() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new RecordingPerformancePanel();
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static void invokePerformGitOperation(TopMenuWorkspaceControls controls) {
        try {
            controls.performGitOperation(workspace(), GitOperation.COMMIT);
        } catch (HeadlessException ignored) {
            // Expected in headless mode after the panel save ordering has already happened.
        }
    }

    private static Workspace workspace() {
        Workspace workspace = new Workspace();
        workspace.setId("git-workspace");
        workspace.setName("Git Workspace");
        workspace.setPath("/tmp/git-workspace");
        return workspace;
    }

    private static final class RecordingFunctionalPanel extends FunctionalPanel {
        private final AtomicInteger saveCount = new AtomicInteger();

        @Override
        protected void initUI() {
        }

        @Override
        protected void registerListeners() {
        }

        @Override
        public void save() {
            saveCount.incrementAndGet();
        }
    }

    private static final class RecordingPerformancePanel extends PerformancePanel {
        private final AtomicInteger saveCount = new AtomicInteger();

        @Override
        protected void initUI() {
        }

        @Override
        protected void registerListeners() {
        }

        @Override
        public void save() {
            saveCount.incrementAndGet();
        }
    }
}
