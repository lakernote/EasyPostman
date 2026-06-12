package com.laker.postman.panel.collections.editor.request;

import lombok.experimental.UtilityClass;

/**
 * 请求编辑器只读态控制。
 * <p>
 * 性能快照和保存响应不能回写集合数据，所有输入组件统一在这里切换只读，避免面板初始化分支散落。
 */
@UtilityClass
class RequestEditorReadOnlyMode {

    static void apply(RequestViewComponents components) {
        components.requestLinePanel.setReadOnlySnapshotMode();
        components.descriptionEditor.setEditable(false);
        components.pathVariablesPanel.setEditable(false);
        components.paramsPanel.setEditable(false);
        components.authTabPanel.setEditable(false);
        components.headersPanel.setEditable(false);
        components.requestBodyPanel.setEditable(false);
        components.requestSettingsPanel.setEditable(false);
        components.scriptPanel.setEditable(false);
    }
}
