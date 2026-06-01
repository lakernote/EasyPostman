package com.laker.postman.panel.collections.tree.adapter;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.service.collections.CollectionTreeNodes;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class SwingSavedResponseTreeMutation {

    /**
     * 同步更新编辑器请求对象、树节点请求对象，以及树下的保存响应子节点。
     */
    public Optional<Result> appendSavedResponse(DefaultMutableTreeNode rootTreeNode,
                                                HttpRequestItem editorRequestItem,
                                                SavedResponse savedResponse) {
        if (editorRequestItem == null || editorRequestItem.getId() == null || editorRequestItem.getId().isEmpty()
                || savedResponse == null) {
            return Optional.empty();
        }

        DefaultMutableTreeNode requestNode = SwingCollectionTreeQueries.findRequestNodeById(
                rootTreeNode,
                editorRequestItem.getId()
        );
        if (requestNode == null) {
            return Optional.empty();
        }

        HttpRequestItem treeRequestItem = CollectionTreeNodes.request(requestNode).orElse(null);
        if (treeRequestItem == null) {
            return Optional.empty();
        }

        appendResponse(treeRequestItem, savedResponse);
        if (treeRequestItem != editorRequestItem) {
            appendResponse(editorRequestItem, savedResponse);
        }
        requestNode.add(CollectionTreeNodes.savedResponseNode(savedResponse));
        return Optional.of(new Result(requestNode, treeRequestItem));
    }

    private void appendResponse(HttpRequestItem item, SavedResponse savedResponse) {
        List<SavedResponse> responses = item.getResponse();
        if (responses == null) {
            responses = new ArrayList<>();
            item.setResponse(responses);
        }
        responses.add(savedResponse);
    }

    public record Result(DefaultMutableTreeNode requestNode, HttpRequestItem treeRequestItem) {
    }
}
