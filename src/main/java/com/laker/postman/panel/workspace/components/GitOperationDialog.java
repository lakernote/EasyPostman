package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitStatusResult;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.git.GitConflictDetector;
import com.laker.postman.util.EasyPostManFontUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

/**
 * Git 操作对话框
 * 提供统一的 Git 操作界面，包括 commit、push、pull 等操作
 * 增强了冲突检测和用户选择机制
 */
@Slf4j
public class GitOperationDialog extends JDialog {

    private final Workspace workspace;
    private final GitOperation operation;
    private final WorkspaceService workspaceService;

    @Getter
    private boolean confirmed = false;
    @Getter
    private String commitMessage;

    private JTextArea changedFilesArea;
    private JTextArea commitMessageArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton executeButton;
    private JButton cancelButton;

    // 新增组件
    private JPanel warningPanel;
    private JTextArea warningArea;
    private JPanel actionChoicePanel;
    private ButtonGroup actionChoiceGroup;
    private GitConflictDetector.GitStatusCheck statusCheck;

    public GitOperationDialog(Window parent, Workspace workspace, GitOperation operation) {
        super(parent, operation.getDisplayName() + " - " + workspace.getName(), ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        this.operation = operation;
        this.workspaceService = WorkspaceService.getInstance();

        initUI();
        performPreOperationCheck();
    }

    /**
     * 执行操作前检查
     */
    private void performPreOperationCheck() {
        SwingUtilities.invokeLater(() -> {
            try {
                statusLabel.setText("正在检查Git状态和潜在冲突...");
                statusLabel.setForeground(Color.BLUE);

                // 获取认证信息
                CredentialsProvider credentialsProvider = null;
                if (workspace.getGitAuthType() != null) {
                    credentialsProvider = getCredentialsProvider();
                }

                // 执行冲突检测，传递认证信息
                statusCheck = GitConflictDetector.checkGitStatus(workspace.getPath(), operation.name(), credentialsProvider);

                // 显示检测结果
                displayStatusCheck(statusCheck);

                // 加载文件变更信息
                GitStatusResult gitStatus = workspaceService.getGitStatus(workspace.getId());
                displayGitStatus(gitStatus);

                statusLabel.setText("Git状态检查完成");
                statusLabel.setForeground(Color.DARK_GRAY);
            } catch (Exception e) {
                log.error("Failed to perform pre-operation check", e);
                statusLabel.setText("状态检查失败: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
            }
        });
    }

    /**
     * 获取认证信息提供者
     */
    private org.eclipse.jgit.transport.CredentialsProvider getCredentialsProvider() {
        if (workspace.getGitAuthType() == com.laker.postman.model.GitAuthType.PASSWORD &&
                workspace.getGitUsername() != null && workspace.getGitPassword() != null) {
            return new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
                    workspace.getGitUsername(), workspace.getGitPassword());
        } else if (workspace.getGitAuthType() == com.laker.postman.model.GitAuthType.TOKEN &&
                workspace.getGitToken() != null && workspace.getGitUsername() != null) {
            return new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
                    workspace.getGitUsername(), workspace.getGitToken());
        }
        return null;
    }

    /**
     * 显示状态检查结果
     */
    private void displayStatusCheck(GitConflictDetector.GitStatusCheck check) {
        StringBuilder warningText = new StringBuilder();

        // 显示警告信息
        if (!check.getWarnings().isEmpty()) {
            warningText.append("⚠️ 警告信息:\n");
            for (String warning : check.getWarnings()) {
                warningText.append("  • ").append(warning).append("\n");
            }
            warningText.append("\n");
        }

        // 显示建议信息
        if (!check.getSuggestions().isEmpty()) {
            warningText.append("💡 建议:\n");
            for (String suggestion : check.getSuggestions()) {
                warningText.append("  • ").append(suggestion).append("\n");
            }
            warningText.append("\n");
        }

        // 显示详细状态
        warningText.append("📊 当前状态:\n");
        if (check.isHasUncommittedChanges()) {
            warningText.append("  • 未提交变更: ").append(check.getUncommittedCount()).append(" 个文件\n");
        }
        if (check.isHasUntrackedFiles()) {
            warningText.append("  • 未跟踪文件: ").append(check.getUntrackedCount()).append(" 个文件\n");
        }
        if (check.isHasLocalCommits()) {
            warningText.append("  • 本地领先: ").append(check.getLocalCommitsAhead()).append(" 个提交\n");
        }
        if (check.isHasRemoteCommits()) {
            warningText.append("  • 远程领先: ").append(check.getRemoteCommitsBehind()).append(" 个提交\n");
        }

        warningArea.setText(warningText.toString());

        // 根据检测结果显示操作选择
        updateActionChoices(check);

        boolean hasIssues = !check.getWarnings().isEmpty() ||
                !check.getSuggestions().isEmpty() ||
                (operation == GitOperation.PULL && check.isHasUncommittedChanges());
        warningPanel.setVisible(hasIssues);

        // 重新布局
        revalidate();
        repaint();
    }

