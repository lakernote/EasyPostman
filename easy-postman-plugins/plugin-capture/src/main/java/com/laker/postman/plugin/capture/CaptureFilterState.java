package com.laker.postman.plugin.capture;

final class CaptureFilterState {
    private volatile CaptureRequestFilter current = CaptureRequestFilter.parse("");

    CaptureRequestFilter current() {
        return current;
    }

    CaptureRequestFilter update(String rawValue) {
        CaptureRequestFilter parsed = CaptureRequestFilter.parse(rawValue);
        current = parsed;
        return parsed;
    }

    String summary() {
        return current.summary();
    }
}
