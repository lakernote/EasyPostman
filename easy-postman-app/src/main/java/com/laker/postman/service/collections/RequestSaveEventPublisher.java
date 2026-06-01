package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@UtilityClass
public class RequestSaveEventPublisher {
    private final List<RequestSaveListener> LISTENERS = new CopyOnWriteArrayList<>();

    public AutoCloseable register(RequestSaveListener listener) {
        if (listener == null) {
            return () -> {
            };
        }
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    public void publishRequestSaved(HttpRequestItem item) {
        if (item == null) {
            return;
        }
        for (RequestSaveListener listener : LISTENERS) {
            listener.onRequestSaved(item);
        }
    }

    static void clearListenersForTest() {
        LISTENERS.clear();
    }
}
