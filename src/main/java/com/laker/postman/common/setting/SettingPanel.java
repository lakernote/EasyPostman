package com.laker.postman.common.setting;

import com.laker.postman.common.panel.SingletonBasePanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class SettingPanel extends SingletonBasePanel {
    private JTextField maxBodySizeField;
    private JTextField requestTimeoutField;
    private JTextField maxDownloadSizeField;
    private JTextField jmeterMaxIdleField;
    private JTextField jmeterKeepAliveField;
    JButton saveBtn;
    JButton cancelBtn;
    private JCheckBox showDownloadProgressCheckBox;
    private JTextField downloadProgressDialogThresholdField;
    private JCheckBox followRedirectsCheckBox;

    // 用于输入验证的映射
    private final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();

    @Override
    protected void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 主面板使用BoxLayout以获得更好的垂直排列
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== 请求设置面板 =====
        JPanel requestPanel = createSectionPanel("请求设置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 响应体最大显示大小
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel maxBodySizeLabel = new JLabel("响应体最大显示大小 (KB):");
        maxBodySizeLabel.setToolTipText("响应体内容最大显示多少 KB，超出将被截断");
        requestPanel.add(maxBodySizeLabel, gbc);

        gbc.gridx = 1;
        maxBodySizeField = new JTextField(10);
        int maxBodySizeKB = SettingManager.getMaxBodySize() / 1024;
        maxBodySizeField.setText(String.valueOf(maxBodySizeKB));
        requestPanel.add(maxBodySizeField, gbc);

        // 请求超时时间
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel requestTimeoutLabel = new JLabel("请求超时时间 (ms, 0=永不超时):");
        requestTimeoutLabel.setToolTipText("接口请求超时时间，0 表示不限制");
        requestPanel.add(requestTimeoutLabel, gbc);

        gbc.gridx = 1;
        requestTimeoutField = new JTextField(10);
        requestTimeoutField.setText(String.valueOf(SettingManager.getRequestTimeout()));
        requestPanel.add(requestTimeoutField, gbc);

        // 最大响应下载大小
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel maxDownloadSizeLabel = new JLabel("最大响应下载大小 (MB, 0=不限制):");
        maxDownloadSizeLabel.setToolTipText("下载响应体最大允许大小，0 表示不限制");
        requestPanel.add(maxDownloadSizeLabel, gbc);

        gbc.gridx = 1;
        maxDownloadSizeField = new JTextField(10);
        int maxDownloadSizeMB = SettingManager.getMaxDownloadSize() / (1024 * 1024);
        maxDownloadSizeField.setText(String.valueOf(maxDownloadSizeMB));
        requestPanel.add(maxDownloadSizeField, gbc);

        // 自动重定向设置
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel followRedirectsLabel = new JLabel("自动重定向 (Follow Redirects):");
        followRedirectsLabel.setToolTipText("请求遇到重定向时是否自动跟随跳转");
        requestPanel.add(followRedirectsLabel, gbc);

        gbc.gridx = 1;
        followRedirectsCheckBox = new JCheckBox("自动重定向", SettingManager.isFollowRedirects());
        requestPanel.add(followRedirectsCheckBox, gbc);

        // ===== JMeter设置面板 =====
        JPanel jmeterPanel = createSectionPanel("压测连接设置");
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 最大空闲连接数
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel jmeterMaxIdleLabel = new JLabel("最大空闲连接数:");
        jmeterMaxIdleLabel.setToolTipText("线程池最大空闲连接数");
        jmeterPanel.add(jmeterMaxIdleLabel, gbc);

        gbc.gridx = 1;
        jmeterMaxIdleField = new JTextField(10);
        jmeterMaxIdleField.setText(String.valueOf(SettingManager.getJmeterMaxIdleConnections()));
        jmeterPanel.add(jmeterMaxIdleField, gbc);

        // 连接保活时间
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel jmeterKeepAliveLabel = new JLabel("连接保活时间 (秒):");
        jmeterKeepAliveLabel.setToolTipText("连接最大保活时间，单位秒");
        jmeterPanel.add(jmeterKeepAliveLabel, gbc);

        gbc.gridx = 1;
        jmeterKeepAliveField = new JTextField(10);
        jmeterKeepAliveField.setText(String.valueOf(SettingManager.getJmeterKeepAliveSeconds()));
        jmeterPanel.add(jmeterKeepAliveField, gbc);

        // ===== 下载设置面板 =====
        JPanel downloadPanel = createSectionPanel("下载设置");
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 下载进度对话框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        showDownloadProgressCheckBox = new JCheckBox("下载大文件时显示进度弹窗", SettingManager.isShowDownloadProgressDialog());
        showDownloadProgressCheckBox.setToolTipText("下载大文件时弹出进度提示窗口");
        downloadPanel.add(showDownloadProgressCheckBox, gbc);

        // 进度弹窗阈值
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel downloadProgressDialogThresholdLabel = new JLabel("进度弹窗阈值 (MB):");
        downloadProgressDialogThresholdLabel.setToolTipText("下载文件大于此阈值时弹窗");
        downloadPanel.add(downloadProgressDialogThresholdLabel, gbc);

        gbc.gridx = 1;
        downloadProgressDialogThresholdField = new JTextField(10);
        int thresholdMB = SettingManager.getDownloadProgressDialogThreshold() / (1024 * 1024);
        downloadProgressDialogThresholdField.setText(String.valueOf(thresholdMB));
        downloadPanel.add(downloadProgressDialogThresholdField, gbc);

        // 设置下载阈值字段的启用状态根据复选框状态
        downloadProgressDialogThresholdField.setEnabled(showDownloadProgressCheckBox.isSelected());
        downloadProgressDialogThresholdLabel.setEnabled(showDownloadProgressCheckBox.isSelected());
        showDownloadProgressCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == 1;
            downloadProgressDialogThresholdField.setEnabled(selected);
            downloadProgressDialogThresholdLabel.setEnabled(selected);
        });

        // 添加所有面板到主面板
        mainPanel.add(requestPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(jmeterPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(downloadPanel);
        mainPanel.add(Box.createVerticalGlue()); // 添加弹性空间使面板保持在顶部

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // 使滚动更流畅

        // 创建按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        saveBtn = new JButton("保存");
        cancelBtn = new JButton("取消");

        // 美化按钮
        saveBtn.setPreferredSize(new Dimension(100, 30));
        cancelBtn.setPreferredSize(new Dimension(100, 30));

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        // 添加到主面板
        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // 设置面板首选大小
        setPreferredSize(new Dimension(500, 450));

        // 设置输入验证
        setupValidators();

        // 支持回车保存、ESC取消
        setupKeyboardNavigation();
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font(panel.getFont().getName(), Font.BOLD, 12)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void setupValidators() {
        // 设置验证器
        validators.put(maxBodySizeField, s -> isInteger(s) && Integer.parseInt(s) >= 0);
        validators.put(requestTimeoutField, s -> isInteger(s) && Integer.parseInt(s) >= 0);
        validators.put(maxDownloadSizeField, s -> isInteger(s) && Integer.parseInt(s) >= 0);
        validators.put(jmeterMaxIdleField, s -> isInteger(s) && Integer.parseInt(s) > 0);
        validators.put(jmeterKeepAliveField, s -> isInteger(s) && Integer.parseInt(s) > 0);
        validators.put(downloadProgressDialogThresholdField, s -> isInteger(s) && Integer.parseInt(s) >= 0);

        // 设置错误消息
        errorMessages.put(maxBodySizeField, "响应体大小不能小于0");
        errorMessages.put(requestTimeoutField, "超时时间不能小于0");
        errorMessages.put(maxDownloadSizeField, "下载大小不能小于0");
        errorMessages.put(jmeterMaxIdleField, "最大连接数必须大于0");
        errorMessages.put(jmeterKeepAliveField, "连接保活时间必须大于0");
        errorMessages.put(downloadProgressDialogThresholdField, "进度弹窗阈值不能小于0");

        // 添加实时验证
        DocumentListener validationListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateField(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateField(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateField(e);
            }

            private void validateField(DocumentEvent e) {
                for (Map.Entry<JTextField, Predicate<String>> entry : validators.entrySet()) {
                    JTextField field = entry.getKey();
                    if (field.getDocument() == e.getDocument()) {
                        String text = field.getText().trim();
                        boolean valid = text.isEmpty() || entry.getValue().test(text);
                        field.setBackground(valid ? Color.WHITE : new Color(255, 220, 220));
                        field.setToolTipText(valid ? null : errorMessages.get(field));
                        break;
                    }
                }
            }
        };

        // 为所有字段添加验证监听器
        for (JTextField field : validators.keySet()) {
            field.getDocument().addDocumentListener(validationListener);
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setupKeyboardNavigation() {
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    saveSettings();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    Window window = SwingUtilities.getWindowAncestor(SettingPanel.this);
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            }
        };

        // 为所有文本字段添加键盘监听器
        maxBodySizeField.addKeyListener(keyAdapter);
        requestTimeoutField.addKeyListener(keyAdapter);
        maxDownloadSizeField.addKeyListener(keyAdapter);
        jmeterMaxIdleField.addKeyListener(keyAdapter);
        jmeterKeepAliveField.addKeyListener(keyAdapter);
        downloadProgressDialogThresholdField.addKeyListener(keyAdapter);
        showDownloadProgressCheckBox.addKeyListener(keyAdapter);
        followRedirectsCheckBox.addKeyListener(keyAdapter);
    }

    @Override
    protected void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings());
        cancelBtn.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog dialog) {
                dialog.dispose();
            }
        });
    }

    private void highlightError(JTextField field) {
        field.setBackground(new Color(255, 220, 220));
        field.requestFocus();
    }

    private void clearHighlight() {
        for (JTextField field : validators.keySet()) {
            field.setBackground(Color.WHITE);
            field.setToolTipText(null);
        }
    }

    private void saveSettings() {
        clearHighlight();

        // 验证所有字段
        for (Map.Entry<JTextField, Predicate<String>> entry : validators.entrySet()) {
            JTextField field = entry.getKey();
            String text = field.getText().trim();

            // 跳过禁用的字段
            if (!field.isEnabled()) {
                continue;
            }

            if (text.isEmpty() || !entry.getValue().test(text)) {
                highlightError(field);
                JOptionPane.showMessageDialog(this, errorMessages.get(field), "验证错误", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            // 保存设置
            int sizeKB = Integer.parseInt(maxBodySizeField.getText().trim());
            int timeout = Integer.parseInt(requestTimeoutField.getText().trim());
            int maxDownloadMB = Integer.parseInt(maxDownloadSizeField.getText().trim());
            int jmeterMaxIdle = Integer.parseInt(jmeterMaxIdleField.getText().trim());
            long jmeterKeepAlive = Long.parseLong(jmeterKeepAliveField.getText().trim());
            int thresholdMB = Integer.parseInt(downloadProgressDialogThresholdField.getText().trim());

            SettingManager.setMaxBodySize(sizeKB * 1024);
            SettingManager.setRequestTimeout(timeout);
            SettingManager.setMaxDownloadSize(maxDownloadMB * 1024 * 1024);
            SettingManager.setJmeterMaxIdleConnections(jmeterMaxIdle);
            SettingManager.setJmeterKeepAliveSeconds(jmeterKeepAlive);
            SettingManager.setShowDownloadProgressDialog(showDownloadProgressCheckBox.isSelected());
            SettingManager.setDownloadProgressDialogThreshold(thresholdMB * 1024 * 1024);
            SettingManager.setFollowRedirects(followRedirectsCheckBox.isSelected());

            JOptionPane.showMessageDialog(this, "设置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog dialog) {
                dialog.dispose();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}