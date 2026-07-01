package com.laker.postman.panel.collections.tree.handler;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

record CollectionTreeClickTarget(TreePath path, DefaultMutableTreeNode node, CollectionTreeClickArea area) {

    boolean isGroupAddRequestAction() {
        return area == CollectionTreeClickArea.GROUP_ADD_REQUEST;
    }

    boolean isGroupMoreActions() {
        return area == CollectionTreeClickArea.GROUP_MORE_ACTIONS;
    }

    boolean isExpansionHandle() {
        return area == CollectionTreeClickArea.EXPANSION_HANDLE;
    }
}
