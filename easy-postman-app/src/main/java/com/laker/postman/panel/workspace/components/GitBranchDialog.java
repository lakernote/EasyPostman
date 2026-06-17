package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.GitBranchInfo;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.NotMergedException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Git branch management dialog for a workspace.
 */
@Slf4j
public class GitBranchDialog extends JDialog {

    private final transient Workspace workspace;
    private final transient WorkspaceService workspaceService;
    private JTable branchTable;
    private BranchTableModel tableModel;
    private JLabel statusLabel;
    private JButton switchButton;
    private JButton fetchButton;
    private JButton createButton;
    private JButton deleteButton;
    private JButton publishButton;
    private boolean busy;
    @Getter
    private boolean needRefresh = false;

    public GitBranchDialog(Window owner, Workspace workspace) {
        super(owner, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_TITLE), ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        this.workspaceService = WorkspaceService.getInstance();
        initUI();
        loadBranches();
    }

    private void initUI() {
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        setSize(640, 420);
        setMinimumSize(new Dimension(560, 340));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createContentPanel(), BorderLayout.CENTER);
        mainPanel.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogHeader(header, 10, 18, 10, 18);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_TITLE));
        title.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        JLabel workspaceLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ": " + workspace.getName());
        workspaceLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        workspaceLabel.setForeground(ModernColors.getTextSecondary());
        workspaceLabel.setToolTipText(workspace.getName());
        titlePanel.add(title);
        titlePanel.add(workspaceLabel);
        header.add(titlePanel, BorderLayout.CENTER);

        return header;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setBorder(new EmptyBorder(10, 18, 12, 18));
        panel.add(createBranchActionToolbar(), BorderLayout.NORTH);
        panel.add(createTableScrollPane(), BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane createTableScrollPane() {
        tableModel = new BranchTableModel(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_BRANCH),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_TRACKING),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_STATUS),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_NO_TRACKING),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_REMOTE_ONLY),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CURRENT)
        );
        branchTable = new JTable(tableModel);
        branchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        branchTable.setRowHeight(28);
        branchTable.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        branchTable.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD));
        branchTable.getColumnModel().getColumn(0).setPreferredWidth(330);
        branchTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        branchTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        branchTable.getColumnModel().getColumn(1).setCellRenderer(centeredRenderer);
        branchTable.getColumnModel().getColumn(2).setCellRenderer(centeredRenderer);

        branchTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtonState();
            }
        });
        branchTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && switchButton != null && switchButton.isEnabled()) {
                    switchSelectedBranch();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(branchTable);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, branchTable);
        ToolWindowSurfaceStyle.applyDialogFrame(scrollPane);
        return scrollPane;
    }

    private JPanel createBranchActionToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 6, 0));
        fetchButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_FETCH),
                false,
                "icons/refresh.svg"
        );
        fetchButton.addActionListener(e -> fetchBranches());
        fitToolbarButton(fetchButton);
        toolbar.add(fetchButton);

        createButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CREATE),
                false,
                "icons/plus.svg"
        );
        createButton.addActionListener(e -> createBranch());
        fitToolbarButton(createButton);
        toolbar.add(createButton);

        publishButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_PUBLISH),
                false,
                "icons/git.svg"
        );
        publishButton.setEnabled(false);
        publishButton.addActionListener(e -> publishSelectedBranch());
        fitToolbarButton(publishButton);
        toolbar.add(publishButton);

        deleteButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE),
                false,
                "icons/delete.svg"
        );
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedBranch());
        fitToolbarButton(deleteButton);
        toolbar.add(deleteButton);
        return toolbar;
    }

    private static void fitToolbarButton(JButton button) {
        int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
        int iconWidth = button.getIcon() == null ? 0 : button.getIcon().getIconWidth();
        Dimension preferredSize = button.getPreferredSize();
        button.setPreferredSize(new Dimension(
                toolbarButtonWidth(textWidth, iconWidth, button.getIconTextGap()),
                preferredSize.height
        ));
    }

    static int toolbarButtonWidth(int textWidth, int iconWidth, int iconTextGap) {
        return Math.max(100, textWidth + iconWidth + iconTextGap + 52);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(footer);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton closeButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CLOSE),
                false,
                "icons/close.svg"
        );
        closeButton.addActionListener(e -> dispose());
        rightActions.add(closeButton);

        switchButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_SWITCH),
                true,
                "icons/switch.svg"
        );
        switchButton.setDisabledIcon(IconUtil.createThemed("icons/switch.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        switchButton.setEnabled(false);
        switchButton.addActionListener(e -> switchSelectedBranch());
        rightActions.add(switchButton);
        footer.add(rightActions, BorderLayout.EAST);
        getRootPane().setDefaultButton(switchButton);
        return footer;
    }

    private void loadBranches() {
        loadBranches(null);
    }

    private void loadBranches(String postLoadStatus) {
        setBusy(true, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_LOADING));
        new SwingWorker<List<GitBranchInfo>, Void>() {
            @Override
            protected List<GitBranchInfo> doInBackground() throws Exception {
                return workspaceService.listGitBranches(workspace.getId());
            }

            @Override
            protected void done() {
                try {
                    List<GitBranchInfo> branches = get();
                    tableModel.setBranches(branches);
                    if (branches.isEmpty()) {
                        statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_NO_BRANCHES));
                    } else {
                        statusLabel.setText(" ");
                    }
                    updateActionButtonState();
                    if (postLoadStatus != null && !postLoadStatus.isBlank()) {
                        statusLabel.setText(postLoadStatus);
                    }
                } catch (Exception e) {
                    log.error("Failed to load Git branches", e);
                    tableModel.setBranches(List.of());
                    statusLabel.setText(e.getMessage());
                    showError(e.getMessage());
                } finally {
                    setBusy(false, statusLabel.getText());
                    if ((postLoadStatus == null || postLoadStatus.isBlank()) && tableModel.getRowCount() > 0) {
                        updateActionButtonState();
                    }
                }
            }
        }.execute();
    }

    private void switchSelectedBranch() {
        int selectedRow = branchTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        GitBranchInfo selectedBranch = tableModel.getBranchAt(branchTable.convertRowIndexToModel(selectedRow));
        if (selectedBranch == null || selectedBranch.isCurrent()) {
            return;
        }

        setBusy(true, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_SWITCHING));
        new SwingWorker<GitOperationResult, Void>() {
            @Override
            protected GitOperationResult doInBackground() throws Exception {
                return workspaceService.switchGitBranch(workspace.getId(), selectedBranch.getName());
            }

            @Override
            protected void done() {
                boolean reloading = false;
                try {
                    GitOperationResult result = get();
                    if (result.success) {
                        needRefresh = true;
                        statusLabel.setText(I18nUtil.getMessage(
                                MessageKeys.GIT_BRANCH_SWITCH_SUCCESS,
                                workspace.getCurrentBranch()
                        ));
                        reloading = true;
                        loadBranches(statusLabel.getText());
                    } else {
                        statusLabel.setText(result.message);
                    }
                } catch (Exception e) {
                    log.error("Failed to switch Git branch", e);
                    String message = rootMessage(e);
                    statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_SWITCH_FAILED, message));
                    showError(statusLabel.getText());
                } finally {
                    if (!reloading) {
                        setBusy(false, statusLabel.getText());
                    }
                }
            }
        }.execute();
    }

    private void fetchBranches() {
        if (!canFetchBranches(workspace)) {
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_NO_REMOTE));
            return;
        }
        setBusy(true, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_FETCHING));
        new SwingWorker<GitOperationResult, Void>() {
            @Override
            protected GitOperationResult doInBackground() throws Exception {
                return workspaceService.fetchGitBranches(workspace.getId());
            }

            @Override
            protected void done() {
                boolean reloading = false;
                try {
                    GitOperationResult result = get();
                    if (result.success) {
                        reloading = true;
                        loadBranches(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_FETCH_SUCCESS));
                    } else {
                        statusLabel.setText(result.message);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch Git branches", e);
                    String message = rootMessage(e);
                    statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_FETCH_FAILED, message));
                    showError(statusLabel.getText());
                } finally {
                    if (!reloading) {
                        setBusy(false, statusLabel.getText());
                    }
                }
            }
        }.execute();
    }

    private void createBranch() {
        TextInputDialog.showRequiredName(
                this,
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CREATE_TITLE),
                "",
                I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID)
        ).ifPresent(branchName -> {
            setBusy(true, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CREATING));
            new SwingWorker<GitOperationResult, Void>() {
                @Override
                protected GitOperationResult doInBackground() throws Exception {
                    return workspaceService.createGitBranch(workspace.getId(), branchName);
                }

                @Override
                protected void done() {
                    boolean reloading = false;
                    try {
                        GitOperationResult result = get();
                        if (result.success) {
                            needRefresh = true;
                            statusLabel.setText(I18nUtil.getMessage(
                                    MessageKeys.GIT_BRANCH_CREATE_SUCCESS,
                                    workspace.getCurrentBranch()
                            ));
                            reloading = true;
                            loadBranches(statusLabel.getText());
                        } else {
                            statusLabel.setText(result.message);
                        }
                    } catch (Exception e) {
                        log.error("Failed to create Git branch", e);
                        String message = rootMessage(e);
                        statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CREATE_FAILED, message));
                        showError(statusLabel.getText());
                    } finally {
                        if (!reloading) {
                            setBusy(false, statusLabel.getText());
                        }
                    }
                }
            }.execute();
        });
    }

    private void deleteSelectedBranch() {
        GitBranchInfo selectedBranch = getSelectedBranch();
        if (selectedBranch == null || selectedBranch.isCurrent() || selectedBranch.isRemote()) {
            updateActionButtonState();
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE_CONFIRM, selectedBranch.getName()),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        deleteBranch(selectedBranch, false);
    }

    private void deleteBranch(GitBranchInfo selectedBranch, boolean force) {
        setBusy(true, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETING));
        new SwingWorker<GitOperationResult, Void>() {
            @Override
            protected GitOperationResult doInBackground() throws Exception {
                return workspaceService.deleteGitBranch(workspace.getId(), selectedBranch.getName(), force);
            }

            @Override
            protected void done() {
                boolean reloading = false;
                try {
                    GitOperationResult result = get();
                    if (result.success) {
                        reloading = true;
                        loadBranches(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE_SUCCESS, selectedBranch.getName()));
                    } else {
                        statusLabel.setText(result.message);
                    }
                } catch (Exception e) {
                    if (!force && isBranchNotMergedFailure(e)) {
                        statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE_UNMERGED_STATUS));
                        if (confirmForceDelete(selectedBranch)) {
                            reloading = true;
                            deleteBranch(selectedBranch, true);
                        }
                        return;
                    }
                    log.error("Failed to delete Git branch", e);
                    String message = rootMessage(e);
                    statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE_FAILED, message));
                    showError(statusLabel.getText());
                } finally {
                    if (!reloading) {
                        setBusy(false, statusLabel.getText());
                    }
                }
            }
        }.execute();
    }

    private boolean confirmForceDelete(GitBranchInfo selectedBranch) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE_UNMERGED_CONFIRM, selectedBranch.getName()),
                I18nUtil.getMessage(MessageKeys.GIT_BRANCH_DELETE_UNMERGED_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    private void publishSelectedBranch() {
        GitBranchInfo selectedBranch = getSelectedBranch();
        if (selectedBranch == null || selectedBranch.isRemote() || hasTrackingBranch(selectedBranch)) {
            updateActionButtonState();
            return;
        }
        if (!canFetchBranches(workspace)) {
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_NO_REMOTE));
            updateActionButtonState();
            return;
        }
        if (!selectedBranch.isCurrent()) {
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_PUBLISH_CURRENT_ONLY));
            return;
        }

        setBusy(true, I18nUtil.getMessage(MessageKeys.GIT_BRANCH_PUBLISHING));
        new SwingWorker<GitOperationResult, Void>() {
            @Override
            protected GitOperationResult doInBackground() throws Exception {
                return workspaceService.publishGitBranch(workspace.getId());
            }

            @Override
            protected void done() {
                boolean reloading = false;
                try {
                    GitOperationResult result = get();
                    if (result.success) {
                        needRefresh = true;
                        reloading = true;
                        loadBranches(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_PUBLISH_SUCCESS, workspace.getRemoteBranch()));
                    } else {
                        statusLabel.setText(result.message);
                    }
                } catch (Exception e) {
                    log.error("Failed to publish Git branch", e);
                    String message = rootMessage(e);
                    statusLabel.setText(I18nUtil.getMessage(MessageKeys.GIT_BRANCH_PUBLISH_FAILED, message));
                    showError(statusLabel.getText());
                } finally {
                    if (!reloading) {
                        setBusy(false, statusLabel.getText());
                    }
                }
            }
        }.execute();
    }

    private void updateActionButtonState() {
        if (switchButton == null || branchTable == null || tableModel == null) {
            return;
        }
        GitBranchInfo selectedBranch = getSelectedBranch();
        boolean canSwitch = selectedBranch != null && !selectedBranch.isCurrent();
        boolean canDelete = selectedBranch != null && !selectedBranch.isCurrent() && !selectedBranch.isRemote();
        boolean canPublish = canPublishBranch(workspace, selectedBranch);
        switchButton.setEnabled(!busy && canSwitch);
        if (fetchButton != null) {
            fetchButton.setEnabled(!busy && canFetchBranches(workspace));
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(!busy && canDelete);
        }
        if (publishButton != null) {
            publishButton.setEnabled(!busy && canPublish);
        }
        if (statusLabel != null && !busy) {
            statusLabel.setText(branchAvailabilityMessage(
                    selectedBranch,
                    workspace,
                    I18nUtil.getMessage(MessageKeys.GIT_BRANCH_SELECT_TO_SWITCH),
                    I18nUtil.getMessage(MessageKeys.GIT_BRANCH_CURRENT_SELECTED),
                    I18nUtil.getMessage(MessageKeys.GIT_BRANCH_READY_TO_SWITCH),
                    I18nUtil.getMessage(MessageKeys.GIT_BRANCH_NO_REMOTE)
            ));
        }
    }

    private void setBusy(boolean busy, String status) {
        this.busy = busy;
        if (branchTable != null) {
            branchTable.setEnabled(!busy);
        }
        if (switchButton != null) {
            GitBranchInfo selectedBranch = getSelectedBranch();
            switchButton.setEnabled(!busy && selectedBranch != null && !selectedBranch.isCurrent());
        }
        if (fetchButton != null) {
            fetchButton.setEnabled(!busy && canFetchBranches(workspace));
        }
        if (createButton != null) {
            createButton.setEnabled(!busy);
        }
        if (publishButton != null) {
            GitBranchInfo selectedBranch = getSelectedBranch();
            publishButton.setEnabled(!busy && canPublishBranch(workspace, selectedBranch));
        }
        if (deleteButton != null) {
            GitBranchInfo selectedBranch = getSelectedBranch();
            deleteButton.setEnabled(!busy
                    && selectedBranch != null
                    && !selectedBranch.isCurrent()
                    && !selectedBranch.isRemote());
        }
        if (statusLabel != null && status != null) {
            statusLabel.setText(status);
        }
    }

    private GitBranchInfo getSelectedBranch() {
        if (branchTable == null || tableModel == null || branchTable.getSelectedRow() < 0) {
            return null;
        }
        return tableModel.getBranchAt(branchTable.convertRowIndexToModel(branchTable.getSelectedRow()));
    }

    static boolean canFetchBranches(Workspace workspace) {
        return hasConfiguredRemote(workspace);
    }

    static boolean canPublishBranch(Workspace workspace, GitBranchInfo branch) {
        return hasConfiguredRemote(workspace)
                && branch != null
                && branch.isCurrent()
                && !branch.isRemote()
                && !hasTrackingBranch(branch);
    }

    private static boolean hasConfiguredRemote(Workspace workspace) {
        return workspace != null
                && workspace.getGitRemoteUrl() != null
                && !workspace.getGitRemoteUrl().isBlank();
    }

    private static boolean hasTrackingBranch(GitBranchInfo branch) {
        return branch.getTrackingBranch() != null && !branch.getTrackingBranch().isBlank();
    }

    static boolean isBranchNotMergedFailure(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof NotMergedException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    static String switchAvailabilityMessage(GitBranchInfo branch,
                                            String noSelectionMessage,
                                            String currentSelectionMessage,
                                            String readyMessage) {
        if (branch == null) {
            return noSelectionMessage;
        }
        return branch.isCurrent() ? currentSelectionMessage : readyMessage;
    }

    static String branchAvailabilityMessage(GitBranchInfo branch,
                                            Workspace workspace,
                                            String noSelectionMessage,
                                            String currentSelectionMessage,
                                            String readyMessage,
                                            String noRemoteMessage) {
        if (branch != null
                && branch.isCurrent()
                && !branch.isRemote()
                && !hasTrackingBranch(branch)
                && !canFetchBranches(workspace)) {
            return noRemoteMessage;
        }
        return switchAvailabilityMessage(branch, noSelectionMessage, currentSelectionMessage, readyMessage);
    }

    private String rootMessage(Exception e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    static class BranchTableModel extends AbstractTableModel {
        private final String[] columnNames;
        private final String noTrackingLabel;
        private final String remoteOnlyLabel;
        private final String currentLabel;
        private List<GitBranchInfo> branches = new ArrayList<>();

        BranchTableModel() {
            this("Branch", "Tracking", "Status", "-", "Remote only", "Current");
        }

        BranchTableModel(String branchColumn,
                         String trackingColumn,
                         String statusColumn,
                         String noTrackingLabel,
                         String remoteOnlyLabel,
                         String currentLabel) {
            this.columnNames = new String[]{branchColumn, trackingColumn, statusColumn};
            this.noTrackingLabel = noTrackingLabel;
            this.remoteOnlyLabel = remoteOnlyLabel;
            this.currentLabel = currentLabel;
        }

        void setBranches(List<GitBranchInfo> branches) {
            this.branches = branches == null ? new ArrayList<>() : new ArrayList<>(branches);
            fireTableDataChanged();
        }

        GitBranchInfo getBranchAt(int row) {
            if (row < 0 || row >= branches.size()) {
                return null;
            }
            return branches.get(row);
        }

        @Override
        public int getRowCount() {
            return branches.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            GitBranchInfo branch = branches.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> branch.getName();
                case 1 -> trackingLabel(branch);
                case 2 -> statusLabel(branch);
                default -> "";
            };
        }

        private String trackingLabel(GitBranchInfo branch) {
            if (branch.isRemote()) {
                return remoteOnlyLabel;
            }
            String trackingBranch = branch.getTrackingBranch();
            return trackingBranch == null || trackingBranch.isBlank() ? noTrackingLabel : trackingBranch;
        }

        private String statusLabel(GitBranchInfo branch) {
            List<String> parts = new ArrayList<>();
            if (branch.isCurrent()) {
                parts.add(currentLabel);
            }
            List<String> syncParts = new ArrayList<>();
            if (branch.getAheadCount() > 0) {
                syncParts.add("↑" + branch.getAheadCount());
            }
            if (branch.getBehindCount() > 0) {
                syncParts.add("↓" + branch.getBehindCount());
            }
            if (!syncParts.isEmpty()) {
                parts.add(String.join(" ", syncParts));
            }
            return String.join(" · ", parts);
        }
    }
}
