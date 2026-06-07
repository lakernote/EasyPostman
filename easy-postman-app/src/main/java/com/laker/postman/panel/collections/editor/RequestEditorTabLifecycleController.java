package com.laker.postman.panel.collections.editor;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.component.tab.PlusTabComponent;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.function.BooleanSupplier;

/**
 * 请求编辑器 Tab 生命周期控制器。
 * <p>
 * 新建请求 Tab、维护末尾 +Tab 这些行为不属于面板布局本身，集中后更容易确认 Tab 顺序规则。
 */
@RequiredArgsConstructor
final class RequestEditorTabLifecycleController {
    private final JTabbedPane tabbedPane;
    private final String plusTabTitle;
    private final String defaultRequestTitle;
    private final BooleanSupplier autoInitializeSelectedTabOnAdd;
    private final Runnable selectedTabInitializer;

    RequestEditSubPanel addNewRequestTab(String title, RequestItemProtocolEnum protocol) {
        removeTrailingPlusTabIfPresent();

        String tabTitle = title != null ? title : defaultRequestTitle;
        RequestEditSubPanel subPanel = new RequestEditSubPanel(IdUtil.simpleUUID(), protocol);
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ClosableTabComponent(tabTitle, protocol));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        addPlusTab();
        RequestEditorTabInserter.setTabNewRequest(subPanel, true);
        return subPanel;
    }

    void addPlusTab() {
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            if (autoInitializeSelectedTabOnAdd.getAsBoolean()) {
                selectedTabInitializer.run();
            }
            return;
        }
        tabbedPane.addTab(plusTabTitle, new RequestEditorEmptyStatePanel());
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new PlusTabComponent());
    }

    boolean isPlusTab(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return false;
        }
        return plusTabTitle.equals(tabbedPane.getTitleAt(index));
    }

    private void removeTrailingPlusTabIfPresent() {
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
        }
    }
}
