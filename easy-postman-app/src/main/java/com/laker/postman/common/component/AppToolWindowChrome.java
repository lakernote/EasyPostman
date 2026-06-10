package com.laker.postman.common.component;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import java.awt.Component;
import java.awt.Insets;

/**
 * Host-app tool-window chrome preferences.
 *
 * <p>The shared {@link ToolWindowChrome} default stays stable for plugins. Host panels use this
 * wrapper when they need the app-specific wider drag-gap split style.</p>
 */
public final class AppToolWindowChrome {
    public static final int DEFAULT_SIDE_WIDTH = ToolWindowChrome.DEFAULT_SIDE_WIDTH;
    public static final int DIVIDER_SIZE = ToolWindowChrome.DRAG_GAP_DIVIDER_SIZE;
    private static final ToolWindowChrome.SplitDividerStyle HOST_SPLIT_STYLE =
            ToolWindowChrome.SplitDividerStyle.DRAG_GAP;

    private AppToolWindowChrome() {
    }

    public static JComponent wrapLeftToolWindow(Component content) {
        return ToolWindowChrome.wrapLeftToolWindow(content);
    }

    public static JComponent wrapLeftInsetToolWindow(Component content) {
        return ToolWindowChrome.wrapLeftInsetToolWindow(content);
    }

    public static JComponent wrapRightToolWindow(Component content) {
        return ToolWindowChrome.wrapRightToolWindow(content);
    }

    public static JComponent wrapToolWindow(Component content) {
        return ToolWindowChrome.wrapToolWindow(content);
    }

    public static JComponent wrapToolWindow(Component content, Insets outerGap) {
        return ToolWindowChrome.wrapToolWindow(content, outerGap);
    }

    public static JComponent wrapInsetToolWindow(Component content) {
        return ToolWindowChrome.wrapInsetToolWindow(content);
    }

    public static JComponent wrapDialogToolWindow(Component content) {
        return ToolWindowChrome.wrapDialogToolWindow(content);
    }

    public static JComponent wrapDialogInsetToolWindow(Component content) {
        return ToolWindowChrome.wrapDialogInsetToolWindow(content);
    }

    public static JSplitPane createHorizontalInnerSplitPane(Component left, Component right, int dividerLocation) {
        return ToolWindowChrome.createHorizontalInnerSplitPane(left, right, dividerLocation, HOST_SPLIT_STYLE);
    }

    public static JSplitPane createVerticalInnerSplitPane(Component top, Component bottom, int dividerLocation) {
        return ToolWindowChrome.createVerticalInnerSplitPane(top, bottom, dividerLocation, HOST_SPLIT_STYLE);
    }

    public static JSplitPane createHorizontalCardSplitPane(Component leftContent, Component rightContent,
                                                          int dividerLocation) {
        return ToolWindowChrome.createHorizontalCardSplitPane(
                leftContent,
                rightContent,
                dividerLocation,
                HOST_SPLIT_STYLE
        );
    }

    public static JSplitPane createVerticalCardSplitPane(Component topContent, Component bottomContent,
                                                        int dividerLocation) {
        return ToolWindowChrome.createVerticalCardSplitPane(
                topContent,
                bottomContent,
                dividerLocation,
                HOST_SPLIT_STYLE
        );
    }

    public static JSplitPane createVerticalStackedCardSplitPane(Component topToolWindow, Component bottomContent,
                                                               int dividerLocation) {
        return ToolWindowChrome.createVerticalStackedCardSplitPane(
                topToolWindow,
                bottomContent,
                dividerLocation,
                HOST_SPLIT_STYLE
        );
    }
}
