package com.laker.postman.panel.collections.editor.request;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.model.RequestEditSubPanelType;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.editor.request.sub.*;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

final class RequestViewFactory {
    private RequestViewFactory() {
    }

    static RequestViewComponents create(RequestItemProtocolEnum protocol,
                                        RequestEditSubPanelType panelType,
                                        ActionListener sendAction) {
        boolean requestOnlySnapshot = panelType == RequestEditSubPanelType.PERFORMANCE_SNAPSHOT;
        RequestLinePanel requestLinePanel = new RequestLinePanel(sendAction, protocol);
        JComboBox<String> methodBox = requestLinePanel.getMethodBox();
        JTextField urlField = requestLinePanel.getUrlField();

        JTabbedPane reqTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        reqTabs.setMinimumSize(new java.awt.Dimension(0, 0));
        reqTabs.putClientProperty(FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER, false);
        reqTabs.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_CONTENT_SEPARATOR, false);

        MarkdownEditorPanel descriptionEditor = new MarkdownEditorPanel();
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE),
                descriptionEditor,
                null,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_DOCS)
        );

        EasyRequestParamsPanel paramsPanel = new EasyRequestParamsPanel();
        IndicatorTabComponent paramsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_PARAMS));
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.TAB_PARAMS),
                paramsPanel,
                paramsTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_PARAMS)
        );

        AuthTabPanel authTabPanel = new AuthTabPanel();
        IndicatorTabComponent authTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION));
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION),
                authTabPanel,
                authTabIndicator,
                protocol.isHttpProtocol()
                        && SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_AUTH)
        );

        EasyRequestHttpHeadersPanel headersPanel = new EasyRequestHttpHeadersPanel();
        IndicatorTabComponent headersTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS));
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS),
                headersPanel,
                headersTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_HEADERS)
        );

        RequestBodyPanel requestBodyPanel = new RequestBodyPanel(protocol);
        IndicatorTabComponent bodyTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY));
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY),
                requestBodyPanel,
                bodyTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_BODY)
        );

        ScriptPanel scriptPanel = new ScriptPanel();
        IndicatorTabComponent scriptsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS));
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS),
                scriptPanel,
                scriptsTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_SCRIPTS)
        );

        RequestSettingsPanel requestSettingsPanel = new RequestSettingsPanel();
        IndicatorTabComponent settingsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_SETTINGS));
        addTabIfVisible(
                reqTabs,
                I18nUtil.getMessage(MessageKeys.TAB_SETTINGS),
                requestSettingsPanel,
                settingsTabIndicator,
                SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_SETTINGS)
        );
        if (reqTabs.getTabCount() == 0) {
            addTabIfVisible(reqTabs, I18nUtil.getMessage(MessageKeys.TAB_PARAMS), paramsPanel, paramsTabIndicator, true);
        }

        boolean enableSaveButton = protocol.isHttpProtocol()
                && panelType != RequestEditSubPanelType.SAVED_RESPONSE
                && !requestOnlySnapshot;
        ResponsePanel responsePanel = requestOnlySnapshot ? null : new ResponsePanel(protocol, enableSaveButton);

        boolean isVertical = SettingManager.isLayoutVertical();
        int orientation = isVertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        JSplitPane splitPane = null;
        JComponent editorContent = reqTabs;
        if (!requestOnlySnapshot) {
            splitPane = new JSplitPane(orientation, reqTabs, responsePanel);
            splitPane.setDividerSize(4);
            splitPane.setOneTouchExpandable(false);
            splitPane.setContinuousLayout(true);
            editorContent = splitPane;
        }

        return new RequestViewComponents(
                requestLinePanel,
                methodBox,
                urlField,
                reqTabs,
                descriptionEditor,
                paramsPanel,
                paramsTabIndicator,
                authTabPanel,
                authTabIndicator,
                headersPanel,
                headersTabIndicator,
                requestBodyPanel,
                bodyTabIndicator,
                settingsTabIndicator,
                requestSettingsPanel,
                scriptPanel,
                scriptsTabIndicator,
                responsePanel,
                splitPane,
                editorContent
        );
    }

    private static void addTabIfVisible(JTabbedPane tabs,
                                        String title,
                                        Component component,
                                        IndicatorTabComponent indicator,
                                        boolean visible) {
        if (!visible) {
            return;
        }

        tabs.addTab(title, component);
        if (indicator != null) {
            tabs.setTabComponentAt(tabs.getTabCount() - 1, indicator);
        }
    }
}
