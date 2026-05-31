package com.laker.postman.panel.performance;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;


import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
final class PerformanceTreeSelectionSupport {

    private final JTree performanceTree;
    private final DefaultTreeModel treeModel;
    private final CardLayout propertyCardLayout;
    private final JPanel propertyPanel;
    private final ThreadGroupPropertyPanel threadGroupPanel;
    private final CsvDataSetPropertyPanel csvDataSetPanel;
    private final LoopPropertyPanel loopPanel;
    private final AssertionPropertyPanel assertionPanel;
    private final ExtractorPropertyPanel extractorPanel;
    private final TimerPropertyPanel timerPanel;
    private final SseStagePropertyPanel sseConnectPanel;
    private final SseStagePropertyPanel sseReadPanel;
    private final WebSocketStagePropertyPanel wsConnectPanel;
    private final WebSocketStagePropertyPanel wsSendPanel;
    private final WebSocketStagePropertyPanel wsReadPanel;
    private final WebSocketStagePropertyPanel wsClosePanel;
    private final PerformanceTreeSupport treeSupport;
    private final Consumer<DefaultMutableTreeNode> saveRequestNodeAction;
    private final Consumer<DefaultMutableTreeNode> saveSseStageAction;
    private final Consumer<DefaultMutableTreeNode> saveWebSocketStageAction;
    private final Consumer<HttpRequestItem> switchRequestEditorAction;
    private final BiConsumer<DefaultMutableTreeNode, PerformanceTreeNode> syncRequestStructureAction;
    private final Consumer<DefaultMutableTreeNode> currentRequestNodeSetter;
    private final String emptyCard;
    private final String threadGroupCard;
    private final String csvDataSetCard;
    private final String loopCard;
    private final String requestCard;
    private final String assertionCard;
    private final String extractorCard;
    private final String timerCard;
    private final String sseConnectCard;
    private final String sseReadCard;
    private final String wsConnectCard;
    private final String wsSendCard;
    private final String wsReadCard;
    private final String wsCloseCard;

    private DefaultMutableTreeNode lastNode;
    private Consumer<String> requestDataMissingAction = NotificationUtil::showError;

    void install() {
        performanceTree.addTreeSelectionListener(e -> handleSelectionChange());
        if (performanceTree.getSelectionPath() != null) {
            handleSelectionChange();
        }
    }

    void setRequestDataMissingAction(Consumer<String> requestDataMissingAction) {
        this.requestDataMissingAction = requestDataMissingAction == null
                ? NotificationUtil::showError
                : requestDataMissingAction;
    }

    void persistLastSelection() {
        if (lastNode == null || !(lastNode.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return;
        }
        switch (nodeData.type) {
            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
            case CSV_DATA_SET -> {
                csvDataSetPanel.saveCsvDataSetData();
                treeModel.nodeChanged(lastNode);
            }
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
            case SSE_CONNECT, SSE_READ -> saveSseStageAction.accept(lastNode);
            case WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE -> saveWebSocketStageAction.accept(lastNode);
            default -> {
            }
        }
    }

    private void handleSelectionChange() {
        TreePath[] selectedPaths = performanceTree.getSelectionPaths();
        if (selectedPaths != null && selectedPaths.length > 1) {
            return;
        }

        persistLastSelection();

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) performanceTree.getLastSelectedPathComponent();
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            showEmpty(node);
            return;
        }

