package com.laker.postman.panel.sidebar.global;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * 全局变量管理对话框。
 */
public class GlobalVariablesDialog extends JDialog {

    private final GlobalVariablesPanel globalVariablesPanel;

    public GlobalVariablesDialog(Window owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_MANAGER_TITLE), ModalityType.MODELESS);
        globalVariablesPanel = new GlobalVariablesPanel();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setContentPane(globalVariablesPanel);
        setSize(960, 620);
        setMinimumSize(new Dimension(760, 480));
        setResizable(true);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        getRootPane().registerKeyboardAction(
                e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            setLocationRelativeTo(getOwner());
            globalVariablesPanel.prepareForDisplay();
        }
        super.setVisible(b);
    }
}
