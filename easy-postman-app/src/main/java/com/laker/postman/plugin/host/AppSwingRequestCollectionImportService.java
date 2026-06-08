package com.laker.postman.plugin.host;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.RequestImportDraft;
import com.laker.postman.model.RequestImportResult;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.tree.CollectionGroupSelectionDialog;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GUI 宿主的请求导入服务实现。
 * 通过 Swing 选择目标集合，并把导入结果同步到集合树和请求编辑器。
 */
@Slf4j
public class AppSwingRequestCollectionImportService implements RequestCollectionImportService {

    @Override
    public RequestImportResult importRequests(List<RequestImportDraft> requests) {
        if (requests == null || requests.isEmpty()) {
            return RequestImportResult.imported(0);
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return importRequestsOnEdt(requests);
        }
        AtomicReference<RequestImportResult> result = new AtomicReference<>(RequestImportResult.unavailable());
        try {
            SwingUtilities.invokeAndWait(() -> result.set(importRequestsOnEdt(requests)));
        } catch (Exception e) {
            log.warn("Failed to import requests into collection", e);
            return RequestImportResult.unavailable();
        }
        return result.get();
    }

    private RequestImportResult importRequestsOnEdt(List<RequestImportDraft> requests) {
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditorPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            return RequestImportResult.unavailable();
        }

        RequestGroup group = CollectionGroupSelectionDialog.chooseGroup(groupTreeModel).orElse(null);
        if (group == null) {
            return RequestImportResult.cancelled();
        }

        HttpRequestItem lastImported = null;
        for (RequestImportDraft draft : requests) {
            HttpRequestItem item = RequestImportDraftMapper.toHttpRequestItem(draft);
            collectionPanel.saveRequestToGroup(group, item);
            lastImported = item;
        }

        if (lastImported != null) {
            collectionPanel.locateAndSelectRequest(lastImported.getId());
            requestEditorPanel.showOrCreateTab(lastImported);
        }
        return RequestImportResult.imported(requests.size());
    }
}
