package com.laker.postman.panel.workspace.components;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 更新 Git 认证信息对话框
 */
public class UpdateAuthDialog extends JDialog {

    @Getter
    private GitAuthType authType;
    @Getter
    private String username;
    @Getter
    private String password;
    @Getter
    private String token;

    private GitAuthPanel gitAuthPanel;
    private JButton confirmButton;
    private JButton cancelButton;
    @Getter
    private boolean confirmed = false;
    private final transient Workspace workspace;

    public UpdateAuthDialog(Window parent, Workspace workspace) {
        super(parent, I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_UPDATE) + " - " + workspace.getName(),
                ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        initComponents();
        initDialog();
        loadCurrentAuth();
    }

    private void initComponents() {
        gitAuthPanel = new GitAuthPanel();
    }

    private void initDialog() {
        setupLayout();
        setupEventHandlers();
        setSize(430, 230); // 设置对话框
        setMinimumSize(new Dimension(430, 230)); // 设置最小尺寸
        setLocationRelativeTo(getParent());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Git认证面板
        JPanel authContainer = new JPanel(new BorderLayout());
        authContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_TYPE),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));
        authContainer.add(gitAuthPanel, BorderLayout.CENTER);
        mainPanel.add(authContainer, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        confirmButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL));
        panel.add(confirmButton);
        panel.add(cancelButton);
        return panel;
    }

    private void setupEventHandlers() {
        confirmButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());
    }

    /**
     * 加载当前的认证信息
     */
    private void loadCurrentAuth() {
        if (workspace.getGitAuthType() != null) {
            gitAuthPanel.getAuthTypeCombo().setSelectedItem(workspace.getGitAuthType());
        }

        // 根据认证类型回填所有信息（包括密码等敏感信息）
        if (workspace.getGitUsername() != null) {
            gitAuthPanel.getPasswordUsernameField().setText(workspace.getGitUsername());
            gitAuthPanel.getTokenUsernameField().setText(workspace.getGitUsername());
        }

        // 回填密码
        if (workspace.getGitPassword() != null) {
            gitAuthPanel.getPasswordField().setText(workspace.getGitPassword());
        }

        // 回填 Token
        if (workspace.getGitToken() != null) {
            gitAuthPanel.getTokenField().setText(workspace.getGitToken());
        }

        // 回填 SSH 私钥路径和密码
        if (workspace.getSshPrivateKeyPath() != null) {
            gitAuthPanel.getSshKeyPathField().setText(workspace.getSshPrivateKeyPath());
        }

        if (workspace.getSshPassphrase() != null) {
            gitAuthPanel.getSshPassphraseField().setText(workspace.getSshPassphrase());
        }
    }

    private void onConfirm() {
        try {
            // 验证认证信息
            gitAuthPanel.validateAuth();

            // 获取输入值
            authType = (GitAuthType) gitAuthPanel.getAuthTypeCombo().getSelectedItem();
            username = gitAuthPanel.getUsername();
            password = gitAuthPanel.getPassword();
            token = gitAuthPanel.getToken();

            // 获取 SSH 认证信息
            String sshKeyPath = gitAuthPanel.getSshKeyPath();
            String sshPassphrase = gitAuthPanel.getSshPassphrase();

            // 调用 WorkspaceService 更新认证信息
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            workspaceService.updateGitAuthentication(
                    workspace.getId(),
                    authType,
                    username,
                    password,
                    token,
                    sshKeyPath,
                    sshPassphrase
            );

            confirmed = true;
            JOptionPane.showMessageDialog(
                    this,
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_UPDATE_SUCCESS),
                    I18nUtil.getMessage(MessageKeys.GENERAL_OK),
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_AUTH_UPDATE_FAILED, ex.getMessage()));
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }
}
