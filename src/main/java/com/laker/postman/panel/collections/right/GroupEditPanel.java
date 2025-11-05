package com.laker.postman.panel.collections.right;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.EasyPostmanTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.ScriptPanel;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

/**
 * 分组编辑面板 - 现代化版本
 * 在右侧主面板显示，参考 Postman 的设计
 * 复用 AuthTabPanel 和 ScriptPanel 组件
 * <p>
 * 设计说明：
 * - 参考 Postman，采用即时自动保存模式，无需显式保存按钮
 * - 数据变化时自动保存到模型并持久化
 * - 无需脏数据追踪和红点提示（Postman 的 Collection/Folder 设置也没有红点）
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

    // 原始数据快照，用于检测变化
    private String originalName;
    private String originalAuthType;
    private String originalAuthUsername;
    private String originalAuthPassword;
    private String originalAuthToken;
    private String originalPrescript;
    private String originalPostscript;

    // 防抖定时器
    private Timer autoSaveTimer;
    private static final int AUTO_SAVE_DELAY = 300; // 300ms 延迟保存（平衡响应速度和性能）

    public GroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group, Runnable onSave) {
        this.groupNode = groupNode;
        this.group = group;
        this.onSave = onSave;
        initAutoSaveTimer();
        initUI();
        loadGroupData();
        setupAutoSaveListeners();
    }

    private void initAutoSaveTimer() {
        // 创建防抖定时器，避免频繁保存
        autoSaveTimer = new Timer(AUTO_SAVE_DELAY, e -> {
            autoSaveGroupData();
            autoSaveTimer.stop();
        });
        autoSaveTimer.setRepeats(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        // 顶部标题栏
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 中间内容区域 - 使用 Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));

        // General Tab
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_TAB_GENERAL), createGeneralPanel());

        // Authorization Tab - 复用 AuthTabPanel
        authTabPanel = new AuthTabPanel();
        JPanel authWrapperPanel = new JPanel(new BorderLayout());
        authWrapperPanel.add(authTabPanel, BorderLayout.CENTER);

        JPanel authInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JLabel authInfoLabel = new JLabel(
                "<html><i style='color: #64748b; font-size: 11px;'>ℹ " +
                        I18nUtil.getMessage(MessageKeys.GROUP_EDIT_AUTH_INFO) +
                        "</i></html>"
        );
        authInfoPanel.add(authInfoLabel);
        authWrapperPanel.add(authInfoPanel, BorderLayout.SOUTH);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION), authWrapperPanel);

        // Scripts Tab - 复用 ScriptPanel
        scriptPanel = new ScriptPanel();
        JPanel scriptWrapperPanel = new JPanel(new BorderLayout());
        scriptWrapperPanel.add(scriptPanel, BorderLayout.CENTER);

        JPanel scriptInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JLabel scriptInfoLabel = new JLabel(
                "<html><i style='color: #64748b; font-size: 11px;'>ℹ " +
                        I18nUtil.getMessage(MessageKeys.GROUP_EDIT_SCRIPT_INFO) +
                        "</i></html>"
        );
        scriptInfoPanel.add(scriptInfoLabel);
        scriptWrapperPanel.add(scriptInfoPanel, BorderLayout.SOUTH);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS), scriptWrapperPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // 左侧：标题和图标
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        JLabel iconLabel = new JLabel("📁");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        leftPanel.add(iconLabel);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 18));
        titleLabel.setForeground(ModernColors.TEXT_PRIMARY);
        leftPanel.add(titleLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);

        // 底部分隔线
        JSeparator separator = new JSeparator();
        separator.setForeground(ModernColors.BORDER_LIGHT);
        JPanel separatorPanel = new JPanel(new BorderLayout());
        separatorPanel.add(separator, BorderLayout.SOUTH);
        headerPanel.add(separatorPanel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_NAME_LABEL));
        nameLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));
        nameLabel.setForeground(ModernColors.TEXT_PRIMARY);
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        nameField = new EasyPostmanTextField(30);
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        formPanel.add(nameField, gbc);

        // 水平填充
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(Box.createHorizontalGlue(), gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel descLabel = new JLabel(
                "<html><i style='color: #64748b; font-size: 11px;'>ℹ " +
                        I18nUtil.getMessage(MessageKeys.GROUP_EDIT_DESCRIPTION) +
                        "</i></html>"
        );
        formPanel.add(descLabel, gbc);

        // 垂直填充
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(formPanel, BorderLayout.NORTH);
        return panel;
    }

    /**
     * 设置自动保存监听器
     */
    private void setupAutoSaveListeners() {
        // 监听名称字段变化
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void removeUpdate(DocumentEvent e) {
                triggerAutoSave();
            }

            public void changedUpdate(DocumentEvent e) {
                triggerAutoSave();
            }
        });

        // 监听认证面板变化
        authTabPanel.addDirtyListener(this::triggerAutoSave);

        // 监听脚本面板变化
        scriptPanel.addDirtyListeners(this::triggerAutoSave);
    }

    /**
     * 触发自动保存（防抖）
     */
    private void triggerAutoSave() {
        // 重启防抖定时器
        if (autoSaveTimer.isRunning()) {
            autoSaveTimer.restart();
        } else {
            autoSaveTimer.start();
        }
    }

    /**
     * 自动保存分组数据
     */
    private void autoSaveGroupData() {
        // 先检查是否有修改
        if (!isModified()) {
            return; // 没有修改，不需要保存
        }

        String newName = nameField.getText().trim();

        // 验证名称
        if (newName.isEmpty()) {
            // 恢复原名称
            nameField.setText(group.getName());
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.GROUP_EDIT_NAME_EMPTY),
                    I18nUtil.getMessage(MessageKeys.GROUP_EDIT_VALIDATION_ERROR),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 检查名称是否改变
        boolean nameChanged = !safeEquals(group.getName(), newName);

        // 保存数据到模型
        group.setName(newName);
        group.setAuthType(authTabPanel.getAuthType());
        group.setAuthUsername(authTabPanel.getUsername());
        group.setAuthPassword(authTabPanel.getPassword());
        group.setAuthToken(authTabPanel.getToken());
        group.setPrescript(scriptPanel.getPrescript());
        group.setPostscript(scriptPanel.getPostscript());

        // 通知保存完成（触发持久化）
        if (onSave != null) {
            onSave.run();
        }

        // 如果名称改变了，更新 Tab 标题
        if (nameChanged) {
            SwingUtilities.invokeLater(() -> {
                SingletonFactory.getInstance(
                        RequestEditPanel.class
                ).updateGroupTabTitle(this, newName);
            });
        }

        // 更新原始数据快照
        updateOriginalSnapshot();
    }

    /**
     * 检查数据是否被修改
     */
    private boolean isModified() {
        if (!safeEquals(originalName, nameField.getText())) return true;
        if (!safeEquals(originalAuthType, authTabPanel.getAuthType())) return true;
        if (!safeEquals(originalAuthUsername, authTabPanel.getUsername())) return true;
        if (!safeEquals(originalAuthPassword, authTabPanel.getPassword())) return true;
        if (!safeEquals(originalAuthToken, authTabPanel.getToken())) return true;
        if (!safeEquals(originalPrescript, scriptPanel.getPrescript())) return true;
        if (!safeEquals(originalPostscript, scriptPanel.getPostscript())) return true;
        return false;
    }

    /**
     * 安全的字符串比较（处理 null 情况）
     */
    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 更新原始数据快照
     */
    private void updateOriginalSnapshot() {
        originalName = nameField.getText();
        originalAuthType = authTabPanel.getAuthType();
        originalAuthUsername = authTabPanel.getUsername();
        originalAuthPassword = authTabPanel.getPassword();
        originalAuthToken = authTabPanel.getToken();
        originalPrescript = scriptPanel.getPrescript();
        originalPostscript = scriptPanel.getPostscript();
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

        // 初始化原始数据快照
        updateOriginalSnapshot();
    }
}

