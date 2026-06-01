package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.compare.HttpRequestDirtyComparator;
import com.laker.postman.request.defaults.GeneratedRequestHeaderPolicy;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.util.JsonUtil;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class RequestDirtyStateHelper {
    private final Supplier<HttpRequestItem> currentRequestFromModelSupplier;
    private final Consumer<Boolean> dirtyIndicatorUpdater;
    private final GeneratedRequestHeaderPolicy generatedHeaderPolicy;
    private HttpRequestItem originalRequestItem;

    RequestDirtyStateHelper(Supplier<HttpRequestItem> currentRequestFromModelSupplier,
                            Consumer<Boolean> dirtyIndicatorUpdater,
                            GeneratedRequestHeaderPolicy generatedHeaderPolicy) {
        this.currentRequestFromModelSupplier = currentRequestFromModelSupplier;
        this.dirtyIndicatorUpdater = dirtyIndicatorUpdater;
        this.generatedHeaderPolicy = generatedHeaderPolicy;
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
        return HttpRequestDirtyComparator.isDirty(
                originalRequestItem,
                current,
                generatedHeaderPolicy.generatedHeaders()
        );
    }

    void updateTabDirty() {
        SwingUtilities.invokeLater(() -> {
            if (originalRequestItem == null) {
                return;
            }
            dirtyIndicatorUpdater.accept(isModified());
        });
    }

}
