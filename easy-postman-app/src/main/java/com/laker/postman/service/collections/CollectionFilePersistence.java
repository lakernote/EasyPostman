package com.laker.postman.service.collections;

import com.laker.postman.collection.model.CollectionDocument;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class CollectionFilePersistence {
    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> LOADING_STATUS = new ConcurrentHashMap<>();

    private String filePath;

    public CollectionFilePersistence(String filePath) {
        this.filePath = filePath;
    }

    public CollectionDocument loadOrCreate(Supplier<CollectionDocument> defaultDocumentSupplier) throws IOException {
        synchronized (fileLock()) {
            setFileLoading(true);
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    CollectionDocument defaultDocument = defaultDocument(defaultDocumentSupplier);
                    saveIgnoringLoadGuard(defaultDocument);
                    log.info("Collection file not found, created default document: {}", filePath);
                    return defaultDocument;
                }
                return CollectionDocumentJsonCodec.read(file);
            } finally {
                setFileLoading(false);
            }
        }
    }

    public boolean save(CollectionDocument document) {
        if (isFileLoading()) {
            log.warn("Skipping save operation for file '{}' because it is being loaded", filePath);
            return false;
        }

        synchronized (fileLock()) {
            if (isFileLoading()) {
                log.warn("Skipping save operation for file '{}' because it is being loaded (double-check)", filePath);
                return false;
            }
            try {
                saveIgnoringLoadGuard(document);
                log.debug("Saved collection document to: {}", filePath);
                return true;
            } catch (Exception ex) {
                log.error("Error saving collection document to file: {}", filePath, ex);
                return false;
            }
        }
    }

    public void export(CollectionDocument document, File fileToSave) throws IOException {
        CollectionDocumentJsonCodec.write(fileToSave, document);
    }

    public CollectionDocument switchFilePathAndLoad(String path,
                                                    Supplier<CollectionDocument> defaultDocumentSupplier) throws IOException {
        if (path == null || path.isBlank()) {
            return loadOrCreate(defaultDocumentSupplier);
        }

        String oldPath = this.filePath;
        synchronized (FILE_LOCKS.computeIfAbsent(oldPath, ignored -> new Object())) {
            this.filePath = path;
            return loadOrCreate(defaultDocumentSupplier);
        }
    }

    private void saveIgnoringLoadGuard(CollectionDocument document) throws IOException {
        CollectionDocumentJsonCodec.write(new File(filePath), document);
    }

    private CollectionDocument defaultDocument(Supplier<CollectionDocument> defaultDocumentSupplier) {
        if (defaultDocumentSupplier == null) {
            return CollectionDocument.empty();
        }
        CollectionDocument document = defaultDocumentSupplier.get();
        return document == null ? CollectionDocument.empty() : document;
    }

    private Object fileLock() {
        return FILE_LOCKS.computeIfAbsent(filePath, ignored -> new Object());
    }

    private boolean isFileLoading() {
        return LOADING_STATUS.getOrDefault(filePath, false);
    }

    private void setFileLoading(boolean loading) {
        if (loading) {
            LOADING_STATUS.put(filePath, true);
        } else {
            LOADING_STATUS.remove(filePath);
        }
    }
}
