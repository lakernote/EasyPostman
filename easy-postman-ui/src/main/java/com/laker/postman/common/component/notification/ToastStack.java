package com.laker.postman.common.component.notification;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
class ToastStack {
    private final int maxActiveToasts;
    private final List<ToastWindow> activeToasts = new ArrayList<>();

    synchronized void add(ToastWindow toast) {
        while (activeToasts.size() >= maxActiveToasts) {
            activeToasts.get(0).closeQuietly();
        }
        activeToasts.add(toast);
        updatePositions();
    }

    synchronized void remove(ToastWindow toast) {
        activeToasts.remove(toast);
        updatePositions();
    }

    synchronized void refreshPositions() {
        updatePositions();
    }

    private void updatePositions() {
        int offset = 0;
        for (ToastWindow toast : activeToasts) {
            toast.updateStackOffset(offset);
            offset += toast.getHeight() + ToastStyle.STACK_GAP;
        }
    }
}
