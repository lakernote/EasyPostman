package com.laker.postman.service.collections;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class RequestsTabsService {

    private RequestsTabsService() {
        // no-op
    }


    public static RequestEditSubPanel addTab(HttpRequestItem item) {
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Request item ID cannot be null or empty");
        }
        RequestEditSubPanel subPanel = new RequestEditSubPanel(id, item.getProtocol());
        subPanel.updateRequestForm(item);
        String tabTitle = CharSequenceUtil.isNotBlank(item.getName()) ? item.getName() : I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
        JTabbedPane tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1,
                new ClosableTabComponent(tabTitle, subPanel, tabbedPane));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        return subPanel;
    }

    public static void updateTabNew(RequestEditSubPanel panel, boolean isNew) {
        JTabbedPane tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0) return;
        Component tabComp = tabbedPane.getTabComponentAt(idx);
        if (tabComp instanceof ClosableTabComponent closable) {
            closable.setNewRequest(isNew);
        }
    }

}
