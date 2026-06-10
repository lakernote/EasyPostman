package com.laker.postman.panel.collections;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;

import javax.swing.*;
import java.awt.*;

/**
 * 请求集合工作区，左侧展示集合树，右侧展示请求编辑器。
 */
public class RequestCollectionsPanel extends UiSingletonPanel {
    private static final int DEFAULT_DIVIDER_LOCATION = ToolWindowChrome.DEFAULT_SIDE_WIDTH;

    @Override
    protected void initUI() {
        ToolWindowSurfaceStyle.applyBackground(this);
        setLayout(new BorderLayout());
        add(createCollectionsSplitPane(), BorderLayout.CENTER);
    }

    private JSplitPane createCollectionsSplitPane() {
        CollectionTreePanel collectionTreePanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditorPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);

        JSplitPane splitPane = createCollectionsSplitPane(collectionTreePanel, requestEditorPanel);
        return splitPane;
    }

    static JSplitPane createCollectionsSplitPane(Component collectionTreePanel, Component requestEditorPanel) {
        return ToolWindowChrome.createHorizontalCardSplitPane(
                collectionTreePanel,
                requestEditorPanel,
                DEFAULT_DIVIDER_LOCATION
        );
    }

    static JComponent createCollectionToolWindow(Component collectionTreePanel) {
        return ToolWindowChrome.wrapLeftInsetToolWindow(collectionTreePanel);
    }

    static JComponent createRequestEditorToolWindow(Component requestEditorPanel) {
        return ToolWindowChrome.wrapRightToolWindow(requestEditorPanel);
    }

    @Override
    protected void registerListeners() {
        // no op
    }
}
