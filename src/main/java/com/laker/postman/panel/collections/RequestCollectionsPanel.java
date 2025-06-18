package com.laker.postman.panel.collections;

import com.laker.postman.common.AbstractBasePanel;
import com.laker.postman.common.SingletonPanelFactory;
import com.laker.postman.panel.collections.edit.RequestEditPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class RequestCollectionsPanel extends AbstractBasePanel {
    @Override
    protected void initUI() {
        // 设置布局为 BorderLayout
        setLayout(new BorderLayout()); // 设置布局为 BorderLayout
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        RequestCollectionsSubPanel requestCollectionsSubPanel = SingletonPanelFactory.getInstance(RequestCollectionsSubPanel.class);
        RequestEditPanel rightRequestEditPanel = RequestEditPanel.getInstance(); // 创建请求编辑面板实例
        // 创建水平分割面板，将左侧的集合面板和右侧的请求编辑面板放入其中
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestCollectionsSubPanel, rightRequestEditPanel);
        mainSplit.setContinuousLayout(true); // 分割条拖动时实时更新布局
        mainSplit.setDividerLocation(300); // 设置初始分割位置
        mainSplit.setDividerSize(1);

        // 将分割面板添加到主面板
        add(mainSplit, BorderLayout.CENTER); // 将分割面板添加到主面板的中心位置
    }

    @Override
    protected void registerListeners() {

    }
}