package com.laker.postman.common.component.button;

import javax.swing.JButton;

/**
 * Compact rounded primary action button for dense toolbars.
 */
public class CompactPrimaryButton extends JButton {

    public CompactPrimaryButton(String text, String iconPath) {
        super(text);
        ModernButtonFactory.configureBaseButton(this, ModernButtonFactory.PRIMARY_STYLE_CLASS);
        ModernButtonFactory.configureButtonIcon(this, true, iconPath, ModernButtonFactory.COMPACT_ICON_SIZE);
        ModernButtonFactory.configureCompactButton(this);
    }
}
