package com.laker.postman.panel.performance;


import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
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
    private final Supplier<RequestEditSubPanel> requestEditSubPanelSupplier;
    private final Supplier<DefaultMutableTreeNode> currentRequestNodeSupplier;
    private final Consumer<DefaultMutableTreeNode> saveRequestNodeAction;
    private final PerformanceTreeSupport treeSupport;
    private final BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction;

    void forceCommitAllSpinners() {
        threadGroupPanel.forceCommitAllSpinners();
        loopPanel.forceCommitAllSpinners();
        extractorPanel.forceCommitAllSpinners();
        timerPanel.forceCommitAllSpinners();
        sseConnectPanel.forceCommitAllSpinners();
        sseReadPanel.forceCommitAllSpinners();
        wsConnectPanel.forceCommitAllSpinners();
        wsSendPanel.forceCommitAllSpinners();
        wsReadPanel.forceCommitAllSpinners();
    }

    void saveAllPropertyPanelData() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return;
        }
        switch (jtNode.type) {
            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
            case CSV_DATA_SET -> {
                csvDataSetPanel.saveCsvDataSetData();
                if (jmeterTree.getModel() instanceof javax.swing.tree.DefaultTreeModel treeModel) {
                    treeModel.nodeChanged(selectedNode);
                }
            }
            case LOOP -> loopPanel.saveLoopData();
            case REQUEST -> {
                if (requestEditSubPanelSupplier.get() != null && currentRequestNodeSupplier.get() != null) {
                    saveRequestNodeAction.accept(currentRequestNodeSupplier.get());
                }
            }
            case ASSERTION -> assertionPanel.saveAssertionData();
            case EXTRACTOR -> {
                extractorPanel.saveExtractorData();
                if (jmeterTree.getModel() instanceof javax.swing.tree.DefaultTreeModel treeModel) {
                    treeModel.nodeChanged(selectedNode);
                }
            }
            case TIMER -> timerPanel.saveTimerData();
            case SSE_CONNECT, SSE_READ -> saveSseStageNode(selectedNode);
            case WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE -> saveWebSocketStageNode(selectedNode);
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
            case SSE_READ -> sseReadPanel.saveData();
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
            case WS_READ -> wsReadPanel.saveData();
            case WS_CLOSE -> wsClosePanel.saveData();
            default -> {
                return;
            }
        }
        syncRequestStructureAction.accept(requestNode, requestJtNode);
    }
}
