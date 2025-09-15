package com.laker.postman.common.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced JTextField for URL input, supporting special rendering and interaction for {{variable}} segments.
 */
public class EasyPostmanUrlField extends JTextField {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    public EasyPostmanUrlField(String text, int columns) {
        super(text, columns);
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int pos = viewToModel2D(e.getPoint());
                String value = getText();
                for (VariableSegment seg : getVariableSegments(value)) {
                    if (pos >= seg.start && pos <= seg.end) {
                        // Show a popup or dialog for editing/selecting variable
                        JOptionPane.showMessageDialog(EasyPostmanUrlField.this, "Edit variable: " + seg.name);
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        String value = getText();
        List<VariableSegment> segments = getVariableSegments(value);
        if (segments.isEmpty()) return;
        try {
            Rectangle startRect = modelToView2D(0).getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int y = startRect.y + fm.getAscent();
            int baseY = startRect.y;
            int h = fm.getHeight();
            int last = 0;
            for (VariableSegment seg : segments) {
                // Draw normal text before variable
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                // Draw background for variable
                g.setColor(new Color(255, 230, 170));
                g.fillRoundRect(x, baseY, varWidth, h, 8, 8);
                // Draw border
                g.setColor(new Color(255, 180, 80));
                g.drawRoundRect(x, baseY, varWidth, h, 8, 8);
                // Draw variable text
                g.setColor(new Color(180, 90, 0));
                g.setFont(getFont().deriveFont(Font.BOLD));
                g.drawString(varText, x, y);
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception ignored) {
        }
    }

    private List<VariableSegment> getVariableSegments(String value) {
        List<VariableSegment> segments = new ArrayList<>();
        Matcher m = VARIABLE_PATTERN.matcher(value);
        while (m.find()) {
            segments.add(new VariableSegment(m.start(), m.end(), m.group(1)));
        }
        return segments;
    }

    private static class VariableSegment {
        int start, end;
        String name;

        VariableSegment(int start, int end, String name) {
            this.start = start;
            this.end = end;
            this.name = name;
        }
    }
}

