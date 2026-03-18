package com.laker.postman.panel.collections.left.handler;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.SavedResponse;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.left.action.RequestTreeActions;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

/**
 * 请求树鼠标事件处理器
 * 负责处理树节点的单击、双击和右键菜单
 */
@Slf4j
public class RequestTreeMouseHandler extends MouseAdapter {
    private final JTree requestTree;
    private final RequestTreePopupMenu popupMenu;
    private final RequestTreeActions actions;

    public RequestTreeMouseHandler(JTree requestTree, RequestCollectionsLeftPanel leftPanel) {
        this.requestTree = requestTree;
        this.popupMenu = new RequestTreePopupMenu(requestTree, leftPanel);
        this.actions = new RequestTreeActions(requestTree, leftPanel);
    }

    // ==================== hover 追踪 ====================

    @Override
    public void mouseMoved(MouseEvent e) {
        handleHover(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        handleHover(e);
    }

    private void handleHover(MouseEvent e) {
        int row = getRowForYPosition(e.getY());
        updateHoveredRow(row);
        updateTooltip(e.getX(), row);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateHoveredRow(-1);
        requestTree.setToolTipText(null);
    }

    /**
     * 根据鼠标位置动态设置 tooltip
     */
    private void updateTooltip(int mouseX, int row) {
        if (row >= 0 && isGroupRow(row)) {
            if (isOnMoreButtonX(mouseX)) {
                requestTree.setToolTipText("More actions");
                return;
            }
            if (isOnAddButtonX(mouseX)) {
                requestTree.setToolTipText("Add Request");
                return;
            }
        }
        requestTree.setToolTipText(null);
    }

    /**
     * 最右侧 MORE_BUTTON_WIDTH 像素 → more 按钮
     */
    private boolean isOnMoreButtonX(int mouseX) {
        return mouseX >= requestTree.getWidth() - RequestTreeCellRenderer.MORE_BUTTON_WIDTH;
    }

    /**
     * more 按钮左侧 ADD_BUTTON_WIDTH 像素 → plus 按钮
     */
    private boolean isOnAddButtonX(int mouseX) {
        int treeWidth = requestTree.getWidth();
        return mouseX >= treeWidth - RequestTreeCellRenderer.MORE_BUTTON_WIDTH - RequestTreeCellRenderer.ADD_BUTTON_WIDTH
                && mouseX < treeWidth - RequestTreeCellRenderer.MORE_BUTTON_WIDTH;
    }

    /**
     * 判断指定行是否是 GROUP 节点
     */
    private boolean isGroupRow(int row) {
        TreePath path = requestTree.getPathForRow(row);
        if (path == null) return false;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return node.getUserObject() instanceof Object[] obj && GROUP.equals(obj[0]);
    }

    /**
     * 根据 Y 坐标找到对应的行号（鼠标在整行高度范围内均有效，不限于节点文本宽度内）
     */
    private int getRowForYPosition(int y) {
        int rowCount = requestTree.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Rectangle bounds = requestTree.getRowBounds(i);
            if (bounds != null && y >= bounds.y && y < bounds.y + bounds.height) {
                return i;
            }
        }
        return -1;
    }

    private void updateHoveredRow(int row) {
        TreeCellRenderer renderer = requestTree.getCellRenderer();
        if (renderer instanceof RequestTreeCellRenderer r) {
            r.setHoveredRow(row);
            requestTree.repaint();
        }
    }

