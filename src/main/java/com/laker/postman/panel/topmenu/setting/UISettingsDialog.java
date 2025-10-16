package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 界面设置对话框
 */
public class UISettingsDialog extends JDialog {
    private UISettingsPanel settingsPanel;

    public UISettingsDialog(Window parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_DOWNLOAD_TITLE) + " & " + I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_TITLE), ModalityType.APPLICATION_MODAL);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);

        settingsPanel = new UISettingsPanel();
        add(settingsPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/icon.png")));
        } catch (Exception e) {
            // ignore
        }
    }
}
