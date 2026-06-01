package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.panel.collections.editor.request.sub.*;
import lombok.RequiredArgsConstructor;

import javax.swing.*;

@RequiredArgsConstructor
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
    final IndicatorTabComponent settingsTabIndicator;
    final RequestSettingsPanel requestSettingsPanel;
    final ScriptPanel scriptPanel;
    final IndicatorTabComponent scriptsTabIndicator;
    final ResponsePanel responsePanel;
    final JSplitPane splitPane;
    final JComponent editorContent;
}
