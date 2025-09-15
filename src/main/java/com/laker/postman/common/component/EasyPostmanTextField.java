package com.laker.postman.common.component;

import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasyPostmanTextField extends JTextField {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    // Postman 风格颜色
    private static final Color DEFINED_VAR_BG = new Color(255, 230, 170); // 浅橙色
    private static final Color DEFINED_VAR_BORDER = new Color(255, 180, 80); // 橙色
    private static final Color DEFINED_VAR_TEXT = new Color(180, 90, 0); // 深橙棕色

    private static final Color UNDEFINED_VAR_BG = new Color(255, 200, 200); // 浅红色
    private static final Color UNDEFINED_VAR_BORDER = new Color(255, 100, 100); // 红色
    private static final Color UNDEFINED_VAR_TEXT = new Color(180, 0, 0); // 深红色

    public EasyPostmanTextField(String text, int columns) {
        super(text, columns);
    }


    private boolean isVariableDefined(String varName) {
        // 检查临时变量
        if (EnvironmentService.getTemporaryVariable(varName) != null) {
            return true;
        }
        // 检查活动环境变量
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        return activeEnv != null && activeEnv.getVariable(varName) != null;
    }


    @Override
    protected void paintComponent(Graphics g) {
        // 先绘制默认的文本（非变量部分）
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
                    g.setColor(Color.BLACK);
                    g.setFont(getFont());
                    g.drawString(before, x, y);
                    x += w;
                }

                // Determine variable state (defined or undefined)
                boolean isDefined = isVariableDefined(seg.name);
                Color bgColor = isDefined ? DEFINED_VAR_BG : UNDEFINED_VAR_BG;
                Color borderColor = isDefined ? DEFINED_VAR_BORDER : UNDEFINED_VAR_BORDER;
                Color textColor = isDefined ? DEFINED_VAR_TEXT : UNDEFINED_VAR_TEXT;
                Font varFont = getFont().deriveFont(Font.BOLD);

                // Draw variable segment
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);

                // Background
                g.setColor(bgColor);
                g.fillRoundRect(x, baseY, varWidth, h, 8, 8);

                // Border
                g.setColor(borderColor);
                g.drawRoundRect(x, baseY, varWidth, h, 8, 8);

                // Text
                g.setColor(textColor);
                g.setFont(varFont);
                g.drawString(varText, x, y);

                x += varWidth;
                last = seg.end;
            }

            // Draw remaining text after the last variable
            if (last < value.length()) {
                String after = value.substring(last);
                g.setColor(Color.BLACK);
                g.setFont(getFont());
                g.drawString(after, x, y);
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