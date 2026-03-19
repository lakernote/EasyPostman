package com.laker.postman.panel.collections.right.request;

import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.panel.collections.right.request.sub.ScriptPanel;

import javax.swing.*;

final class RequestViewComponents {
    final RequestLinePanel requestLinePanel;
    final JComboBox<String> methodBox;
    final JTextField urlField;
    final JTabbedPane reqTabs;
    final MarkdownEditorPanel descriptionEditor;
    final EasyRequestParamsPanel paramsPanel;
    final IndicatorTabComponent paramsTabIndicator;
    final AuthTabPanel authTabPanel;
    final IndicatorTabComponent authTabIndicator;
    final EasyRequestHttpHeadersPanel headersPanel;
    final IndicatorTabComponent headersTabIndicator;
    final RequestBodyPanel requestBodyPanel;
    final IndicatorTabComponent bodyTabIndicator;
    final ScriptPanel scriptPanel;
    final IndicatorTabComponent scriptsTabIndicator;
    final ResponsePanel responsePanel;
    final JSplitPane splitPane;

    RequestViewComponents(RequestLinePanel requestLinePanel,
                          JComboBox<String> methodBox,
                          JTextField urlField,
                          JTabbedPane reqTabs,
                          MarkdownEditorPanel descriptionEditor,
                          EasyRequestParamsPanel paramsPanel,
                          IndicatorTabComponent paramsTabIndicator,
                          AuthTabPanel authTabPanel,
                          IndicatorTabComponent authTabIndicator,
                          EasyRequestHttpHeadersPanel headersPanel,
                          IndicatorTabComponent headersTabIndicator,
                          RequestBodyPanel requestBodyPanel,
                          IndicatorTabComponent bodyTabIndicator,
                          ScriptPanel scriptPanel,
                          IndicatorTabComponent scriptsTabIndicator,
                          ResponsePanel responsePanel,
                          JSplitPane splitPane) {
        this.requestLinePanel = requestLinePanel;
        this.methodBox = methodBox;
        this.urlField = urlField;
        this.reqTabs = reqTabs;
        this.descriptionEditor = descriptionEditor;
        this.paramsPanel = paramsPanel;
        this.paramsTabIndicator = paramsTabIndicator;
        this.authTabPanel = authTabPanel;
        this.authTabIndicator = authTabIndicator;
        this.headersPanel = headersPanel;
        this.headersTabIndicator = headersTabIndicator;
        this.requestBodyPanel = requestBodyPanel;
        this.bodyTabIndicator = bodyTabIndicator;
        this.scriptPanel = scriptPanel;
        this.scriptsTabIndicator = scriptsTabIndicator;
        this.responsePanel = responsePanel;
        this.splitPane = splitPane;
    }
}
