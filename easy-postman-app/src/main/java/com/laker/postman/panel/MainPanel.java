package com.laker.postman.panel;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.panel.sidebar.SidebarTabPanel;

import java.awt.*;

/**
 * 主工作区容器，当前承载侧边栏标签页及其对应内容区。
 */
public class MainPanel extends UiSingletonPanel {

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);
        add(UiSingletonFactory.getInstance(SidebarTabPanel.class), BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        // no-op
    }
}
