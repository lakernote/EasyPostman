package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.SavedResponse;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.tree.action.RequestTreeActions;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.service.collections.CollectionTreeNodes;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static com.laker.postman.panel.collections.tree.CollectionTreePanel.*;

/**
 * 请求树键盘事件处理器
 * 负责处理 F2 重命名、Delete 删除、Ctrl+C 复制、Ctrl+V 粘贴等快捷键
 */
public class RequestTreeKeyboardHandler extends KeyAdapter {
    private final JTree requestTree;
    private final CollectionTreePanel leftPanel;
    private final RequestTreeActions actions;

    public RequestTreeKeyboardHandler(JTree requestTree, CollectionTreePanel leftPanel) {
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

        if (CollectionTreeNodes.isGroup(selectedNode)) {
            handleGroupEnter(selectedNode);
        } else if (CollectionTreeNodes.isRequest(selectedNode)) {
            CollectionTreeNodes.request(selectedNode).ifPresent(this::handleRequestEnter);
        } else if (CollectionTreeNodes.isSavedResponse(selectedNode)) {
            CollectionTreeNodes.savedResponse(selectedNode).ifPresent(this::handleSavedResponseEnter);
        }
    }

    /**
     * 处理分组 Enter 键事件：预览分组
     */
    private void handleGroupEnter(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0) return;

        RequestGroup group = CollectionTreeNodes.group(node).orElse(null);
        if (group == null) {
            return;
        }
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        editPanel.showOrCreatePreviewTabForGroup(node, group);
    }

    /**
     * 处理请求 Enter 键事件：预览请求
     */
    private void handleRequestEnter(HttpRequestItem item) {
        UiSingletonFactory.getInstance(RequestEditorPanel.class).showOrCreatePreviewTab(item);
    }

    /**
     * 处理保存的响应 Enter 键事件：预览响应
     */
    private void handleSavedResponseEnter(SavedResponse savedResponse) {
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        editPanel.showOrCreatePreviewTabForSavedResponse(savedResponse);
    }

}
