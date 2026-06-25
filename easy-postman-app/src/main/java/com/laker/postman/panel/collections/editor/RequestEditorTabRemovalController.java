package com.laker.postman.panel.collections.editor;

import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;

/**
 * 请求编辑器 Tab 删除控制器。
 * <p>
 * 删除 Tab 时同时负责运行资源释放和临时 Tab 索引修正，避免关闭逻辑散落在面板里。
 */
@RequiredArgsConstructor
final class RequestEditorTabRemovalController {
    private final JTabbedPane tabbedPane;
    private final RequestEditorTransientTabManager transientTabManager;

    void removeAt(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return;
        }

        Component component = tabbedPane.getComponentAt(index);
        RequestEditorTabResourceCleaner.cleanup(component);
        transientTabManager.onTabRemoved(component, index);
        tabbedPane.removeTabAt(index);
    }

    void removeComponent(Component component) {
        int index = tabbedPane.indexOfComponent(component);
        if (index >= 0) {
            removeAt(index);
        }
    }
}
