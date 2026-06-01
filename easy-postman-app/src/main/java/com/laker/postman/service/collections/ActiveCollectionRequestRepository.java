package com.laker.postman.service.collections;

import com.laker.postman.collection.CollectionTreeQueries;
import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionRequestContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ActiveCollectionRequestRepository implements CollectionRequestRepository {

    @Override
    public Optional<CollectionRequestContext> findRequestContextById(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<CollectionDocument> document = getDocument();
        if (document.isEmpty()) {
            log.trace("Collection document is empty, cannot find request: {}", requestId);
            return Optional.empty();
        }
        return CollectionTreeQueries.findRequestContextById(document.get(), requestId);
    }

    @Override
    public Optional<CollectionDocument> getDocument() {
        return CollectionDocumentRegistry.getDocument();
    }
}