    /**
     * 根据状态检查结果更新操作选择
     */
    private void updateActionChoices(GitConflictDetector.GitStatusCheck check) {
        actionChoicePanel.removeAll();
        actionChoiceGroup = new ButtonGroup();

        if (operation == GitOperation.PULL && check.isHasUncommittedChanges()) {
            // Pull操作且有未提交变更时，提供选择
            JLabel choiceLabel = new JLabel("检测到未提交变更，请选择处理方式:");
            choiceLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
            actionChoicePanel.add(choiceLabel);

            JRadioButton commitFirstRadio = new JRadioButton("先提交本地变更，再拉取", true);
            JRadioButton stashRadio = new JRadioButton("暂存本地变更，拉取后恢复");
            JRadioButton forceRadio = new JRadioButton("强制拉取（丢弃本地变更）");
            JRadioButton cancelRadio = new JRadioButton("取消操作，手动处理");

            actionChoiceGroup.add(commitFirstRadio);
            actionChoiceGroup.add(stashRadio);
            actionChoiceGroup.add(forceRadio);
            actionChoiceGroup.add(cancelRadio);

            // 设置颜色提示
            forceRadio.setForeground(Color.RED);

            actionChoicePanel.add(commitFirstRadio);
            actionChoicePanel.add(stashRadio);
            actionChoicePanel.add(forceRadio);
            actionChoicePanel.add(cancelRadio);

        } else if (operation == GitOperation.PUSH && check.isHasRemoteCommits()) {
            // Push操作且远程有新提交时，提供选择
            JLabel choiceLabel = new JLabel("远程仓库有新提交，请选择处理方式:");
            choiceLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
            actionChoicePanel.add(choiceLabel);

            JRadioButton pullFirstRadio = new JRadioButton("先拉取远程变更，再推送", true);
            JRadioButton forcePushRadio = new JRadioButton("强制推送（覆盖远程变更）");

            actionChoiceGroup.add(pullFirstRadio);
            actionChoiceGroup.add(forcePushRadio);

            // 设置颜色提示
            forcePushRadio.setForeground(Color.RED);

            actionChoicePanel.add(pullFirstRadio);
            actionChoicePanel.add(forcePushRadio);
        }

        // 根据操作类型更新按钮状态
        updateExecuteButtonState(check);

        actionChoicePanel.revalidate();
        actionChoicePanel.repaint();
    }

