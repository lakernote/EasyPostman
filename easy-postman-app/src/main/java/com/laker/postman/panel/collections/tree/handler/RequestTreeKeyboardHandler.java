package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.tree.coordinator.RequestTreeCoordinator;
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
    private final RequestTreeCoordinator coordinator;
    private final RequestTreeOpenActions openActions;

    public RequestTreeKeyboardHandler(JTree requestTree, CollectionTreePanel leftPanel) {
        this(requestTree, leftPanel, new RequestTreeCoordinator(requestTree, leftPanel));
    }

    public RequestTreeKeyboardHandler(JTree requestTree, CollectionTreePanel leftPanel, RequestTreeCoordinator coordinator) {
        this(requestTree, leftPanel, coordinator, new RequestEditorTreeOpenActions());
    }

    RequestTreeKeyboardHandler(
            JTree requestTree,
            CollectionTreePanel leftPanel,
            RequestTreeCoordinator coordinator,
            RequestTreeOpenActions openActions
    ) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
        this.coordinator = coordinator;
        this.openActions = openActions;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!hasSelection()) return;

        int cmdMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+C / Cmd+C 复制
        if (isKeyWithModifier(e, KeyEvent.VK_C, cmdMask)) {
            coordinator.copySelectedRequests();
            e.consume();
            return;
        }

        // Ctrl+V / Cmd+V 粘贴
        if (isKeyWithModifier(e, KeyEvent.VK_V, cmdMask)) {
            coordinator.pasteRequests();
            e.consume();
            return;
        }

        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null || (leftPanel != null && selectedNode == leftPanel.getRootTreeNode())) {
            return;
        }

        boolean isMultipleSelection = hasMultipleSelection();

        // Enter 键：模拟单击事件，临时打开请求或分组（仅单选）
        if (e.getKeyCode() == KeyEvent.VK_ENTER && !isMultipleSelection) {
            handleEnterKey();
            e.consume();
        }
        // F2 重命名（仅单选）
        else if (e.getKeyCode() == KeyEvent.VK_F2 && !isMultipleSelection) {
            coordinator.renameSelectedItem();
        }
        // Delete 或 Backspace 删除（支持多选）
        else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            coordinator.deleteSelectedItem();
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
     * 处理 Enter 键：模拟单击事件，临时打开请求或分组
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
     * 处理分组 Enter 键事件：临时打开分组
     */
    private void handleGroupEnter(DefaultMutableTreeNode node) {
        RequestGroup group = CollectionTreeNodes.group(node).orElse(null);
        if (group == null) {
            return;
        }
        openActions.openTransientGroup(node, group);
    }

    /**
     * 处理请求 Enter 键事件：临时打开请求
     */
    private void handleRequestEnter(HttpRequestItem item) {
        openActions.openTransientRequest(item);
    }

    /**
     * 处理保存的响应 Enter 键事件：临时打开响应
     */
    private void handleSavedResponseEnter(SavedResponse savedResponse) {
        openActions.openTransientSavedResponse(savedResponse);
    }

}
