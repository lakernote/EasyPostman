package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CollectionRequestLookup {
    private final CollectionRequestRepository requestRepository;

    public CollectionRequestLookup() {
        this(new ActiveCollectionRequestRepository());
    }

    CollectionRequestLookup(CollectionRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Optional<HttpRequestItem> findRequestItemById(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return requestRepository.findRequestContextById(requestId)
                    .map(com.laker.postman.collection.model.CollectionRequestContext::getRequest);
        } catch (Exception e) {
            log.error("Failed to find request item by ID {}: {}", requestId, e.getMessage());
            return Optional.empty();
        }
    }
}
