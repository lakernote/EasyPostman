package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 客户端证书设置对话框
 * 优化后的版本：使用国际化常量、改进对话框布局
 */
public class ClientCertificateSettingsDialog extends JDialog {
    private final ClientCertificateSettingsPanel settingsPanel;

    public ClientCertificateSettingsDialog(Frame owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.CERT_TITLE), true);
        settingsPanel = new ClientCertificateSettingsPanel();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton closeBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_CLOSE));
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(900, 650);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public static void showDialog(Frame owner) {
        ClientCertificateSettingsDialog dialog = new ClientCertificateSettingsDialog(owner);
        dialog.setVisible(true);
    }
}

