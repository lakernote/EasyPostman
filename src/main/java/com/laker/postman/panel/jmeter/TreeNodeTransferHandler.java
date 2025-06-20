package com.laker.postman.panel.jmeter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class TreeNodeTransferHandler extends TransferHandler {
    private final DataFlavor nodeFlavor;
    private final DataFlavor[] flavors;
    private DefaultMutableTreeNode nodeToRemove;

    private final JTree jmeterTree;
    private final DefaultTreeModel treeModel;

    public TreeNodeTransferHandler(JTree jmeterTree, DefaultTreeModel treeModel) {
        // ...existing code...
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
        System.out.println("[canImport] called");
        if (!support.isDrop()) {
            System.out.println("[canImport] not a drop");
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(nodeFlavor)) {
            System.out.println("[canImport] data flavor not supported");
            return false;
        }
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        if (dest == null) {
            System.out.println("[canImport] dest is null");
            return false;
        }
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dest.getLastPathComponent();
        if (nodeToRemove == null) {
            System.out.println("[canImport] nodeToRemove is null");
            return false;
        }
        Object dragObj = nodeToRemove.getUserObject();
        Object userObj = targetNode.getUserObject();
        if (!(dragObj instanceof JMeterTreeNode dragJtNode)) {
            System.out.println("[canImport] dragObj not JMeterTreeNode");
            return false;
        }
        if (!(userObj instanceof JMeterTreeNode jtNode)) {
            System.out.println("[canImport] userObj not JMeterTreeNode");
            return false;
        }
        // 线程组只能拖到root���
        if (dragJtNode.type == NodeType.THREAD_GROUP) {
            if (jtNode.type != NodeType.ROOT) {
                System.out.println("[canImport] THREAD_GROUP只能拖到ROOT下");
                return false;
            }
        }
        // 请求只能���到线程组下
        if (dragJtNode.type == NodeType.REQUEST) {
            if (jtNode.type != NodeType.THREAD_GROUP) {
                System.out.println("[canImport] REQUEST��能拖到THREAD_GROUP下");
                return false;
            }
        }
        // 断言、定时器只能在请求下
        if (dragJtNode.type == NodeType.ASSERTION || dragJtNode.type == NodeType.TIMER) {
            if (jtNode.type != NodeType.REQUEST) {
                System.out.println("[canImport] ASSERTION/TIMER���能拖到REQUEST下");
                return false;
            }
        }
        // 不允许拖到自己或子孙节点
        if (nodeToRemove == targetNode) {
            System.out.println("[canImport] 不能拖到自己");
            return false;
        }
        // 修正：判断targetNode���不是nodeToRemove的子孙节点
        if (isNodeDescendant(nodeToRemove, targetNode)) {
            System.out.println("[canImport] 不能拖到子孙节��");
            return false;
        }
        // 不允��拖动根节点
        if (dragJtNode.type == NodeType.ROOT) {
            System.out.println("[canImport] 不能拖动ROOT节点");
            return false;
        }
        System.out.println("[canImport] 可以导入");
        return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        System.out.println("[importData] called");
        if (!canImport(support)) {
            System.out.println("[importData] canImport=false");
            return false;
        }
        try {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) support.getTransferable().getTransferData(nodeFlavor);
            if (node == null) {
                System.out.println("[importData] node is null");
                return false;
            }
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
            if (support.getDropAction() != MOVE) {
                System.out.println("[importData] dropAction!=MOVE");
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
            System.out.printf("[importData] insert node '%s' to parent '%s' at %d\n", node, parent, insertIndex);
            treeModel.insertNodeInto(node, parent, insertIndex);
            jmeterTree.expandPath(new TreePath(parent.getPath()));
            jmeterTree.updateUI(); // 强制刷新
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
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