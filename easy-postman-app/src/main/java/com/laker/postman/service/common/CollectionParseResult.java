package com.laker.postman.service.common;

import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一的 Collection 解析结果
 * 支持树形结构的分组和请求
 * 可用于 Postman、ApiPost、IntelliJ HTTP 等各种格式的解析结果
 */
@Getter
public class CollectionParseResult {
    /**
     * 分组信息
     */
    private final RequestGroup group;

    /**
     * 子节点列表（可以是子分组或请求）
     */
    private final List<CollectionNode> children;

    /**
     * 环境变量列表（可选，用于导入时创建环境）
     */
    private final List<Environment> environments;

    public CollectionParseResult(RequestGroup group) {
        this.group = group;
        this.children = new ArrayList<>();
        this.environments = new ArrayList<>();
    }

    public void addChild(CollectionNode child) {
        this.children.add(child);
    }

    /**
     * 添加环境变量
     */
    public void addEnvironment(Environment environment) {
        if (environment != null) {
            this.environments.add(environment);
        }
    }

    /**
     * 添加多个环境变量
     */
    public void addEnvironments(List<Environment> envList) {
        if (envList != null && !envList.isEmpty()) {
            this.environments.addAll(envList);
        }
    }

    /**
     * 便捷工厂方法：创建扁平结构的解析结果（用于 IntelliJ HTTP 等简单格式）
     *
     * @param group 分组信息
     * @param requests 请求列表
     * @return 解析结果
     */
    public static CollectionParseResult createFlat(RequestGroup group, List<HttpRequestItem> requests) {
        CollectionParseResult result = new CollectionParseResult(group);
        for (HttpRequestItem request : requests) {
            result.addChild(new CollectionNode(NodeType.REQUEST, request));
        }
        return result;
    }
}
