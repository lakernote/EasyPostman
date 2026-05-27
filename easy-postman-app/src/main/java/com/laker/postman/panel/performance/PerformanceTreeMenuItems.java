package com.laker.postman.panel.performance;

import javax.swing.JMenuItem;

record PerformanceTreeMenuItems(
        JMenuItem addThreadGroup,
        JMenuItem addCsvDataSet,
        JMenuItem addRequest,
        JMenuItem addLoop,
        JMenuItem addSseConnect,
        JMenuItem addSseAwait,
        JMenuItem addWsConnect,
        JMenuItem addWsSend,
        JMenuItem addWsAwait,
        JMenuItem addWsClose,
        JMenuItem addAssertion,
        JMenuItem addExtractor,
        JMenuItem addTimer,
        JMenuItem enableNode,
        JMenuItem disableNode,
        JMenuItem copyNode,
        JMenuItem pasteNode,
        JMenuItem renameNode,
        JMenuItem deleteNode
) {

    boolean hasVisibleAddItem() {
        return addThreadGroup.isVisible() || addCsvDataSet.isVisible() || addRequest.isVisible()
                || addLoop.isVisible()
                || addSseConnect.isVisible() || addSseAwait.isVisible()
                || addWsConnect.isVisible()
                || addWsSend.isVisible() || addWsAwait.isVisible() || addWsClose.isVisible()
                || addAssertion.isVisible() || addExtractor.isVisible() || addTimer.isVisible();
    }

    boolean hasVisibleToggleItem() {
        return enableNode.isVisible() || disableNode.isVisible();
    }

    boolean hasVisibleClipboardItem() {
        return copyNode.isVisible() || pasteNode.isVisible();
    }

    boolean hasVisibleEditItem() {
        return renameNode.isVisible() || deleteNode.isVisible();
    }
}
