package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.StepIndicator;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.git.SshCredentialsProvider;
import com.laker.postman.service.render.HttpHtmlRenderer;
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
import java.util.List;

import static com.laker.postman.service.git.GitConflictDetector.checkGitStatus;

/**
 * Git 操作对话框
 * 场景1：提交 推送 拉取
 * 场景2：本地提交和远程提交不冲突
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
    private JEditorPane detailsArea;
    private JEditorPane fileChangesArea;
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
        setSize(750, 600);
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

        JLabel subtitleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_WORKSPACE, workspace.getName()));
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

        JLabel currentBranchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CURRENT_BRANCH,
                workspace.getCurrentBranch() != null ? workspace.getCurrentBranch() : I18nUtil.getMessage(MessageKeys.GIT_DIALOG_UNKNOWN)));
        currentBranchLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        currentBranchLabel.setForeground(Color.WHITE);
        currentBranchLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel remoteBranchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_BRANCH,
                workspace.getRemoteBranch() != null ? workspace.getRemoteBranch() : I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NOT_SET)));
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
                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_STATUS_CHECK),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));

        JPanel statusInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusIcon = new JLabel(new FlatSVGIcon("icons/refresh.svg", 16, 16));
        statusMessage = new JLabel(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CHECKING_STATUS));
        statusMessage.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));

        statusInfoPanel.add(statusIcon);
        statusInfoPanel.add(statusMessage);

        panel.add(statusInfoPanel, BorderLayout.NORTH);

        detailsArea = new JEditorPane();
        detailsArea.setEditable(false);
        detailsArea.setFont(detailsArea.getFont().deriveFont(10f)); // 设置较小字体
        detailsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        detailsArea.setContentType("text/html"); // 支持HTML渲染

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
                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_FILE_CHANGES),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));

        JPanel fileChangesPanel = new JPanel(new BorderLayout());

        fileChangesArea = new JEditorPane();
        fileChangesArea.setEditable(false);
        fileChangesArea.setFont(fileChangesArea.getFont().deriveFont(10f)); // 设置较小字体
        fileChangesArea.setText(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_LOADING_FILE_CHANGES));
        fileChangesArea.setContentType("text/html"); // 支持HTML渲染

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
                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_COMMIT_MESSAGE),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));
        panel.setPreferredSize(new Dimension(0, 60)); // 设置固定高度

        commitMessageArea = new JTextArea(1, 0);
        commitMessageArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        commitMessageArea.setLineWrap(true);
        commitMessageArea.setWrapStyleWord(true);
        commitMessageArea.setText(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_DEFAULT_COMMIT_MESSAGE,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

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
                updateStatus(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CHECKING_STATUS_AND_CONFLICT), "icons/refresh.svg", Color.BLUE);
                CredentialsProvider credentialsProvider = null;
                SshCredentialsProvider sshCredentialsProvider = null;
                if (workspace.getGitAuthType() != null) {
                    credentialsProvider = workspaceService.getCredentialsProvider(workspace);
                    sshCredentialsProvider = workspaceService.getSshCredentialsProvider(workspace);
                }
                statusCheck = checkGitStatus(workspace.getPath(), operation.name(), credentialsProvider, sshCredentialsProvider);
                displayStatusCheck(statusCheck);
                displayFileChangesStatus();
                stepIndicator.setCurrentStep(1);
                updateStatus(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_STATUS_CHECK_DONE), "icons/check.svg", new Color(34, 139, 34));
            } catch (Exception e) {
                log.error("Failed to perform pre-operation check", e);
                updateStatus(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_STATUS_CHECK_FAILED, e.getMessage()), "icons/warning.svg", Color.RED);
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
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; font-size: 8px;'>");

        html.append("<div style='margin-bottom: 10px;'>");
        html.append("<b>").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_STATUS_SUMMARY))).append("</b><br/>");
        html.append("&nbsp;&nbsp;").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_HAS_UNCOMMITTED_CHANGES)))
                .append(" ").append(check.hasUncommittedChanges ? escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_YES)) : escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO))).append("<br/>");
        html.append("&nbsp;&nbsp;").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_HAS_LOCAL_COMMITS)))
                .append(" ").append(check.hasLocalCommits ? escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_YES)) : escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO))).append("<br/>");
        html.append("&nbsp;&nbsp;📡 ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_HAS_REMOTE_COMMITS)))
                .append(" ").append(check.hasRemoteCommits ? escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_YES)) : escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO))).append("<br/>");

        if (check.localCommitsAhead > 0) {
            String msg = I18nUtil.getMessage(MessageKeys.GIT_DIALOG_LOCAL_AHEAD, check.localCommitsAhead);
            html.append("&bull; ").append(escapeHtml(msg)).append("<br/>");
        }
        if (check.remoteCommitsBehind > 0) {
            String msg = I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_AHEAD, check.remoteCommitsBehind);
            html.append("&bull; ").append(escapeHtml(msg)).append("<br/>");
        }
        html.append("</div>");

        if (!check.warnings.isEmpty()) {
            html.append("<div style='margin-bottom: 10px;'>");
            html.append("<b>").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_WARNINGS))).append("</b><br/>");
            for (String warning : check.warnings) {
                html.append("&bull; ").append(escapeHtml(warning)).append("<br/>");
            }
            html.append("</div>");
        }

        if (!check.suggestions.isEmpty()) {
            html.append("<div style='margin-bottom: 10px;'>");
            html.append("<b>").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_SUGGESTIONS))).append("</b><br/>");
            for (String suggestion : check.suggestions) {
                html.append("&bull; ").append(escapeHtml(suggestion)).append("<br/>");
            }
            html.append("</div>");
        }

        html.append("</body></html>");
        detailsArea.setText(html.toString());
        detailsArea.setCaretPosition(0);
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
            addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_TITLE));
            addOption(OPTION_COMMIT_FIRST,
                    I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_FIRST),
                    I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_FIRST_DESC), true);
            if (!check.hasActualConflicts) {
                addOption(OPTION_COMMIT_AND_PUSH,
                        I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_AND_PUSH),
                        I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_AND_PUSH_DESC), false);
            }
        } else if (operation == GitOperation.PULL) {
            if (check.hasActualConflicts) {
                showOptions = true;
                addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PULL_CONFLICT_TITLE));
                addOption(OPTION_CANCEL,
                        I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_CANCEL),
                        I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_CANCEL_DESC), true);
                addOption(OPTION_FORCE,
                        I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PULL),
                        I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PULL_DESC), false, Color.RED);
            } else if (check.hasUncommittedChanges) {
                showOptions = true;
                // 如果可以自动合并，优先推荐提交后拉取
                if (check.canAutoMerge) {
                    addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PULL_UNCOMMITTED_AUTO_MERGE_TITLE));
                    addOption(OPTION_COMMIT_FIRST, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_FIRST_PULL), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_FIRST_PULL_AUTO_MERGE_DESC), true);
                    addOption(OPTION_FORCE, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PULL_DISCARD), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PULL_DISCARD_WARNING_DESC), false, Color.RED);
                } else {
                    addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PULL_UNCOMMITTED_CHOOSE_TITLE));
                    addOption(OPTION_COMMIT_FIRST, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_FIRST_PULL), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_COMMIT_FIRST_PULL_KEEP_DESC), true);
                    addOption(OPTION_STASH, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_STASH_PULL), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_STASH_PULL_DESC), false);
                    addOption(OPTION_FORCE, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PULL_DISCARD), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PULL_LOSE_DESC), false, Color.RED);
                }
            }
        } else if (operation == GitOperation.PUSH) {
            // 优先处理实际冲突
            if (check.hasActualConflicts) {
                showOptions = true;
                addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PUSH_CONFLICT_TITLE));
                addOption(OPTION_CANCEL, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_CANCEL_EXTERNAL_TOOL), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_CANCEL_EXTERNAL_TOOL_DESC), true);
                addOption(OPTION_FORCE, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PUSH_OVERWRITE), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PUSH_OVERWRITE_COMMITS_DESC, check.remoteCommitsBehind), false, Color.RED);
            } else if (check.hasRemoteCommits) {
                // 远程有新提交
                showOptions = true;
                if (check.canAutoMerge && check.localCommitsAhead > 0) {
                    addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PUSH_REMOTE_AUTO_MERGE_TITLE));
                    addOption(OPTION_PULL_FIRST, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PULL_FIRST_PUSH), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PULL_FIRST_PUSH_DESC), true);
                    addOption(OPTION_FORCE, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PUSH_OVERWRITE), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PUSH_OVERWRITE_REMOTE_DESC), false, Color.RED);
                } else {
                    addOptionTitle(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_PUSH_REMOTE_CHOOSE_TITLE));
                    addOption(OPTION_FORCE, I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PUSH_OVERWRITE), I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPTION_FORCE_PUSH_OVERWRITE_REMOTE_DESC), true, Color.RED);
                }
            }
        }

        optionsPanel.setVisible(showOptions);
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
                if (check.isFirstPush) {
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
    private void displayFileChangesStatus() {
        if (statusCheck == null) {
            fileChangesArea.setText("<html><body>" + escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_FILE_CHANGES_NOT_AVAILABLE)) + "</body></html>");
            return;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: monospace; font-size: 8px;'>");

        // 本地变更
        html.append("<div style='margin-bottom: 10px;'>");
        html.append("<b>").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_LOCAL_CHANGES_TITLE))).append("</b><br/>");

        if (statusCheck.added != null && !statusCheck.added.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_ADDED_FILES))).append(statusCheck.added.size()).append("<br/>");
            for (String file : statusCheck.added) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: green;'>+</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.changed != null && !statusCheck.changed.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CHANGED_FILES))).append(statusCheck.changed.size()).append("<br/>");
            for (String file : statusCheck.changed) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: orange;'>~</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.modified != null && !statusCheck.modified.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_MODIFIED_FILES))).append(statusCheck.modified.size()).append("<br/>");
            for (String file : statusCheck.modified) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: blue;'>*</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.removed != null && !statusCheck.removed.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOVED_FILES))).append(statusCheck.removed.size()).append("<br/>");
            for (String file : statusCheck.removed) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red;'>-</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.missing != null && !statusCheck.missing.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_MISSING_FILES))).append(statusCheck.missing.size()).append("<br/>");
            for (String file : statusCheck.missing) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red;'>!</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.untracked != null && !statusCheck.untracked.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_UNTRACKED_FILES))).append(statusCheck.untracked.size()).append("<br/>");
            for (String file : statusCheck.untracked) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: gray;'>?</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.conflicting != null && !statusCheck.conflicting.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICTING_FILES))).append(statusCheck.conflicting.size()).append("<br/>");
            for (String file : statusCheck.conflicting) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red;'>#</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        // 如无本地变更，提示
        if ((statusCheck.added == null || statusCheck.added.isEmpty()) &&
                (statusCheck.changed == null || statusCheck.changed.isEmpty()) &&
                (statusCheck.modified == null || statusCheck.modified.isEmpty()) &&
                (statusCheck.removed == null || statusCheck.removed.isEmpty()) &&
                (statusCheck.missing == null || statusCheck.missing.isEmpty()) &&
                (statusCheck.untracked == null || statusCheck.untracked.isEmpty()) &&
                (statusCheck.conflicting == null || statusCheck.conflicting.isEmpty())) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO_LOCAL_CHANGES))).append("<br/>");
        }
        html.append("</div>");

        // 远程变更分组展示
        html.append("<div style='margin-bottom: 10px;'>");
        html.append("<b>📡 ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_CHANGES_TITLE))).append("</b><br/>");

        if (statusCheck.remoteAdded != null && !statusCheck.remoteAdded.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_ADDED_FILES))).append(statusCheck.remoteAdded.size()).append("<br/>");
            for (String file : statusCheck.remoteAdded) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: green;'>[+]</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.remoteModified != null && !statusCheck.remoteModified.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_MODIFIED_FILES))).append(statusCheck.remoteModified.size()).append("<br/>");
            for (String file : statusCheck.remoteModified) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: orange;'>[~]</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.remoteRemoved != null && !statusCheck.remoteRemoved.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_REMOVED_FILES))).append(statusCheck.remoteRemoved.size()).append("<br/>");
            for (String file : statusCheck.remoteRemoved) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red;'>[-]</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.remoteRenamed != null && !statusCheck.remoteRenamed.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_RENAMED_FILES))).append(statusCheck.remoteRenamed.size()).append("<br/>");
            for (String file : statusCheck.remoteRenamed) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: blue;'>[R]</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        if (statusCheck.remoteCopied != null && !statusCheck.remoteCopied.isEmpty()) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_REMOTE_COPIED_FILES))).append(statusCheck.remoteCopied.size()).append("<br/>");
            for (String file : statusCheck.remoteCopied) {
                html.append("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: blue;'>[C]</span> ").append(escapeHtml(file)).append("<br/>");
            }
        }

        // 如无远程变更，提示
        if ((statusCheck.remoteAdded == null || statusCheck.remoteAdded.isEmpty()) &&
                (statusCheck.remoteModified == null || statusCheck.remoteModified.isEmpty()) &&
                (statusCheck.remoteRemoved == null || statusCheck.remoteRemoved.isEmpty()) &&
                (statusCheck.remoteRenamed == null || statusCheck.remoteRenamed.isEmpty()) &&
                (statusCheck.remoteCopied == null || statusCheck.remoteCopied.isEmpty())) {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO_REMOTE_CHANGES))).append("<br/>");
        }
        html.append("</div>");

        // 冲突文件详情展示
        html.append("<div style='margin-bottom: 10px;'>");
        html.append("<b>").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_DETAILS_TITLE))).append("</b><br/>");

        if (statusCheck.conflictingFiles != null && !statusCheck.conflictingFiles.isEmpty()) {
            for (String file : statusCheck.conflictingFiles) {
                html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_FILE))).append(escapeHtml(file)).append("<br/>");
                List<com.laker.postman.model.ConflictBlock> blocks = statusCheck.conflictDetails.get(file);
                if (blocks != null && !blocks.isEmpty()) {
                    for (int i = 0; i < blocks.size(); i++) {
                        com.laker.postman.model.ConflictBlock block = blocks.get(i);
                        html.append("&nbsp;&nbsp;&nbsp;&nbsp;")
                                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_BLOCK)))
                                .append(i + 1)
                                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_BLOCK_LINES)))
                                .append(block.getBegin()).append("-").append(block.getEnd()).append("]<br/>");
                        html.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_BASE)))
                                .append(escapeHtml(String.join(" | ", block.getBaseLines()))).append("<br/>");
                        html.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_LOCAL)))
                                .append(escapeHtml(String.join(" | ", block.getLocalLines()))).append("<br/>");
                        html.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_CONFLICT_REMOTE)))
                                .append(escapeHtml(String.join(" | ", block.getRemoteLines()))).append("<br/>");
                    }
                } else {
                    html.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO_CONFLICT_DETAILS))).append("<br/>");
                }
            }
        } else {
            html.append("&bull; ").append(escapeHtml(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_NO_FILE_CONFLICTS))).append("<br/>");
        }
        html.append("</div>");

        html.append("</body></html>");
        fileChangesArea.setText(html.toString());
        fileChangesArea.setCaretPosition(0);
    }

    /**
     * HTML转义帮助方法，防止HTML注入
     */
    private String escapeHtml(String text) {
        return HttpHtmlRenderer.escapeHtml(text);
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
                        updateStatus(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_COMPLETED), "icons/check.svg", new Color(34, 139, 34));

                        JOptionPane.showMessageDialog(
                                GitOperationDialog.this,
                                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_SUCCESS_MESSAGE, operation.getDisplayName()),
                                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_SUCCESS_TITLE),
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        SwingUtilities.invokeLater(GitOperationDialog.this::dispose);

                    } catch (Exception ex) {
                        log.error("Git operation failed", ex);
                        updateStatus(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_FAILED, ex.getMessage()), "icons/warning.svg", Color.RED);

                        String errorMessage = ex.getMessage();
                        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                            errorMessage = ex.getCause().getMessage();
                        }

                        JOptionPane.showMessageDialog(
                                GitOperationDialog.this,
                                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_FAILED_MESSAGE, errorMessage),
                                I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_FAILED_TITLE),
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }

                private void executeGitOperationWithChoice(String choice) throws Exception {
                    publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_EXECUTING_PROGRESS, operation.getDisplayName()));

                    switch (operation) {
                        case COMMIT -> {
                            if (OPTION_COMMIT_AND_PUSH.equals(choice)) {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_COMMITTING));
                                var commitResult = workspaceService.commitChanges(workspace.getId(), commitMessage);
                                notifyWorkspacePanel(commitResult);
                                if (statusCheck.remoteCommitsBehind > 0) {
                                    publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_REMOTE_COMMITS_PULL_FIRST));
                                    var pullResult = workspaceService.pullUpdates(workspace.getId());
                                    notifyWorkspacePanel(pullResult);
                                }
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_COMMIT_DONE_PUSHING));
                                var pushResult = workspaceService.pushChanges(workspace.getId());
                                notifyWorkspacePanel(pushResult);
                            } else {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_COMMITTING));
                                var result = workspaceService.commitChanges(workspace.getId(), commitMessage);
                                notifyWorkspacePanel(result);
                            }
                        }
                        case PUSH -> {
                            if (OPTION_FORCE.equals(choice)) {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_FORCE_PUSHING));
                                var result = workspaceService.forcePushChanges(workspace.getId());
                                notifyWorkspacePanel(result);
                            } else if (OPTION_PULL_FIRST.equals(choice)) {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_PULL_FIRST));
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);

                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_THEN_PUSH));
                                var pushResult = workspaceService.pushChanges(workspace.getId());
                                notifyWorkspacePanel(pushResult);
                            } else {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_PUSHING));
                                var result = workspaceService.pushChanges(workspace.getId());
                                notifyWorkspacePanel(result);
                            }
                        }
                        case PULL -> {
                            if (OPTION_COMMIT_FIRST.equals(choice)) {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_COMMIT_LOCAL_FIRST));
                                String autoCommitMsg = "Auto commit before pull - " +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                var commitResult = workspaceService.commitChanges(workspace.getId(), autoCommitMsg);
                                notifyWorkspacePanel(commitResult);

                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_THEN_PULL));
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);
                            } else if (OPTION_STASH.equals(choice)) {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_STASHING));
                                var stashResult = workspaceService.stashChanges(workspace.getId());
                                notifyWorkspacePanel(stashResult);

                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_PULLING_REMOTE));
                                var pullResult = workspaceService.pullUpdates(workspace.getId());
                                notifyWorkspacePanel(pullResult);

                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_RESTORING_STASH));
                                var popResult = workspaceService.popStashChanges(workspace.getId());
                                notifyWorkspacePanel(popResult);
                            } else if (OPTION_FORCE.equals(choice)) {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_FORCE_PULL_DISCARD));
                                var result = workspaceService.forcePullUpdates(workspace.getId());
                                notifyWorkspacePanel(result);
                            } else if (OPTION_CANCEL.equals(choice)) {
                                throw new RuntimeException(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_USER_CANCELLED));
                            } else {
                                publish(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_PROGRESS_PULLING_FROM_REMOTE));
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
                            I18nUtil.getMessage(MessageKeys.GIT_DIALOG_VALIDATION_COMMIT_MESSAGE_EMPTY),
                            I18nUtil.getMessage(MessageKeys.GIT_DIALOG_VALIDATION_COMMIT_MESSAGE_TITLE),
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
            progressBar.setString(I18nUtil.getMessage(MessageKeys.GIT_DIALOG_OPERATION_EXECUTING));
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
