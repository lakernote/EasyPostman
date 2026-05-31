package com.laker.postman.panel.performance;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.service.collections.ActiveCollectionTreeNodeRepository;
import com.laker.postman.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;

@Slf4j
final class PerformanceCollectionRequestResolver {

    HttpRequestItem findRequestItemById(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return null;
        }

        try {
            DefaultMutableTreeNode requestNode = new ActiveCollectionTreeNodeRepository()
                    .findNodeByRequestId(requestId)
                    .orElse(null);

            if (requestNode != null) {
                Object userObj = requestNode.getUserObject();
                if (userObj instanceof Object[] obj
                        && obj.length > 1
                        && obj[1] instanceof HttpRequestItem originalItem) {
                    return deepCopyRequestItem(originalItem);
                }
            }
        } catch (Exception e) {
            log.error("Failed to find request item by ID {}: {}", requestId, e.getMessage());
        }

        return null;
    }

    private HttpRequestItem deepCopyRequestItem(HttpRequestItem original) {
        try {
            return JsonUtil.deepCopy(original, HttpRequestItem.class);
        } catch (Exception e) {
            log.error("Failed to deep copy request item: {}", e.getMessage());
            return original;
        }
    }
}
