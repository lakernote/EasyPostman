package com.laker.postman.panel.collections;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 请求集合面板，包含左侧的请求集合列表和右侧的请求编辑面板
 */
@Slf4j
public class RequestCollectionsPanel extends SingletonBasePanel {
    @Override
    protected void initUI() {
        // 设置布局为 BorderLayout
        setLayout(new BorderLayout()); // 设置布局为 BorderLayout
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        // 1.创建左侧的请求集合面板
        RequestCollectionsLeftPanel requestCollectionsLeftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        // 2. 创建右侧的请求编辑面板
        RequestEditPanel rightRequestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class); // 创建请求编辑面板实例
        // 创建水平分割面板，将左侧的集合面板和右侧的请求编辑面板放入其中
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestCollectionsLeftPanel, rightRequestEditPanel);
        mainSplit.setContinuousLayout(true); // 分割条拖动时实时更新布局
        mainSplit.setDividerLocation(250); // 设置初始分割位置
        mainSplit.setDividerSize(1);

        // 将分割面板添加到主面板
        add(mainSplit, BorderLayout.CENTER); // 将分割面板添加到主面板的中心位置
    }

    @Override
    protected void registerListeners() {

    }
}