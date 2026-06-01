package com.laker.postman.panel.collections.editor;

import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * 请求编辑器 Tab 初始化调度器。
 * <p>
 * 编辑器内容采用延迟初始化，调度逻辑集中在这里，避免面板布局代码混入启动恢复和预热细节。
 */
@RequiredArgsConstructor
final class RequestEditorTabInitializationScheduler {
    private final JTabbedPane tabbedPane;
    private final BooleanSupplier startupRestoreSelectionInProgress;

    void initializeSelectedTabSoon() {
        if (startupRestoreSelectionInProgress.getAsBoolean()) {
            return;
        }
        scheduleSelectedRequestTabInitialization(false);
    }

    void initializeSelectedStartupRestoreTab() {
        scheduleSelectedRequestTabInitialization(true);
    }

    void warmUpDeferredRequestTabsAfterStartup() {
        SwingUtilities.invokeLater(() -> warmUpDeferredRequestTabsSequentially(0));
    }

    private void scheduleSelectedRequestTabInitialization(boolean animatePlaceholderTransition) {
        SwingUtilities.invokeLater(() -> {
            if (startupRestoreSelectionInProgress.getAsBoolean()) {
                return;
            }
            ensureSelectedRequestTabInitialized(animatePlaceholderTransition);
        });
    }

    private void ensureSelectedRequestTabInitialized(boolean animatePlaceholderTransition) {
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof RequestEditSubPanel requestEditSubPanel
                && !requestEditSubPanel.isEditorInitialized()) {
            requestEditSubPanel.ensureEditorInitialized(animatePlaceholderTransition);
        }
    }

    private void warmUpDeferredRequestTabsSequentially(int startIndex) {
        for (int i = Math.max(0, startIndex); i < tabbedPane.getTabCount(); i++) {
            if (i == tabbedPane.getSelectedIndex()) {
                continue;
            }
            Component component = tabbedPane.getComponentAt(i);
            if (!(component instanceof RequestEditSubPanel requestEditSubPanel)
                    || requestEditSubPanel.isEditorInitialized()) {
                continue;
            }
            requestEditSubPanel.ensureEditorInitialized(false);
            int nextIndex = i + 1;
            SwingUtilities.invokeLater(() -> warmUpDeferredRequestTabsSequentially(nextIndex));
            return;
        }
    }
}
