package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CollectionRequestLookup {
    private final TreeNodeRepository treeRepository;

    public CollectionRequestLookup() {
        this(new ActiveCollectionTreeNodeRepository());
    }

    CollectionRequestLookup(TreeNodeRepository treeRepository) {
        this.treeRepository = treeRepository;
    }

    public Optional<HttpRequestItem> findRequestItemById(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return treeRepository.findNodeByRequestId(requestId)
                    .flatMap(CollectionTreeNodes::request);
        } catch (Exception e) {
            log.error("Failed to find request item by ID {}: {}", requestId, e.getMessage());
            return Optional.empty();
        }
    }
}