    /**
     * 更新执行按钮状态
     */
    private void updateExecuteButtonState(GitConflictDetector.GitStatusCheck check) {
        boolean canExecute = false;
        String disabledReason = "";

        switch (operation) {
            case COMMIT -> {
                canExecute = check.isCanCommit();
                if (!canExecute) {
                    if (!check.isHasUncommittedChanges() && !check.isHasUntrackedFiles()) {
                        disabledReason = "没有可提交的变更";
                    } else {
                        disabledReason = "检测到问题，请查看操作检查信息";
                    }
                }
            }
            case PUSH -> {
                // Push 操作的复杂判断逻辑
                boolean hasRemoteRepo = !check.getWarnings().stream()
                        .anyMatch(warning -> warning.contains("没有设置远程仓库"));
                boolean hasTracking = !check.getWarnings().stream()
                        .anyMatch(warning -> warning.contains("没有设置远程跟踪分支"));
                boolean isFirstPush = check.getWarnings().stream()
                        .anyMatch(warning -> warning.contains("没有设置远程跟踪分支"));
                boolean isEmptyRemote = check.getSuggestions().stream()
                        .anyMatch(suggestion -> suggestion.contains("远程仓库没有同名分支") ||
                                suggestion.contains("远程仓库为空") ||
                                suggestion.contains("等待首次推送"));

                if (!hasRemoteRepo) {
                    canExecute = false;
                    disabledReason = "没有配置远程仓库";
                } else if (!check.isHasLocalCommits()) {
                    canExecute = false;
                    disabledReason = "没有本地提交需要推送";
                } else if (isFirstPush || isEmptyRemote) {
                    // 首次推送或远程仓库为空的情况，允许推送
                    canExecute = true;
                } else {
                    // 普通推送情况，使用 GitConflictDetector 的判断
                    canExecute = check.isCanPush() || check.isHasLocalCommits();
                    if (!canExecute) {
                        disabledReason = "推送条件不满足，请查看操作检查信息";
                    }
                }
            }
            case PULL -> {
                boolean hasRemoteRepo = !check.getWarnings().stream()
                        .anyMatch(warning -> warning.contains("没有设置远程仓库"));
                boolean hasTracking = !check.getWarnings().stream()
                        .anyMatch(warning -> warning.contains("没有设置远程跟踪分支"));
                boolean isEmptyRemote = check.getSuggestions().stream()
                        .anyMatch(suggestion -> suggestion.contains("远程仓库为空") ||
                                suggestion.contains("无内容可拉取") ||
                                suggestion.contains("已是最新状态"));

                if (!hasRemoteRepo) {
                    canExecute = false;
                    disabledReason = "没有配置远程仓库";
                } else if (!hasTracking) {
                    canExecute = false;
                    disabledReason = "当前分支没有设置远程跟踪分支";
                } else if (isEmptyRemote) {
                    // 远程仓库为空，虽然可以尝试拉取，但实际上没有内容
                    canExecute = true; // 允许用户执行，让他们看到"无内容可拉取"的结果
                } else {
                    canExecute = check.isCanPull();
                    if (!canExecute) {
                        disabledReason = "无法连接到远程仓库或拉取条件不满足";
                    }
                }
            }
        }

        executeButton.setEnabled(canExecute);
        executeButton.setToolTipText(canExecute ? null : disabledReason);

        // 记录按钮状态到日志，方便调试
        log.debug("Operation: {}, CanExecute: {}, Reason: {}",
                operation, canExecute, disabledReason);
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 700); // 增加高度以容纳新组件
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 顶部信息面板
        mainPanel.add(createInfoPanel(), BorderLayout.NORTH);

        // 中心内容面板
        JPanel centerPanel = new JPanel(new BorderLayout());

        // 警告面板
        warningPanel = createWarningPanel();
        centerPanel.add(warningPanel, BorderLayout.NORTH);

        // 原有内容面板
        centerPanel.add(createContentPanel(), BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 底部按钮面板
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * 创建警告和选择面板
     */
    private JPanel createWarningPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("操作检查"));
        panel.setVisible(false); // 初始隐藏

        // 警告信息区域
        warningArea = new JTextArea(6, 50);
        warningArea.setEditable(false);
        warningArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        warningArea.setBackground(new Color(255, 248, 220)); // 淡黄色背景
        warningArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane warningScrollPane = new JScrollPane(warningArea);
        warningScrollPane.setPreferredSize(new Dimension(0, 120));
        panel.add(warningScrollPane, BorderLayout.CENTER);

        // 操作选择面板
        actionChoicePanel = new JPanel();
        actionChoicePanel.setLayout(new BoxLayout(actionChoicePanel, BoxLayout.Y_AXIS));
        actionChoicePanel.setBorder(BorderFactory.createTitledBorder("操作选择"));
        panel.add(actionChoicePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("操作信息"));

        // 操作类型和工作区信息
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 操作类型
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel operationIcon = new JLabel(new FlatSVGIcon("icons/" + operation.getIconName(), 20, 20));
        infoPanel.add(operationIcon, gbc);

