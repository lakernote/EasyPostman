package com.laker.postman.panel.performance;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

final class PerformanceTreeMenuVisibilitySupport {

    private final PerformanceTreeActionPolicy actionPolicy;
    private final Supplier<List<DefaultMutableTreeNode>> copiedNodesSupplier;

    PerformanceTreeMenuVisibilitySupport(PerformanceTreeSupport treeSupport,
                                         Supplier<List<DefaultMutableTreeNode>> copiedNodesSupplier) {
        this.actionPolicy = new PerformanceTreeActionPolicy(treeSupport);
        this.copiedNodesSupplier = copiedNodesSupplier;
    }

    void configureMultiSelectionMenu(TreePath[] selectedPaths, PerformanceTreeMenuItems items) {
        applyActionVisibility(items, actionPolicy.actionsForMultiSelection(selectedPaths));
    }

    void configureSingleSelectionMenu(DefaultMutableTreeNode node, PerformanceTreeMenuItems items) {
        applyActionVisibility(items, actionPolicy.actionsForSingleSelection(node, copiedNodesSupplier.get()));
    }

    private void applyActionVisibility(PerformanceTreeMenuItems items, EnumSet<PerformanceTreeAction> actions) {
        items.addThreadGroup().setVisible(actions.contains(PerformanceTreeAction.ADD_THREAD_GROUP));
        items.addCsvDataSet().setVisible(actions.contains(PerformanceTreeAction.ADD_CSV_DATA_SET));
        items.addRequest().setVisible(actions.contains(PerformanceTreeAction.ADD_REQUEST));
        items.addLoop().setVisible(actions.contains(PerformanceTreeAction.ADD_LOOP));
        items.addSseConnect().setVisible(actions.contains(PerformanceTreeAction.ADD_SSE_CONNECT));
        items.addSseAwait().setVisible(actions.contains(PerformanceTreeAction.ADD_SSE_AWAIT));
        items.addWsConnect().setVisible(actions.contains(PerformanceTreeAction.ADD_WS_CONNECT));
        items.addWsSend().setVisible(actions.contains(PerformanceTreeAction.ADD_WS_SEND));
        items.addWsAwait().setVisible(actions.contains(PerformanceTreeAction.ADD_WS_AWAIT));
        items.addWsClose().setVisible(actions.contains(PerformanceTreeAction.ADD_WS_CLOSE));
        items.addAssertion().setVisible(actions.contains(PerformanceTreeAction.ADD_ASSERTION));
        items.addExtractor().setVisible(actions.contains(PerformanceTreeAction.ADD_EXTRACTOR));
        items.addTimer().setVisible(actions.contains(PerformanceTreeAction.ADD_TIMER));
        items.enableNode().setVisible(actions.contains(PerformanceTreeAction.ENABLE));
        items.disableNode().setVisible(actions.contains(PerformanceTreeAction.DISABLE));
        items.copyNode().setVisible(actions.contains(PerformanceTreeAction.COPY));
        items.pasteNode().setVisible(actions.contains(PerformanceTreeAction.PASTE));
        items.renameNode().setVisible(actions.contains(PerformanceTreeAction.RENAME));
        items.deleteNode().setVisible(actions.contains(PerformanceTreeAction.DELETE));
    }
}
