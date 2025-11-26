package com.laker.postman.panel.collections.left.handler;

import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.left.action.RequestTreeActions;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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

        // F2 重命名（仅单选）
        if (e.getKeyCode() == KeyEvent.VK_F2 && !isMultipleSelection) {
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
}

