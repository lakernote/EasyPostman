package com.laker.postman.collection.model;

import com.laker.postman.request.model.HttpRequestItem;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CollectionNode {
    private final CollectionNodeType type;
    private final Object data; // RequestGroup 或 HttpRequestItem
    private final List<CollectionNode> children; // 仅对分组类型有效

    public CollectionNode(CollectionNodeType type, Object data) {
        this.type = type;
        this.data = data;
        this.children = new ArrayList<>();
    }

    public void addChild(CollectionNode child) {
        this.children.add(child);
    }

    public boolean isGroup() {
        return type == CollectionNodeType.GROUP;
    }

    public boolean isRequest() {
        return type == CollectionNodeType.REQUEST;
    }

    public RequestGroup asGroup() {
        return (RequestGroup) data;
    }

    public HttpRequestItem asRequest() {
        return (HttpRequestItem) data;
    }
}
