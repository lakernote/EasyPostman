package com.laker.postman.panel.workspace.components;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.EasyPostManFontUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 远程仓库配置对话框
 * 用于为 INITIALIZED 工作区配置远程仓库
 */
public class RemoteConfigDialog extends ProgressDialog {

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

    // UI组件
    private JTextField remoteUrlField;
    private JTextField remoteBranchField;
    private GitAuthPanel gitAuthPanel;

    private final transient Workspace workspace;

    public RemoteConfigDialog(Window parent, Workspace workspace) {
        super(parent, "配置远程仓库 - " + workspace.getName());
        this.workspace = workspace;
        initComponents();
        initDialog();
    }

    private void initComponents() {
        remoteUrlField = new JTextField(30);
        remoteBranchField = new JTextField(15);
        remoteBranchField.setText("origin/main"); // 默认远程分支

        gitAuthPanel = new GitAuthPanel();

        // 进度相关组件
        progressPanel = new ProgressPanel("配置进度");
        progressPanel.setVisible(false);

        // 设置默认字体
        Font defaultFont = EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12);
        remoteUrlField.setFont(defaultFont);
        remoteBranchField.setFont(defaultFont);
    }

    @Override
    protected void setupLayout() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 远程仓库配置面板
        JPanel configPanel = createConfigPanel();
        mainPanel.add(configPanel, BorderLayout.CENTER);

        // 创建南部面板，包含进度面板和按钮面板
        JPanel southPanel = new JPanel(new BorderLayout());

        // 进度面板
        southPanel.add(progressPanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = createStandardButtonPanel("确定");
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "远程仓库配置",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));

        // 基本配置
        JPanel basicPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // 远程仓库URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        basicPanel.add(new JLabel("远程仓库 URL:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        basicPanel.add(remoteUrlField, gbc);

        // 远程分支
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        basicPanel.add(new JLabel("远程分支:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        basicPanel.add(remoteBranchField, gbc);

        panel.add(basicPanel, BorderLayout.NORTH);

        // Git认证面板
        panel.add(gitAuthPanel, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected void setupEventHandlers() {
        // 基础事件处理已在父类处理
    }

    @Override
    protected void validateInput() throws IllegalArgumentException {
        String url = remoteUrlField.getText().trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException("远程仓库 URL 不能为空");
        }

        // 简单的URL格式验证
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("git@")) {
            throw new IllegalArgumentException("请输入有效的 Git 仓库 URL");
        }

        String branch = remoteBranchField.getText().trim();
        if (branch.isEmpty()) {
            throw new IllegalArgumentException("远程分支不能为空");
        }

        // 验证认证信息
        gitAuthPanel.validateAuth();
    }

    @Override
    protected SwingWorker<Void, String> createWorkerTask() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish("正在配置远程仓库...");
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

                    publish("正在验证远程仓库配置...");
                    setProgress(30);

                    // 使用 WorkspaceService 添加远程仓库
                    workspaceService.addRemoteRepository(
                            workspace.getId(),
                            url,
                            branch,
                            authType,
                            username,
                            password,
                            token
                    );

                    publish("远程仓库配置完成！");
                    setProgress(100);
                } catch (Exception e) {
                    // 重新抛出异常，让 SwingWorker 的错误处理机制处理
                    throw new RuntimeException("配置远程仓库失败: " + e.getMessage(), e);
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressPanel.getStatusLabel().setText(chunks.get(chunks.size() - 1));
                }
            }
        };
    }

    @Override
    protected void onOperationSuccess() {
        // 获取输入值
        remoteUrl = remoteUrlField.getText().trim();
        remoteBranch = remoteBranchField.getText().trim();
        authType = (GitAuthType) gitAuthPanel.getAuthTypeCombo().getSelectedItem();
        username = gitAuthPanel.getUsername();
        password = gitAuthPanel.getPassword();
        token = gitAuthPanel.getToken();

        super.onOperationSuccess();
    }

    @Override
    protected void setInputComponentsEnabled(boolean enabled) {
        remoteUrlField.setEnabled(enabled);
        remoteBranchField.setEnabled(enabled);
        gitAuthPanel.setComponentsEnabled(enabled);
    }
}
