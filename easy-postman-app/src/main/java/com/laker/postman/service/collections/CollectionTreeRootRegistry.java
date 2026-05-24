package com.laker.postman.service.collections;

import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;
import java.util.function.Supplier;

@UtilityClass
public class CollectionTreeRootRegistry {

    private static volatile Supplier<DefaultMutableTreeNode> rootSupplier;

    public static void registerRootSupplier(Supplier<DefaultMutableTreeNode> supplier) {
        rootSupplier = supplier;
    }

    public static Optional<DefaultMutableTreeNode> getRootNode() {
        Supplier<DefaultMutableTreeNode> supplier = rootSupplier;
        if (supplier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(supplier.get());
    }

    public static void clear() {
        rootSupplier = null;
    }
}
