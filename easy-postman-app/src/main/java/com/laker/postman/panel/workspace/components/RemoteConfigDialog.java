package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.laker.postman.util.MessageKeys.*;

/**
 * 远程仓库配置对话框
 * 用于为 INITIALIZED 工作区配置远程仓库
 */
public class RemoteConfigDialog extends JDialog {

    @Getter
    private String remoteUrl;
    @Getter
    private String remoteBranch;
    @Getter
    private GitAuthType authType;
    @Getter
    private String username;
    @Getter
    private String password;
    @Getter
    private String token;
    @Getter
    private String sshPrivateKeyPath;
    @Getter
    private String sshPassphrase;

    // UI组件
    private JTextField remoteUrlField;
    private JTextField remoteBranchField;
    private GitAuthPanel gitAuthPanel;
    private ProgressPanel progressPanel;
    private JButton confirmButton;
    private JButton cancelButton;
    @Getter
    private boolean confirmed = false;
    private final transient Workspace workspace;

    public RemoteConfigDialog(Window parent, Workspace workspace) {
        super(parent, I18nUtil.getMessage(MessageKeys.WORKSPACE_REMOTE_CONFIG_TITLE) + " - " + workspace.getName(),
                ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        initComponents();
        initDialog();
    }

    private void initComponents() {
        remoteUrlField = new JTextField(24);
        SettingsInputStyle.apply(remoteUrlField);
        remoteBranchField = new JTextField(12);
        remoteBranchField.setText("master"); // 默认远程分支
        SettingsInputStyle.apply(remoteBranchField);

        gitAuthPanel = new GitAuthPanel();

        // 进度相关组件
        progressPanel = new ProgressPanel(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS));
        progressPanel.setVisible(false);
    }

    private void initDialog() {
        setupLayout();
        setupEventHandlers();
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        if (getWidth() < 560) {
            setSize(560, getHeight());
        }
        setLocationRelativeTo(getParent());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);

        // 远程仓库配置面板
        mainPanel.add(createConfigPanel(), BorderLayout.CENTER);

        // 创建南部面板，包含进度面板和按钮面板
        JPanel southPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(southPanel);

