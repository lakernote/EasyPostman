package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.SavedResponse;
import com.laker.postman.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
final class RequestDirtyStateHelper {
    private final Supplier<HttpRequestItem> currentRequestFromModelSupplier;
    private final Consumer<Boolean> dirtyIndicatorUpdater;
    private HttpRequestItem originalRequestItem;

    RequestDirtyStateHelper(Supplier<HttpRequestItem> currentRequestFromModelSupplier,
                            Consumer<Boolean> dirtyIndicatorUpdater) {
        this.currentRequestFromModelSupplier = currentRequestFromModelSupplier;
        this.dirtyIndicatorUpdater = dirtyIndicatorUpdater;
    }

    HttpRequestItem getOriginalRequestItem() {
        return originalRequestItem;
    }

    void setOriginalRequestItem(HttpRequestItem item) {
        if (item != null && !item.isNewRequest()) {
            originalRequestItem = JsonUtil.deepCopy(item, HttpRequestItem.class);
        } else {
            originalRequestItem = null;
        }
    }

    boolean isModified() {
        if (originalRequestItem == null) {
            return false;
        }

        // 脏检查始终基于“原始快照 vs 当前表单快照”，这样 UI 不需要了解比较细节。
        HttpRequestItem current = currentRequestFromModelSupplier.get();
        boolean modified = !equalsIgnoringResponse(originalRequestItem, current);
        if (modified) {
            log.debug("Request form has been modified, Request Name: {}", current.getName());
        }
        return modified;
    }

    void updateTabDirty() {
        SwingUtilities.invokeLater(() -> {
            if (originalRequestItem == null) {
                return;
            }
            dirtyIndicatorUpdater.accept(isModified());
        });
    }

    private boolean equalsIgnoringResponse(HttpRequestItem original, HttpRequestItem current) {
        if (original == current) {
            return true;
        }
        if (original == null || current == null) {
            return false;
        }

        try {
            List<SavedResponse> originalSavedResponses = original.getResponse();
            List<SavedResponse> currentSavedResponses = current.getResponse();

            try {
                // response 是历史结果，不属于请求编辑内容；比较时剔除，避免误判整页已修改。
                original.setResponse(null);
                current.setResponse(null);
                return JsonUtil.toJsonStr(original).equals(JsonUtil.toJsonStr(current));
            } finally {
                original.setResponse(originalSavedResponses);
                current.setResponse(currentSavedResponses);
            }
        } catch (Exception ex) {
            log.error("比较请求时发生异常", ex);
            return false;
        }
    }
}
