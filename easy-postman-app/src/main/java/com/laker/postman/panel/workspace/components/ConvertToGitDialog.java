package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Workspace;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
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

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // 顶部信息区域
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 中间输入区域
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 底部按钮区域
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 创建顶部信息面板
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // 左侧图标
        JLabel iconLabel = new JLabel(com.laker.postman.util.IconUtil.create(
                "icons/git.svg", 32, 32));
        panel.add(iconLabel, BorderLayout.WEST);

        // 右侧信息
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_TO_GIT));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD).deriveFont(14f));
        infoPanel.add(titleLabel);

        JLabel workspaceLabel = new JLabel("Workspace: " + workspace.getName());
        workspaceLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.anchor = GridBagConstraints.WEST;

        // 分支名称标签
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel branchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_CONVERT_BRANCH_PROMPT) + ":");
        branchLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        panel.add(branchLabel, gbc);

        // 分支输入框
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        branchField.setPreferredSize(new Dimension(300, 28));
        panel.add(branchField, gbc);


        return panel;
    }

    /**
     * 创建底部按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getBorderLightColor()),
                BorderFactory.createEmptyBorder(15, 0, 0, 0)
        ));

        confirmButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL));

        confirmButton.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        cancelButton.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // 设置按钮样式
        confirmButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setPreferredSize(new Dimension(80, 30));

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

