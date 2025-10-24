package com.laker.postman.panel;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.panel.topmenu.TopMenuBarPanel;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * 包含了左侧的标签页面板和右侧的请求编辑面板。
 * 左侧标签页面板包含了集合、环境变量、压测三个标签页，
 */
@Slf4j
public class MainPanel extends SingletonBasePanel {

    @Override
    protected void initUI() {
        setLayout(new BorderLayout()); // 设置布局为 BorderLayout
        // 顶部菜单栏（含环境选择器）
        add(SingletonFactory.getInstance(TopMenuBarPanel.class), BorderLayout.NORTH);
        // 中间SidebarTabPanel + 底部控制台面板
        add(SingletonFactory.getInstance(SidebarTabPanel.class), BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        // no-op
    }
}