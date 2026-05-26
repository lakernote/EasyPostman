package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
final class PerformanceTreeSelectionSupport {

    private final JTree jmeterTree;
    private final DefaultTreeModel treeModel;
    private final CardLayout propertyCardLayout;
    private final JPanel propertyPanel;
    private final ThreadGroupPropertyPanel threadGroupPanel;
    private final LoopPropertyPanel loopPanel;
    private final AssertionPropertyPanel assertionPanel;
    private final ExtractorPropertyPanel extractorPanel;
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
    private final Consumer<DefaultMutableTreeNode> currentRequestNodeSetter;
    private final String emptyCard;
    private final String threadGroupCard;
    private final String loopCard;
    private final String requestCard;
    private final String assertionCard;
    private final String extractorCard;
    private final String timerCard;
    private final String sseConnectCard;
    private final String sseAwaitCard;
    private final String wsConnectCard;
    private final String wsSendCard;
    private final String wsAwaitCard;
    private final String wsCloseCard;

    private DefaultMutableTreeNode lastNode;

    void install() {
        jmeterTree.addTreeSelectionListener(e -> handleSelectionChange());
    }

    void persistLastSelection() {
        if (lastNode == null || !(lastNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return;
        }
        switch (jtNode.type) {
            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
            case LOOP -> {
                loopPanel.saveLoopData();
                treeModel.nodeChanged(lastNode);
            }
            case REQUEST -> saveRequestNodeAction.accept(lastNode);
            case ASSERTION -> assertionPanel.saveAssertionData();
            case EXTRACTOR -> {
                extractorPanel.saveExtractorData();
                treeModel.nodeChanged(lastNode);
            }
            case TIMER -> timerPanel.saveTimerData();
            case SSE_CONNECT, SSE_AWAIT -> saveSseStageAction.accept(lastNode);
            case WS_CONNECT, WS_SEND, WS_AWAIT, WS_CLOSE -> saveWebSocketStageAction.accept(lastNode);
            default -> {
            }
        }
    }

    private void handleSelectionChange() {
        TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
        if (selectedPaths != null && selectedPaths.length > 1) {
            return;
        }

        persistLastSelection();

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            showEmpty(node);
            return;
        }

        showNode(node, jtNode);
        lastNode = node;
    }

    private void showNode(DefaultMutableTreeNode node, JMeterTreeNode jtNode) {
        switch (jtNode.type) {
            case THREAD_GROUP -> {
                propertyCardLayout.show(propertyPanel, threadGroupCard);
                threadGroupPanel.setThreadGroupData(jtNode);
                currentRequestNodeSetter.accept(null);
            }
            case LOOP -> {
                propertyCardLayout.show(propertyPanel, loopCard);
                loopPanel.setLoopData(jtNode);
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
            case EXTRACTOR -> {
                propertyCardLayout.show(propertyPanel, extractorCard);
                extractorPanel.setExtractorData(jtNode);
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
            default -> showEmpty(node);
        }
    }

    private void showEmpty(DefaultMutableTreeNode node) {
        propertyCardLayout.show(propertyPanel, emptyCard);
        currentRequestNodeSetter.accept(null);
        lastNode = node;
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
}
