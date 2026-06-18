package com.laker.postman.common.component;

import java.awt.Dimension;

/**
 * Shared metrics for compact tool-window stripes.
 */
public final class ToolWindowStripeMetrics {
    public static final int ACTION_SIZE = 32;
    public static final int STRIPE_THICKNESS = ACTION_SIZE;

    private ToolWindowStripeMetrics() {
    }

    public static Dimension actionSize() {
        return new Dimension(ACTION_SIZE, ACTION_SIZE);
    }
}
