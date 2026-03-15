package com.laker.postman.panel.collections.left.handler;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.SavedResponse;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.left.action.RequestTreeActions;
import com.laker.postman.panel.collections.right.RequestEditPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

/**
 * 请求树键盘事件处理器
 * 负责处理 F2 重命名、Delete 删除、Ctrl+C 复制、Ctrl+V 粘贴等快捷键
 */
public class RequestTreeKeyboardHandler extends KeyAdapter {
    private final JTree requestTree;
    private final RequestCollectionsLeftPanel leftPanel;
    private final RequestTreeActions actions;

    public RequestTreeKeyboardHandler(JTree requestTree, RequestCollectionsLeftPanel leftPanel) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
        this.actions = new RequestTreeActions(requestTree, leftPanel);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!hasSelection()) return;

        int cmdMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+C / Cmd+C 复制
        if (isKeyWithModifier(e, KeyEvent.VK_C, cmdMask)) {
            actions.copySelectedRequests();
            e.consume();
            return;
        }

        // Ctrl+V / Cmd+V 粘贴
        if (isKeyWithModifier(e, KeyEvent.VK_V, cmdMask)) {
            actions.pasteRequests();
            e.consume();
            return;
        }

        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null || selectedNode == leftPanel.getRootTreeNode()) {
            return;
        }

        boolean isMultipleSelection = hasMultipleSelection();

        // Enter 键：模拟单击事件，预览请求或分组（仅单选）
        if (e.getKeyCode() == KeyEvent.VK_ENTER && !isMultipleSelection) {
            handleEnterKey();
            e.consume();
        }
        // F2 重命名（仅单选）
        else if (e.getKeyCode() == KeyEvent.VK_F2 && !isMultipleSelection) {
            actions.renameSelectedItem();
        }
        // Delete 或 Backspace 删除（支持多选）
        else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            actions.deleteSelectedItem();
        }
    }

    private boolean hasSelection() {
        return requestTree.getSelectionPaths() != null && requestTree.getSelectionPaths().length > 0;
    }

    private boolean hasMultipleSelection() {
        var paths = requestTree.getSelectionPaths();
        return paths != null && paths.length > 1;
    }

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
    }

    private boolean isKeyWithModifier(KeyEvent e, int keyCode, int modifier) {
        return e.getKeyCode() == keyCode && (e.getModifiersEx() & modifier) != 0;
    }

    /**
     * 处理 Enter 键：模拟单击事件，预览请求或分组
     */
    private void handleEnterKey() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        if (!(selectedNode.getUserObject() instanceof Object[] obj)) return;

        if (GROUP.equals(obj[0])) {
            handleGroupEnter(selectedNode, obj);
        } else if (REQUEST.equals(obj[0])) {
            handleRequestEnter(obj);
        } else if (SAVED_RESPONSE.equals(obj[0])) {
            handleSavedResponseEnter((SavedResponse) obj[1]);
        }
    }

    /**
     * 处理分组 Enter 键事件：预览分组
     */
    private void handleGroupEnter(DefaultMutableTreeNode node, Object[] obj) {
        if (node.getChildCount() == 0) return;

        RequestGroup group = ensureRequestGroup(obj);
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        editPanel.showOrCreatePreviewTabForGroup(node, group);
    }

    /**
     * 处理请求 Enter 键事件：预览请求
     */
    private void handleRequestEnter(Object[] obj) {
        HttpRequestItem item = (HttpRequestItem) obj[1];
        SingletonFactory.getInstance(RequestEditPanel.class).showOrCreatePreviewTab(item);
    }

    /**
     * 处理保存的响应 Enter 键事件：预览响应
     */
    private void handleSavedResponseEnter(SavedResponse savedResponse) {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        editPanel.showOrCreatePreviewTabForSavedResponse(savedResponse);
    }

    /**
     * 确保返回 RequestGroup 对象
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
