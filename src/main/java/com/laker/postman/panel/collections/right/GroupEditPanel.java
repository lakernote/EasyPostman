package com.laker.postman.panel.collections.right;

import com.laker.postman.model.RequestGroup;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.ScriptPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

/**
 * 分组编辑面板 - 在右侧主面板显示
 * 参考 Postman 的设计，不使用弹窗
 * 复用 AuthTabPanel 和 ScriptPanel 组件
 */
public class GroupEditPanel extends JPanel {
    @Getter
    private final DefaultMutableTreeNode groupNode;
    @Getter
    private final RequestGroup group;
    private final Runnable onSave;

    // UI Components
    private JTextField nameField;
    private AuthTabPanel authTabPanel;
    private ScriptPanel scriptPanel;

    public GroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group, Runnable onSave) {
        this.groupNode = groupNode;
        this.group = group;
        this.onSave = onSave;
        initUI();
        loadGroupData();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部标题
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Group Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(headerPanel, BorderLayout.NORTH);

        // 中间内容区域 - 使用 Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("General", createGeneralPanel());

        // 复用 AuthTabPanel
        authTabPanel = new AuthTabPanel();
        JPanel authWrapperPanel = new JPanel(new BorderLayout());
        authWrapperPanel.add(authTabPanel, BorderLayout.CENTER);
        JPanel authInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel authInfoLabel = new JLabel("<html><i>All requests in this group will inherit this authorization.</i></html>");
        authInfoLabel.setForeground(Color.GRAY);
        authInfoPanel.add(authInfoLabel);
        authWrapperPanel.add(authInfoPanel, BorderLayout.SOUTH);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION), authWrapperPanel);

        // 复用 ScriptPanel
        scriptPanel = new ScriptPanel();
        JPanel scriptWrapperPanel = new JPanel(new BorderLayout());
        scriptWrapperPanel.add(scriptPanel, BorderLayout.CENTER);
        JPanel scriptInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel scriptInfoLabel = new JLabel("<html><i>Scripts will run for all requests in this group.</i></html>");
        scriptInfoLabel.setForeground(Color.GRAY);
        scriptInfoPanel.add(scriptInfoLabel);
        scriptWrapperPanel.add(scriptInfoPanel, BorderLayout.SOUTH);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS), scriptWrapperPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 底部保存按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveGroupData());
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Group Name:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        nameField = new JTextField(30);
        formPanel.add(nameField, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        JLabel descLabel = new JLabel("<html><i>This group contains requests and other groups.</i></html>");
        descLabel.setForeground(Color.GRAY);
        formPanel.add(descLabel, gbc);

        panel.add(formPanel, BorderLayout.NORTH);
        return panel;
    }

    private void loadGroupData() {
        // Load general data
        nameField.setText(group.getName());

        // Load auth data using AuthTabPanel
        authTabPanel.setAuthType(group.getAuthType());
        authTabPanel.setUsername(group.getAuthUsername() != null ? group.getAuthUsername() : "");
        authTabPanel.setPassword(group.getAuthPassword() != null ? group.getAuthPassword() : "");
        authTabPanel.setToken(group.getAuthToken() != null ? group.getAuthToken() : "");

        // Load script data using ScriptPanel
        scriptPanel.setPrescript(group.getPrescript() != null ? group.getPrescript() : "");
        scriptPanel.setPostscript(group.getPostscript() != null ? group.getPostscript() : "");
    }

    private void saveGroupData() {
        String oldName = group.getName();
        String newName = nameField.getText().trim();

        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Group name cannot be empty",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Save general data
        group.setName(newName);

        // Save auth data from AuthTabPanel
        group.setAuthType(authTabPanel.getAuthType());
        group.setAuthUsername(authTabPanel.getUsername());
        group.setAuthPassword(authTabPanel.getPassword());
        group.setAuthToken(authTabPanel.getToken());

        // Save script data from ScriptPanel
        group.setPrescript(scriptPanel.getPrescript());
        group.setPostscript(scriptPanel.getPostscript());

        // 通知保存完成
        if (onSave != null) {
            onSave.run();
        }

        JOptionPane.showMessageDialog(this,
            "Group settings saved successfully",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    }
}

