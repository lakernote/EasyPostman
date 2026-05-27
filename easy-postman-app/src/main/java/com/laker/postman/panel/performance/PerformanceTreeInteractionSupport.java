package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction;
    private final Runnable saveConfigAction;
    private final Supplier<DefaultMutableTreeNode> currentRequestNodeSupplier;
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

    private PerformanceTreeSelectionSupport selectionSupport;
    private PerformanceTreeNodeActionSupport nodeActionSupport;

    void install() {
        selectionSupport = createSelectionSupport();
        nodeActionSupport = createNodeActionSupport();
        selectionSupport.install();
        installPopupMenu();
    }

    private void persistLastSelection() {
        selectionSupport.persistLastSelection();
    }

    private PerformanceTreeSelectionSupport createSelectionSupport() {
        return new PerformanceTreeSelectionSupport(
                jmeterTree,
                treeModel,
                propertyCardLayout,
                propertyPanel,
                threadGroupPanel,
                csvDataSetPanel,
                loopPanel,
                assertionPanel,
                extractorPanel,
                timerPanel,
                sseConnectPanel,
                sseReadPanel,
                wsConnectPanel,
                wsSendPanel,
                wsReadPanel,
                wsClosePanel,
                treeSupport,
                saveRequestNodeAction,
                saveSseStageAction,
                saveWebSocketStageAction,
                switchRequestEditorAction,
                syncRequestStructureAction,
                currentRequestNodeSetter,
                emptyCard,
                threadGroupCard,
                csvDataSetCard,
                loopCard,
                requestCard,
                assertionCard,
                extractorCard,
                timerCard,
                sseConnectCard,
                sseReadCard,
                wsConnectCard,
                wsSendCard,
                wsReadCard,
                wsCloseCard
        );
    }

    private PerformanceTreeNodeActionSupport createNodeActionSupport() {
        return new PerformanceTreeNodeActionSupport(
                parentComponent,
                jmeterTree,
                treeModel,
                propertyCardLayout,
                propertyPanel,
                treeSupport,
                this::persistLastSelection,
                switchRequestEditorAction,
                currentRequestNodeSupplier,
                currentRequestNodeSetter,
                saveConfigAction,
                requestCard
        );
    }

    private void installPopupMenu() {
        JPopupMenu treeMenu = new JPopupMenu();
        JMenuItem addThreadGroup = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_THREAD_GROUP));
        JMenuItem addCsvDataSet = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_CSV_DATA_SET));
        JMenuItem addRequest = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_REQUEST));
        JMenuItem addSseConnect = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_SSE_CONNECT));
        JMenuItem addSseRead = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_SSE_READ));
        JMenuItem addWsConnect = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_CONNECT));
        JMenuItem addWsSend = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_SEND));
        JMenuItem addWsRead = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_READ));
        JMenuItem addWsClose = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_CLOSE));
        JMenuItem addLoop = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_LOOP));
        JMenuItem addAssertion = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_ASSERTION));
        JMenuItem addExtractor = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_EXTRACTOR));
        JMenuItem addTimer = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_TIMER));
        JMenuItem renameNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_RENAME));
        JMenuItem deleteNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DELETE));
        JMenuItem enableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ENABLE));
        JMenuItem disableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DISABLE));
        JMenuItem copyNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_COPY));
        JMenuItem pasteNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_PASTE));
        PerformanceTreeMenuItems menuItems = new PerformanceTreeMenuItems(
                addThreadGroup,
                addCsvDataSet,
                addRequest,
                addLoop,
                addSseConnect,
                addSseRead,
                addWsConnect,
                addWsSend,
                addWsRead,
                addWsClose,
                addAssertion,
                addExtractor,
                addTimer,
                enableNode,
                disableNode,
                copyNode,
                pasteNode,
                renameNode,
                deleteNode
        );
        PerformanceTreeMenuVisibilitySupport menuVisibilitySupport = new PerformanceTreeMenuVisibilitySupport(
                treeSupport,
                nodeActionSupport::copiedNodes
        );
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        renameNode.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        deleteNode.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        copyNode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask));
        pasteNode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));

        JSeparator separator1 = new JSeparator();
        JSeparator separator2 = new JSeparator();
        JSeparator separator3 = new JSeparator();

        treeMenu.add(addThreadGroup);
        treeMenu.add(addCsvDataSet);
        treeMenu.add(addRequest);
        treeMenu.add(addLoop);
        treeMenu.add(addSseConnect);
        treeMenu.add(addSseRead);
        treeMenu.add(addWsConnect);
        treeMenu.add(addWsSend);
        treeMenu.add(addWsRead);
        treeMenu.add(addWsClose);
        treeMenu.add(addAssertion);
        treeMenu.add(addExtractor);
        treeMenu.add(addTimer);
        treeMenu.add(separator1);
        treeMenu.add(enableNode);
        treeMenu.add(disableNode);
        treeMenu.add(separator2);
        treeMenu.add(copyNode);
        treeMenu.add(pasteNode);
        treeMenu.add(separator3);
        treeMenu.add(renameNode);
        treeMenu.add(deleteNode);

        Runnable updateMenuSeparators = () -> {
            boolean hasAddGroup = menuItems.hasVisibleAddItem();
            boolean hasToggleGroup = menuItems.hasVisibleToggleItem();
            boolean hasClipboardGroup = menuItems.hasVisibleClipboardItem();
            boolean hasEditGroup = menuItems.hasVisibleEditItem();
            separator1.setVisible(hasAddGroup && (hasToggleGroup || hasClipboardGroup || hasEditGroup));
            separator2.setVisible(hasToggleGroup && (hasClipboardGroup || hasEditGroup));
            separator3.setVisible(hasClipboardGroup && hasEditGroup);
        };

        addThreadGroup.addActionListener(e -> nodeActionSupport.addThreadGroupNode());
        addCsvDataSet.addActionListener(e -> treeSupport.addCsvDataSetNode(jmeterTree, saveConfigAction));
        addRequest.addActionListener(e -> nodeActionSupport.addRequestNodes());
        addLoop.addActionListener(e -> treeSupport.addLoopNode(jmeterTree, saveConfigAction));
        addSseConnect.addActionListener(e -> treeSupport.addSseStageNode(jmeterTree, NodeType.SSE_CONNECT, saveConfigAction));
        addSseRead.addActionListener(e -> treeSupport.addSseStageNode(jmeterTree, NodeType.SSE_READ, saveConfigAction));
        addWsConnect.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_CONNECT, saveConfigAction));
        addWsSend.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_SEND, saveConfigAction));
        addWsRead.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_READ, saveConfigAction));
        addWsClose.addActionListener(e -> treeSupport.addWebSocketStepNode(jmeterTree, NodeType.WS_CLOSE, saveConfigAction));
        addAssertion.addActionListener(e -> nodeActionSupport.addAssertionNode());
        addExtractor.addActionListener(e -> nodeActionSupport.addExtractorNode());
        addTimer.addActionListener(e -> treeSupport.addTimerNode(jmeterTree, saveConfigAction));

        Action renameAction = nodeActionSupport.createRenameAction();
        Action deleteAction = nodeActionSupport.createDeleteAction();
        Action copyAction = nodeActionSupport.createCopyAction();
        Action pasteAction = nodeActionSupport.createPasteAction();
        renameNode.addActionListener(renameAction);
        deleteNode.addActionListener(deleteAction);
        copyNode.addActionListener(copyAction);
        pasteNode.addActionListener(pasteAction);

        InputMap treeInputMap = jmeterTree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap treeActionMap = jmeterTree.getActionMap();
        treeInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "renamePerformanceNode");
        treeActionMap.put("renamePerformanceNode", renameAction);
        treeInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "deletePerformanceNode");
        treeActionMap.put("deletePerformanceNode", deleteAction);
        treeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask), "copyPerformanceNode");
        treeActionMap.put("copyPerformanceNode", copyAction);
        treeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask), "pastePerformanceNode");
        treeActionMap.put("pastePerformanceNode", pasteAction);

        enableNode.addActionListener(e -> nodeActionSupport.setSelectedNodesEnabled(true));
        disableNode.addActionListener(e -> nodeActionSupport.setSelectedNodesEnabled(false));

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
                    menuVisibilitySupport.configureMultiSelectionMenu(selectedPaths, menuItems);
                } else {
                    menuVisibilitySupport.configureSingleSelectionMenu(
                            (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent(),
                            menuItems
                    );
                }
                updateMenuSeparators.run();
                treeMenu.show(jmeterTree, e.getX(), e.getY());
            }
        });
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

}
