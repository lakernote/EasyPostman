package com.laker.postman.panel.collections.right;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.table.EasyPostmanEnvironmentTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.right.request.sub.ScriptPanel;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

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
    private MarkdownEditorPanel descriptionEditor;
    private AuthTabPanel authTabPanel;
    private ScriptPanel scriptPanel;
    private EasyRequestHttpHeadersPanel headersPanel;
    private EasyPostmanEnvironmentTablePanel variablesPanel;
    private JTabbedPane tabbedPane;

    // 原始数据快照，用于检测变化
    private String originalName;
    private String originalDescription;
    private String originalAuthType;
    private String originalAuthUsername;
    private String originalAuthPassword;
    private String originalAuthToken;
    private String originalPrescript;
    private String originalPostscript;
    private List<HttpHeader> originalHeaders;
    private List<com.laker.postman.model.EnvironmentVariable> originalVariables;

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
        setupSaveShortcut();
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
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));

        // General Tab
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_TAB_GENERAL), createGeneralPanel());

        // Headers Tab - 公共请求头配置
        headersPanel = new EasyRequestHttpHeadersPanel();
        JPanel headersWrapperPanel = new JPanel(new BorderLayout());
        headersWrapperPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headersWrapperPanel.add(headersPanel, BorderLayout.CENTER);

        JPanel headersInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JLabel headersInfoLabel = new JLabel(
                "<html><i style='color: #64748b; font-size: 11px;'>ℹ " +
                        I18nUtil.getMessage(MessageKeys.GROUP_EDIT_HEADERS_INFO) +
                        "</i></html>"
        );
        headersInfoPanel.add(headersInfoLabel);
        headersWrapperPanel.add(headersInfoPanel, BorderLayout.SOUTH);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_HEADERS), headersWrapperPanel);

        // Variables Tab - 分组级别的变量
        variablesPanel = new EasyPostmanEnvironmentTablePanel();
        JPanel variablesWrapperPanel = new JPanel(new BorderLayout());
        variablesWrapperPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        variablesWrapperPanel.add(variablesPanel, BorderLayout.CENTER);

        JPanel variablesInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JLabel variablesInfoLabel = new JLabel(
                "<html><i style='color: #64748b; font-size: 11px;'>ℹ " +
                        I18nUtil.getMessage(MessageKeys.GROUP_EDIT_VARIABLES_INFO) +
                        "</i></html>"
        );
        variablesInfoPanel.add(variablesInfoLabel);
        variablesWrapperPanel.add(variablesInfoPanel, BorderLayout.SOUTH);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_VARIABLES), variablesWrapperPanel);


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

        // 左侧：标题
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GROUP_EDIT_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +6));
        leftPanel.add(titleLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);

        // 底部分隔线
        JSeparator separator = new JSeparator();
        separator.setForeground(ModernColors.getBorderLightColor());
        JPanel separatorPanel = new JPanel(new BorderLayout());
        separatorPanel.add(separator, BorderLayout.SOUTH);
        headerPanel.add(separatorPanel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

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
        nameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        nameField = new EasyTextField(30);
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1)); // 比标准字体大1号
        formPanel.add(nameField, gbc);

        // 水平填充
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(Box.createHorizontalGlue(), gbc);

        // Description Markdown Editor - 占据两行 (gridy 0 和 1)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;  // 占据所有列
        gbc.gridheight = 2; // 占据两行
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        descriptionEditor = new MarkdownEditorPanel();
        // 使用最小尺寸而不是固定尺寸，让编辑器自适应容器大小
        descriptionEditor.setMinimumSize(new Dimension(600, 250));
        descriptionEditor.setPreferredSize(new Dimension(800, 350));
        formPanel.add(descriptionEditor, gbc);

        // Description hint
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel hintLabel = new JLabel(
                "<html><i style='color: #64748b; font-size: 10px;'>ℹ " +
                        I18nUtil.getMessage(MessageKeys.GROUP_EDIT_DESCRIPTION_PLACEHOLDER) +
                        "</i></html>"
        );
        formPanel.add(hintLabel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
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

        // 监听描述字段变化
        descriptionEditor.addDocumentListener(new DocumentListener() {
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

        // 监听请求头面板变化
        headersPanel.addTableModelListener(e -> triggerAutoSave());

        // 监听变量面板变化
        variablesPanel.addTableModelListener(e -> triggerAutoSave());
    }

    /**
     * 设置保存快捷键 (Command+S / Ctrl+S)
     */
    private void setupSaveShortcut() {
        // 获取快捷键修饰符（Mac 使用 Command，其他系统使用 Ctrl）
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // 创建 KeyStroke (Cmd+S / Ctrl+S)
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, modifierKey);

        // 绑定快捷键到 InputMap
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        String actionKey = "saveGroup";
        inputMap.put(saveKeyStroke, actionKey);

        // 定义快捷键动作：立即保存（绕过防抖）
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 停止防抖定时器
                if (autoSaveTimer.isRunning()) {
                    autoSaveTimer.stop();
                }
                // 立即保存
                autoSaveGroupData();
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.SAVE_SUCCESS));

            }
        });
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
        group.setDescription(descriptionEditor.getText());
        group.setAuthType(authTabPanel.getAuthType());
        group.setAuthUsername(authTabPanel.getUsername());
        group.setAuthPassword(authTabPanel.getPassword());
        group.setAuthToken(authTabPanel.getToken());
        group.setPrescript(scriptPanel.getPrescript());
        group.setPostscript(scriptPanel.getPostscript());
        group.setHeaders(headersPanel.getHeadersList());
        group.setVariables(variablesPanel.getVariableList());

        // ⚡ 重要：使缓存失效，确保分组修改立即生效
        PreparedRequestBuilder.invalidateCache();

        // 通知保存完成（触发持久化）
        if (onSave != null) {
            onSave.run();
        }

        // 如果名称改变了，更新 Tab 标题
        if (nameChanged) {
            SwingUtilities.invokeLater(() -> SingletonFactory.getInstance(
                    RequestEditPanel.class
            ).updateGroupTabTitle(this, newName));
        }

        // 更新原始数据快照
        updateOriginalSnapshot();
    }

    /**
     * 检查数据是否被修改
     */
    private boolean isModified() {
        if (!safeEquals(originalName, nameField.getText())) return true;
        if (!safeEquals(originalDescription, descriptionEditor.getText())) return true;
        if (!safeEquals(originalAuthType, authTabPanel.getAuthType())) return true;
        if (!safeEquals(originalAuthUsername, authTabPanel.getUsername())) return true;
        if (!safeEquals(originalAuthPassword, authTabPanel.getPassword())) return true;
        if (!safeEquals(originalAuthToken, authTabPanel.getToken())) return true;
        if (!safeEquals(originalPrescript, scriptPanel.getPrescript())) return true;
        if (!safeEquals(originalPostscript, scriptPanel.getPostscript())) return true;
        if (!headersEquals(originalHeaders, headersPanel.getHeadersList())) return true;
        if (!variablesEquals(originalVariables, variablesPanel.getVariableList())) return true;
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
     * 比较两个 Headers 列表是否相同
     */
    private boolean headersEquals(List<HttpHeader> a, List<HttpHeader> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            HttpHeader ha = a.get(i);
            HttpHeader hb = b.get(i);
            if (ha.isEnabled() != hb.isEnabled()) return false;
            if (!safeEquals(ha.getKey(), hb.getKey())) return false;
            if (!safeEquals(ha.getValue(), hb.getValue())) return false;
        }
        return true;
    }

    /**
     * 比较两个 Variables 列表是否相同
     */
    private boolean variablesEquals(List<com.laker.postman.model.EnvironmentVariable> a,
                                    List<com.laker.postman.model.EnvironmentVariable> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            com.laker.postman.model.EnvironmentVariable va = a.get(i);
            com.laker.postman.model.EnvironmentVariable vb = b.get(i);
            if (va.isEnabled() != vb.isEnabled()) return false;
            if (!safeEquals(va.getKey(), vb.getKey())) return false;
            if (!safeEquals(va.getValue(), vb.getValue())) return false;
        }
        return true;
    }

    /**
     * 更新原始数据快照
     */
    private void updateOriginalSnapshot() {
        originalName = nameField.getText();
        originalDescription = descriptionEditor.getText();
        originalAuthType = authTabPanel.getAuthType();
        originalAuthUsername = authTabPanel.getUsername();
        originalAuthPassword = authTabPanel.getPassword();
        originalAuthToken = authTabPanel.getToken();
        originalPrescript = scriptPanel.getPrescript();
        originalPostscript = scriptPanel.getPostscript();
        originalHeaders = new java.util.ArrayList<>(headersPanel.getHeadersList());
        originalVariables = new java.util.ArrayList<>(variablesPanel.getVariableList());
    }

    private void loadGroupData() {
        // Load general data
        nameField.setText(group.getName());
        descriptionEditor.setText(group.getDescription() != null ? group.getDescription() : "");

        // Load auth data using AuthTabPanel
        authTabPanel.setAuthType(group.getAuthType());
        authTabPanel.setUsername(group.getAuthUsername() != null ? group.getAuthUsername() : "");
        authTabPanel.setPassword(group.getAuthPassword() != null ? group.getAuthPassword() : "");
        authTabPanel.setToken(group.getAuthToken() != null ? group.getAuthToken() : "");

        // Load script data using ScriptPanel
        scriptPanel.setPrescript(group.getPrescript() != null ? group.getPrescript() : "");
        scriptPanel.setPostscript(group.getPostscript() != null ? group.getPostscript() : "");

        // Load headers data using EasyRequestHttpHeadersPanel
        if (group.getHeaders() != null && !group.getHeaders().isEmpty()) {
            headersPanel.setHeadersList(group.getHeaders());
        }

        // Load variables data using EasyPostmanEnvironmentTablePanel
        if (group.getVariables() != null && !group.getVariables().isEmpty()) {
            variablesPanel.setVariableList(group.getVariables());
        }

        // 如果 headers 中有超过 4 个请求头，默认选中第二个 tab (Headers)
        if (group.getHeaders() != null && group.getHeaders().size() > 4) {
            tabbedPane.setSelectedIndex(1);
        }

        // 初始化原始数据快照
        updateOriginalSnapshot();
    }
}

