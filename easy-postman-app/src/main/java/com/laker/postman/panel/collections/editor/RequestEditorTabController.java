package com.laker.postman.panel.collections.editor;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.service.collections.ActiveCollectionTreeNodeRepository;
import com.laker.postman.service.collections.CollectionTreeQueryService;
import com.laker.postman.service.variable.RequestContext;
import com.laker.postman.startup.StartupDiagnostics;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JTabbedPane;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Component;

@UtilityClass
public class RequestEditorTabController {

    public static RequestEditSubPanel openRequestTab(HttpRequestItem item) {
        return openRequestTab(item, true, false);
    }

    public static RequestEditSubPanel openRequestTab(HttpRequestItem item, boolean selectTab) {
        return openRequestTab(item, selectTab, false);
    }

    public static RequestEditSubPanel openRequestTab(
            HttpRequestItem item,
            boolean selectTab,
            boolean deferEditorInitialization) {
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Request item ID cannot be null or empty");
        }
        long totalStartNanos = System.nanoTime();

        ActiveCollectionTreeNodeRepository repository = new ActiveCollectionTreeNodeRepository();
        repository.getRootNode().ifPresent(rootNode -> {
            DefaultMutableTreeNode requestNode = CollectionTreeQueryService.findRequestNodeById(rootNode, id);
            if (requestNode != null) {
                RequestContext.setCurrentRequestNode(requestNode);
            }
        });

        long panelCreateStartNanos = System.nanoTime();
        RequestEditSubPanel subPanel = new RequestEditSubPanel(id, item.getProtocol(), deferEditorInitialization);
        String panelCreateDuration = StartupDiagnostics.formatSince(panelCreateStartNanos);
        long initPanelStartNanos = System.nanoTime();
        subPanel.initPanelData(item);
        String initPanelDuration = StartupDiagnostics.formatSince(initPanelStartNanos);
        String tabTitle = CharSequenceUtil.isNotBlank(item.getName())
                ? item.getName()
                : I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
        RequestEditorPanel requestEditPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        JTabbedPane tabbedPane = requestEditPanel.getTabbedPane();
        int insertIndex = tabbedPane.getTabCount();
        if (insertIndex > 0 && RequestEditorPanel.PLUS_TAB.equals(tabbedPane.getTitleAt(insertIndex - 1))) {
            insertIndex--;
        }
        long uiInsertStartNanos = System.nanoTime();
        tabbedPane.insertTab(tabTitle, null, subPanel, null, insertIndex);
        tabbedPane.setTabComponentAt(insertIndex, new ClosableTabComponent(tabTitle, item.getProtocol()));
        boolean shouldSelectInsertedTab = selectTab;
        if (shouldSelectInsertedTab && deferEditorInitialization) {
            shouldSelectInsertedTab = requestEditPanel.isStartupRestoreSelectingLastTab();
        }
        if (shouldSelectInsertedTab) {
            tabbedPane.setSelectedIndex(insertIndex);
            if (!deferEditorInitialization) {
                requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(true);
                requestEditPanel.initializeSelectedTabSoon();
            }
        }
        StartupDiagnostics.mark("Restored tab '" + tabTitle + "' in "
                + StartupDiagnostics.formatSince(totalStartNanos)
                + " (construct=" + panelCreateDuration
                + ", init=" + initPanelDuration
                + ", insert=" + StartupDiagnostics.formatSince(uiInsertStartNanos)
                + ", select=" + shouldSelectInsertedTab
                + ", deferred=" + deferEditorInitialization + ")");
        return subPanel;
    }

    public static void setTabNewRequest(RequestEditSubPanel panel, boolean isNew) {
        JTabbedPane tabbedPane = UiSingletonFactory.getInstance(RequestEditorPanel.class).getTabbedPane();
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0) {
            return;
        }
        Component tabComp = tabbedPane.getTabComponentAt(idx);
        if (tabComp instanceof ClosableTabComponent closable) {
            closable.setNewRequest(isNew);
        }
    }
}
