package com.laker.postman.collection.model;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class CollectionDocument {
    List<CollectionNode> roots;

    public CollectionDocument(List<CollectionNode> roots) {
        this.roots = roots == null ? List.of() : List.copyOf(roots);
    }

    public static CollectionDocument empty() {
        return new CollectionDocument(List.of());
    }

    public List<CollectionNode> mutableRootsCopy() {
        return new ArrayList<>(roots);
    }
}
