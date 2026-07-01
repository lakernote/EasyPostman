package com.laker.postman.panel.collections.tree;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.service.collections.CollectionTreeNodes;
import lombok.experimental.UtilityClass;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.Component;
import java.util.Objects;

@UtilityClass
class CollectionGroupTreeFactory {

    static JTree createTree(TreeModel sourceModel) {
        JTree groupTree = new JTree(createModel(sourceModel));
        groupTree.setRootVisible(false);
        groupTree.setShowsRootHandles(true);
        groupTree.setRowHeight(28);
        groupTree.putClientProperty("FlatLaf.style", "wideCellRenderer: true");
        groupTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean selected,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node) {
                    RequestGroup group = CollectionTreeNodes.group(node).orElse(null);
                    if (group != null) {
                        setText(group.getName());
                        setIcon(new FlatSVGIcon(node.getLevel() == 1 ? "icons/collection.svg" : "icons/group.svg", 16, 16));
                        return this;
                    }
                }
                setText("");
                setIcon(null);
                return this;
            }
        });
        if (groupTree.getRowCount() > 0) {
            groupTree.setSelectionRow(0);
        }
        return groupTree;
    }

    private static TreeModel createModel(TreeModel sourceModel) {
        Object root = Objects.requireNonNull(sourceModel, "sourceModel").getRoot();
        if (!(root instanceof DefaultMutableTreeNode rootNode)) {
            throw new IllegalArgumentException("Group tree model root must be a DefaultMutableTreeNode");
        }
        return new DefaultTreeModel(rootNode) {
            @Override
            public int getChildCount(Object parent) {
                return countGroupChildren(parent);
            }

            @Override
            public Object getChild(Object parent, int index) {
                return groupChild(parent, index);
            }

            @Override
            public boolean isLeaf(Object node) {
                return countGroupChildren(node) == 0;
            }
        };
    }

    private static int countGroupChildren(Object parent) {
        if (!(parent instanceof DefaultMutableTreeNode node)) {
            return 0;
        }
        int groupCount = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof DefaultMutableTreeNode child && CollectionTreeNodes.isGroup(child)) {
                groupCount++;
            }
        }
        return groupCount;
    }

    private static Object groupChild(Object parent, int index) {
        if (!(parent instanceof DefaultMutableTreeNode node)) {
            return null;
        }
        int groupIndex = -1;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof DefaultMutableTreeNode child && CollectionTreeNodes.isGroup(child)) {
                groupIndex++;
                if (groupIndex == index) {
                    return child;
                }
            }
        }
        return null;
    }

}
