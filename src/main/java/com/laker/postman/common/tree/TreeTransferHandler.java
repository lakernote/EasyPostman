package com.laker.postman.common.tree;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

/**
 * 支持JTree节点拖拽排序的TransferHandler
 */
@Slf4j
public class TreeTransferHandler extends TransferHandler {
    private final JTree requestTree;
    private final DefaultMutableTreeNode rootTreeNode;
    private final DefaultTreeModel treeModel;
    private final Runnable saveRequestGroups;
    private DataFlavor nodesFlavor; // 用于传输节点的DataFlavor
    private DefaultMutableTreeNode[] nodesToRemove; // 被拖动的节点数组

    public TreeTransferHandler(JTree requestTree, DefaultTreeModel treeModel, Runnable saveRequestGroups) {
        this.requestTree = requestTree;
        this.rootTreeNode = (DefaultMutableTreeNode) treeModel.getRoot();
        this.treeModel = treeModel;
        this.saveRequestGroups = saveRequestGroups;
        try {
            nodesFlavor = new DataFlavor(DefaultMutableTreeNode[].class, "Node Array");
        } catch (Exception e) {
            log.error("拖转发生异常", e);
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) return false; // 只处理拖放操作
        support.setShowDropLocation(true); // 显示拖放位置
        if (!support.isDataFlavorSupported(nodesFlavor)) return false; // 只支持节点数组的DataFlavor
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation(); // 获取拖放位置
        TreePath dest = dl.getPath(); // 获取目标路径
        TreePath[] selPaths = requestTree.getSelectionPaths(); // 获取当前选中的路径
        if (selPaths == null || dest == null) return false; // 必须有选中的路径和目标路径

        DefaultMutableTreeNode draggedNode = (DefaultMutableTreeNode) selPaths[0].getLastPathComponent(); // 获取拖动的节点
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dest.getLastPathComponent(); // 获取目标节点
        if (draggedNode == rootTreeNode) return false; // 禁止拖动根节点

        Object draggedObj = draggedNode.getUserObject(); // 获取拖动节点的用户对象
        Object targetObj = targetNode.getUserObject(); // 获取目标节点的用户对象

        if (draggedObj instanceof Object[] && "group".equals(((Object[]) draggedObj)[0])) {
            // 禁止拖动到自己或自己的子节点
            if (isNodeDescendant(draggedNode, targetNode)) return false;
            if (targetNode == rootTreeNode) return true;
            return targetObj instanceof Object[] && "group".equals(((Object[]) targetObj)[0]);
        }

        // 请求节点只能拖到分组节点下，且不能拖到请求节点下
        if (draggedObj instanceof Object[] && "request".equals(((Object[]) draggedObj)[0])) {
            return targetObj instanceof Object[] && "group".equals(((Object[]) targetObj)[0]);
        }
        // 分组节点不能拖到请求节点下
        if (draggedObj instanceof Object[] && "group".equals(((Object[]) draggedObj)[0])) {
            if (targetNode == rootTreeNode) return true;
            return targetObj instanceof Object[] && "group".equals(((Object[]) targetObj)[0]);
        }
        return false;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return null;
        List<DefaultMutableTreeNode> copies = new ArrayList<>();
        List<DefaultMutableTreeNode> toRemove = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            copies.add(node);
            toRemove.add(node);
        }
        DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[0]);
        nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[0]);
        return new NodesTransferable(nodes);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        DefaultMutableTreeNode[] nodes = null;
        try {
            nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(nodesFlavor);
        } catch (Exception e) {
            log.error("import data error", e);
            return false;
        }
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
        if (childIndex == -1) childIndex = parent.getChildCount();
        for (DefaultMutableTreeNode node : nodes) {
            // 拖动请求节点时，始终插入到分组下
            Object nodeObj = node.getUserObject();
            if (nodeObj instanceof Object[] && "request".equals(((Object[]) nodeObj)[0])) {
                treeModel.insertNodeInto(node, parent, childIndex++);
            } else if (nodeObj instanceof Object[] && "group".equals(((Object[]) nodeObj)[0])) {
                treeModel.insertNodeInto(node, parent, childIndex++);
            }
        }
        return true;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if ((action & MOVE) == MOVE) {
            for (DefaultMutableTreeNode node : nodesToRemove) {
                treeModel.removeNodeFromParent(node);
            }
            if (saveRequestGroups != null) {
                saveRequestGroups.run();
            }
        }
    }

    // 判断目标节点是否是拖动节点本身或其子节点
    private boolean isNodeDescendant(DefaultMutableTreeNode node, DefaultMutableTreeNode possibleDescendant) {
        if (node == possibleDescendant) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (isNodeDescendant(child, possibleDescendant)) return true;
        }
        return false;
    }

    class NodesTransferable implements Transferable {
        private final DefaultMutableTreeNode[] nodes;

        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            if (!isDataFlavorSupported(flavor)) return null;
            return nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{nodesFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodesFlavor.equals(flavor);
        }
    }
}