    // ==================== 点击处理 ====================

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
            if (isClickOnMoreButton(e)) {
                e.consume();
                handleMoreButtonClick(e);
                return;
            }
            if (isClickOnAddButton(e)) {
                e.consume(); // 阻止 BasicTreeUI 在 mousePressed/mouseReleased 里把选中覆盖回 group 节点
                handleAddButtonClick(e);
                return;
            }
            handleSingleClick(e);
        } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            handleDoubleClick(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e);
        }
    }

    /**
     * mouseReleased 中拦截 Add/More 按钮区域的点击，避免触发节点展开/收起等默认行为
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
            if (isClickOnMoreButton(e) || isClickOnAddButton(e)) {
                e.consume();
            }
        }
    }

    /**
     * 是否点到 "+" 按钮（more 左侧区域，GROUP 行）
     */
    private boolean isClickOnAddButton(MouseEvent e) {
        int row = getRowForYPosition(e.getY());
        if (row < 0 || !isGroupRow(row)) return false;
        return isOnAddButtonX(e.getX());
    }

    /**
     * 是否点到 "⋯" more 按钮（最右侧区域，GROUP 行）
     */
    private boolean isClickOnMoreButton(MouseEvent e) {
        int row = getRowForYPosition(e.getY());
        if (row < 0 || !isGroupRow(row)) return false;
        return isOnMoreButtonX(e.getX());
    }

    /**
     * 点击 "+" 按钮：直接创建 HTTP 请求，不弹对话框
     */
    private void handleAddButtonClick(MouseEvent e) {
        int row = getRowForYPosition(e.getY());
        if (row < 0) return;
        TreePath path = requestTree.getPathForRow(row);
        if (path == null) return;
        DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        // 不在此处 setSelectionPath(path)，避免 group 选中状态与后续 invokeLater 里的
        // 新请求节点选中产生竞争（addHttpRequestDirectly 内部会定位到新请求节点）
        actions.addHttpRequestDirectly(groupNode);
    }

    /**
     * 点击 "⋯" more 按钮：弹出 group 专属操作菜单
     */
    private void handleMoreButtonClick(MouseEvent e) {
        int row = getRowForYPosition(e.getY());
        if (row < 0) return;
        TreePath path = requestTree.getPathForRow(row);
        if (path == null) return;
        requestTree.setSelectionPath(path);
        DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        showGroupActionMenu(groupNode, e.getX(), e.getY());
    }

    /**
     * 弹出 group 专属菜单（新增请求、新增分组等）
     */
    private void showGroupActionMenu(DefaultMutableTreeNode groupNode, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addRequest = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_REQUEST),
                IconUtil.createThemed("icons/request.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        addRequest.addActionListener(ev -> actions.showAddRequestDialog(groupNode));
        menu.add(addRequest);

        JMenuItem addGroup = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_GROUP),
                IconUtil.create("icons/group.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        addGroup.addActionListener(ev -> actions.addGroupUnderSelected());
        menu.add(addGroup);

        menu.addSeparator();

        JMenuItem rename = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_RENAME),
                IconUtil.createThemed("icons/edit.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        rename.addActionListener(ev -> actions.renameSelectedItem());
        rename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        menu.add(rename);

        JMenuItem duplicate = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DUPLICATE),
                IconUtil.createThemed("icons/duplicate.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        duplicate.addActionListener(ev -> actions.duplicateSelectedGroup());
        menu.add(duplicate);

        menu.addSeparator();

        JMenuItem delete = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DELETE),
                IconUtil.createThemed("icons/delete.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        delete.addActionListener(ev -> actions.deleteSelectedItem());
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        menu.add(delete);

        menu.show(requestTree, x, y);
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
        } else if (SAVED_RESPONSE.equals(obj[0])) {
            handleSavedResponseClick((SavedResponse) obj[1]);
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
        } else if (SAVED_RESPONSE.equals(obj[0])) {
            // 双击保存的响应：打开固定 Tab
            handleSavedResponseDoubleClick((SavedResponse) obj[1]);
            e.consume();
        }
    }

    /**
     * 处理右键点击：显示弹出菜单
     */
    private void handleRightClick(MouseEvent e) {
        int row = requestTree.getClosestRowForLocation(e.getX(), e.getY());
        if (row != -1) {
            TreePath clickedPath = requestTree.getPathForRow(row);
            // 只有当点击的路径未被选中时，才设置为当前选中项
            // 如果点击的路径已经被选中，则保持当前的多选状态
            if (clickedPath != null && !requestTree.isPathSelected(clickedPath)) {
                requestTree.setSelectionPath(clickedPath);
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
     * 处理保存的响应单击事件：预览
     */
    private void handleSavedResponseClick(SavedResponse savedResponse) {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        editPanel.showOrCreatePreviewTabForSavedResponse(savedResponse);
    }

    /**
     * 处理保存的响应双击事件：打开固定 Tab
     */
    private void handleSavedResponseDoubleClick(SavedResponse savedResponse) {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        editPanel.showOrCreateTabForSavedResponse(savedResponse);
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

