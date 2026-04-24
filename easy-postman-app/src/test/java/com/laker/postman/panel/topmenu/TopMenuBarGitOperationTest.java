package com.laker.postman.panel.topmenu;

import com.laker.postman.common.SingletonBaseMenuBar;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        TopMenuBar menuBar = newTopMenuBarWithoutInit();
        try {
            singletonMap.put(FunctionalPanel.class, functionalPanel);
            singletonMap.put(PerformancePanel.class, performancePanel);

            invokePerformGitOperation(menuBar);

            assertEquals(functionalPanel.saveCount.get(), 1);
            assertEquals(performancePanel.saveCount.get(), 1);
        } finally {
            singletonMap.clear();
            singletonMap.putAll(snapshot);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Object> singletonMap() throws Exception {
        Field field = SingletonFactory.class.getDeclaredField("INSTANCE_MAP");
        field.setAccessible(true);
        return (Map<Class<?>, Object>) field.get(null);
    }

    private static TopMenuBar newTopMenuBarWithoutInit() {
        SingletonBaseMenuBar.setCreatingAllowed(true);
        try {
            return new TopMenuBar() {
                @Override
                protected void initUI() {
                }

                @Override
                protected void registerListeners() {
                }
            };
        } finally {
            SingletonBaseMenuBar.setCreatingAllowed(false);
        }
    }

    private static RecordingFunctionalPanel newRecordingFunctionalPanel() {
        SingletonBasePanel.setCreatingAllowed(true);
        try {
            return new RecordingFunctionalPanel();
        } finally {
            SingletonBasePanel.setCreatingAllowed(false);
        }
    }

    private static RecordingPerformancePanel newRecordingPerformancePanel() {
        SingletonBasePanel.setCreatingAllowed(true);
        try {
            return new RecordingPerformancePanel();
        } finally {
            SingletonBasePanel.setCreatingAllowed(false);
        }
    }

    private static void invokePerformGitOperation(TopMenuBar menuBar) throws Exception {
        Method method = TopMenuBar.class.getDeclaredMethod("performGitOperation", Workspace.class, GitOperation.class);
        method.setAccessible(true);
        try {
            method.invoke(menuBar, workspace(), GitOperation.COMMIT);
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof HeadlessException)) {
                throw e;
            }
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
