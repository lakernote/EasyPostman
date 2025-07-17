package com.laker.postman.panel.jmeter.component;

import com.laker.postman.panel.jmeter.model.JMeterTreeNode;
import com.laker.postman.panel.jmeter.model.NodeType;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

@Slf4j
public class TreeNodeTransferHandler extends TransferHandler {
    private final DataFlavor nodeFlavor;
    private final DataFlavor[] flavors;
    private DefaultMutableTreeNode nodeToRemove;

    private final JTree jmeterTree;
    private final DefaultTreeModel treeModel;

    public TreeNodeTransferHandler(JTree jmeterTree, DefaultTreeModel treeModel) {
        this.jmeterTree = jmeterTree;
        this.treeModel = treeModel;
        try {
            nodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=javax.swing.tree.DefaultMutableTreeNode");
            flavors = new DataFlavor[]{nodeFlavor};
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        TreePath path = ((JTree) c).getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                if (jtNode.type == NodeType.ROOT) return NONE;
            }
        }
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        nodeToRemove = node;
        return new NodeTransferable(node);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(nodeFlavor)) {
            return false;
        }
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        if (dest == null) {
            return false;
        }
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dest.getLastPathComponent();
        if (nodeToRemove == null) {
            return false;
        }
        Object dragObj = nodeToRemove.getUserObject();
        Object userObj = targetNode.getUserObject();
        if (!(dragObj instanceof JMeterTreeNode dragJtNode)) {
            return false;
        }
        if (!(userObj instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        if (dragJtNode.type == NodeType.THREAD_GROUP) {
            if (jtNode.type != NodeType.ROOT) {
                return false;
            }
        }
        if (dragJtNode.type == NodeType.REQUEST) {
            if (jtNode.type != NodeType.THREAD_GROUP) {
                return false;
            }
        }
        // 断言、定时器只能在请求下
        if (dragJtNode.type == NodeType.ASSERTION || dragJtNode.type == NodeType.TIMER) {
            if (jtNode.type != NodeType.REQUEST) {
                return false;
            }
        }
        // 不允许拖到自己或子孙节点
        if (nodeToRemove == targetNode) {
            return false;
        }
        if (isNodeDescendant(nodeToRemove, targetNode)) {
            return false;
        }
        return dragJtNode.type != NodeType.ROOT;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) support.getTransferable().getTransferData(nodeFlavor);
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
            if (support.getDropAction() != MOVE) {
                return false;
            }
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
            int oldIndex = oldParent != null ? oldParent.getIndex(node) : -1;
            if (oldParent != null) treeModel.removeNodeFromParent(node);
            int insertIndex = childIndex;
            if (insertIndex == -1) insertIndex = parent.getChildCount();
            // 允许同级排序
            if (oldParent == parent && oldIndex < insertIndex) {
                insertIndex--;
            }
            treeModel.insertNodeInto(node, parent, insertIndex);
            jmeterTree.expandPath(new TreePath(parent.getPath()));
            jmeterTree.updateUI(); // 强制刷新
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        nodeToRemove = null;
    }

    private boolean isNodeDescendant(DefaultMutableTreeNode ancestor, DefaultMutableTreeNode node) {
        if (node == ancestor) return true;
        for (int i = 0; i < ancestor.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) ancestor.getChildAt(i);
            if (isNodeDescendant(child, node)) return true;
        }
        return false;
    }

    class NodeTransferable implements Transferable {
        private final DefaultMutableTreeNode node;

        public NodeTransferable(DefaultMutableTreeNode node) {
            this.node = node;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(nodeFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return node;
        }
    }
}