        // 进度面板
        southPanel.add(progressPanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = createStandardButtonPanel(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // MigLayout 表单模板：左侧标签，右侧输入框自适应拉伸
        JPanel basicPanel = new JPanel(new MigLayout(
                "insets 0, fillx, wrap 2, novisualpadding",
                "[right]12[grow,fill]",
                "[]10[]"
        ));
        basicPanel.setOpaque(false);

        // 远程仓库URL
        basicPanel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_URL) + ":"));
        basicPanel.add(remoteUrlField, "growx");

        // 远程分支
        basicPanel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_BRANCH) + ":"));
        basicPanel.add(remoteBranchField, "growx");

        JPanel formPanel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[grow,fill]",
                "[]10[]10[]"
        ));
        formPanel.setOpaque(false);
        formPanel.add(basicPanel, "cell 0 0, growx");
        JSeparator separator = new JSeparator();
        separator.setForeground(ModernColors.getTabSeparatorColor());
        separator.setBackground(ModernColors.getTabSeparatorColor());
        formPanel.add(separator, "cell 0 1, growx");
        // Git认证面板
        formPanel.add(gitAuthPanel, "cell 0 2, growx");
        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStandardButtonPanel(String okText) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(panel);
        cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);
        confirmButton = ModernButtonFactory.createButton(okText, true);
        panel.add(cancelButton);
        panel.add(confirmButton);
        getRootPane().setDefaultButton(confirmButton);
        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        return label;
    }

    private void setupEventHandlers() {
        confirmButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());
    }

    private void onConfirm() {
        try {
            validateInput();
            setInputComponentsEnabled(false);
            confirmButton.setEnabled(false);
            progressPanel.setVisible(true);
            SwingWorker<Void, String> worker = createWorkerTask();
            worker.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equals("progress")) {
                    progressPanel.getProgressBar().setValue((Integer) evt.getNewValue());
                } else if (evt.getPropertyName().equals("state") && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                    try {
                        worker.get();
                        onOperationSuccess();
                    } catch (Exception ex) {
                        if (ex instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        onOperationFailure(ex);
                    }
                }

            });
            worker.execute();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    protected void onOperationSuccess() {
        // Get input values
        remoteUrl = remoteUrlField.getText().trim();
        remoteBranch = remoteBranchField.getText().trim();
        authType = (GitAuthType) gitAuthPanel.getAuthTypeCombo().getSelectedItem();
        username = gitAuthPanel.getUsername();
        password = gitAuthPanel.getPassword();
        token = gitAuthPanel.getToken();
        sshPrivateKeyPath = gitAuthPanel.getSshKeyPath();
        sshPassphrase = gitAuthPanel.getSshPassphrase();

        confirmed = true;
        progressPanel.setProgressText(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_SUCCESS));
        progressPanel.getStatusLabel().setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_COMPLETED_CLOSING));
        Timer timer = new Timer(1000, e -> dispose());
        timer.setRepeats(false);
        timer.start();
    }

    protected void onOperationFailure(Exception e) {
        confirmButton.setEnabled(true);
        setInputComponentsEnabled(true);
        progressPanel.setVisible(false);
        pack();
        progressPanel.reset();
        progressPanel.setProgressText(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_FAILED));
        progressPanel.getStatusLabel().setText(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_FAILED_TIP));
        showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_FAILED_DETAIL, e.getMessage()));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
    }

    protected void validateInput() throws IllegalArgumentException {
        String url = remoteUrlField.getText().trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_URL_REQUIRED));
        }

        // 简单的URL格式验证
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("git@")) {
            throw new IllegalArgumentException(I18nUtil.getMessage(WORKSPACE_VALIDATION_GIT_URL_INVALID));
        }

        String branch = remoteBranchField.getText().trim();
        if (branch.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID));
        }

        // 验证认证信息
        gitAuthPanel.validateAuth();
    }

    protected SwingWorker<Void, String> createWorkerTask() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_START));
                setProgress(10);

                // 调用 WorkspaceService 配置远程仓库
                WorkspaceService workspaceService = WorkspaceService.getInstance();

                try {
                    // 获取输入值
                    String url = remoteUrlField.getText().trim();
                    String branch = remoteBranchField.getText().trim();
                    GitAuthType authType = (GitAuthType) gitAuthPanel.getAuthTypeCombo().getSelectedItem();
                    String username = gitAuthPanel.getUsername();
                    String password = gitAuthPanel.getPassword();
                    String token = gitAuthPanel.getToken();
                    String sshPrivateKeyPath = gitAuthPanel.getSshKeyPath();
                    String sshPassphrase = gitAuthPanel.getSshPassphrase();

                    publish(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_VALIDATING));
                    setProgress(30);

                    // 使用 WorkspaceService 添加远程仓库
                    workspaceService.addRemoteRepository(
                            workspace.getId(),
                            url,
                            branch,
                            authType,
                            username,
                            password,
                            token,
                            sshPrivateKeyPath,
                            sshPassphrase
                    );

                    publish(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_DONE));
                    setProgress(100);
                } catch (Exception e) {
                    // 重新抛出异常，让 SwingWorker 的错误处理机制处理
                    throw new RuntimeException(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_FAILED, e.getMessage()), e);
                }

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressPanel.getStatusLabel().setText(chunks.get(chunks.size() - 1));
                }
            }
        };
    }

    protected void setInputComponentsEnabled(boolean enabled) {
        remoteUrlField.setEnabled(enabled);
        remoteBranchField.setEnabled(enabled);
        gitAuthPanel.setComponentsEnabled(enabled);
    }

}
