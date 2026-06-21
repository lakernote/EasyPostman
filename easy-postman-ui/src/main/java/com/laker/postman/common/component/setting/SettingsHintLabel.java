package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Wrapping hint text for settings forms.
 */
public class SettingsHintLabel extends JTextArea {

    private final int maxLineWidth;
    private String plainText;

    public SettingsHintLabel(String text, int maxLineWidth) {
        if (maxLineWidth <= 0) {
            throw new IllegalArgumentException("maxLineWidth must be positive");
        }
        this.maxLineWidth = maxLineWidth;
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        setForeground(ModernColors.getTextSecondary());
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(maxLineWidth, Short.MAX_VALUE));
        setPlainText(text);
    }

    public final void setPlainText(String text) {
        this.plainText = text == null ? "" : text;
        super.setText(this.plainText);
        setToolTipText(this.plainText.isBlank() ? null : this.plainText);
        setCaretPosition(0);
    }

    public String getPlainText() {
        return plainText;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension originalSize = getSize();
        if (originalSize.width <= 0 || originalSize.width > maxLineWidth) {
            setSize(maxLineWidth, Short.MAX_VALUE);
        }
        Dimension preferredSize = super.getPreferredSize();
        if (originalSize.width <= 0 || originalSize.width > maxLineWidth) {
            setSize(originalSize);
        }
        return new Dimension(maxLineWidth, preferredSize.height);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferredSize = getPreferredSize();
        return new Dimension(maxLineWidth, preferredSize.height);
    }
}
