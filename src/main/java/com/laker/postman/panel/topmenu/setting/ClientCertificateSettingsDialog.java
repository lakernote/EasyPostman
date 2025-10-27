package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 客户端证书设置对话框
 * 优化后的版本：使用国际化常量、改进对话框布局
 * Dialog 只负责显示 Panel，所有逻辑由 Panel 管理
 */
public class ClientCertificateSettingsDialog extends JDialog {
    private final ClientCertificateSettingsPanel settingsPanel;

    public ClientCertificateSettingsDialog(Frame owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.CERT_TITLE), true);
        settingsPanel = new ClientCertificateSettingsPanel(this);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 主面板 - Panel 自己管理所有内容包括关闭按钮
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);


        setSize(700, 450);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public static void showDialog(Frame owner) {
        ClientCertificateSettingsDialog dialog = new ClientCertificateSettingsDialog(owner);
        dialog.setVisible(true);
    }
}

