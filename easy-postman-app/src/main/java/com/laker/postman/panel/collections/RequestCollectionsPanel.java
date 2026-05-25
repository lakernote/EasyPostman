package com.laker.postman.panel.collections;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;

import javax.swing.*;
import java.awt.*;

/**
 * 请求集合工作区，左侧展示集合树，右侧展示请求编辑器。
 */
public class RequestCollectionsPanel extends UiSingletonPanel {
    private static final int DEFAULT_DIVIDER_LOCATION = 250;
    private static final int DIVIDER_SIZE = 3;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        add(createCollectionsSplitPane(), BorderLayout.CENTER);
    }

    private JSplitPane createCollectionsSplitPane() {
        CollectionTreePanel collectionTreePanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditorPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, collectionTreePanel, requestEditorPanel);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);
        splitPane.setDividerSize(DIVIDER_SIZE);
        return splitPane;
    }

    @Override
    protected void registerListeners() {
        // no op
    }
}
