package com.laker.postman.common.setting;

import com.laker.postman.common.panel.BasePanel;

import javax.swing.*;
import java.awt.*;

public class SettingPanel extends BasePanel {
    private JTextField maxBodySizeField;
    private JTextField requestTimeoutField;
    private JTextField maxDownloadSizeField;
    JButton saveBtn;
    JButton cancelBtn;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel maxBodySizeLabel = new JLabel("响应体最大显示大小 (KB):");
        maxBodySizeField = new JTextField(10);
        // 读取时字节转KB
        int maxBodySizeKB = SettingManager.getMaxBodySize() / 1024;
        maxBodySizeField.setText(String.valueOf(maxBodySizeKB));

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(maxBodySizeLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(maxBodySizeField, gbc);

        JLabel requestTimeoutLabel = new JLabel("请求超时时间 (ms, 0=永不超时):");
        requestTimeoutField = new JTextField(10);
        requestTimeoutField.setText(String.valueOf(SettingManager.getRequestTimeout()));
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(requestTimeoutLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(requestTimeoutField, gbc);

        JLabel maxDownloadSizeLabel = new JLabel("最大响应下载大小 (MB, 0=不限制):");
        maxDownloadSizeField = new JTextField(10);
        // 读取时字节转MB
        int maxDownloadSizeMB = SettingManager.getMaxDownloadSize() / (1024 * 1024);
        maxDownloadSizeField.setText(String.valueOf(maxDownloadSizeMB));
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(maxDownloadSizeLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(maxDownloadSizeField, gbc);
        saveBtn = new JButton("Save");
        cancelBtn = new JButton("Cancel");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        add(formPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings());
        cancelBtn.addActionListener(e -> {
            // 获取顶层窗口并关闭
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog dialog) {
                dialog.dispose();
            }
        });
    }


    private void saveSettings() {
        try {
            int sizeKB = Integer.parseInt(maxBodySizeField.getText().trim());
            if (sizeKB < 0) {
                JOptionPane.showMessageDialog(this, "不能小于0", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int timeout = Integer.parseInt(requestTimeoutField.getText().trim());
            if (timeout < 0) {
                JOptionPane.showMessageDialog(this, "不能小于0", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int maxDownloadMB = Integer.parseInt(maxDownloadSizeField.getText().trim());
            if (maxDownloadMB < 0) {
                JOptionPane.showMessageDialog(this, "不能小于0", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 保存时换算为字节
            SettingManager.setMaxBodySize(sizeKB * 1024);
            SettingManager.setRequestTimeout(timeout);
            SettingManager.setMaxDownloadSize(maxDownloadMB * 1024 * 1024);
            JOptionPane.showMessageDialog(this, "设置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}