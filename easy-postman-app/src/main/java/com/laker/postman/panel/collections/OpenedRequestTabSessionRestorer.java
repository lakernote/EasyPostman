package com.laker.postman.panel.collections;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.editor.RequestEditorTabController;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.service.collections.OpenedRequestTabsStore;
import com.laker.postman.service.collections.CollectionTreeQueryService;
import lombok.experimental.UtilityClass;

import javax.swing.SwingUtilities;
import java.util.List;

@UtilityClass
public class OpenedRequestTabSessionRestorer {

    public static void restoreOpenedRequests() {
        restoreOpenedRequests(OpenedRequestTabsStore.loadAll(), null);
    }

    public static void restoreOpenedRequests(List<HttpRequestItem> requestItems, Runnable onComplete) {
        Runnable restoreTask = () -> {
            CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
            List<HttpRequestItem> restorableRequests = CollectionTreeQueryService.buildRestorableOpenedRequests(
                    requestItems,
                    leftPanel.getRootTreeNode()
            );
            for (int i = 0; i < restorableRequests.size(); i++) {
                HttpRequestItem item = restorableRequests.get(i);
                boolean selectTab = i == restorableRequests.size() - 1;
                RequestEditSubPanel panel = RequestEditorTabController.openRequestTab(item, selectTab, true);
                RequestEditorTabController.setTabNewRequest(panel, item.isNewRequest());
            }
            OpenedRequestTabsStore.clear();
            if (onComplete != null) {
                onComplete.run();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            restoreTask.run();
        } else {
            SwingUtilities.invokeLater(restoreTask);
        }
    }
}
