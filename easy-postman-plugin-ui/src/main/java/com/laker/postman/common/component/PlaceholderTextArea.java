package com.laker.postman.common.component;

import javax.swing.*;
import java.awt.*;

/**
 * 支持 Placeholder 提示文字的 JTextArea。
 * PlaceholderTextArea area = new PlaceholderTextArea(3, 0);
 * area.setPlaceholder("key1:value1\nkey2:value2");
 */
public class PlaceholderTextArea extends JTextArea {

    private String placeholder;

    public PlaceholderTextArea() {
        super();
    }

    public PlaceholderTextArea(int rows, int columns) {
        super(rows, columns);
    }

    public PlaceholderTextArea(String text) {
        super(text);
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder == null || placeholder.isEmpty() || !getText().isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 使用与 FlatLaf TextField.placeholderForeground 一致的颜色，回退到灰色
            Color placeholderColor = UIManager.getColor("TextField.placeholderForeground");
            if (placeholderColor == null) {
                placeholderColor = UIManager.getColor("Label.disabledForeground");
            }
            if (placeholderColor == null) {
                placeholderColor = new Color(128, 128, 128);
            }
            g2.setColor(placeholderColor);
            g2.setFont(getFont());

            Insets insets = getInsets();
            int x = insets.left + 2;
            int y = insets.top;

            FontMetrics fm = g2.getFontMetrics();
            int lineHeight = fm.getHeight();
            int ascent = fm.getAscent();

            String[] lines = placeholder.split("\n", -1);
            for (String line : lines) {
                y += ascent;
                g2.drawString(line, x, y);
                y += lineHeight - ascent;
            }
        } finally {
            g2.dispose();
        }
    }
}

