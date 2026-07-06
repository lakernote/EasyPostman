package com.laker.postman.panel.collections.editor;

import com.laker.postman.service.collections.CollectionRequestExecutionScopeResolver;

/**
 * 请求编辑器变量作用域同步器。
 * <p>
 * 打开集合里的请求时，需要把所在分组的继承变量写入当前执行上下文。该逻辑依赖集合树，不属于面板布局。
 */
final class RequestEditorExecutionScopeSynchronizer {

    void syncScopeForRequest(String requestId) {
        CollectionRequestExecutionScopeResolver.syncCurrentScopeOrEmpty(requestId);
    }
}
