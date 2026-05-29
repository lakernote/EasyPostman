package com.laker.postman.panel.collections.tree;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.tree.CollectionGroupTreeFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.experimental.UtilityClass;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@UtilityClass
public class CollectionGroupSelectionDialog {

    public static Optional<RequestNameSelection> chooseGroupAndRequestName(TreeModel groupTreeModel,
                                                                          String defaultName) {
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_GROUP));
            return Optional.empty();
        }

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_NAME) + ":");
        nameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        mainPanel.add(nameLabel, gbc);

        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        JTextField nameField = new JTextField(25);
        nameField.setPreferredSize(new Dimension(350, 32));
        if (defaultName != null && !defaultName.trim().isEmpty()) {
            nameField.setText(defaultName);
            nameField.selectAll();
        }
        mainPanel.add(nameField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel groupLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SELECT_GROUP) + ":");
        groupLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        mainPanel.add(groupLabel, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        JTree groupTree = CollectionGroupTreeFactory.createTree(groupTreeModel);
        expandRows(groupTree);
        mainPanel.add(wrapTree(groupTree, 350, 200), gbc);

        JDialog dialog = createDialog(I18nUtil.getMessage(MessageKeys.SAVE_REQUEST), mainPanel);
        AtomicReference<RequestNameSelection> result = new AtomicReference<>();

        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        JButton okButton = createPrimaryButton();
        Runnable okAction = () -> {
            String requestName = nameField.getText();
            if (requestName == null || requestName.trim().isEmpty()) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_ENTER_REQUEST_NAME));
                nameField.requestFocusInWindow();
                return;
            }

            Optional<RequestGroup> selectedGroup = selectedGroup(groupTree);
            if (selectedGroup.isEmpty()) {
                warnInvalidGroupSelection(groupTree);
                return;
            }

            result.set(new RequestNameSelection(selectedGroup.get(), requestName.trim()));
            dialog.dispose();
        };

        installButtons(dialog, cancelButton, okButton, okAction);
        nameField.addActionListener(e -> okAction.run());
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        dialog.setResizable(false);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return Optional.ofNullable(result.get());
    }

    public static Optional<RequestGroup> chooseGroup(TreeModel groupTreeModel) {
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            return Optional.empty();
        }

        JTree groupTree = CollectionGroupTreeFactory.createTree(groupTreeModel);
        expandRows(groupTree);
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel groupLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SELECT_GROUP) + ":");
        groupLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        mainPanel.add(groupLabel, BorderLayout.NORTH);
        mainPanel.add(wrapTree(groupTree, 350, 220), BorderLayout.CENTER);

        JDialog dialog = createDialog(I18nUtil.getMessage(MessageKeys.SELECT_GROUP), mainPanel);
        AtomicReference<RequestGroup> result = new AtomicReference<>();
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        Runnable okAction = () -> {
            Optional<RequestGroup> selectedGroup = selectedGroup(groupTree);
            if (selectedGroup.isEmpty()) {
                warnInvalidGroupSelection(groupTree);
                return;
            }

            result.set(selectedGroup.get());
            dialog.dispose();
        };

        installButtons(dialog, cancelButton, okButton, okAction);
        dialog.setSize(420, 390);
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        dialog.setResizable(false);
        SwingUtilities.invokeLater(groupTree::requestFocusInWindow);
        dialog.setVisible(true);

        return Optional.ofNullable(result.get());
    }

    private static JDialog createDialog(String title, JPanel mainPanel) {
        JDialog dialog = new JDialog(UiSingletonFactory.getInstance(MainFrame.class), title, true);
        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);
        return dialog;
    }

    private static JButton createPrimaryButton() {
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        okButton.setBackground(ModernColors.getPrimary());
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setOpaque(true);
        return okButton;
    }

    private static void installButtons(JDialog dialog, JButton cancelButton, JButton okButton, Runnable okAction) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
        cancelButton.setPreferredSize(new Dimension(80, 32));
        okButton.setPreferredSize(new Dimension(80, 32));
        okButton.addActionListener(e -> okAction.run());
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private static JScrollPane wrapTree(JTree groupTree, int width, int height) {
        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(width, height));
        treeScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getDividerBorderColor(), 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        return treeScroll;
    }

    private static void expandRows(JTree groupTree) {
        for (int i = 0; i < groupTree.getRowCount(); i++) {
            groupTree.expandRow(i);
        }
    }

    private static Optional<RequestGroup> selectedGroup(JTree groupTree) {
        TreePath selectedPath = groupTree.getSelectionPath();
        if (selectedPath == null) {
            return Optional.empty();
        }
        Object selectedNode = selectedPath.getLastPathComponent();
        if (selectedNode instanceof DefaultMutableTreeNode node) {
            return CollectionTreeNodes.group(node);
        }
        return Optional.empty();
    }

    private static void warnInvalidGroupSelection(JTree groupTree) {
        if (groupTree.getSelectionPath() == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_GROUP));
            groupTree.requestFocusInWindow();
            return;
        }
        NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_VALID_GROUP));
    }

    public record RequestNameSelection(RequestGroup group, String requestName) {
    }
}
