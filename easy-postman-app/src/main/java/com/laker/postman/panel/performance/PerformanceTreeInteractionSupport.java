package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class PerformanceTreeInteractionSupport {

    private final Component parentComponent;
    private final JTree jmeterTree;
    private final DefaultTreeModel treeModel;
    private final CardLayout propertyCardLayout;
    private final JPanel propertyPanel;
    private final ThreadGroupPropertyPanel threadGroupPanel;
    private final AssertionPropertyPanel assertionPanel;
    private final TimerPropertyPanel timerPanel;
    private final SseStagePropertyPanel sseConnectPanel;
    private final SseStagePropertyPanel sseAwaitPanel;
    private final WebSocketStagePropertyPanel wsConnectPanel;
    private final WebSocketStagePropertyPanel wsSendPanel;
    private final WebSocketStagePropertyPanel wsAwaitPanel;
    private final WebSocketStagePropertyPanel wsClosePanel;
    private final PerformanceTreeSupport treeSupport;
    private final Consumer<DefaultMutableTreeNode> saveRequestNodeAction;
    private final Consumer<DefaultMutableTreeNode> saveSseStageAction;
    private final Consumer<DefaultMutableTreeNode> saveWebSocketStageAction;
    private final Consumer<HttpRequestItem> switchRequestEditorAction;
    private final BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction;
    private final Runnable saveConfigAction;
    private final Supplier<DefaultMutableTreeNode> currentRequestNodeSupplier;
    private final Consumer<DefaultMutableTreeNode> currentRequestNodeSetter;
    private final String emptyCard;
    private final String threadGroupCard;
    private final String requestCard;
    private final String assertionCard;
    private final String timerCard;
    private final String sseConnectCard;
    private final String sseAwaitCard;
    private final String wsConnectCard;
    private final String wsSendCard;
    private final String wsAwaitCard;
    private final String wsCloseCard;

    private DefaultMutableTreeNode lastNode;

    void install() {
        installSelectionListener();
        installPopupMenu();
    }

    private void installSelectionListener() {
        jmeterTree.addTreeSelectionListener(e -> {
            TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
            if (selectedPaths != null && selectedPaths.length > 1) {
                return;
            }

            persistLastSelection();

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
                propertyCardLayout.show(propertyPanel, emptyCard);
                currentRequestNodeSetter.accept(null);
                lastNode = node;
                return;
            }

            switch (jtNode.type) {
                case THREAD_GROUP -> {
                    propertyCardLayout.show(propertyPanel, threadGroupCard);
                    threadGroupPanel.setThreadGroupData(jtNode);
                    currentRequestNodeSetter.accept(null);
                }
                case REQUEST -> {
                    propertyCardLayout.show(propertyPanel, requestCard);
                    currentRequestNodeSetter.accept(node);
                    if (jtNode.httpRequestItem != null) {
                        syncRequestStructureAction.accept(node, jtNode);
                        switchRequestEditorAction.accept(jtNode.httpRequestItem);
                    }
                }
                case ASSERTION -> {
                    propertyCardLayout.show(propertyPanel, assertionCard);
                    assertionPanel.setAssertionData(jtNode);
                    currentRequestNodeSetter.accept(null);
                }
                case TIMER -> {
                    propertyCardLayout.show(propertyPanel, timerCard);
                    timerPanel.setTimerData(jtNode);
                    currentRequestNodeSetter.accept(null);
                }
                case SSE_CONNECT -> showSsePanel(node, sseConnectCard, true);
                case SSE_AWAIT -> showSsePanel(node, sseAwaitCard, false);
                case WS_CONNECT -> showWebSocketConnectPanel(node);
                case WS_SEND -> {
                    propertyCardLayout.show(propertyPanel, wsSendCard);
                    wsSendPanel.setNode(jtNode);
                    currentRequestNodeSetter.accept(null);
                }
                case WS_AWAIT -> {
                    propertyCardLayout.show(propertyPanel, wsAwaitCard);
                    wsAwaitPanel.setNode(jtNode);
                    currentRequestNodeSetter.accept(null);
                }
                case WS_CLOSE -> {
                    propertyCardLayout.show(propertyPanel, wsCloseCard);
                    wsClosePanel.setNode(jtNode);
                    currentRequestNodeSetter.accept(null);
                }
                default -> {
                    propertyCardLayout.show(propertyPanel, emptyCard);
                    currentRequestNodeSetter.accept(null);
                }
            }
            lastNode = node;
        });
    }

    private void persistLastSelection() {
        if (lastNode == null || !(lastNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return;
        }
        switch (jtNode.type) {
            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
            case REQUEST -> saveRequestNodeAction.accept(lastNode);
            case ASSERTION -> assertionPanel.saveAssertionData();
            case TIMER -> timerPanel.saveTimerData();
            case SSE_CONNECT, SSE_AWAIT -> saveSseStageAction.accept(lastNode);
            case WS_CONNECT, WS_SEND, WS_AWAIT, WS_CLOSE -> saveWebSocketStageAction.accept(lastNode);
            default -> {
            }
        }
    }

    private void showSsePanel(DefaultMutableTreeNode node, String cardName, boolean connectStage) {
        DefaultMutableTreeNode requestNode = treeSupport.getParentRequestNode(node);
        if (requestNode != null && requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode) {
            propertyCardLayout.show(propertyPanel, cardName);
            if (connectStage) {
                sseConnectPanel.setRequestNode(requestJtNode);
            } else {
                sseAwaitPanel.setRequestNode(requestJtNode);
            }
        } else {
            propertyCardLayout.show(propertyPanel, emptyCard);
        }
        currentRequestNodeSetter.accept(null);
    }

    private void showWebSocketConnectPanel(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode requestNode = treeSupport.getParentRequestNode(node);
        if (requestNode != null && requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode) {
            propertyCardLayout.show(propertyPanel, wsConnectCard);
            wsConnectPanel.setNode(requestJtNode);
        } else {
            propertyCardLayout.show(propertyPanel, emptyCard);
        }
        currentRequestNodeSetter.accept(null);
    }

    private void installPopupMenu() {
        JPopupMenu treeMenu = new JPopupMenu();
        JMenuItem addThreadGroup = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_THREAD_GROUP));
        JMenuItem addRequest = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_REQUEST));
        JMenuItem addWsSend = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_SEND));
        JMenuItem addWsAwait = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_AWAIT));
        JMenuItem addWsClose = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_CLOSE));
        JMenuItem addAssertion = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_ASSERTION));
        JMenuItem addTimer = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_TIMER));
        JMenuItem renameNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_RENAME));
        JMenuItem deleteNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DELETE));
        JMenuItem enableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ENABLE));
        JMenuItem disableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DISABLE));
        renameNode.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        deleteNode.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));

        JSeparator separator1 = new JSeparator();
        JSeparator separator2 = new JSeparator();

        treeMenu.add(addThreadGroup);
        treeMenu.add(addRequest);
        treeMenu.add(addWsSend);
        treeMenu.add(addWsAwait);
        treeMenu.add(addWsClose);
        treeMenu.add(addAssertion);
        treeMenu.add(addTimer);
        treeMenu.add(separator1);
        treeMenu.add(enableNode);
        treeMenu.add(disableNode);
        treeMenu.add(separator2);
        treeMenu.add(renameNode);
        treeMenu.add(deleteNode);

        Runnable updateMenuSeparators = () -> {
            boolean hasAddGroup = addThreadGroup.isVisible() || addRequest.isVisible()
                    || addWsSend.isVisible() || addWsAwait.isVisible() || addWsClose.isVisible()
                    || addAssertion.isVisible() || addTimer.isVisible();
            boolean hasToggleGroup = enableNode.isVisible() || disableNode.isVisible();
            boolean hasEditGroup = renameNode.isVisible() || deleteNode.isVisible();
            separator1.setVisible(hasAddGroup && (hasToggleGroup || hasEditGroup));
            separator2.setVisible(hasToggleGroup && hasEditGroup);
        };

        addThreadGroup.addActionListener(e -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP));
            treeModel.insertNodeInto(group, root, root.getChildCount());
            jmeterTree.expandPath(new TreePath(root.getPath()));
            saveConfigAction.run();
        });
        addRequest.addActionListener(e -> addRequestNodes());
        addWsSend.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_SEND, saveConfigAction));
        addWsAwait.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_AWAIT, saveConfigAction));
        addWsClose.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_CLOSE, saveConfigAction));
        addAssertion.addActionListener(e -> addAssertionNode());
        addTimer.addActionListener(e -> treeSupport.addTimerNode(jmeterTree, saveConfigAction));

        Action renameAction = createRenameAction();
        Action deleteAction = createDeleteAction();
        renameNode.addActionListener(renameAction);
        deleteNode.addActionListener(deleteAction);

        InputMap treeInputMap = jmeterTree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap treeActionMap = jmeterTree.getActionMap();
        treeInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "renamePerformanceNode");
        treeActionMap.put("renamePerformanceNode", renameAction);
        treeInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "deletePerformanceNode");
        treeActionMap.put("deletePerformanceNode", deleteAction);

        enableNode.addActionListener(e -> setSelectedNodesEnabled(true));
        disableNode.addActionListener(e -> setSelectedNodesEnabled(false));

        jmeterTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger() && !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                int row = jmeterTree.getClosestRowForLocation(e.getX(), e.getY());
                if (row < 0) {
                    return;
                }

                TreePath clickedPath = jmeterTree.getPathForLocation(e.getX(), e.getY());
                alignSelectionForPopup(clickedPath);

                DefaultMutableTreeNode currentRequestNode = currentRequestNodeSupplier.get();
                if (currentRequestNode != null) {
                    saveRequestNodeAction.accept(currentRequestNode);
                }

                TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                if (selectedPaths == null || selectedPaths.length == 0) {
                    return;
                }

                if (selectedPaths.length > 1) {
                    configureMultiSelectionMenu(selectedPaths, addThreadGroup, addRequest, addWsSend, addWsAwait, addWsClose,
                            addAssertion, addTimer, renameNode, deleteNode, enableNode, disableNode, updateMenuSeparators);
                } else {
                    configureSingleSelectionMenu((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent(),
                            addThreadGroup, addRequest, addWsSend, addWsAwait, addWsClose, addAssertion, addTimer,
                            renameNode, deleteNode, enableNode, disableNode, updateMenuSeparators, treeMenu, e);
                    if (isRootNode((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent())) {
                        return;
                    }
                }
                treeMenu.show(jmeterTree, e.getX(), e.getY());
            }
        });
    }

    private void addRequestNodes() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        Object userObj = node.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.THREAD_GROUP) {
            JOptionPane.showMessageDialog(
                    parentComponent,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SELECT_THREAD_GROUP),
                    I18nUtil.getMessage(MessageKeys.GENERAL_INFO),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        RequestCollectionsService.showMultiSelectRequestDialog(selectedList -> {
            if (selectedList == null || selectedList.isEmpty()) {
                return;
            }

            List<HttpRequestItem> supportedList = selectedList.stream()
                    .filter(reqItem -> {
                        RequestItemProtocolEnum protocol = treeSupport.resolveRequestProtocol(reqItem);
                        return protocol.isHttpProtocol() || protocol.isSseProtocol() || protocol.isWebSocketProtocol();
                    })
                    .toList();

            if (supportedList.isEmpty()) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.MSG_ONLY_HTTP_SSE_WS_SUPPORTED));
                return;
            }

            List<DefaultMutableTreeNode> newNodes = new ArrayList<>();
            for (HttpRequestItem reqItem : supportedList) {
                DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(reqItem.getName(), NodeType.REQUEST, reqItem));
                treeModel.insertNodeInto(req, node, node.getChildCount());
                syncRequestStructureAction.accept(req, (JMeterTreeNode) req.getUserObject());
                newNodes.add(req);
            }
            jmeterTree.expandPath(new TreePath(node.getPath()));
            TreePath newPath = new TreePath(newNodes.get(0).getPath());
            jmeterTree.setSelectionPath(newPath);
            propertyCardLayout.show(propertyPanel, requestCard);
            JMeterTreeNode newRequestNode = (JMeterTreeNode) newNodes.get(0).getUserObject();
            switchRequestEditorAction.accept(newRequestNode.httpRequestItem);
            saveConfigAction.run();
        });
    }

    private void addAssertionNode() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        DefaultMutableTreeNode parentNode = node;
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode
                && (jtNode.type == NodeType.SSE_AWAIT || jtNode.type == NodeType.WS_AWAIT)) {
            parentNode = node;
        }
        DefaultMutableTreeNode assertion = new DefaultMutableTreeNode(new JMeterTreeNode("Assertion", NodeType.ASSERTION));
        treeModel.insertNodeInto(assertion, parentNode, parentNode.getChildCount());
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        saveConfigAction.run();
    }

    private Action createRenameAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                Object userObj = node.getUserObject();
                if (!(userObj instanceof JMeterTreeNode jtNode)) {
                    return;
                }
                if (jtNode.type == NodeType.ROOT
                        || jtNode.type == NodeType.SSE_CONNECT
                        || jtNode.type == NodeType.SSE_AWAIT
                        || jtNode.type == NodeType.WS_CONNECT
                        || treeSupport.isWebSocketStepNode(jtNode.type)) {
                    return;
                }
                String oldName = jtNode.name;
                String newName = JOptionPane.showInputDialog(
                        parentComponent,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_RENAME_NODE),
                        oldName
                );
                if (newName != null && !newName.trim().isEmpty()) {
                    jtNode.name = newName.trim();
                    if (jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
                        jtNode.httpRequestItem.setName(newName.trim());
                        switchRequestEditorAction.accept(jtNode.httpRequestItem);
                    }
                    treeModel.nodeChanged(node);
                    saveConfigAction.run();
                }
            }
        };
    }

    private Action createDeleteAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                if (selectedPaths == null || selectedPaths.length == 0) {
                    return;
                }

                List<DefaultMutableTreeNode> nodesToDelete = new ArrayList<>();
                for (TreePath path : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object userObj = node.getUserObject();
                    if (userObj instanceof JMeterTreeNode jtNode
                            && jtNode.type != NodeType.ROOT
                            && jtNode.type != NodeType.SSE_CONNECT
                            && jtNode.type != NodeType.SSE_AWAIT
                            && jtNode.type != NodeType.WS_CONNECT) {
                        nodesToDelete.add(node);
                    }
                }

                if (nodesToDelete.isEmpty()) {
                    return;
                }

                for (DefaultMutableTreeNode node : nodesToDelete) {
                    if (node == currentRequestNodeSupplier.get()) {
                        currentRequestNodeSetter.accept(null);
                    }
                    treeModel.removeNodeFromParent(node);
                }
                saveConfigAction.run();
            }
        };
    }

    private void setSelectedNodesEnabled(boolean enabled) {
        TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) {
            return;
        }
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                jtNode.enabled = enabled;
                treeModel.nodeChanged(node);
            }
        }
        saveConfigAction.run();
    }

    private void alignSelectionForPopup(TreePath clickedPath) {
        if (clickedPath == null) {
            return;
        }
        TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
        boolean isInSelection = false;
        if (selectedPaths != null) {
            for (TreePath path : selectedPaths) {
                if (path.equals(clickedPath)) {
                    isInSelection = true;
                    break;
                }
            }
        }
        if (!isInSelection) {
            jmeterTree.setSelectionPath(clickedPath);
        }
    }

    private void configureMultiSelectionMenu(TreePath[] selectedPaths,
                                             JMenuItem addThreadGroup,
                                             JMenuItem addRequest,
                                             JMenuItem addWsSend,
                                             JMenuItem addWsAwait,
                                             JMenuItem addWsClose,
                                             JMenuItem addAssertion,
                                             JMenuItem addTimer,
                                             JMenuItem renameNode,
                                             JMenuItem deleteNode,
                                             JMenuItem enableNode,
                                             JMenuItem disableNode,
                                             Runnable updateMenuSeparators) {
        addThreadGroup.setVisible(false);
        addRequest.setVisible(false);
        addWsSend.setVisible(false);
        addWsAwait.setVisible(false);
        addWsClose.setVisible(false);
        addAssertion.setVisible(false);
        addTimer.setVisible(false);
        renameNode.setVisible(false);
        deleteNode.setVisible(true);

        boolean hasDisabled = false;
        boolean hasEnabled = false;
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                if (jtNode.enabled) {
                    hasEnabled = true;
                } else {
                    hasDisabled = true;
                }
            }
        }
        enableNode.setVisible(hasDisabled);
        disableNode.setVisible(hasEnabled);
        updateMenuSeparators.run();
    }

    private void configureSingleSelectionMenu(DefaultMutableTreeNode node,
                                              JMenuItem addThreadGroup,
                                              JMenuItem addRequest,
                                              JMenuItem addWsSend,
                                              JMenuItem addWsAwait,
                                              JMenuItem addWsClose,
                                              JMenuItem addAssertion,
                                              JMenuItem addTimer,
                                              JMenuItem renameNode,
                                              JMenuItem deleteNode,
                                              JMenuItem enableNode,
                                              JMenuItem disableNode,
                                              Runnable updateMenuSeparators,
                                              JPopupMenu treeMenu,
                                              MouseEvent event) {
        Object userObj = node.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jtNode)) {
            return;
        }

        if (jtNode.type == NodeType.ROOT) {
            addThreadGroup.setVisible(true);
            addRequest.setVisible(false);
            addWsSend.setVisible(false);
            addWsAwait.setVisible(false);
            addWsClose.setVisible(false);
            addAssertion.setVisible(false);
            addTimer.setVisible(false);
            renameNode.setVisible(false);
            deleteNode.setVisible(false);
            enableNode.setVisible(false);
            disableNode.setVisible(false);
            updateMenuSeparators.run();
            treeMenu.show(jmeterTree, event.getX(), event.getY());
            return;
        }

        addThreadGroup.setVisible(false);
        addRequest.setVisible(jtNode.type == NodeType.THREAD_GROUP);
        boolean isSseRequestNode = jtNode.type == NodeType.REQUEST && treeSupport.isSsePerfRequest(jtNode.httpRequestItem);
        boolean isWebSocketRequestNode = jtNode.type == NodeType.REQUEST && treeSupport.isWebSocketPerfRequest(jtNode.httpRequestItem);
        boolean canManageWsSteps = treeSupport.resolveWebSocketStepParent(node) != null;
        addWsSend.setVisible(canManageWsSteps);
        addWsAwait.setVisible(canManageWsSteps);
        addWsClose.setVisible(canManageWsSteps);
        addAssertion.setVisible((jtNode.type == NodeType.REQUEST && !isSseRequestNode && !isWebSocketRequestNode)
                || jtNode.type == NodeType.SSE_AWAIT
                || jtNode.type == NodeType.WS_AWAIT);
        addTimer.setVisible(jtNode.type == NodeType.REQUEST || canManageWsSteps);
        boolean structuralNode = jtNode.type == NodeType.SSE_CONNECT
                || jtNode.type == NodeType.SSE_AWAIT
                || jtNode.type == NodeType.WS_CONNECT;
        renameNode.setVisible(!structuralNode && !treeSupport.isWebSocketStepNode(jtNode.type));
        deleteNode.setVisible(!structuralNode);
        enableNode.setVisible(!structuralNode && !jtNode.enabled);
        disableNode.setVisible(!structuralNode && jtNode.enabled);
        updateMenuSeparators.run();
    }

    private boolean isRootNode(DefaultMutableTreeNode node) {
        return node.getUserObject() instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.ROOT;
    }
}
