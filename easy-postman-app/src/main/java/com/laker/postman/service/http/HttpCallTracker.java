package com.laker.postman.service.http;

import okhttp3.Call;

public interface HttpCallTracker {
    HttpCallTracker NOOP = new HttpCallTracker() {
    };

    default void onCallStarted(Call call) {
    }

    default void onCallFinished(Call call) {
    }
}
