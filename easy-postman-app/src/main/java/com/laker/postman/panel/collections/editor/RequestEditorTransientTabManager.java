package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.component.tab.RequestTabMarkers;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * 请求编辑器临时 Tab 管理器。
 * <p>
 * 临时 Tab 由单击树节点创建，可被下一次单击复用；保存、发送、双击或编辑后会 pin 为普通 Tab。
 */
@RequiredArgsConstructor
final class RequestEditorTransientTabManager {
    private final JTabbedPane tabbedPane;
    private final IntPredicate plusTabPredicate;
    private final Runnable plusTabRestorer;
    private final Consumer<Component> componentCleanupAction;

    private Component transientTab;
    private int transientTabIndex = -1;

    int transientTabIndex() {
        return transientTabIndex;
    }

    void setTransientTabIndex(int transientTabIndex) {
        this.transientTabIndex = transientTabIndex;
    }

    boolean isTransientRequest(String requestId) {
        validate();
        return requestId != null
                && transientTab instanceof RequestEditSubPanel requestEditSubPanel
                && requestId.equals(requestEditSubPanel.getId());
    }

    boolean isTransientGroup(String groupId) {
        validate();
        return groupId != null
                && transientTab instanceof GroupEditPanel groupEditPanel
                && groupId.equals(groupEditPanel.getGroup().getId());
    }

    void pin() {
        validate();
        if (transientTab == null) {
            return;
        }
        Component tabComponent = tabbedPane.getTabComponentAt(transientTabIndex);
        if (tabComponent instanceof ClosableTabComponent closableTab) {
            closableTab.updateMarkers(RequestTabMarkers::pinned);
        }
        clear();
    }

    boolean pinIfTransient(Component component) {
        validate();
        if (component == null || component != transientTab) {
            return false;
        }
        pin();
        return true;
    }

    void validate() {
        if (transientTab != null
                && transientTabIndex >= 0
                && transientTabIndex < tabbedPane.getTabCount()
                && tabbedPane.getComponentAt(transientTabIndex) == transientTab) {
            return;
        }
        clear();
    }

    void showOrReplace(Component panel, String name, RequestItemProtocolEnum protocol) {
        showOrReplace(panel, name, protocol, false);
    }

    void showOrReplace(Component panel, String name, HttpRequestItem item) {
        showOrReplace(panel, name, ClosableTabComponent.forRequest(name, item));
    }

    void showOrReplace(Component panel, String name, RequestItemProtocolEnum protocol, boolean rootGroup) {
        showOrReplace(panel, name, new ClosableTabComponent(name, protocol, rootGroup));
    }

    private void showOrReplace(Component panel, String name, ClosableTabComponent tabComponent) {
        validate();
        if (transientTab != null && transientTabIndex >= 0) {
            componentCleanupAction.accept(transientTab);
            transientTab = panel;
            tabbedPane.setComponentAt(transientTabIndex, transientTab);
            tabbedPane.setTitleAt(transientTabIndex, name);
            tabbedPane.setTabComponentAt(transientTabIndex, asPreview(tabComponent));
            tabbedPane.setSelectedIndex(transientTabIndex);
            return;
        }

        int plusTabIndex = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
        if (plusTabPredicate.test(plusTabIndex)) {
            tabbedPane.removeTabAt(plusTabIndex);
        }
        transientTab = panel;
        tabbedPane.addTab(name, transientTab);
        transientTabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(transientTabIndex, asPreview(tabComponent));
        tabbedPane.setSelectedIndex(transientTabIndex);
        plusTabRestorer.run();
    }

    void onTabRemoved(Component removedComponent, int removedIndex) {
        if (removedComponent == transientTab) {
            clear();
            return;
        }
        if (transientTabIndex > removedIndex) {
            transientTabIndex--;
        }
    }

    private ClosableTabComponent asPreview(ClosableTabComponent tabComponent) {
        tabComponent.updateMarkers(markers -> markers.withPreviewMode(true));
        return tabComponent;
    }

    private void clear() {
        transientTab = null;
        transientTabIndex = -1;
    }
}
