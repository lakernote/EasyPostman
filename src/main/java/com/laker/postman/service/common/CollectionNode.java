package com.laker.postman.service.common;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CollectionNode {
    private final NodeType type;
    private final Object data; // RequestGroup 或 HttpRequestItem
    private final List<CollectionNode> children; // 仅对分组类型有效

    public CollectionNode(NodeType type, Object data) {
        this.type = type;
        this.data = data;
        this.children = new ArrayList<>();
    }

    public void addChild(CollectionNode child) {
        this.children.add(child);
    }

    public boolean isGroup() {
        return type == NodeType.GROUP;
    }

    public boolean isRequest() {
        return type == NodeType.REQUEST;
    }

    public RequestGroup asGroup() {
        return (RequestGroup) data;
    }

    public HttpRequestItem asRequest() {
        return (HttpRequestItem) data;
    }
}
