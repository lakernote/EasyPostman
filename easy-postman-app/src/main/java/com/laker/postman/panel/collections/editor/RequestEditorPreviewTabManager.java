package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * 请求编辑器预览 Tab 管理器。
 * <p>
 * 预览 Tab 是一个有状态的小生命周期：创建、复用、升级为固定 Tab、随删除/拖拽修正索引。
 * 集中管理后，面板不再直接维护 previewTab/previewTabIndex 两个易错字段。
 */
@RequiredArgsConstructor
final class RequestEditorPreviewTabManager {
    private final JTabbedPane tabbedPane;
    private final IntPredicate plusTabPredicate;
    private final Runnable plusTabRestorer;
    private final Consumer<Component> componentCleanupAction;

    private Component previewTab;
    private int previewTabIndex = -1;

    int previewTabIndex() {
        return previewTabIndex;
    }

    void setPreviewTabIndex(int previewTabIndex) {
        this.previewTabIndex = previewTabIndex;
    }

    boolean isPreviewRequest(String requestId) {
        validate();
        return requestId != null
                && previewTab instanceof RequestEditSubPanel requestEditSubPanel
                && requestId.equals(requestEditSubPanel.getId());
    }

    boolean isPreviewGroup(String groupId) {
        validate();
        return groupId != null
                && previewTab instanceof GroupEditPanel groupEditPanel
                && groupId.equals(groupEditPanel.getGroup().getId());
    }

    void promoteToPermanent() {
        validate();
        if (previewTab == null) {
            return;
        }
        Component tabComponent = tabbedPane.getTabComponentAt(previewTabIndex);
        if (tabComponent instanceof ClosableTabComponent closableTab) {
            closableTab.setPreviewMode(false);
        }
        clear();
    }

    void validate() {
        if (previewTab != null
                && previewTabIndex >= 0
                && previewTabIndex < tabbedPane.getTabCount()
                && tabbedPane.getComponentAt(previewTabIndex) == previewTab) {
            return;
        }
        clear();
    }

    void showOrUpdate(Component panel, String name, RequestItemProtocolEnum protocol) {
        showOrUpdate(panel, name, protocol, false);
    }

    void showOrUpdate(Component panel, String name, RequestItemProtocolEnum protocol, boolean rootGroup) {
        validate();
        if (previewTab != null && previewTabIndex >= 0) {
            componentCleanupAction.accept(previewTab);
            previewTab = panel;
            tabbedPane.setComponentAt(previewTabIndex, previewTab);
            tabbedPane.setTitleAt(previewTabIndex, name);
            tabbedPane.setTabComponentAt(previewTabIndex, createPreviewTabComponent(name, protocol, rootGroup));
            tabbedPane.setSelectedIndex(previewTabIndex);
            return;
        }

        int plusTabIndex = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
        if (plusTabPredicate.test(plusTabIndex)) {
            tabbedPane.removeTabAt(plusTabIndex);
        }
        previewTab = panel;
        tabbedPane.addTab(name, previewTab);
        previewTabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(previewTabIndex, createPreviewTabComponent(name, protocol, rootGroup));
        tabbedPane.setSelectedIndex(previewTabIndex);
        plusTabRestorer.run();
    }

    void onTabRemoved(Component removedComponent, int removedIndex) {
        if (removedComponent == previewTab) {
            clear();
            return;
        }
        if (previewTabIndex > removedIndex) {
            previewTabIndex--;
        }
    }

    private ClosableTabComponent createPreviewTabComponent(String name, RequestItemProtocolEnum protocol, boolean rootGroup) {
        ClosableTabComponent tabComponent = new ClosableTabComponent(name, protocol, rootGroup);
        tabComponent.setPreviewMode(true);
        return tabComponent;
    }

    private void clear() {
        previewTab = null;
        previewTabIndex = -1;
    }
}
