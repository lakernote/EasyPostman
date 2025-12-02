package com.laker.postman.panel.collections.left.handler;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.GROUP;
import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

/**
 * 请求树鼠标事件处理器
 * 负责处理树节点的单击、双击和右键菜单
 */
public class RequestTreeMouseHandler extends MouseAdapter {
    private final JTree requestTree;
    private final RequestTreePopupMenu popupMenu;

    public RequestTreeMouseHandler(JTree requestTree, RequestCollectionsLeftPanel leftPanel) {
        this.requestTree = requestTree;
        this.popupMenu = new RequestTreePopupMenu(requestTree, leftPanel);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
            handleSingleClick(e);
        } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            handleDoubleClick(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e);
        }
    }

    /**
     * 处理单击事件：预览请求或分组
     */
    private void handleSingleClick(MouseEvent e) {
        TreePath selPath = getTreePathAt(e.getX(), e.getY());
        if (selPath == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();

        if (!(node.getUserObject() instanceof Object[] obj)) return;

        if (GROUP.equals(obj[0])) {
            handleGroupClick(e, node, obj, selPath);
        } else if (REQUEST.equals(obj[0])) {
            handleRequestClick(obj);
        }
    }

    /**
     * 处理双击事件：打开请求或分组编辑面板
     */
    private void handleDoubleClick(MouseEvent e) {
        TreePath selPath = getTreePathAt(e.getX(), e.getY());
        if (selPath == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
        if (!(node.getUserObject() instanceof Object[] obj)) return;

        if (REQUEST.equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            SingletonFactory.getInstance(RequestEditPanel.class).showOrCreateTab(item);
        } else if (GROUP.equals(obj[0])) {
            RequestGroup group = ensureRequestGroup(obj);
            RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
            editPanel.showGroupEditPanel(node, group);
            e.consume(); // 阻止展开/收起
        }
    }

    /**
     * 处理右键点击：显示弹出菜单
     */
    private void handleRightClick(MouseEvent e) {
        int row = requestTree.getClosestRowForLocation(e.getX(), e.getY());
        if (row != -1) {
            TreePath clickedPath = requestTree.getPathForRow(row);
            if (clickedPath != null && !requestTree.isPathSelected(clickedPath)) {
                requestTree.setSelectionRow(row);
            }
        } else {
            requestTree.clearSelection();
        }
        popupMenu.show(e.getX(), e.getY());
    }

    /**
     * 处理分组点击事件
     */
    private void handleGroupClick(MouseEvent e, DefaultMutableTreeNode node, Object[] obj, TreePath selPath) {
        if (node.getChildCount() == 0) return;

        Rectangle rowBounds = requestTree.getRowBounds(requestTree.getRowForPath(selPath));
        if (rowBounds == null) return;

        if (!isClickOnHandle(e.getX(), selPath)) {
            RequestGroup group = ensureRequestGroup(obj);
            RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
            editPanel.showOrCreatePreviewTabForGroup(node, group);
        }
    }

    /**
     * 处理请求点击事件
     */
    private void handleRequestClick(Object[] obj) {
        HttpRequestItem item = (HttpRequestItem) obj[1];
        SingletonFactory.getInstance(RequestEditPanel.class).showOrCreatePreviewTab(item);
    }

    /**
     * 判断是否点击在展开/收起图标上
     */
    private boolean isClickOnHandle(int clickX, TreePath path) {
        Integer leftIndent = UIManager.getInt("Tree.leftChildIndent");
        Integer rightIndent = UIManager.getInt("Tree.rightChildIndent");
        int totalIndent = leftIndent + rightIndent;

        int depth = path.getPathCount() - (requestTree.isRootVisible() ? 1 : 2);
        int depthOffset = depth * totalIndent;
        int handleEndX = depthOffset + totalIndent;

        return clickX <= handleEndX;
    }

    /**
     * 获取鼠标位置的树路径
     */
    private TreePath getTreePathAt(int x, int y) {
        TreePath selPath = requestTree.getPathForLocation(x, y);

        if (selPath == null) {
            selPath = requestTree.getClosestPathForLocation(x, y);
            if (selPath != null) {
                // 判断Y坐标是否在树节点的矩形范围内，解决Y轴被误选中问题
                Rectangle bounds = requestTree.getPathBounds(selPath);
                if (bounds == null) {
                    return null;
                }
                if (y > bounds.y && y < bounds.y + bounds.height) {
                    return selPath;
                } else {
                    return null;
                }
            }
        }
        return selPath;
    }

    /**
     * 确保返回 RequestGroup 对象（升级旧格式）
     */
    private RequestGroup ensureRequestGroup(Object[] obj) {
        Object groupData = obj[1];
        if (groupData instanceof RequestGroup requestGroup) {
            return requestGroup;
        } else {
            RequestGroup group = new RequestGroup(String.valueOf(groupData));
            obj[1] = group;
            return group;
        }
    }
}

