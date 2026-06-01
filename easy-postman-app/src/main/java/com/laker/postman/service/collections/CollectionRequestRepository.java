package com.laker.postman.service.collections;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionRequestContext;

import java.util.Optional;

public interface CollectionRequestRepository {

    Optional<CollectionRequestContext> findRequestContextById(String requestId);

    Optional<CollectionDocument> getDocument();
}
