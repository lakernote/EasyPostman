package com.laker.postman.panel.collections.right.request;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.model.RequestEditSubPanelType;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.sub.*;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.event.ActionListener;

final class RequestViewFactory {
    private RequestViewFactory() {
    }

    static RequestViewComponents create(RequestItemProtocolEnum protocol,
                                        RequestEditSubPanelType panelType,
                                        ActionListener sendAction) {
        RequestLinePanel requestLinePanel = new RequestLinePanel(sendAction, protocol);
        JComboBox<String> methodBox = requestLinePanel.getMethodBox();
        JTextField urlField = requestLinePanel.getUrlField();

        JTabbedPane reqTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        reqTabs.setMinimumSize(new java.awt.Dimension(0, 0));
        reqTabs.putClientProperty(FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER, false);
        reqTabs.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_CONTENT_SEPARATOR, false);

        MarkdownEditorPanel descriptionEditor = new MarkdownEditorPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE), descriptionEditor);

        EasyRequestParamsPanel paramsPanel = new EasyRequestParamsPanel();
        IndicatorTabComponent paramsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_PARAMS));
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_PARAMS), paramsPanel);
        reqTabs.setTabComponentAt(1, paramsTabIndicator);

        AuthTabPanel authTabPanel = new AuthTabPanel();
        IndicatorTabComponent authTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION));
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION), authTabPanel);
        reqTabs.setTabComponentAt(2, authTabIndicator);

        EasyRequestHttpHeadersPanel headersPanel = new EasyRequestHttpHeadersPanel();
        IndicatorTabComponent headersTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS));
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS), headersPanel);
        reqTabs.setTabComponentAt(3, headersTabIndicator);

        RequestBodyPanel requestBodyPanel = new RequestBodyPanel(protocol);
        IndicatorTabComponent bodyTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY));
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY), requestBodyPanel);
        reqTabs.setTabComponentAt(4, bodyTabIndicator);

        ScriptPanel scriptPanel = new ScriptPanel();
        IndicatorTabComponent scriptsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS));
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS), scriptPanel);
        reqTabs.setTabComponentAt(5, scriptsTabIndicator);

        RequestSettingsPanel requestSettingsPanel = new RequestSettingsPanel();
        IndicatorTabComponent settingsTabIndicator = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.TAB_SETTINGS));
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_SETTINGS), requestSettingsPanel);
        reqTabs.setTabComponentAt(6, settingsTabIndicator);

        boolean enableSaveButton = protocol.isHttpProtocol() && panelType != RequestEditSubPanelType.SAVED_RESPONSE;
        ResponsePanel responsePanel = new ResponsePanel(protocol, enableSaveButton);

        boolean isVertical = SettingManager.isLayoutVertical();
        int orientation = isVertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        JSplitPane splitPane = new JSplitPane(orientation, reqTabs, responsePanel);
        splitPane.setDividerSize(4);
        splitPane.setOneTouchExpandable(false);
        splitPane.setContinuousLayout(true);

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
                splitPane
        );
    }
}
