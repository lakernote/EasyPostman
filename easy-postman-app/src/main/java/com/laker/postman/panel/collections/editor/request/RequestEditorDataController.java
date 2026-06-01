package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.panel.collections.editor.request.sub.RequestSettingsPanel;
import com.laker.postman.request.edit.HttpRequestEditorDraftMapper;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 请求编辑器数据控制器。
 * <p>
 * 负责当前请求 ID、名称、协议、延迟初始化 pending 数据、原始请求基线和表单草稿收集。
 */
final class RequestEditorDataController {
    private final RequestItemProtocolEnum baseProtocol;
    private final BooleanSupplier editorInitialized;
    private final BooleanSupplier performanceSnapshot;
    private final Consumer<Boolean> loadingStateSetter;
    private final Runnable tabIndicatorsUpdater;

    private String requestId;
    private String requestName;
    private volatile RequestItemProtocolEnum currentProtocol;
    private HttpRequestItem pendingRequestItem;
    private HttpRequestItem pendingOriginalRequestItem;

    private RequestEditorBinder requestEditorBinder;
    private RequestEditorDefaultTabSelector requestEditorDefaultTabSelector;
    private Supplier<RequestDirtyStateHelper> dirtyStateHelperSupplier;
    private RequestSettingsPanel requestSettingsPanel;

    RequestEditorDataController(String requestId,
                                RequestItemProtocolEnum baseProtocol,
                                BooleanSupplier editorInitialized,
                                BooleanSupplier performanceSnapshot,
                                Consumer<Boolean> loadingStateSetter,
                                Runnable tabIndicatorsUpdater) {
        this.requestId = requestId;
        this.baseProtocol = baseProtocol;
        this.currentProtocol = baseProtocol;
        this.editorInitialized = editorInitialized;
        this.performanceSnapshot = performanceSnapshot;
        this.loadingStateSetter = loadingStateSetter;
        this.tabIndicatorsUpdater = tabIndicatorsUpdater;
    }

    void bindEditor(RequestEditorBinder requestEditorBinder,
                    RequestEditorDefaultTabSelector requestEditorDefaultTabSelector,
                    Supplier<RequestDirtyStateHelper> dirtyStateHelperSupplier,
                    RequestSettingsPanel requestSettingsPanel) {
        this.requestEditorBinder = requestEditorBinder;
        this.requestEditorDefaultTabSelector = requestEditorDefaultTabSelector;
        this.dirtyStateHelperSupplier = dirtyStateHelperSupplier;
        this.requestSettingsPanel = requestSettingsPanel;
    }

    String requestId() {
        return requestId;
    }

    RequestItemProtocolEnum effectiveProtocol() {
        return currentProtocol != null ? currentProtocol : baseProtocol;
    }

    void setCurrentProtocol(RequestItemProtocolEnum protocol) {
        currentProtocol = protocol;
    }

    void setOriginalRequestItem(HttpRequestItem item) {
        if (!editorInitialized.getAsBoolean()) {
            rememberPending(item, item);
            return;
        }
        if (performanceSnapshot.getAsBoolean()) {
            rememberPending(item, item);
            requestSettingsPanel.rebaseline();
            return;
        }
        RequestDirtyStateHelper dirtyStateHelper = dirtyStateHelper();
        if (dirtyStateHelper != null) {
            dirtyStateHelper.setOriginalRequestItem(item);
        }
        requestSettingsPanel.rebaseline();
    }

    HttpRequestItem originalRequestItem() {
        if (!editorInitialized.getAsBoolean() || performanceSnapshot.getAsBoolean()) {
            return pendingOriginalRequestItem;
        }
        RequestDirtyStateHelper dirtyStateHelper = dirtyStateHelper();
        return dirtyStateHelper != null ? dirtyStateHelper.getOriginalRequestItem() : pendingOriginalRequestItem;
    }

    boolean isModified() {
        if (!editorInitialized.getAsBoolean() || performanceSnapshot.getAsBoolean()) {
            return false;
        }
        RequestDirtyStateHelper dirtyStateHelper = dirtyStateHelper();
        return dirtyStateHelper != null && dirtyStateHelper.isModified();
    }

    HttpRequestItem currentRequestFromModel() {
        if (!editorInitialized.getAsBoolean()) {
            return pendingRequestOrOriginal();
        }
        return collectCurrentRequest(true);
    }

    void initPanelData(HttpRequestItem item) {
        if (!editorInitialized.getAsBoolean()) {
            rememberPending(item, item);
            applyRequestIdentity(item);
            return;
        }
        populatePanelData(item, item);
    }

    void populatePendingRequestIfPresent() {
        if (pendingRequestItem != null) {
            populatePanelData(pendingRequestItem,
                    pendingOriginalRequestItem != null ? pendingOriginalRequestItem : pendingRequestItem);
        }
    }

    HttpRequestItem currentRequest() {
        if (!editorInitialized.getAsBoolean()) {
            return pendingRequestOrOriginal();
        }
        return collectCurrentRequest(false);
    }

    void clearPending() {
        pendingRequestItem = null;
        pendingOriginalRequestItem = null;
    }

    private void populatePanelData(HttpRequestItem item, HttpRequestItem originalItem) {
        loadingStateSetter.accept(true);
        try {
            applyRequestIdentity(item);
            requestEditorBinder.populate(item);

            RequestDirtyStateHelper dirtyStateHelper = dirtyStateHelper();
            if (dirtyStateHelper != null) {
                dirtyStateHelper.setOriginalRequestItem(originalItem);
            }
            requestSettingsPanel.rebaseline();

            requestEditorDefaultTabSelector.selectByRequestType(effectiveProtocol(), item);
            tabIndicatorsUpdater.run();
        } finally {
            loadingStateSetter.accept(false);
        }
        rememberPending(item, originalItem);
    }

    private HttpRequestItem collectCurrentRequest(boolean fromModel) {
        HttpRequestItem originalRequestItem = originalRequestItem();
        return HttpRequestEditorDraftMapper.toRequestItem(requestEditorBinder.collectCurrentDraft(
                requestId,
                requestName,
                currentProtocol,
                originalRequestItem != null ? originalRequestItem.getResponse() : null,
                fromModel
        ));
    }

    private void applyRequestIdentity(HttpRequestItem item) {
        requestId = item.getId();
        requestName = item.getName();
        currentProtocol = item.getProtocol() != null ? item.getProtocol() : baseProtocol;
    }

    private void rememberPending(HttpRequestItem requestItem, HttpRequestItem originalItem) {
        pendingRequestItem = requestItem;
        pendingOriginalRequestItem = originalItem;
    }

    private HttpRequestItem pendingRequestOrOriginal() {
        return pendingRequestItem != null ? pendingRequestItem : pendingOriginalRequestItem;
    }

    private RequestDirtyStateHelper dirtyStateHelper() {
        return dirtyStateHelperSupplier != null ? dirtyStateHelperSupplier.get() : null;
    }
}
