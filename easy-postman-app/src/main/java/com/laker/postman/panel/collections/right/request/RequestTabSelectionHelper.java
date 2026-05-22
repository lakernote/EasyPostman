package com.laker.postman.panel.collections.right.request;

import javax.swing.*;
import java.awt.*;

final class RequestTabSelectionHelper {
    private RequestTabSelectionHelper() {
    }

    static void selectFirstVisible(JTabbedPane tabs, Component... preferredComponents) {
        if (tabs == null) {
            return;
        }

        if (preferredComponents != null) {
            for (Component preferredComponent : preferredComponents) {
                if (preferredComponent == null) {
                    continue;
                }
                int index = tabs.indexOfComponent(preferredComponent);
                if (index >= 0) {
                    tabs.setSelectedIndex(index);
                    return;
                }
            }
        }

        if (tabs.getTabCount() > 0 && tabs.getSelectedIndex() < 0) {
            tabs.setSelectedIndex(0);
        }
    }

    static void removeIfPresent(JTabbedPane tabs, Component component) {
        if (tabs == null || component == null) {
            return;
        }

        int index = tabs.indexOfComponent(component);
        if (index >= 0) {
            tabs.removeTabAt(index);
        }
    }
}
