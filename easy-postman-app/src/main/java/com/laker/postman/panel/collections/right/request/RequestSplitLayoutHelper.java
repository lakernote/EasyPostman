package com.laker.postman.panel.collections.right.request;

import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;

import javax.swing.*;
import java.util.function.Supplier;

final class RequestSplitLayoutHelper {
    private final JSplitPane splitPane;
    private final ResponsePanel responsePanel;
    private final Supplier<Boolean> isEffectiveSseProtocolSupplier;
    private final Supplier<Boolean> isEffectiveWebSocketProtocolSupplier;
    private boolean initialDividerLocationSet;

    RequestSplitLayoutHelper(JSplitPane splitPane,
                             ResponsePanel responsePanel,
                             Supplier<Boolean> isEffectiveSseProtocolSupplier,
                             Supplier<Boolean> isEffectiveWebSocketProtocolSupplier) {
        this.splitPane = splitPane;
        this.responsePanel = responsePanel;
        this.isEffectiveSseProtocolSupplier = isEffectiveSseProtocolSupplier;
        this.isEffectiveWebSocketProtocolSupplier = isEffectiveWebSocketProtocolSupplier;
    }

    void updateLayoutOrientation(boolean isVertical) {
        int targetOrientation = isVertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        if (splitPane.getOrientation() == targetOrientation) {
            return;
        }

        double ratio = isVertical ? getDefaultResizeWeight() : 0.5;
        splitPane.setOrientation(targetOrientation);
        splitPane.setResizeWeight(ratio);
        initialDividerLocationSet = false;

        splitPane.revalidate();
        int totalSize = isVertical ? splitPane.getHeight() : splitPane.getWidth();
        if (totalSize > 0) {
            splitPane.setDividerLocation(ratio);
        }

        if (responsePanel != null) {
            responsePanel.updateLayoutOrientation(isVertical);
        }
    }

    void handleInitialLayout() {
        if (initialDividerLocationSet || splitPane == null) {
            return;
        }

        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT && splitPane.getWidth() > 0) {
            initialDividerLocationSet = true;
            splitPane.setDividerLocation(0.5);
            return;
        }
        if (splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT && splitPane.getHeight() > 0) {
            initialDividerLocationSet = true;
            splitPane.setDividerLocation(getDefaultResizeWeight());
        }
    }

    double getDefaultResizeWeight() {
        if (Boolean.TRUE.equals(isEffectiveWebSocketProtocolSupplier.get())
                || Boolean.TRUE.equals(isEffectiveSseProtocolSupplier.get())) {
            return 0.3;
        }
        return 0.5;
    }
}
