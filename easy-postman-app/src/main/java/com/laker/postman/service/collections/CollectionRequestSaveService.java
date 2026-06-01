package com.laker.postman.service.collections;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates collection request mutations with persistence side effects.
 * Swing presenters decide how to refresh trees/tabs after receiving the result.
 */
public final class CollectionRequestSaveService {
    private final DefaultMutableTreeNode rootTreeNode;
    private final Runnable persistAction;

    public CollectionRequestSaveService(DefaultMutableTreeNode rootTreeNode, Runnable persistAction) {
        this.rootTreeNode = rootTreeNode;
        this.persistAction = persistAction == null ? () -> {
        } : persistAction;
    }

    public Optional<RequestSaveResult> addRequestToGroup(RequestGroup targetGroup, HttpRequestItem item) {
        if (targetGroup == null || item == null) {
            return Optional.empty();
        }

        DefaultMutableTreeNode groupNode = findGroupNode(rootTreeNode, targetGroup);
        if (groupNode == null) {
            return Optional.empty();
        }

        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(item);
        groupNode.add(requestNode);
        persist();
        return Optional.of(new RequestSaveResult(groupNode, requestNode, item));
    }

    public Optional<RequestSaveResult> updateExistingRequest(HttpRequestItem item) {
        Optional<CollectionRequestMutation.Result> mutation = CollectionRequestMutation
                .updateExistingRequest(rootTreeNode, item);
        if (mutation.isEmpty()) {
            return Optional.empty();
        }

        HttpRequestItem updatedItem = mutation.get().updatedItem();
        PreparedRequestFactory.invalidateCacheForRequest(updatedItem.getId());
        persist();
        return Optional.of(new RequestSaveResult(null, mutation.get().requestNode(), updatedItem));
    }

    public Optional<SavedResponseSaveResult> appendSavedResponse(HttpRequestItem requestItem,
                                                                 SavedResponse savedResponse) {
        Optional<SavedResponseTreeMutation.Result> mutation = SavedResponseTreeMutation
                .appendSavedResponse(rootTreeNode, requestItem, savedResponse);
        if (mutation.isEmpty()) {
            return Optional.empty();
        }

        persist();
        SavedResponseTreeMutation.Result result = mutation.get();
        return Optional.of(new SavedResponseSaveResult(
                result.requestNode(),
                result.treeRequestItem(),
                savedResponse
        ));
    }

    public DefaultMutableTreeNode findGroupNode(DefaultMutableTreeNode node, RequestGroup targetGroup) {
        if (node == null || targetGroup == null) {
            return null;
        }
        RequestGroup currentGroup = CollectionTreeNodes.group(node).orElse(null);
        if (currentGroup != null && sameGroup(currentGroup, targetGroup)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findGroupNode(child, targetGroup);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean sameGroup(RequestGroup currentGroup, RequestGroup targetGroup) {
        if (currentGroup.getId() != null && !currentGroup.getId().isBlank()) {
            return currentGroup.getId().equals(targetGroup.getId());
        }
        return currentGroup == targetGroup || Objects.equals(currentGroup.getName(), targetGroup.getName());
    }

    private void persist() {
        persistAction.run();
    }

    public record RequestSaveResult(DefaultMutableTreeNode groupNode,
                                    DefaultMutableTreeNode requestNode,
                                    HttpRequestItem requestItem) {
    }

    public record SavedResponseSaveResult(DefaultMutableTreeNode requestNode,
                                          HttpRequestItem treeRequestItem,
                                          SavedResponse savedResponse) {
    }
}