        showNode(node, nodeData);
        lastNode = isPersistableSelection(nodeData) ? node : null;
    }

    private void showNode(DefaultMutableTreeNode node, PerformanceTreeNode nodeData) {
        switch (nodeData.type) {
            case THREAD_GROUP -> {
                propertyCardLayout.show(propertyPanel, threadGroupCard);
                threadGroupPanel.setThreadGroupData(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case CSV_DATA_SET -> {
                propertyCardLayout.show(propertyPanel, csvDataSetCard);
                csvDataSetPanel.setNode(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case LOOP -> {
                propertyCardLayout.show(propertyPanel, loopCard);
                loopPanel.setLoopData(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case REQUEST -> {
                if (nodeData.httpRequestItem == null) {
                    showMissingRequestData(nodeData);
                } else {
                    propertyCardLayout.show(propertyPanel, requestCard);
                    currentRequestNodeSetter.accept(node);
                    syncRequestStructureAction.accept(node, nodeData);
                    switchRequestEditorAction.accept(nodeData.httpRequestItem);
                }
            }
            case ASSERTION -> {
                propertyCardLayout.show(propertyPanel, assertionCard);
                assertionPanel.setAssertionData(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case EXTRACTOR -> {
                propertyCardLayout.show(propertyPanel, extractorCard);
                extractorPanel.setExtractorData(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case TIMER -> {
                propertyCardLayout.show(propertyPanel, timerCard);
                timerPanel.setTimerData(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case SSE_CONNECT -> showSsePanel(node, sseConnectCard, true);
            case SSE_READ -> showSsePanel(node, sseReadCard, false);
            case WS_CONNECT -> showWebSocketConnectPanel(node);
            case WS_SEND -> {
                propertyCardLayout.show(propertyPanel, wsSendCard);
                wsSendPanel.setNode(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case WS_READ -> {
                propertyCardLayout.show(propertyPanel, wsReadCard);
                wsReadPanel.setNode(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            case WS_CLOSE -> {
                propertyCardLayout.show(propertyPanel, wsCloseCard);
                wsClosePanel.setNode(nodeData);
                currentRequestNodeSetter.accept(null);
            }
            default -> showEmpty(node);
        }
    }

    private boolean isPersistableSelection(PerformanceTreeNode nodeData) {
        return nodeData.type != NodeType.REQUEST
                || nodeData.httpRequestItem != null;
    }

    private void showMissingRequestData(PerformanceTreeNode nodeData) {
        propertyCardLayout.show(propertyPanel, emptyCard);
        currentRequestNodeSetter.accept(null);
        requestDataMissingAction.accept(I18nUtil.getMessage(
                MessageKeys.PERFORMANCE_MSG_REQUEST_DATA_MISSING,
                nodeData.name
        ));
    }

    private void showEmpty(DefaultMutableTreeNode node) {
        propertyCardLayout.show(propertyPanel, emptyCard);
        currentRequestNodeSetter.accept(null);
        lastNode = node;
    }

    private void showSsePanel(DefaultMutableTreeNode node, String cardName, boolean connectStage) {
        DefaultMutableTreeNode requestNode = treeSupport.getParentRequestNode(node);
        if (requestNode != null
                && node.getUserObject() instanceof PerformanceTreeNode stageNodeData) {
            if (stageNodeData.ssePerformanceData == null) {
                stageNodeData.ssePerformanceData = new SsePerformanceData();
            }
            propertyCardLayout.show(propertyPanel, cardName);
            if (connectStage) {
                sseConnectPanel.setNode(stageNodeData);
            } else {
                sseReadPanel.setNode(stageNodeData);
            }
        } else {
            propertyCardLayout.show(propertyPanel, emptyCard);
        }
        currentRequestNodeSetter.accept(null);
    }

    private void showWebSocketConnectPanel(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode requestNode = treeSupport.getParentRequestNode(node);
        if (requestNode != null
                && requestNode.getUserObject() instanceof PerformanceTreeNode requestNodeData
                && node.getUserObject() instanceof PerformanceTreeNode connectNodeData) {
            if (connectNodeData.webSocketPerformanceData == null && requestNodeData.webSocketPerformanceData != null) {
                connectNodeData.webSocketPerformanceData = JsonUtil.deepCopy(
                        requestNodeData.webSocketPerformanceData,
                        WebSocketPerformanceData.class
                );
            }
            propertyCardLayout.show(propertyPanel, wsConnectCard);
            wsConnectPanel.setNode(connectNodeData);
        } else {
            propertyCardLayout.show(propertyPanel, emptyCard);
        }
        currentRequestNodeSetter.accept(null);
    }
}
