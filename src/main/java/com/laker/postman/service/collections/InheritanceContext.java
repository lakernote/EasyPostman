package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 继承上下文
 * <p>
 * 值对象（Value Object），封装继承计算所需的所有信息
 * <p>
 * 特性：
 * - 不可变（Immutable）
 * - 线程安全
 * - 便于测试
 *
 * @author laker
 * @since 4.3.22
 */
@Getter
public class InheritanceContext {

    /**
     * 原始请求项
     */
    private final HttpRequestItem originalItem;

    /**
     * 分组链（从外到内）
     */
    private final List<RequestGroup> groupChain;

    /**
     * 是否需要继承认证
     */
    private final boolean needsAuthInheritance;

    /**
     * 创建继承上下文
     *
     * @param originalItem 原始请求项
     * @param groupChain 分组链（从外到内）
     */
    public InheritanceContext(HttpRequestItem originalItem, List<RequestGroup> groupChain) {
        this.originalItem = originalItem;
        // 防御性复制，确保不可变
        this.groupChain = groupChain != null ?
            Collections.unmodifiableList(groupChain) :
            Collections.emptyList();
        this.needsAuthInheritance = checkNeedsAuthInheritance(originalItem);
    }

    /**
     * 检查是否需要继承认证
     */
    private boolean checkNeedsAuthInheritance(HttpRequestItem item) {
        if (item == null || item.getAuthType() == null) {
            return false;
        }
        return "inherit".equalsIgnoreCase(item.getAuthType());
    }

    /**
     * 是否有分组链
     */
    public boolean hasGroups() {
        return !groupChain.isEmpty();
    }

    /**
     * 获取分组数量
     */
    public int getGroupCount() {
        return groupChain.size();
    }

    /**
     * 获取最内层分组（就近原则使用）
     */
    public RequestGroup getInnermostGroup() {
        if (groupChain.isEmpty()) {
            return null;
        }
        return groupChain.get(groupChain.size() - 1);
    }

    /**
     * 获取最外层分组
     */
    public RequestGroup getOutermostGroup() {
        if (groupChain.isEmpty()) {
            return null;
        }
        return groupChain.get(0);
    }

    @Override
    public String toString() {
        return String.format("InheritanceContext{request=%s, groups=%d, needsAuth=%b}",
            originalItem != null ? originalItem.getName() : "null",
            groupChain.size(),
            needsAuthInheritance);
    }
}
