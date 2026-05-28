package com.laker.postman.service.variable;

import com.laker.postman.service.collections.GroupInheritanceHelper;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 请求上下文管理器
 * <p>
 * 管理当前用户正在编辑/使用的请求节点，供变量解析等功能使用
 */
@UtilityClass
public class RequestContext {

    /**
     * 当前请求节点（ThreadLocal 保证线程安全）
     */
    private static final ThreadLocal<DefaultMutableTreeNode> CURRENT_REQUEST_NODE = new ThreadLocal<>();
    private static final ThreadLocal<RequestExecutionScope> CURRENT_EXECUTION_SCOPE = new ThreadLocal<>();

    /**
     * 设置当前请求节点
     *
     * @param requestNode 请求节点
     */
    public static void setCurrentRequestNode(DefaultMutableTreeNode requestNode) {
        if (requestNode == null) {
            CURRENT_REQUEST_NODE.remove();
            CURRENT_EXECUTION_SCOPE.set(RequestExecutionScope.empty());
            return;
        }
        CURRENT_REQUEST_NODE.set(requestNode);
        CURRENT_EXECUTION_SCOPE.set(RequestExecutionScope.fromVariables(
                GroupInheritanceHelper.getMergedGroupVariables(requestNode)
        ));
    }

    /**
     * 获取当前请求节点
     *
     * @return 当前请求节点（可能为 null）
     */
    public static DefaultMutableTreeNode getCurrentRequestNode() {
        return CURRENT_REQUEST_NODE.get();
    }

    public static RequestExecutionScope getCurrentExecutionScope() {
        return CURRENT_EXECUTION_SCOPE.get();
    }

    public static RequestExecutionScope captureCurrentExecutionScope() {
        RequestExecutionScope scope = CURRENT_EXECUTION_SCOPE.get();
        if (scope != null) {
            return scope;
        }

        DefaultMutableTreeNode requestNode = CURRENT_REQUEST_NODE.get();
        if (requestNode == null) {
            return null;
        }

        RequestExecutionScope capturedScope = RequestExecutionScope.fromVariables(
                GroupInheritanceHelper.getMergedGroupVariables(requestNode)
        );
        CURRENT_EXECUTION_SCOPE.set(capturedScope);
        return capturedScope;
    }

    public static void setCurrentExecutionScope(RequestExecutionScope executionScope) {
        CURRENT_REQUEST_NODE.remove();
        CURRENT_EXECUTION_SCOPE.set(executionScope == null ? RequestExecutionScope.empty() : executionScope);
    }

    /**
     * 清除当前请求节点
     */
    public static void clearCurrentRequestNode() {
        CURRENT_REQUEST_NODE.remove();
        CURRENT_EXECUTION_SCOPE.remove();
    }
}
