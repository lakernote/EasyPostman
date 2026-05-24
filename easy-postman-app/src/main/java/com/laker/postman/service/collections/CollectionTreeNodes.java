package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.SavedResponse;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;

@UtilityClass
public class CollectionTreeNodes {

    public static DefaultMutableTreeNode groupNode(RequestGroup group) {
        return new DefaultMutableTreeNode(userObject(CollectionTreeNodeTypes.GROUP, group));
    }

    public static DefaultMutableTreeNode requestNode(HttpRequestItem request) {
        return new DefaultMutableTreeNode(userObject(CollectionTreeNodeTypes.REQUEST, request));
    }

    public static DefaultMutableTreeNode savedResponseNode(SavedResponse savedResponse) {
        return new DefaultMutableTreeNode(userObject(CollectionTreeNodeTypes.SAVED_RESPONSE, savedResponse));
    }

    public static Object[] userObject(String type, Object payload) {
        return new Object[]{type, payload};
    }

    public static boolean isGroup(DefaultMutableTreeNode node) {
        return group(node).isPresent();
    }

    public static boolean isRequest(DefaultMutableTreeNode node) {
        return request(node).isPresent();
    }

    public static boolean isSavedResponse(DefaultMutableTreeNode node) {
        return savedResponse(node).isPresent();
    }

    public static Optional<RequestGroup> group(DefaultMutableTreeNode node) {
        return payload(node, CollectionTreeNodeTypes.GROUP, RequestGroup.class);
    }

    public static Optional<HttpRequestItem> request(DefaultMutableTreeNode node) {
        return payload(node, CollectionTreeNodeTypes.REQUEST, HttpRequestItem.class);
    }

    public static Optional<SavedResponse> savedResponse(DefaultMutableTreeNode node) {
        return payload(node, CollectionTreeNodeTypes.SAVED_RESPONSE, SavedResponse.class);
    }

    public static Optional<String> type(DefaultMutableTreeNode node) {
        Object userObject = userObject(node);
        if (userObject instanceof Object[] obj && obj.length >= 1 && obj[0] instanceof String type) {
            return Optional.of(type);
        }
        return Optional.empty();
    }

    private static <T> Optional<T> payload(DefaultMutableTreeNode node, String expectedType, Class<T> payloadType) {
        Object userObject = userObject(node);
        if (userObject instanceof Object[] obj
                && obj.length >= 2
                && expectedType.equals(obj[0])
                && payloadType.isInstance(obj[1])) {
            return Optional.of(payloadType.cast(obj[1]));
        }
        return Optional.empty();
    }

    private static Object userObject(DefaultMutableTreeNode node) {
        return node != null ? node.getUserObject() : null;
    }
}
