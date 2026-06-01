package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.panel.collections.editor.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestSettingsPanel;
import com.laker.postman.panel.collections.editor.request.sub.ScriptPanel;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;

/**
 * 请求编辑区 Tab 可见性控制器。
 * <p>
 * 用户可以在设置里隐藏 Docs/Params/Auth/Headers/Body/Scripts/Settings，重建和恢复选中项集中在这里。
 */
@RequiredArgsConstructor
final class RequestEditorTabsVisibilityController {
    private final RequestItemProtocolEnum protocol;
    private final JTabbedPane reqTabs;
    private final MarkdownEditorPanel descriptionEditor;
    private final EasyRequestParamsPanel paramsPanel;
    private final IndicatorTabComponent paramsTabIndicator;
    private final AuthTabPanel authTabPanel;
    private final IndicatorTabComponent authTabIndicator;
    private final EasyRequestHttpHeadersPanel headersPanel;
    private final IndicatorTabComponent headersTabIndicator;
    private final RequestBodyPanel requestBodyPanel;
    private final IndicatorTabComponent bodyTabIndicator;
    private final ScriptPanel scriptPanel;
    private final IndicatorTabComponent scriptsTabIndicator;
    private final RequestSettingsPanel requestSettingsPanel;
    private final IndicatorTabComponent settingsTabIndicator;
    private final Runnable indicatorUpdater;

    void updateVisibility() {
        Component selectedComponent = reqTabs.getSelectedComponent();
        reqTabs.removeAll();
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE),
                descriptionEditor,
                null,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_DOCS)
        );
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.TAB_PARAMS),
                paramsPanel,
                paramsTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_PARAMS)
        );
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION),
                authTabPanel,
                authTabIndicator,
                protocol.isHttpProtocol()
                        && SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_AUTH)
        );
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS),
                headersPanel,
                headersTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_HEADERS)
        );
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY),
                requestBodyPanel,
                bodyTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_BODY)
        );
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS),
                scriptPanel,
                scriptsTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_SCRIPTS)
        );
        addTabIfVisible(
                I18nUtil.getMessage(MessageKeys.TAB_SETTINGS),
                requestSettingsPanel,
                settingsTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_SETTINGS)
        );
        if (reqTabs.getTabCount() == 0) {
            addTabIfVisible(
                    I18nUtil.getMessage(MessageKeys.TAB_PARAMS),
                    paramsPanel,
                    paramsTabIndicator,
                    true
            );
        }

        RequestTabSelectionHelper.selectFirstVisible(reqTabs, selectedComponent, paramsPanel, requestBodyPanel);
        indicatorUpdater.run();
        reqTabs.revalidate();
        reqTabs.repaint();
    }

    private void addTabIfVisible(String title,
                                 Component component,
                                 IndicatorTabComponent indicator,
                                 boolean visible) {
        if (!visible) {
            return;
        }

        reqTabs.addTab(title, component);
        if (indicator != null) {
            reqTabs.setTabComponentAt(reqTabs.getTabCount() - 1, indicator);
        }
    }
}
