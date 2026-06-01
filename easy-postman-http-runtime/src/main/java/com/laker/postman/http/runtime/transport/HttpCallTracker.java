package com.laker.postman.http.runtime.transport;

import okhttp3.Call;

public interface HttpCallTracker {
    HttpCallTracker NOOP = new HttpCallTracker() {
    };

    default void onCallStarted(Call call) {
    }

    default void onCallFinished(Call call) {
    }
}
