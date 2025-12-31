package com.laker.postman.panel.collections.left.handler;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.left.action.RequestTreeActions;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

/**
 * 请求树右键弹出菜单
 */
public class RequestTreePopupMenu {
    private final JTree requestTree;
    private final RequestCollectionsLeftPanel leftPanel;
    private final RequestTreeActions actions;

    public RequestTreePopupMenu(JTree requestTree, RequestCollectionsLeftPanel leftPanel) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
        this.actions = new RequestTreeActions(requestTree, leftPanel);
    }

    /**
     * 显示弹出菜单
     */
    public void show(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        Object userObj = selectedNode != null ? selectedNode.getUserObject() : null;

        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        boolean isMultipleSelection = selectedPaths != null && selectedPaths.length > 1;

        // 始终显示"创建根分组"选项
        addRootGroupMenuItem(menu);

        if (selectedNode == null || selectedNode == leftPanel.getRootTreeNode()) {
            menu.show(requestTree, x, y);
            return;
        }

        // 分组节点菜单
        if (userObj instanceof Object[] && GROUP.equals(((Object[]) userObj)[0])) {
            addGroupMenuItems(menu, selectedNode, isMultipleSelection);
        }

        // 请求节点菜单
        if (userObj instanceof Object[] && REQUEST.equals(((Object[]) userObj)[0])) {
            addRequestMenuItems(menu, isMultipleSelection);
        }

        // 保存的响应节点菜单 - 只显示重命名和删除，不显示粘贴等其他选项
        if (userObj instanceof Object[] && SAVED_RESPONSE.equals(((Object[]) userObj)[0])) {
            addRenameAndDeleteMenuItems(menu, isMultipleSelection);
            menu.show(requestTree, x, y);
            return;
        }

        // 粘贴选项
        if (!actions.isCopiedRequestsEmpty()) {
            addPasteMenuItem(menu);
        }

        // 重命名和删除（非根节点）
        if (selectedNode != leftPanel.getRootTreeNode()) {
            addRenameAndDeleteMenuItems(menu, isMultipleSelection);
        }

        menu.show(requestTree, x, y);
    }

    /**
     * 添加创建根分组菜单项
     */
    private void addRootGroupMenuItem(JPopupMenu menu) {
        JMenuItem item = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_ROOT_GROUP),
                new FlatSVGIcon("icons/root_group.svg", 16, 16)
        );
        item.addActionListener(e -> actions.showAddGroupDialog(leftPanel.getRootTreeNode()));
        menu.add(item);
    }

    /**
     * 添加分组相关菜单项
     */
    private void addGroupMenuItems(JPopupMenu menu, DefaultMutableTreeNode selectedNode, boolean isMultipleSelection) {
        menu.addSeparator();

        // 添加到功能测试
        JMenuItem addToFunctional = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_TO_FUNCTIONAL,
                "icons/functional.svg",
                e -> actions.addSelectedRequestsToFunctionalTest()
        );
        menu.add(addToFunctional);
        menu.addSeparator();

        // 新增请求
        JMenuItem addRequest = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_REQUEST,
                "icons/request.svg",
                e -> actions.showAddRequestDialog(selectedNode)
        );
        addRequest.setEnabled(!isMultipleSelection);
        menu.add(addRequest);

        // 新增分组
        JMenuItem addGroup = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_GROUP,
                "icons/group.svg",
                e -> actions.addGroupUnderSelected()
        );
        addGroup.setEnabled(!isMultipleSelection);
        menu.add(addGroup);

        // 复制分组
        JMenuItem duplicate = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_DUPLICATE,
                "icons/duplicate.svg",
                e -> actions.duplicateSelectedGroup()
        );
        duplicate.setEnabled(!isMultipleSelection);
        menu.add(duplicate);

        // 导出为 Postman
        JMenuItem exportPostman = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN,
                "icons/postman.svg",
                e -> actions.exportGroupAsPostman(selectedNode)
        );
        exportPostman.setEnabled(!isMultipleSelection);
        menu.add(exportPostman);

        // 转移到其他工作区
        JMenuItem moveToWorkspace = createMenuItem(
                MessageKeys.WORKSPACE_TRANSFER_MENU_ITEM,
                "icons/workspace.svg",
                e -> actions.moveCollectionToWorkspace(selectedNode)
        );
        moveToWorkspace.setEnabled(!isMultipleSelection);
        menu.add(moveToWorkspace);

        menu.addSeparator();
    }

    /**
     * 添加请求相关菜单项
     */
    private void addRequestMenuItems(JPopupMenu menu, boolean isMultipleSelection) {
        // 添加到功能测试
        JMenuItem addToFunctional = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_TO_FUNCTIONAL,
                "icons/functional.svg",
                e -> actions.addSelectedRequestsToFunctionalTest()
        );
        menu.add(addToFunctional);
        menu.addSeparator();

        // 复制（创建副本）
        JMenuItem duplicate = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_DUPLICATE,
                "icons/duplicate.svg",
                e -> actions.duplicateSelectedRequests()
        );
        menu.add(duplicate);

        // 复制到剪贴板
        JMenuItem copy = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_COPY,
                "icons/copy.svg",
                e -> actions.copySelectedRequests()
        );
        int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, cmdMask));
        menu.add(copy);

        // 复制为 cURL
        JMenuItem copyAsCurl = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_COPY_CURL,
                "icons/curl.svg",
                e -> actions.copySelectedRequestAsCurl()
        );
        copyAsCurl.setEnabled(!isMultipleSelection);
        menu.add(copyAsCurl);
        menu.addSeparator();
    }


    /**
     * 添加粘贴菜单项
     */
    private void addPasteMenuItem(JPopupMenu menu) {
        JMenuItem paste = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_PASTE,
                "icons/paste.svg",
                e -> actions.pasteRequests()
        );
        int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, cmdMask));
        menu.add(paste);
        menu.addSeparator();
    }

    /**
     * 添加重命名和删除菜单项
     */
    private void addRenameAndDeleteMenuItems(JPopupMenu menu, boolean isMultipleSelection) {
        // 重命名
        JMenuItem rename = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_RENAME,
                "icons/refresh.svg",
                e -> actions.renameSelectedItem()
        );
        rename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        rename.setEnabled(!isMultipleSelection);
        menu.add(rename);

        // 删除
        JMenuItem delete = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_DELETE,
                "icons/close.svg",
                e -> actions.deleteSelectedItem()
        );
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        menu.add(delete);
    }

    /**
     * 创建菜单项的辅助方法
     */
    private JMenuItem createMenuItem(String messageKey, String iconPath, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(
                I18nUtil.getMessage(messageKey),
                new FlatSVGIcon(iconPath, 16, 16)
        );
        item.addActionListener(listener);
        return item;
    }
}
