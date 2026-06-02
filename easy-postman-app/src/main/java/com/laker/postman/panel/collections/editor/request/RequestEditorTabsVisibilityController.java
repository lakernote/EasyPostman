package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import java.awt.Component;

/**
 * 请求编辑区 Tab 可见性控制器。
 * <p>
 * 用户可以在设置里隐藏 Docs/Params/Auth/Headers/Body/Scripts/Settings，重建和恢复选中项集中在这里。
 */
@RequiredArgsConstructor
final class RequestEditorTabsVisibilityController {
    private final RequestItemProtocolEnum protocol;
    private final RequestViewComponents view;
    private final Runnable indicatorUpdater;

    void updateVisibility() {
        Component selectedComponent = view.reqTabs.getSelectedComponent();
        RequestViewFactory.rebuildRequestTabs(view, protocol);
        RequestTabSelectionHelper.selectFirstVisible(view.reqTabs, selectedComponent, view.paramsPanel, view.requestBodyPanel);
        indicatorUpdater.run();
        view.reqTabs.revalidate();
        view.reqTabs.repaint();
    }
}
