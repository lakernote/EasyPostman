package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;

import javax.swing.tree.DefaultMutableTreeNode;

interface RequestTreeOpenActions {
    void openTransientRequest(HttpRequestItem item);

    void openFixedRequest(HttpRequestItem item);

    void openTransientGroup(DefaultMutableTreeNode groupNode, RequestGroup group);

    void openFixedGroup(DefaultMutableTreeNode groupNode, RequestGroup group);

    void openTransientSavedResponse(SavedResponse savedResponse);

    void openFixedSavedResponse(SavedResponse savedResponse);
}
