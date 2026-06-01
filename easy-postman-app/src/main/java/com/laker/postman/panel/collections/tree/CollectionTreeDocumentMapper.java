package com.laker.postman.panel.collections.tree;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.collection.model.RequestGroup;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class CollectionTreeDocumentMapper {

    public static CollectionDocument fromRoot(DefaultMutableTreeNode rootNode) {
        if (rootNode == null) {
            return CollectionDocument.empty();
        }

        List<CollectionNode> roots = new ArrayList<>();
        Optional<CollectionNode> rootAsDomainNode = mapNode(rootNode);
        if (rootAsDomainNode.isPresent()) {
            roots.add(rootAsDomainNode.get());
        } else {
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                if (rootNode.getChildAt(i) instanceof DefaultMutableTreeNode childNode) {
                    mapNode(childNode).ifPresent(roots::add);
                }
            }
        }
        return new CollectionDocument(roots);
    }

    private static Optional<CollectionNode> mapNode(DefaultMutableTreeNode treeNode) {
        Optional<RequestGroup> group = CollectionTreeNodes.group(treeNode);
        if (group.isPresent()) {
            CollectionNode groupNode = CollectionNode.group(group.get());
            for (int i = 0; i < treeNode.getChildCount(); i++) {
                if (treeNode.getChildAt(i) instanceof DefaultMutableTreeNode childNode) {
                    mapNode(childNode).ifPresent(groupNode::addChild);
                }
            }
            return Optional.of(groupNode);
        }

        Optional<HttpRequestItem> request = CollectionTreeNodes.request(treeNode);
        return request.map(CollectionNode::request);
    }
}
