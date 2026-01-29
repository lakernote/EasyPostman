package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.model.GitCommitInfo;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Git 历史记录对话框
 * 显示工作区的 Git 提交历史，支持查看详情和恢复到指定版本
 */
@Slf4j
public class GitHistoryDialog extends JDialog {

    private final Workspace workspace;
    private final WorkspaceService workspaceService;
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private List<GitCommitInfo> commits;
    /**
     * -- GETTER --
     * 是否需要刷新请求集合面板
     */
    @Getter
    private boolean needRefresh = false;

    public GitHistoryDialog(Window owner, Workspace workspace) {
        super(owner, I18nUtil.getMessage(MessageKeys.GIT_HISTORY_TITLE) + " - " + workspace.getName(),
                ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        this.workspaceService = WorkspaceService.getInstance();
        initUI();
        loadHistory();
    }

    private void initUI() {
        setSize(900, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        // 创建工具栏
        add(createToolbar(), BorderLayout.NORTH);

        // 创建表格
        add(createTablePanel(), BorderLayout.CENTER);

        // 创建按钮面板
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        RefreshButton refreshButton = new RefreshButton();
        refreshButton.addActionListener(e -> loadHistory());
        toolbar.add(refreshButton);

        return toolbar;
    }

    private JScrollPane createTablePanel() {
        String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_COMMIT_ID),
                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_MESSAGE),
                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_AUTHOR),
                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_DATE)
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(tableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD));

        // 设置列宽
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(180);

        // 设置渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // 双击查看详情
        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewCommitDetails();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton viewDetailsButton = new JButton(I18nUtil.getMessage(MessageKeys.GIT_HISTORY_VIEW_DETAILS));
        viewDetailsButton.setIcon(IconUtil.createThemed("icons/detail.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        viewDetailsButton.addActionListener(e -> viewCommitDetails());
        panel.add(viewDetailsButton);

        JButton restoreButton = new JButton(I18nUtil.getMessage(MessageKeys.GIT_HISTORY_RESTORE));
        restoreButton.setIcon(IconUtil.createThemed("icons/refresh.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        restoreButton.addActionListener(e -> restoreToCommit());
        panel.add(restoreButton);

        JButton closeButton = new JButton(I18nUtil.getMessage(MessageKeys.GIT_HISTORY_CLOSE));
        closeButton.setIcon(IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);

        return panel;
    }

    private void loadHistory() {
        // 在后台线程加载
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{I18nUtil.getMessage(MessageKeys.GIT_HISTORY_LOADING), "", "", ""});
        });

        new SwingWorker<List<GitCommitInfo>, Void>() {
            @Override
            protected List<GitCommitInfo> doInBackground() throws Exception {
                return workspaceService.getGitHistory(workspace.getId(), 100); // 获取最近100条
            }

            @Override
            protected void done() {
                try {
                    commits = get();
                    displayHistory();
                } catch (Exception e) {
                    log.error("Failed to load Git history", e);
                    tableModel.setRowCount(0);
                    JOptionPane.showMessageDialog(
                            GitHistoryDialog.this,
                            "Failed to load history: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void displayHistory() {
        tableModel.setRowCount(0);

        if (commits == null || commits.isEmpty()) {
            tableModel.addRow(new Object[]{I18nUtil.getMessage(MessageKeys.GIT_HISTORY_NO_COMMITS), "", "", ""});
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (GitCommitInfo commit : commits) {
            String message = commit.getMessage().split("\n")[0]; // 只显示第一行
            if (message.length() > 80) {
                message = message.substring(0, 77) + "...";
            }

            tableModel.addRow(new Object[]{
                    commit.getShortCommitId(),
                    message,
                    commit.getAuthorName(),
                    dateFormat.format(new Date(commit.getCommitTime()))
            });
        }
    }

    private void viewCommitDetails() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow < 0 || commits == null || commits.isEmpty()) {
            return;
        }

        GitCommitInfo commit = commits.get(selectedRow);

        // 在后台加载详情
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return workspaceService.getCommitDetails(workspace.getId(), commit.getCommitId());
            }

            @Override
            protected void done() {
                try {
                    String details = get();

                    JTextArea textArea = new JTextArea(details);
                    textArea.setEditable(false);
                    textArea.setCaretPosition(0);

                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(700, 500));

                    JOptionPane.showMessageDialog(
                            GitHistoryDialog.this,
                            scrollPane,
                            I18nUtil.getMessage(MessageKeys.GIT_HISTORY_COMMIT_DETAILS) + " - " + commit.getShortCommitId(),
                            JOptionPane.PLAIN_MESSAGE
                    );
                } catch (Exception e) {
                    log.error("Failed to get commit details", e);
                    JOptionPane.showMessageDialog(
                            GitHistoryDialog.this,
                            "Failed to get details: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void restoreToCommit() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow < 0 || commits == null || commits.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a commit to restore",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        GitCommitInfo commit = commits.get(selectedRow);

        // 创建确认对话框，带备份选项
        JCheckBox backupCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_RESTORE_BACKUP),
                false
        );

        Object[] message = {
                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_RESTORE_CONFIRM, commit.getShortCommitId()),
                backupCheckBox
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        boolean createBackup = backupCheckBox.isSelected();

        // 在后台执行恢复
        new SwingWorker<WorkspaceService.GitOperationResult, Void>() {
            @Override
            protected WorkspaceService.GitOperationResult doInBackground() throws Exception {
                return workspaceService.restoreToCommit(workspace.getId(), commit.getCommitId(), createBackup);
            }

            @Override
            protected void done() {
                try {
                    WorkspaceService.GitOperationResult result = get();

                    if (result.success) {
                        JOptionPane.showMessageDialog(
                                GitHistoryDialog.this,
                                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_RESTORE_SUCCESS, commit.getShortCommitId()),
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        needRefresh = true;
                        loadHistory(); // 刷新历史
                    } else {
                        JOptionPane.showMessageDialog(
                                GitHistoryDialog.this,
                                I18nUtil.getMessage(MessageKeys.GIT_HISTORY_RESTORE_FAILED, result.message),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception e) {
                    log.error("Failed to restore to commit", e);
                    JOptionPane.showMessageDialog(
                            GitHistoryDialog.this,
                            I18nUtil.getMessage(MessageKeys.GIT_HISTORY_RESTORE_FAILED, e.getMessage()),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

}
