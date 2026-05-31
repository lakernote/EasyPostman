package com.laker.postman.panel.collections.editor.request;

/**
 * 请求编辑子面板的类型
 */
public enum RequestEditSubPanelType {
    /**
     * 普通请求编辑面板
     */
    NORMAL,
    /**
     * 保存的响应预览面板
     */
    SAVED_RESPONSE,
    /**
     * 性能测试中的请求快照，只用于查看，不允许直接编辑或发送。
     */
    PERFORMANCE_SNAPSHOT
}
