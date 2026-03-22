package com.laker.postman.panel.performance;

import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class PerformancePropertyPanelSupport {

    private final JTree jmeterTree;
    private final ThreadGroupPropertyPanel threadGroupPanel;
    private final AssertionPropertyPanel assertionPanel;
    private final TimerPropertyPanel timerPanel;
    private final SseStagePropertyPanel sseConnectPanel;
    private final SseStagePropertyPanel sseAwaitPanel;
    private final WebSocketStagePropertyPanel wsConnectPanel;
    private final WebSocketStagePropertyPanel wsSendPanel;
    private final WebSocketStagePropertyPanel wsAwaitPanel;
    private final WebSocketStagePropertyPanel wsClosePanel;
    private final Supplier<RequestEditSubPanel> requestEditSubPanelSupplier;
    private final Supplier<DefaultMutableTreeNode> currentRequestNodeSupplier;
    private final Consumer<DefaultMutableTreeNode> saveRequestNodeAction;
    private final PerformanceTreeSupport treeSupport;
    private final BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction;

    void forceCommitAllSpinners() {
        threadGroupPanel.forceCommitAllSpinners();
        sseConnectPanel.forceCommitAllSpinners();
        sseAwaitPanel.forceCommitAllSpinners();
        wsConnectPanel.forceCommitAllSpinners();
        wsSendPanel.forceCommitAllSpinners();
        wsAwaitPanel.forceCommitAllSpinners();
    }

    void saveAllPropertyPanelData() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return;
        }
        switch (jtNode.type) {
            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
            case REQUEST -> {
                if (requestEditSubPanelSupplier.get() != null && currentRequestNodeSupplier.get() != null) {
                    saveRequestNodeAction.accept(currentRequestNodeSupplier.get());
                }
            }
            case ASSERTION -> assertionPanel.saveAssertionData();
            case TIMER -> timerPanel.saveTimerData();
            case SSE_CONNECT, SSE_AWAIT -> saveSseStageNode(selectedNode);
            case WS_CONNECT, WS_SEND, WS_AWAIT, WS_CLOSE -> saveWebSocketStageNode(selectedNode);
            default -> {
            }
        }
    }

    void saveSseStageNode(DefaultMutableTreeNode stageNode) {
        if (stageNode == null || !(stageNode.getUserObject() instanceof JMeterTreeNode stageJtNode)) {
            return;
        }
        DefaultMutableTreeNode requestNode = treeSupport.getParentRequestNode(stageNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        switch (stageJtNode.type) {
            case SSE_CONNECT -> sseConnectPanel.saveData();
            case SSE_AWAIT -> sseAwaitPanel.saveData();
            default -> {
                return;
            }
        }
        syncRequestStructureAction.accept(requestNode, requestJtNode);
    }

    void saveWebSocketStageNode(DefaultMutableTreeNode stageNode) {
        if (stageNode == null || !(stageNode.getUserObject() instanceof JMeterTreeNode stageJtNode)) {
            return;
        }
        DefaultMutableTreeNode requestNode = treeSupport.getParentRequestNode(stageNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        switch (stageJtNode.type) {
            case WS_CONNECT -> wsConnectPanel.saveData();
            case WS_SEND -> wsSendPanel.saveData();
            case WS_AWAIT -> wsAwaitPanel.saveData();
            case WS_CLOSE -> wsClosePanel.saveData();
            default -> {
                return;
            }
        }
        syncRequestStructureAction.accept(requestNode, requestJtNode);
    }
}
