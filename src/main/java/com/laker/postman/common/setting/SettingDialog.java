package com.laker.postman.common.setting;

import com.laker.postman.common.SingletonFactory;

import javax.swing.*;
import java.awt.*;

public class SettingDialog extends JDialog {
    public SettingDialog(Window owner) {
        super(owner, "全局设置", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        SettingPanel panel = SingletonFactory.getInstance(SettingPanel.class);
        add(panel, BorderLayout.CENTER);
        setSize(500, 600);
        setLocationRelativeTo(owner);
        setResizable(false); // 设置窗口不可调整大小
    }
}