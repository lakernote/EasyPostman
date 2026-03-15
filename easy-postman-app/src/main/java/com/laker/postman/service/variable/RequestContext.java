package com.laker.postman.service.variable;

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

    /**
     * 设置当前请求节点
     *
     * @param requestNode 请求节点
     */
    public static void setCurrentRequestNode(DefaultMutableTreeNode requestNode) {
        CURRENT_REQUEST_NODE.set(requestNode);
    }

    /**
     * 获取当前请求节点
     *
     * @return 当前请求节点（可能为 null）
     */
    public static DefaultMutableTreeNode getCurrentRequestNode() {
        return CURRENT_REQUEST_NODE.get();
    }

    /**
     * 清除当前请求节点
     */
    public static void clearCurrentRequestNode() {
        CURRENT_REQUEST_NODE.remove();
    }
}
