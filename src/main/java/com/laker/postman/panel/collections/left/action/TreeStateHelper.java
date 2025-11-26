package com.laker.postman.panel.collections.left.action;

import com.laker.postman.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

/**
 * 树状态辅助类
 * 用于保存和恢复树的展开状态
 */
@UtilityClass
public class TreeStateHelper {

    /**
     * 保存所有展开的路径
     */
    public static List<TreePath> saveExpandedPaths(JTree tree) {
        List<TreePath> expandedPaths = new ArrayList<>();
        int rowCount = tree.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            TreePath path = tree.getPathForRow(i);
            if (path != null && tree.isExpanded(path)) {
                expandedPaths.add(path);
            }
        }
        return expandedPaths;
    }

    /**
     * 恢复展开的路径
     */
    public static void restoreExpandedPaths(JTree tree, List<TreePath> expandedPaths,
                                            DefaultMutableTreeNode rootNode) {
        if (expandedPaths == null || expandedPaths.isEmpty()) {
            return;
        }

        for (TreePath oldPath : expandedPaths) {
            TreePath newPath = findMatchingPath(oldPath, rootNode);
            if (newPath != null) {
                tree.expandPath(newPath);
            }
        }
    }

    /**
     * 根据旧路径的节点userObject查找匹配的新路径
     */
    private static TreePath findMatchingPath(TreePath oldPath, DefaultMutableTreeNode rootNode) {
        Object[] oldNodes = oldPath.getPath();
        if (oldNodes.length == 0) {
            return null;
        }

        DefaultMutableTreeNode currentNode = rootNode;
        List<DefaultMutableTreeNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(currentNode);

        // 跳过根节点，从第一个子节点开始匹配
        for (int i = 1; i < oldNodes.length; i++) {
            DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) oldNodes[i];
            Object oldUserObj = oldNode.getUserObject();

            DefaultMutableTreeNode matchedChild = findMatchingChild(currentNode, oldUserObj);
            if (matchedChild == null) {
                return null;
            }

            matchedNodes.add(matchedChild);
            currentNode = matchedChild;
        }

        return new TreePath(matchedNodes.toArray());
    }

    /**
     * 在当前节点的子节点中查找匹配的节点
     */
    private static DefaultMutableTreeNode findMatchingChild(DefaultMutableTreeNode parent, Object targetUserObj) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (isSameNode(child.getUserObject(), targetUserObj)) {
                return child;
            }
        }
        return null;
    }

    /**
     * 判断两个节点的userObject是否表示同一个节点
     */
    private static boolean isSameNode(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }

        // 根节点比较
        if (ROOT.equals(obj1) && ROOT.equals(obj2)) {
            return true;
        }

        // 数组类型节点比较
        if (obj1 instanceof Object[] arr1 && obj2 instanceof Object[] arr2) {
            if (arr1.length < 2 || arr2.length < 2) {
                return false;
            }

            String type1 = (String) arr1[0];
            String type2 = (String) arr2[0];

            if (!type1.equals(type2)) {
                return false;
            }

            if (GROUP.equals(type1)) {
                // 分组节点按名称比较
                return arr1[1].equals(arr2[1]);
            } else if (REQUEST.equals(type1)) {
                // 请求节点按ID比较
                HttpRequestItem item1 = (HttpRequestItem) arr1[1];
                HttpRequestItem item2 = (HttpRequestItem) arr2[1];
                return item1.getId().equals(item2.getId());
            }
        }

        return false;
    }
}

