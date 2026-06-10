package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.model.Workspace;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * 转换为Git工作区对话框
 */
public class ConvertToGitDialog extends JDialog {

    @Getter
    private String branchName;
    @Getter
    private boolean confirmed = false;

    private JTextField branchField;
    private JButton confirmButton;
    private final transient Workspace workspace;

    public ConvertToGitDialog(Window parent, Workspace workspace) {
        super(parent, I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_TO_GIT),
                ModalityType.APPLICATION_MODAL);
        this.workspace = workspace;
        initComponents();
        initDialog();
    }

    private void initComponents() {
        branchField = new JTextField("master", 20);
        branchField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        SettingsInputStyle.apply(branchField);

        // 添加回车键确认支持
        branchField.addActionListener(e -> onConfirm());
    }

    private void initDialog() {
        setupLayout();
        setupKeyBindings();
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);

        // 设置焦点到输入框并全选文本，方便用户快速修改
        SwingUtilities.invokeLater(() -> {
            branchField.requestFocusInWindow();
            branchField.selectAll();
        });
    }

    /**
     * 设置快捷键绑定
     */
    private void setupKeyBindings() {
        // ESC键取消
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 设置默认按钮（回车键确认）
        getRootPane().setDefaultButton(confirmButton);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 顶部信息区域
        JPanel topPanel = createTopPanel();
        contentPanel.add(topPanel, BorderLayout.NORTH);

        // 中间输入区域
        JPanel centerPanel = createCenterPanel();
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 底部按钮区域
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 创建顶部信息面板
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 4, 0));

        // 左侧图标
        JLabel iconLabel = new JLabel(com.laker.postman.util.IconUtil.create(
                "icons/git.svg", 24, 24));
        panel.add(iconLabel, BorderLayout.WEST);

        // 右侧信息
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 3));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_TO_GIT));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        infoPanel.add(titleLabel);

        JLabel workspaceLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ": " + workspace.getName());
        workspaceLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        infoPanel.add(workspaceLabel);

        // 添加提示信息
        JLabel tipLabel = new JLabel("<html><i>" + I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_SUCCESS_TIP) + "</i></html>");
        tipLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        infoPanel.add(tipLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建中间输入面板
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyDialogTopSeparator(panel, 10, 0, 0, 0);

        // 分支名称标签
        JLabel branchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_BRANCH_PROMPT) + ":");
        branchLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        panel.add(branchLabel, BorderLayout.NORTH);

        // 分支输入框
        branchField.setPreferredSize(new Dimension(320, branchField.getPreferredSize().height));
        panel.add(branchField, BorderLayout.CENTER);


        return panel;
    }

    /**
     * 创建底部按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(panel);

        confirmButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);

        confirmButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());

        panel.add(cancelButton);
        panel.add(confirmButton);

        return panel;
    }

    private void onConfirm() {
        // 验证分支名称
        branchName = branchField.getText().trim();

        // 检查是否为空
        if (branchName.isEmpty()) {
            showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID));
            branchField.requestFocusInWindow();
            return;
        }

        // 检查Git分支名格式（基本规则）
        if (!isValidGitBranchName(branchName)) {
            showError(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_FORMAT));
            branchField.requestFocusInWindow();
            branchField.selectAll();
            return;
        }

        confirmed = true;
        dispose();
    }


    /**
     * 验证Git分支名是否合法
     * 基于Git分支命名规则的简化验证
     */
    private boolean isValidGitBranchName(String name) {
        // 不能包含空格
        if (name.contains(" ")) {
            return false;
        }

        // 不能以 . 或 / 开头
        if (name.startsWith(".") || name.startsWith("/")) {
            return false;
        }

        // 不能以 / 结尾
        if (name.endsWith("/")) {
            return false;
        }

        // 不能包含连续的斜杠 //
        if (name.contains("//")) {
            return false;
        }

        // 不能包含特殊字符
        String[] invalidChars = {"~", "^", ":", "?", "*", "[", "@{", ".."};
        for (String invalidChar : invalidChars) {
            if (name.contains(invalidChar)) {
                return false;
            }
        }

        return true;
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
