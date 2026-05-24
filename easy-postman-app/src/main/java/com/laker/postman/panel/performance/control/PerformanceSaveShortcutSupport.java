package com.laker.postman.panel.performance.control;

import javax.swing.*;
import java.awt.*;

public final class PerformanceSaveShortcutSupport {

    public void install(JComponent component, Runnable saveAction) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, modifierKey);

        inputMap.put(saveKeyStroke, "savePerformanceConfig");
        actionMap.put("savePerformanceConfig", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveAction.run();
            }
        });
    }
}
