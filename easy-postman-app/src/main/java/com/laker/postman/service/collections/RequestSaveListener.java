package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;

@FunctionalInterface
public interface RequestSaveListener {
    void onRequestSaved(HttpRequestItem item);
}
