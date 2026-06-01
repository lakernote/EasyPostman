package com.laker.postman.panel.collections.editor;

import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.service.setting.ShortcutManager;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

/**
 * 请求编辑器快捷键安装器。
 * <p>
 * 快捷键属于 Swing 控制逻辑，统一放在面板外部，避免 RequestEditorPanel 同时承担布局和动作绑定职责。
 */
@RequiredArgsConstructor
final class RequestEditorShortcutInstaller {
    private static final String ACTION_SEND_REQUEST = "sendRequest";
    private static final String ACTION_SAVE_REQUEST = "saveRequest";
    private static final String ACTION_NEW_REQUEST_TAB = "newRequestTab";
    private static final String ACTION_CLOSE_CURRENT_TAB = "closeCurrentTab";
    private static final String ACTION_CLOSE_OTHER_TABS = "closeOtherTabs";
    private static final String ACTION_CLOSE_ALL_TABS = "closeAllTabs";

    private final JComponent shortcutOwner;
    private final Supplier<RequestEditSubPanel> currentSubPanelSupplier;
    private final Runnable newRequestTabAction;
    private final Runnable closeCurrentTabAction;
    private final Runnable closeOtherTabsAction;
    private final Runnable closeAllTabsAction;

    void install() {
        InputMap inputMap = shortcutOwner.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = shortcutOwner.getActionMap();

        inputMap.clear();
        actionMap.clear();

        bind(inputMap, actionMap, ShortcutManager.SEND_REQUEST, ACTION_SEND_REQUEST, this::clickCurrentSendButton);
        bind(inputMap, actionMap, ShortcutManager.SAVE_REQUEST, ACTION_SAVE_REQUEST, this::clickCurrentSaveButton);
        bind(inputMap, actionMap, ShortcutManager.NEW_REQUEST, ACTION_NEW_REQUEST_TAB, newRequestTabAction);
        bind(inputMap, actionMap, ShortcutManager.CLOSE_CURRENT_TAB, ACTION_CLOSE_CURRENT_TAB, closeCurrentTabAction);
        bind(inputMap, actionMap, ShortcutManager.CLOSE_OTHER_TABS, ACTION_CLOSE_OTHER_TABS, closeOtherTabsAction);
        bind(inputMap, actionMap, ShortcutManager.CLOSE_ALL_TABS, ACTION_CLOSE_ALL_TABS, closeAllTabsAction);
    }

    private void bind(InputMap inputMap, ActionMap actionMap, String shortcutKey, String actionName, Runnable action) {
        KeyStroke keyStroke = ShortcutManager.getKeyStroke(shortcutKey);
        if (keyStroke == null) {
            return;
        }
        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void clickCurrentSendButton() {
        RequestEditSubPanel currentSubPanel = currentSubPanelSupplier.get();
        if (currentSubPanel == null || currentSubPanel.getRequestLinePanel() == null) {
            return;
        }
        JButton sendButton = currentSubPanel.getRequestLinePanel().getSendButton();
        clickAfterCommittingCellEditor(sendButton);
    }

    private void clickCurrentSaveButton() {
        RequestEditSubPanel currentSubPanel = currentSubPanelSupplier.get();
        if (currentSubPanel == null || currentSubPanel.getRequestLinePanel() == null) {
            return;
        }
        JButton saveButton = currentSubPanel.getRequestLinePanel().getSaveButton();
        clickAfterCommittingCellEditor(saveButton);
    }

    private void clickAfterCommittingCellEditor(JButton button) {
        if (button == null || !button.isEnabled()) {
            return;
        }
        // 焦点先落到按钮，触发表格 cell editor 的 terminateEditOnFocusLost，再执行真实动作。
        button.requestFocusInWindow();
        SwingUtilities.invokeLater(button::doClick);
    }
}
