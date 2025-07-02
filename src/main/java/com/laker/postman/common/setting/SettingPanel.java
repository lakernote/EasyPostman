package com.laker.postman.common.setting;

import javax.swing.*;
import java.awt.*;

public class SettingPanel extends JPanel {
    private final JTextField maxBodySizeField;
    private final JButton saveBtn;

    public SettingPanel() {
        setLayout(new BorderLayout(10, 10));
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel maxBodySizeLabel = new JLabel("响应体最大显示大小 (字节):");
        maxBodySizeField = new JTextField(10);
        maxBodySizeField.setText(String.valueOf(SettingManager.getMaxBodySize()));

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(maxBodySizeLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(maxBodySizeField, gbc);

        saveBtn = new JButton("保存");
        saveBtn.addActionListener(e -> saveSettings());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(saveBtn);

        add(formPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void saveSettings() {
        try {
            int size = Integer.parseInt(maxBodySizeField.getText().trim());
            if (size < 1024) {
                JOptionPane.showMessageDialog(this, "最小值为1024字节", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            SettingManager.setMaxBodySize(size);
            JOptionPane.showMessageDialog(this, "设置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}