        gbc.gridx = 1;
        JLabel operationLabel = new JLabel(operation.getDisplayName() + " 操作");
        operationLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 14));
        infoPanel.add(operationLabel, gbc);

        // 工作区信息
        gbc.gridx = 0;
        gbc.gridy = 1;
        infoPanel.add(new JLabel("工作区:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(new JLabel(workspace.getName()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        infoPanel.add(new JLabel("当前分支:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(new JLabel(workspace.getCurrentBranch() != null ? workspace.getCurrentBranch() : "未知"), gbc);

        if (workspace.getRemoteBranch() != null) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            infoPanel.add(new JLabel("远程分支:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(workspace.getRemoteBranch()), gbc);
        }

        panel.add(infoPanel, BorderLayout.CENTER);

        // 状态标签
        statusLabel = new JLabel("准备执行 " + operation.getDisplayName() + " 操作");
        statusLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 12));
        statusLabel.setForeground(Color.BLUE);
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建选项卡面板
        JTabbedPane tabbedPane = new JTabbedPane();

        // 文件变更选项卡
        tabbedPane.addTab("文件变更", new FlatSVGIcon("icons/file.svg", 16, 16), createChangedFilesPanelWithCommit());

        // 操作历史选项卡（可选）
        tabbedPane.addTab("操作说明", new FlatSVGIcon("icons/info.svg", 16, 16), createOperationDescPanel());

        panel.add(tabbedPane, BorderLayout.CENTER);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("等待执行...");
        progressBar.setVisible(false);
        panel.add(progressBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createChangedFilesPanelWithCommit() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("变更文件列表"));

        // 文件变更区域
        changedFilesArea = new JTextArea();
        changedFilesArea.setEditable(false);
        changedFilesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        changedFilesArea.setBackground(new Color(248, 248, 248));
        changedFilesArea.setText("正在加载文件变更信息...");
        JScrollPane scrollPane = new JScrollPane(changedFilesArea);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        panel.add(scrollPane);


        // 若为提交操作，直接在下方显示提交信息区域
        if (operation == GitOperation.COMMIT) {
            JPanel commitPanel = new JPanel(new BorderLayout());
            commitPanel.setBorder(new TitledBorder("提交信息"));

            commitMessageArea = new JTextArea(3, 10);
            commitMessageArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
            commitMessageArea.setLineWrap(true);
            commitMessageArea.setWrapStyleWord(true);
            commitMessageArea.setBackground(Color.WHITE);
            commitMessageArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane commitScroll = new JScrollPane(commitMessageArea);
            commitPanel.add(commitScroll, BorderLayout.CENTER);
            panel.add(commitPanel);
        }

        return panel;
    }

    private JPanel createOperationDescPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("操作说明"));

        JTextArea descArea = new JTextArea();
        descArea.setEditable(false);
        descArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        descArea.setBackground(new Color(252, 252, 252));
        descArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        String description = getOperationDescription();
        descArea.setText(description);

        JScrollPane scrollPane = new JScrollPane(descArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String getOperationDescription() {
        return switch (operation) {
            case COMMIT -> """
                    提交操作说明：
                    
                    1. 将暂存区的所有变更提交到本地仓库
                    2. 需要填写提交信息，建议使用清晰的描述
                    3. 提交后可以选择推送到远程仓库
                    
                    提交信息规范建议：
                    • feat: 新功能
                    • fix: 修复问题
                    • docs: 文档更新
                    • style: 代码格式
                    • refactor: 重构
                    • test: 测试相关
                    • chore: 构建配置等
                    """;
            case PUSH -> """
                    推送操作说明：
                    
                    1. 将本地提交推送到远程仓库
                    2. 需要确保本地分支有未推送的提交
                    3. 如果远程分支有更新，可能需要先拉取
                    
                    注意事项：
                    • 推送前建议先拉取最新变更
                    • 确保认证信息正确
                    • 推送会影响远程仓库，请谨慎操作
                    """;
            case PULL -> """
                    拉取操作说明：
                    
                    1. 从远程仓库拉取最新变更并合并到本地
                    2. 如果有冲突会自动尝试处理
                    3. 本地未提交的变更会被重置
                    
                    注意事项：
                    • 拉取前建议先提交本地变更
                    • 如果有冲突需要手动解决
                    • 操作会重置本地未提交的变更
                    """;
        };
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // 创建刷新按钮
        JButton refreshButton = new JButton("刷新", new FlatSVGIcon("icons/refresh.svg", 16, 16));
        refreshButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        refreshButton.addActionListener(e -> loadGitStatus());

        executeButton = new JButton(operation.getDisplayName(), new FlatSVGIcon("icons/" + operation.getIconName(), 16, 16));
        executeButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        executeButton.addActionListener(new ExecuteActionListener());

        cancelButton = new JButton("取消", new FlatSVGIcon("icons/cancel.svg", 16, 16));
        cancelButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        cancelButton.addActionListener(e -> dispose());

        // 按照从左到右的顺序添加按钮：刷新、执行、取消
        panel.add(refreshButton);
        panel.add(executeButton);
        panel.add(cancelButton);

        return panel;
    }

    /**
     * 通知 WorkspacePanel 记录 Git 操作结果
     */
    private void notifyWorkspacePanel(WorkspaceService.GitOperationResult result) {
        SwingUtilities.invokeLater(() -> {
            try {
                WorkspacePanel workspacePanel =
                        SingletonFactory.getInstance(WorkspacePanel.class);

                // 记录操作结果到日志
                workspacePanel.logGitOperationResult(result);
            } catch (Exception e) {
                log.warn("Failed to notify WorkspacePanel", e);
            }
        });
    }

    private void displayGitStatus(GitStatusResult status) {
        StringBuilder sb = new StringBuilder();

        // 统计信息
        int totalChanges = status.added.size() + status.modified.size() + status.removed.size() +
                status.untracked.size() + status.changed.size() + status.missing.size();

        sb.append(String.format("总变更文件: %d", totalChanges));
        sb.append("\n\n");

        if (totalChanges == 0) {
            sb.append("🎉 没有检测到文件变更\n");
        } else {
            appendFileList(sb, "📝 新增文件", status.added);
            appendFileList(sb, "✏️ 修改文件", status.modified);
            appendFileList(sb, "📦 暂存文件", status.changed);
            appendFileList(sb, "❓ 未跟踪文件", status.untracked);
            appendFileList(sb, "❌ 删除文件", status.removed);
            appendFileList(sb, "⚠️ 缺失文件", status.missing);
            appendFileList(sb, "🔄 未提交变更", status.uncommitted);
        }

        changedFilesArea.setText(sb.toString());
        changedFilesArea.setCaretPosition(0);
    }

    private void appendFileList(StringBuilder sb, String title, List<String> files) {
        if (!files.isEmpty()) {
            sb.append(title).append(" (").append(files.size()).append("):\n");
            for (String file : files) {
                sb.append("  ").append(file).append("\n");
            }
            sb.append("\n");
        }
    }

    private class ExecuteActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 验证操作前提条件
            if (!validateOperation()) {
                return;
            }

            confirmed = true;

            if (operation == GitOperation.COMMIT) {
                commitMessage = commitMessageArea.getText().trim();
            }

            // 检查用户选择
            String userChoice = getUserChoice();

            // 显示进度条
            showProgress();

            // 在后台线程执行操作
            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    executeGitOperationWithChoice(userChoice);
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String message : chunks) {
                        statusLabel.setText(message);
                    }
                }

                @Override
                protected void done() {
                    hideProgress();
                    try {
                        get(); // 检查是否有异常
                        statusLabel.setText(operation.getDisplayName() + " 操作完成");
                        statusLabel.setForeground(Color.GREEN);

                        // 操作成功后重新加载Git状态
                        SwingUtilities.invokeLater(GitOperationDialog.this::loadGitStatus);

                        // 显示成功对话框
                        JOptionPane.showMessageDialog(
                                GitOperationDialog.this,
                                operation.getDisplayName() + " 操作执行成功！",
                                "操作成功",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        // 关闭对话框
                        SwingUtilities.invokeLater(GitOperationDialog.this::dispose);

                    } catch (Exception ex) {
                        log.error("Git operation failed", ex);
                        statusLabel.setText("操作失败: " + ex.getMessage());
                        statusLabel.setForeground(Color.RED);

                        // 修复错误消息显示的 bug
                        String errorMessage = ex.getMessage();
                        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                            errorMessage = ex.getCause().getMessage();
                        }

                        JOptionPane.showMessageDialog(
                                GitOperationDialog.this,
                                "操作失败: " + errorMessage,
                                "操作失败",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }

                /**
                 * 根据用户选择执行Git操作
                 */
                private void executeGitOperationWithChoice(String choice) throws Exception {
                    publish("正在执行 " + operation.getDisplayName() + " 操作...");

                    switch (operation) {
                        case COMMIT -> {
                            publish("正在提交变更...");
                            var result = workspaceService.commitChanges(workspace.getId(), commitMessage);
                            notifyWorkspacePanel(result);
                        }
                        case PUSH -> {
                            if ("force".equals(choice)) {
                                publish("正在强制推送到远程仓库（覆盖远程变更）...");
                                var result = workspaceService.forcePushChanges(workspace.getId());
                                notifyWorkspacePanel(result);
                            } else if ("pull_first".equals(choice)) {
                                publish("先拉取远程变更...");
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);

                                publish("然后推送本地变更...");
                                var pushResult = workspaceService.pushChanges(workspace.getId());
                                notifyWorkspacePanel(pushResult);
                            } else {
                                publish("正在推送到远程仓库...");
                                var result = workspaceService.pushChanges(workspace.getId());
                                notifyWorkspacePanel(result);
                            }
                        }
                        case PULL -> {
                            if ("commit_first".equals(choice)) {
                                publish("先提交本地变更...");
                                String autoCommitMsg = "Auto commit before pull - " +
                                        java.time.LocalDateTime.now().format(
                                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                var commitResult = workspaceService.commitChanges(workspace.getId(), autoCommitMsg);
                                notifyWorkspacePanel(commitResult);

                                publish("然后拉取远程变更...");
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);
                            } else if ("stash".equals(choice)) {
                                publish("暂存本地变更...");
                                var stashResult = workspaceService.stashChanges(workspace.getId());
                                notifyWorkspacePanel(stashResult);

                                publish("拉取远程变更...");
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);

                                publish("恢复暂存的变更...");
                                var popResult = workspaceService.popStashChanges(workspace.getId());
                                notifyWorkspacePanel(popResult);
                            } else if ("force".equals(choice)) {
                                publish("强制拉取（丢弃本地变更）...");
                                var result = workspaceService.forcePullUpdates(workspace.getId());
                                notifyWorkspacePanel(result);
                            } else if ("cancel".equals(choice)) {
                                throw new RuntimeException("用户取消操作");
                            } else {
                                publish("正在从远程仓库拉取...");
                                var result = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(result);
                            }
                        }
                    }
                }
            };

            worker.execute();
        }

        /**
         * 获取用户选择
         */
        private String getUserChoice() {
            if (actionChoiceGroup == null) {
                return "default";
            }

            for (AbstractButton button : Collections.list(actionChoiceGroup.getElements())) {
                if (button.isSelected()) {
                    String text = button.getText();

                    // 根据选择文本返回对应的操作代码
                    if (text.contains("先提交本地变更")) return "commit_first";
                    if (text.contains("暂存本地变更")) return "stash";
                    if (text.contains("强制拉取")) return "force";
                    if (text.contains("取消操作")) return "cancel";
                    if (text.contains("先拉取远程变更")) return "pull_first";
                    if (text.contains("强制推送")) return "force";
                }
            }

            return "default";
        }

        private boolean validateOperation() {
            if (operation == GitOperation.COMMIT) {
                String message = commitMessageArea.getText().trim();
                if (message.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            GitOperationDialog.this,
                            "请输入提交信息",
                            "提交信息不能为空",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return false;
                }
            }

            // 检查用户是否选择了取消操作
            String choice = getUserChoice();
            if ("cancel".equals(choice)) {
                dispose();
                return false;
            }

            return true;
        }

        private void showProgress() {
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            progressBar.setString("正在执行操作...");
            executeButton.setEnabled(false);
        }

        private void hideProgress() {
            progressBar.setVisible(false);
            progressBar.setIndeterminate(false);
            executeButton.setEnabled(true);
        }
    }

    /**
     * 重新加载Git状态
     */
    private void loadGitStatus() {
        // 显示加载状态
        statusLabel.setText("正在刷新Git状态...");
        statusLabel.setForeground(Color.BLUE);
        changedFilesArea.setText("正在刷新文件变更信息...");

        // 在后台线程执行刷新操作，避免阻塞UI
        SwingWorker<Void, String> refreshWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 重新执行预检查来刷新状态
                publish("正在检查Git状态和潜在冲突...");

                // 执行冲突检测
                statusCheck = GitConflictDetector.checkGitStatus(workspace.getPath(), operation.name());

                publish("正在加载文件变更信息...");
                // 加载文件变更信息
                GitStatusResult gitStatus = workspaceService.getGitStatus(workspace.getId());

                // 在EDT中更新UI
                SwingUtilities.invokeLater(() -> {
                    // 显示检测结果
                    displayStatusCheck(statusCheck);

                    // 显示文件变更信息
                    displayGitStatus(gitStatus);

                    statusLabel.setText("Git状态刷新完成");
                    statusLabel.setForeground(Color.DARK_GRAY);
                });

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // 检查是否有异常
                } catch (Exception e) {
                    log.error("Failed to refresh git status", e);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("刷新失败: " + e.getMessage());
                        statusLabel.setForeground(Color.RED);
                        changedFilesArea.setText("刷新文件变更信息失败: " + e.getMessage());
                    });
                }
            }
        };

        refreshWorker.execute();
    }
}
