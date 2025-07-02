package com.laker.postman.common.setting;

import javax.swing.*;
import java.awt.*;

public class SettingDialog extends JDialog {
    public SettingDialog(Window owner) {
        super(owner, "全局设置", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        SettingPanel panel = new SettingPanel();
        add(panel, BorderLayout.CENTER);
        setSize(500, 300);
        setLocationRelativeTo(owner);
        setResizable(false);
        // 不全屏，不最大化
    }
}