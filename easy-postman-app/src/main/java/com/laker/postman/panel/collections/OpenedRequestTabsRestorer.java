package com.laker.postman.panel.collections;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditorTabs;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.collections.OpenedRequestsStore;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.startup.StartupDiagnostics;
import lombok.experimental.UtilityClass;

import javax.swing.SwingUtilities;
import java.util.List;

@UtilityClass
public class OpenedRequestTabsRestorer {

    public static void restoreOpenedRequests() {
        restoreOpenedRequests(OpenedRequestsStore.loadAll(), null);
    }

    public static void restoreOpenedRequests(List<HttpRequestItem> requestItems, Runnable onComplete) {
        Runnable restoreTask = () -> {
            RequestCollectionsLeftPanel leftPanel = UiSingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
            List<HttpRequestItem> restorableRequests = RequestCollectionsService.buildRestorableOpenedRequests(
                    requestItems,
                    leftPanel.getRootTreeNode()
            );
            long restoreStartNanos = System.nanoTime();
            StartupDiagnostics.mark("Opened requests restore prepared; validRequests=" + restorableRequests.size());
            for (int i = 0; i < restorableRequests.size(); i++) {
                HttpRequestItem item = restorableRequests.get(i);
                boolean selectTab = i == restorableRequests.size() - 1;
                RequestEditSubPanel panel = RequestEditorTabs.openRequestTab(item, selectTab, true);
                RequestEditorTabs.setTabNewRequest(panel, item.isNewRequest());
            }
            OpenedRequestsStore.clear();
            StartupDiagnostics.mark("Opened requests restore finished in "
                    + StartupDiagnostics.formatSince(restoreStartNanos));
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
