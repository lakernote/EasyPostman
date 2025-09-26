package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.StepIndicator;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.model.GitStatusResult;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.git.SshCredentialsProvider;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.laker.postman.service.git.GitConflictDetector.checkGitStatus;

/**
 * Git 操作对话框
 */
@Slf4j
public class GitOperationDialog extends JDialog {

    // 常量定义
    private static final String OPTION_FORCE = "force"; // 强制操作
    private static final String OPTION_CANCEL = "cancel"; // 取消操作
    private static final String OPTION_COMMIT_FIRST = "commit_first"; // 先提交
    private static final String OPTION_STASH = "stash"; // 暂存
    private static final String OPTION_PULL_FIRST = "pull_first"; // 先拉取
    private static final String OPTION_COMMIT_AND_PUSH = "commit_and_push"; // 提交并推送

    private final transient Workspace workspace;
    private final GitOperation operation;
    private final transient WorkspaceService workspaceService;

    @Getter
    private boolean confirmed = false;
    @Getter
    private String commitMessage;

    // 步骤指示器
    private StepIndicator stepIndicator;

    // 状态和文件信息
    private JTextArea fileChangesArea;
    private JTextArea commitMessageArea;
    private JLabel statusIcon;
    private JLabel statusMessage;

    // 操作选择
    private JPanel optionsPanel;
    private ButtonGroup optionGroup;

    // 进度和按钮
    private JProgressBar progressBar;
    private JButton executeButton;

    // 检测结果
    private GitStatusCheck statusCheck;

