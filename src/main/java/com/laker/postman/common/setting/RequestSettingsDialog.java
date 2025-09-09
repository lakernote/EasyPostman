package com.laker.postman.common.setting;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 请求设置对话框
 */
public class RequestSettingsDialog extends JDialog {
    private RequestSettingsPanel settingsPanel;

    public RequestSettingsDialog(Window parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE), ModalityType.APPLICATION_MODAL);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);

        settingsPanel = new RequestSettingsPanel();
        add(settingsPanel);

        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // 设置图标
        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/icon.png")));
        } catch (Exception e) {
            // ignore
        }
    }
}
