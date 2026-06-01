package com.laker.postman.collection;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.CollectionRequestContext;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class CollectionTreeQueries {

    public static Optional<CollectionRequestContext> findRequestContextById(CollectionDocument document,
                                                                            String requestId) {
        if (document == null || requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }
        for (CollectionNode root : document.getRoots()) {
            Optional<CollectionRequestContext> found = findRequestContextById(root, requestId, new ArrayList<>());
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public static Optional<HttpRequestItem> findRequestById(CollectionDocument document, String requestId) {
        return findRequestContextById(document, requestId)
                .map(CollectionRequestContext::getRequest);
    }

    public static List<HttpRequestItem> collectRequests(CollectionDocument document) {
        List<HttpRequestItem> requests = new ArrayList<>();
        if (document == null) {
            return requests;
        }
        for (CollectionNode root : document.getRoots()) {
            collectRequests(root, requests);
        }
        return requests;
    }

    private static Optional<CollectionRequestContext> findRequestContextById(CollectionNode node,
                                                                             String requestId,
                                                                             List<RequestGroup> groupChain) {
        if (node == null) {
            return Optional.empty();
        }
        if (node.isRequest()) {
            HttpRequestItem request = node.getRequest();
            if (request != null && requestId.equals(request.getId())) {
                return Optional.of(new CollectionRequestContext(request, groupChain));
            }
            return Optional.empty();
        }

        List<RequestGroup> childGroupChain = groupChain;
        RequestGroup group = node.getGroup();
        if (group != null) {
            childGroupChain = new ArrayList<>(groupChain);
            childGroupChain.add(group);
        }
        for (CollectionNode child : node.getChildren()) {
            Optional<CollectionRequestContext> found = findRequestContextById(child, requestId, childGroupChain);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static void collectRequests(CollectionNode node, List<HttpRequestItem> requests) {
        if (node == null) {
            return;
        }
        if (node.isRequest()) {
            HttpRequestItem request = node.getRequest();
            if (request != null) {
                requests.add(request);
            }
            return;
        }
        for (CollectionNode child : node.getChildren()) {
            collectRequests(child, requests);
        }
    }
}
