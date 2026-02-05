package com.laker.postman.service.collections;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.variable.RequestContext;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
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

        // 查找请求节点并设置到全局上下文（供分组变量使用）
        DefaultTreeNodeRepository repository = SingletonFactory.getInstance(DefaultTreeNodeRepository.class);
        repository.getRootNode().ifPresent(rootNode -> {
            DefaultMutableTreeNode requestNode = RequestCollectionsService.findRequestNodeById(rootNode, id);
            if (requestNode != null) {
                RequestContext.setCurrentRequestNode(requestNode);
            }
        });

        RequestEditSubPanel subPanel = new RequestEditSubPanel(id, item.getProtocol());
        subPanel.initPanelData(item);
        String tabTitle = CharSequenceUtil.isNotBlank(item.getName()) ? item.getName() : I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
        JTabbedPane tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ClosableTabComponent(tabTitle, item.getProtocol()));
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
