package com.laker.postman.service.collections;

import com.laker.postman.collection.model.CollectionDocument;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.function.Supplier;

@UtilityClass
public class CollectionDocumentRegistry {

    private static volatile Supplier<CollectionDocument> documentSupplier;

    public static void registerDocumentSupplier(Supplier<CollectionDocument> supplier) {
        documentSupplier = supplier;
    }

    public static Optional<CollectionDocument> getDocument() {
        Supplier<CollectionDocument> supplier = documentSupplier;
        if (supplier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(supplier.get());
    }
}
