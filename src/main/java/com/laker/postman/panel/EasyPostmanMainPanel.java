package com.laker.postman.panel;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.TopMenuBarPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 包含了左侧的标签页面板和右侧的请求编辑面板。
 * 左侧标签页面板包含了集合、环境变量、压测三个标签页，
 */
@Slf4j
public class EasyPostmanMainPanel extends JPanel {

    private static class Holder {
        private static final EasyPostmanMainPanel INSTANCE = new EasyPostmanMainPanel();
    }

    public static EasyPostmanMainPanel getInstance() {
        return Holder.INSTANCE;
    }

    private EasyPostmanMainPanel() {
        setLayout(new BorderLayout()); // 设置布局为 BorderLayout
        // 顶部菜单栏（含环境选择器）
        add(TopMenuBarPanel.getInstance(), BorderLayout.NORTH);
        // 创建左侧标签页面板（包含集合、环境变量、压测三个标签页）
        SidebarTabPanel sidebarTabPanel = SingletonFactory.getInstance(SidebarTabPanel.class); // 创建标签页面板实例
        add(sidebarTabPanel, BorderLayout.CENTER);
    }
}
