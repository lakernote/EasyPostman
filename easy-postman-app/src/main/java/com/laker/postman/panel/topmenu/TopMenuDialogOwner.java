package com.laker.postman.panel.topmenu;

import lombok.experimental.UtilityClass;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;

/**
 * Resolves a stable top-level owner for dialogs opened from menu components.
 */
@UtilityClass
class TopMenuDialogOwner {

    Component resolve(Component parent) {
        Window owner = parent == null
                ? KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()
                : SwingUtilities.getWindowAncestor(parent);
        return owner != null ? owner : parent;
    }
}
