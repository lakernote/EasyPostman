package com.laker.postman.common.setting;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 网络代理设置对话框
 */
public class ProxySettingsDialog extends JDialog {

    public ProxySettingsDialog(Window parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE), ModalityType.APPLICATION_MODAL);
        initUI();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        ProxySettingsPanel panel = new ProxySettingsPanel();
        add(panel, BorderLayout.CENTER);

        pack();
        setResizable(false);
    }
}
