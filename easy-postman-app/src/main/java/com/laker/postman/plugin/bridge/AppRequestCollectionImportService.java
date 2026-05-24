package com.laker.postman.plugin.bridge;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.tree.CollectionGroupTreeFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestImportDraft;
import com.laker.postman.model.RequestImportResult;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AppRequestCollectionImportService implements RequestCollectionImportService {

    @Override
    public RequestImportResult importRequests(List<RequestImportDraft> requests) {
        if (requests == null || requests.isEmpty()) {
            return RequestImportResult.imported(0);
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return importRequestsOnEdt(requests);
        }
        AtomicReference<RequestImportResult> result = new AtomicReference<>(RequestImportResult.unavailable());
        try {
            SwingUtilities.invokeAndWait(() -> result.set(importRequestsOnEdt(requests)));
        } catch (Exception e) {
            log.warn("Failed to import requests into collection", e);
            return RequestImportResult.unavailable();
        }
        return result.get();
    }

    private RequestImportResult importRequestsOnEdt(List<RequestImportDraft> requests) {
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditorPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            return RequestImportResult.unavailable();
        }

        Object[] groupObj = chooseGroup(groupTreeModel);
        if (groupObj == null) {
            return RequestImportResult.cancelled();
        }

        HttpRequestItem lastImported = null;
        for (RequestImportDraft draft : requests) {
            HttpRequestItem item = RequestImportDraftMapper.toHttpRequestItem(draft);
            collectionPanel.saveRequestToGroup(groupObj, item);
            lastImported = item;
        }

        if (lastImported != null) {
            collectionPanel.locateAndSelectRequest(lastImported.getId());
            requestEditorPanel.showOrCreateTab(lastImported);
        }
        return RequestImportResult.imported(requests.size());
    }

    private Object[] chooseGroup(TreeModel groupTreeModel) {
        JTree groupTree = CollectionGroupTreeFactory.createTree(groupTreeModel);
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel groupLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SELECT_GROUP) + ":");
        groupLabel.setFont(groupLabel.getFont().deriveFont(groupLabel.getFont().getStyle() | java.awt.Font.BOLD, 13f));
        mainPanel.add(groupLabel, BorderLayout.NORTH);

        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(350, 220));
        treeScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        mainPanel.add(treeScroll, BorderLayout.CENTER);

        JDialog dialog = new JDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.SELECT_GROUP), true);
        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        cancelButton.setPreferredSize(new Dimension(80, 32));
        okButton.setPreferredSize(new Dimension(80, 32));
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        final Object[][] result = {null};
        Runnable okAction = () -> {
            TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_GROUP));
                groupTree.requestFocusInWindow();
                return;
            }

            Object selectedNode = selectedPath.getLastPathComponent();
            if (selectedNode instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof Object[] groupObj
                    && CollectionTreePanel.GROUP.equals(groupObj[0])) {
                result[0] = groupObj;
                dialog.dispose();
                return;
            }

            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_VALID_GROUP));
        };

        okButton.addActionListener(e -> okAction.run());
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.setSize(420, 390);
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        dialog.setResizable(false);
        SwingUtilities.invokeLater(groupTree::requestFocusInWindow);
        dialog.setVisible(true);
        return result[0];
    }
}