    public GitOperationDialog(Window parent, Workspace workspace, GitOperation operation) {
        super(parent, operation.getDisplayName() + " - " + workspace.getName(), ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        this.operation = operation;
        this.workspaceService = WorkspaceService.getInstance();

        setupDialog();
        initializeUI();
        performPreOperationCheck();
    }

    /**
     * 设置对话框基本属性
     */
    private void setupDialog() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        // 设置现代化外观
        getRootPane().setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        // 创建各个区域
        JPanel headerPanel = createHeaderPanel();
        JPanel stepPanel = createStepPanel();
        JPanel summaryPanel = createSummaryPanel();
        JPanel actionPanel = createActionPanel();
        JPanel footerPanel = createFooterPanel();

        // 布局主面板
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(stepPanel, BorderLayout.NORTH);
        centerPanel.add(summaryPanel, BorderLayout.CENTER);
        centerPanel.add(actionPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 创建头部面板 - 显示操作类型和基本信息
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(operation.getColor());
        panel.setBorder(new EmptyBorder(5, 20, 5, 20));

        // 左侧：操作图标和名称
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        JLabel operationIcon = new JLabel(new FlatSVGIcon("icons/" + operation.getIconName(), 32, 32));
        operationIcon.setBorder(new EmptyBorder(0, 0, 0, 15));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(operation.getDisplayName());
        titleLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("工作区: " + workspace.getName());
        subtitleLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.WHITE);

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        leftPanel.add(operationIcon);
        leftPanel.add(textPanel);

        // 右侧：分支信息
        JPanel rightPanel = createBranchInfoPanel();

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建分支信息面板
     */
    private JPanel createBranchInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 5));
        panel.setOpaque(false);

        JLabel currentBranchLabel = new JLabel("当前分支: " +
                (workspace.getCurrentBranch() != null ? workspace.getCurrentBranch() : "未知"));
        currentBranchLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        currentBranchLabel.setForeground(Color.WHITE);
        currentBranchLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel remoteBranchLabel = new JLabel("远程分支: " +
                (workspace.getRemoteBranch() != null ? workspace.getRemoteBranch() : "未设置"));
        remoteBranchLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        remoteBranchLabel.setForeground(Color.WHITE);
        remoteBranchLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(currentBranchLabel);
        panel.add(remoteBranchLabel);

        return panel;
    }

    /**
     * 创建步骤指示器面板
     */
    private JPanel createStepPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        stepIndicator = new StepIndicator(operation);
        panel.add(stepIndicator);

        return panel;
    }

    /**
     * 创建摘要信息面板
     */
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 20, 5, 20));

        // 状态显示区域
        JPanel statusPanel = createStatusPanel();

        // 文件变更区域
        JPanel filesPanel = createFilesPanel();

        // 使用水平分割面板 - 左边状态检查，右边文件变更
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, statusPanel, filesPanel);
        splitPane.setResizeWeight(0.5); // 左右各占50%
        splitPane.setBorder(null);
        splitPane.setDividerSize(0);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建状态显示面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "状态检查",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));

        // 状态图标和消息
        JPanel statusInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusIcon = new JLabel(new FlatSVGIcon("icons/refresh.svg", 16, 16));
        statusMessage = new JLabel("正在检查Git状态...");
        statusMessage.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));

        statusInfoPanel.add(statusIcon);
        statusInfoPanel.add(statusMessage);

        panel.add(statusInfoPanel, BorderLayout.NORTH);

        // 添加详细信息区域 - 调整高度适应左右布局
        JTextArea detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 10));
        detailsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
        detailsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        detailsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailsScrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        panel.add(detailsScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建文件变更面板
     */
    private JPanel createFilesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "文件变更",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));

        // 文件变更显示区域
        JPanel fileChangesPanel = new JPanel(new BorderLayout());

        fileChangesArea = new JTextArea();
        fileChangesArea.setEditable(false);
        fileChangesArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        fileChangesArea.setText("正在加载文件变更信息...");
        fileChangesArea.setLineWrap(true);
        fileChangesArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(fileChangesArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        fileChangesPanel.add(scrollPane, BorderLayout.CENTER);

        // 如果是提交操作，添加提交信息输入区域
        if (operation == GitOperation.COMMIT) {
            JPanel commitPanel = createCommitMessagePanel();
            fileChangesPanel.add(commitPanel, BorderLayout.SOUTH);
        }

        panel.add(fileChangesPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建提交信息输入面板
     */
    private JPanel createCommitMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "提交信息 *",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));
        panel.setPreferredSize(new Dimension(0, 60)); // 设置固定高度

        commitMessageArea = new JTextArea(1, 0);
        commitMessageArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        commitMessageArea.setLineWrap(true);
        commitMessageArea.setWrapStyleWord(true);
        commitMessageArea.setText("Update at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        JScrollPane scrollPane = new JScrollPane(commitMessageArea);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建操作选择面板
     */
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 20, 5, 20));

        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setVisible(false);

        panel.add(optionsPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建底部面板
     */
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 20, 10, 20));

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setVisible(false);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        cancelButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        cancelButton.setPreferredSize(new Dimension(80, 32));
        cancelButton.addActionListener(e -> dispose());

        executeButton = new JButton(operation.getDisplayName());
        executeButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        executeButton.setPreferredSize(new Dimension(100, 32));
        executeButton.setBackground(operation.getColor());
        executeButton.setForeground(Color.WHITE);
        executeButton.setFocusPainted(false);
        executeButton.addActionListener(new ExecuteActionListener());

        buttonPanel.add(cancelButton);
        buttonPanel.add(executeButton);

        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }


    /**
     * 执行操作前检查
     */
    private void performPreOperationCheck() {
        stepIndicator.setCurrentStep(0);

        SwingUtilities.invokeLater(() -> {
            try {
                updateStatus("正在检查Git状态和潜在冲突...", "icons/refresh.svg", Color.BLUE);

                // 获取认证信息
                CredentialsProvider credentialsProvider = null;
                SshCredentialsProvider sshCredentialsProvider = null;
                if (workspace.getGitAuthType() != null) {
                    credentialsProvider = workspaceService.getCredentialsProvider(workspace);
                    sshCredentialsProvider = workspaceService.getSshCredentialsProvider(workspace);
                }

                // 执行冲突检测，传递认证信息
                statusCheck = checkGitStatus(workspace.getPath(), operation.name(), credentialsProvider, sshCredentialsProvider);

                // 显示检测结果
                displayStatusCheck(statusCheck);

                // 加载文件变更信息
                GitStatusResult gitStatus = workspaceService.getGitStatus(workspace.getId());
                displayGitStatus(gitStatus);

                stepIndicator.setCurrentStep(1);
                updateStatus("Git状态检查完成", "icons/check.svg", new Color(34, 139, 34));
            } catch (Exception e) {
                log.error("Failed to perform pre-operation check", e);
                updateStatus("状态检查失败: " + e.getMessage(), "icons/warning.svg", Color.RED);
            }
        });
    }

    /**
     * 更新状态显示
     */
    private void updateStatus(String message, String iconPath, Color color) {
        statusIcon.setIcon(new FlatSVGIcon(iconPath, 16, 16));
        statusMessage.setText(message);
        statusMessage.setForeground(color);
    }

    /**
     * 显示状态检查结果
     */
    private void displayStatusCheck(GitStatusCheck check) {
        stepIndicator.setCurrentStep(2);

        // 显示详细的状态检查信息
        displayStatusDetails(check);

        // 更新执行按钮状态
        updateExecuteButtonState(check);

        // 显示操作选择（如果需要）
        updateActionChoices(check);
    }

    /**
     * 显示详细的状态检查信息
     */
    private void displayStatusDetails(GitStatusCheck check) {
        StringBuilder details = new StringBuilder();

        // 显示基本状态
        details.append("📊 Git 状态摘要:\n");
        details.append(String.format("  • 未提交变更: %s\n", check.hasUncommittedChanges ? "是" : "否"));
        details.append(String.format("  • 未跟踪文件: %s\n", check.hasUntrackedFiles ? "是" : "否"));
        details.append(String.format("  • 本地提交: %s\n", check.hasLocalCommits ? "是" : "否"));
        details.append(String.format("  • 远程新提交: %s\n", check.hasRemoteCommits ? "是" : "否"));

        if (check.localCommitsAhead > 0) {
            details.append(String.format("  • 本地领先: %d 个提交\n", check.localCommitsAhead));
        }
        if (check.remoteCommitsBehind > 0) {
            details.append(String.format("  • 远程领先: %d 个提交\n", check.remoteCommitsBehind));
        }

        // 显示警告
        if (!check.warnings.isEmpty()) {
            details.append("\n❗ 警告:\n");
            for (String warning : check.warnings) {
                details.append("  • ").append(warning).append("\n");
            }
        }

        // 显示建议
        if (!check.suggestions.isEmpty()) {
            details.append("\n💡 建议:\n");
            for (String suggestion : check.suggestions) {
                details.append("  • ").append(suggestion).append("\n");
            }
        }

        // 查找状态面板中的详细信息区域并更新
        Container parent = statusIcon.getParent().getParent();
        if (parent instanceof JPanel statusPanel) {
            Component[] components = statusPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JScrollPane scrollPane &&
                        scrollPane.getViewport().getView() instanceof JTextArea detailsArea) {
                    detailsArea.setText(details.toString());
                    detailsArea.setCaretPosition(0);
                    break;
                }
            }
        }
    }

    /**
     * 根据状态检查结果更新操作选择
     */
    private void updateActionChoices(GitStatusCheck check) {
        optionsPanel.removeAll();
        optionGroup = new ButtonGroup();

        boolean showOptions = false;

        if (operation == GitOperation.COMMIT && check.canCommit) {
            showOptions = true;
            addOptionTitle("💡 请选择提交方式：");
            addOption(OPTION_COMMIT_FIRST, "仅提交本地变更", "只执行提交操作", true);
            if (!check.hasActualConflicts) {
                addOption(OPTION_COMMIT_AND_PUSH, "提交并推送", "提交后自动推送到远程仓库（适合多人协作）", false);
            }
        } else if (operation == GitOperation.PULL) {
            // 优先处理实际冲突（无论是否有未提交变更）
            if (check.hasActualConflicts) {
                showOptions = true;
                addOptionTitle("❗检测到文件冲突，请选择处理方式");
                addOption(OPTION_CANCEL, "取消操作，在外部工具处理", "推荐在Git客户端或IDE中手动处理冲突", true);
                addOption(OPTION_FORCE, "强制拉取（丢弃本地变更）", "❗避免冲突但会丢失本地变更", false, Color.RED);
            } else if (check.hasUncommittedChanges) {
                showOptions = true;
                // 如果可以自动合并，优先推荐提交后拉取
                if (check.canAutoMerge) {
                    addOptionTitle("💡 检测到未提交变更，可自动合并");
                    addOption(OPTION_COMMIT_FIRST, "先提交本地变更，再拉取", "本地变更可自动合并", true);
                    addOption(OPTION_FORCE, "强制拉取（丢弃本地变更）", "❗此操作会丢弃所有未提交的本地变更，请谨慎使用", false, Color.RED);
                } else {
                    addOptionTitle("💡 检测到未提交变更，请选择处理方式：");
                    addOption(OPTION_COMMIT_FIRST, "先提交本地变更，再拉取", "保留所有变更", true);
                    addOption(OPTION_STASH, "暂存本地变更，拉取后恢复", "适用于临时变更", false);
                    addOption(OPTION_FORCE, "强制拉取（丢弃本地变更）", "❗将丢失未提交的变更", false, Color.RED);
                }
            }
        } else if (operation == GitOperation.PUSH) {
            // 优先处理实际冲突
            if (check.hasActualConflicts) {
                showOptions = true;
                addOptionTitle("❗检测到文件冲突，请选择处理方式");
                addOption(OPTION_CANCEL, "取消操作，在外部工具处理", "推荐在Git客户端或IDE中手动处理冲突", true);
                addOption(OPTION_FORCE, "强制推送（覆盖远程变更）", "❗将覆盖远程的 " + check.remoteCommitsBehind + " 个提交", false, Color.RED);
            } else if (check.hasRemoteCommits) {
                showOptions = true;
                if (check.canAutoMerge) {
                    addOptionTitle("💡 远程仓库有新提交，可自动合并");
                    addOption(OPTION_PULL_FIRST, "先拉取远程变更，再推送", "无冲突，可安全自动合并", true);
                    addOption(OPTION_FORCE, "强制推送（覆盖远程变更）", "❗将覆盖远程仓库的变更", false, Color.RED);
                } else {
                    addOptionTitle("💡 远程仓库有新提交，请选择处理方式：");
                    addOption(OPTION_PULL_FIRST, "先拉取远程变更，再推送", "推荐选项，避免冲突", true);
                    addOption(OPTION_FORCE, "强制推送（覆盖远程变更）", "❗将覆盖远程仓库的变更", false, Color.RED);
                }
            }
        }

        if (showOptions) {
            optionsPanel.setVisible(true);
            stepIndicator.setCurrentStep(2);
            updateExecuteButtonStateByChoice();
        } else {
            optionsPanel.setVisible(false);
            stepIndicator.setCurrentStep(3);
        }

        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    /**
     * 添加选项标题
     */
    private void addOptionTitle(String title) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        titleLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        optionsPanel.add(titleLabel);
    }

    /**
     * 添加单个选项
     */
    private void addOption(String value, String text, String description, boolean selected) {
        addOption(value, text, description, selected, null);
    }

    private void addOption(String value, String text, String description, boolean selected, Color textColor) {
        JPanel optionPanel = new JPanel(new BorderLayout());
        optionPanel.setBorder(new EmptyBorder(5, 20, 5, 20));
        optionPanel.setOpaque(false);

        JRadioButton radio = new JRadioButton(text, selected);
        radio.setActionCommand(value);
        radio.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        if (textColor != null) {
            radio.setForeground(textColor);
        }
        optionGroup.add(radio);

        // 监听选项变化，动态更新按钮状态
        radio.addActionListener(e -> updateExecuteButtonStateByChoice());

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 10));
        descLabel.setForeground(Color.GRAY);
        descLabel.setBorder(new EmptyBorder(0, 25, 0, 0));

        optionPanel.add(radio, BorderLayout.NORTH);
        optionPanel.add(descLabel, BorderLayout.CENTER);

        optionsPanel.add(optionPanel);
    }

    // 根据当前选项动态判断按钮可用性
    private void updateExecuteButtonStateByChoice() {
        String choice = getUserChoice();
        boolean canExecute = false;
        switch (operation) {
            case PULL -> {
                // 只要不是取消，选了强制拉取/暂存/先提交都允许
                if (OPTION_FORCE.equals(choice) || OPTION_STASH.equals(choice) || OPTION_COMMIT_FIRST.equals(choice)) {
                    canExecute = true;
                } else if (OPTION_CANCEL.equals(choice)) {
                    canExecute = false;
                } else {
                    // 默认按 canPull
                    canExecute = statusCheck != null && statusCheck.canPull;
                }
            }
            case PUSH -> {
                if (OPTION_FORCE.equals(choice) || OPTION_PULL_FIRST.equals(choice)) {
                    canExecute = true;
                } else if (OPTION_CANCEL.equals(choice)) {
                    canExecute = false;
                } else {
                    canExecute = statusCheck != null && statusCheck.canPush;
                }
            }
            case COMMIT -> {
                // 提交和提交并推送都允许
                if (OPTION_COMMIT_FIRST.equals(choice) || OPTION_COMMIT_AND_PUSH.equals(choice)) {
                    canExecute = true;
                } else {
                    canExecute = statusCheck != null && statusCheck.canCommit;
                }
            }
        }
        executeButton.setEnabled(canExecute);
    }

    /**
     * 更新执行按钮状态
     */
    private void updateExecuteButtonState(GitStatusCheck check) {
        boolean canExecute = false;

        switch (operation) {
            case COMMIT -> {
                canExecute = check.canCommit;
            }
            case PUSH -> {
                if (check.isFirstPush || check.isRemoteRepositoryEmpty) {
                    canExecute = true;
                } else {
                    canExecute = check.canPush;
                }
            }
            case PULL -> {
                if (check.isRemoteRepositoryEmpty) {
                    canExecute = true;
                } else {
                    canExecute = check.canPull;
                }
            }
        }

        executeButton.setEnabled(canExecute);

        log.debug("Operation: {}, CanExecute: {}", operation, canExecute);
    }

    /**
     * 通知 WorkspacePanel 记录 Git 操作结果
     */
    private void notifyWorkspacePanel(WorkspaceService.GitOperationResult result) {
        SwingUtilities.invokeLater(() -> {
            try {
                WorkspacePanel workspacePanel =
                        SingletonFactory.getInstance(WorkspacePanel.class);
                workspacePanel.logGitOperationResult(result);
            } catch (Exception e) {
                log.warn("Failed to notify WorkspacePanel", e);
            }
        });
    }

    /**
     * 展示文件变更信息，并在有冲突的文件下展示冲突详情
     */
    private void displayGitStatus(GitStatusResult gitStatus) {
        if (gitStatus == null) {
            fileChangesArea.setText("未获取到文件变更信息。");
            return;
        }
        StringBuilder sb = new StringBuilder();
        // 合并所有本地变更相关字段
        Set<String> changedFiles = new LinkedHashSet<>();
        if (gitStatus.added != null) changedFiles.addAll(gitStatus.added);
        if (gitStatus.changed != null) changedFiles.addAll(gitStatus.changed);
        if (gitStatus.modified != null) changedFiles.addAll(gitStatus.modified);
        if (gitStatus.removed != null) changedFiles.addAll(gitStatus.removed);
        if (gitStatus.missing != null) changedFiles.addAll(gitStatus.missing);
        if (gitStatus.untracked != null) changedFiles.addAll(gitStatus.untracked);
        if (gitStatus.uncommitted != null) changedFiles.addAll(gitStatus.uncommitted);
        // 合并冲突文件（无论本地是否有变更）
        if (statusCheck != null && statusCheck.conflictDetails != null) {
            changedFiles.addAll(statusCheck.conflictDetails.keySet());
        }
        if (changedFiles.isEmpty()) {
            sb.append("无文件变更。");
        } else {
            sb.append("文件变更列表：\n");
            for (String file : changedFiles) {
                // 构建标签
                StringBuilder tags = new StringBuilder();
                if (statusCheck != null && statusCheck.conflictDetails != null && statusCheck.conflictDetails.containsKey(file)) {
                    tags.append("【冲突】");
                }
                if (gitStatus.added != null && gitStatus.added.contains(file)) {
                    tags.append("【新增】");
                }
                if (gitStatus.modified != null && gitStatus.modified.contains(file)) {
                    tags.append("【修改】");
                }
                if (gitStatus.removed != null && gitStatus.removed.contains(file)) {
                    tags.append("【删除】");
                }
                if (gitStatus.untracked != null && gitStatus.untracked.contains(file)) {
                    tags.append("【未跟踪】");
                }
                if (gitStatus.missing != null && gitStatus.missing.contains(file)) {
                    tags.append("【丢失】");
                }
                if (gitStatus.changed != null && gitStatus.changed.contains(file)) {
                    tags.append("【变更】");
                }
                if (gitStatus.uncommitted != null && gitStatus.uncommitted.contains(file)) {
                    tags.append("【未提交】");
                }
                // 默认都为本地变更，冲突文件也可加【本地】标签
                if (tags.length() == 0) {
                    tags.append("【本地】");
                }
                sb.append("• ").append(file).append(" ").append(tags).append("\n");
                // 展示冲突详情
                if (statusCheck != null && statusCheck.conflictDetails != null && statusCheck.conflictDetails.containsKey(file)) {
                    List<com.laker.postman.model.ConflictBlock> blocks = statusCheck.conflictDetails.get(file);
                    if (blocks != null && !blocks.isEmpty()) {
                        sb.append("  ❗ 冲突详情：\n");
                        for (com.laker.postman.model.ConflictBlock block : blocks) {
                            sb.append(String.format("    行 %d-%d\n", block.begin, block.end));
                            // 展示三方内容摘要
                            sb.append("      【基线】: ");
                            String baseSummary = block.baseLines != null && !block.baseLines.isEmpty() ? String.join(" ", block.baseLines) : "(无)";
                            if (baseSummary.length() > 80) baseSummary = baseSummary.substring(0, 80) + "...";
                            sb.append(baseSummary.replaceAll("\n", " ")).append("\n");
                            sb.append("      【本地】: ");
                            String localSummary = block.localLines != null && !block.localLines.isEmpty() ? String.join(" ", block.localLines) : "(无)";
                            if (localSummary.length() > 80) localSummary = localSummary.substring(0, 80) + "...";
                            sb.append(localSummary.replaceAll("\n", " ")).append("\n");
                            sb.append("      【远程】: ");
                            String remoteSummary = block.remoteLines != null && !block.remoteLines.isEmpty() ? String.join(" ", block.remoteLines) : "(无)";
                            if (remoteSummary.length() > 80) remoteSummary = remoteSummary.substring(0, 80) + "...";
                            sb.append(remoteSummary.replaceAll("\n", " ")).append("\n");
                        }
                    }
                }
            }
        }

        fileChangesArea.setText(sb.toString());
        fileChangesArea.setCaretPosition(0);
    }

    private class ExecuteActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!validateOperation()) {
                return;
            }

            confirmed = true;

            if (operation == GitOperation.COMMIT) {
                commitMessage = commitMessageArea.getText().trim();
            }

            String userChoice = getUserChoice();

            stepIndicator.setCurrentStep(3);
            showProgress();

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    executeGitOperationWithChoice(userChoice);
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String message : chunks) {
                        progressBar.setString(message);
                    }
                }

                @Override
                protected void done() {
                    hideProgress();
                    try {
                        get();
                        updateStatus("操作完成", "icons/check.svg", new Color(34, 139, 34));

                        JOptionPane.showMessageDialog(
                                GitOperationDialog.this,
                                operation.getDisplayName() + " 操作执行成功！",
                                "操作成功",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        SwingUtilities.invokeLater(GitOperationDialog.this::dispose);

                    } catch (Exception ex) {
                        log.error("Git operation failed", ex);
                        updateStatus("操作失败: " + ex.getMessage(), "icons/warning.svg", Color.RED);

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

                private void executeGitOperationWithChoice(String choice) throws Exception {
                    publish("正在执行 " + operation.getDisplayName() + " 操作...");

                    switch (operation) {
                        case COMMIT -> {
                            if (OPTION_COMMIT_AND_PUSH.equals(choice)) {
                                publish("正在提交变更...");
                                var commitResult = workspaceService.commitChanges(workspace.getId(), commitMessage);
                                notifyWorkspacePanel(commitResult);
                                publish("提交完成，正在推送到远程仓库...");
                                var pushResult = workspaceService.pushChanges(workspace.getId());
                                notifyWorkspacePanel(pushResult);
                            } else {
                                publish("正在提交变更...");
                                var result = workspaceService.commitChanges(workspace.getId(), commitMessage);
                                notifyWorkspacePanel(result);
                            }
                        }
                        case PUSH -> {
                            if (OPTION_FORCE.equals(choice)) {
                                publish("正在强制推送到远程仓库...");
                                var result = workspaceService.forcePushChanges(workspace.getId());
                                notifyWorkspacePanel(result);
                            } else if (OPTION_PULL_FIRST.equals(choice)) {
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
                            if (OPTION_COMMIT_FIRST.equals(choice)) {
                                publish("先提交本地变更...");
                                String autoCommitMsg = "Auto commit before pull - " +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                var commitResult = workspaceService.commitChanges(workspace.getId(), autoCommitMsg);
                                notifyWorkspacePanel(commitResult);

                                publish("然后拉取远程变更...");
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);
                            } else if (OPTION_STASH.equals(choice)) {
                                publish("暂存本地变更...");
                                var stashResult = workspaceService.stashChanges(workspace.getId());
                                notifyWorkspacePanel(stashResult);

                                publish("拉取远程变更...");
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);

                                publish("恢复暂存的变更...");
                                var popResult = workspaceService.popStashChanges(workspace.getId());
                                notifyWorkspacePanel(popResult);
                            } else if (OPTION_FORCE.equals(choice)) {
                                publish("强制拉取（丢弃本地变更）...");
                                var result = workspaceService.forcePullUpdates(workspace.getId());
                                notifyWorkspacePanel(result);
                            } else if (OPTION_CANCEL.equals(choice)) {
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

            String choice = getUserChoice();
            if (OPTION_CANCEL.equals(choice)) {
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
     * 获取用户当前选择的操作选项
     */
    private String getUserChoice() {
        if (optionGroup == null) {
            return "default";
        }
        ButtonModel selection = optionGroup.getSelection();
        if (selection != null) {
            return selection.getActionCommand();
        }

        return "default";
    }
}
