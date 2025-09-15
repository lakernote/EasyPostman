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
    private static final Color DEFINED_VAR_BORDER = new Color(255, 180, 80); // 橙色

    private static final Color UNDEFINED_VAR_BORDER = new Color(255, 100, 100); // 红色

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
        // 先绘制文本、光标、选区
        super.paintComponent(g);

        String value = getText();
        List<VariableSegment> segments = getVariableSegments(value);
        if (segments.isEmpty()) return;

        try {
            Rectangle startRect = modelToView2D(0).getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int baseY = startRect.y;
            int h = fm.getHeight();
            int last = 0;

            for (VariableSegment seg : segments) {
                // 计算变量前的文本宽度
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                // 判断变量状态
                boolean isDefined = isVariableDefined(seg.name);
                Color bgColor = isDefined ? new Color(255, 230, 170, 120) : new Color(255, 200, 200, 120); // 半透明
                Color borderColor = isDefined ? DEFINED_VAR_BORDER : UNDEFINED_VAR_BORDER;
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                // 只绘制变量的半透明背景和边框，不绘制文本
                g.setColor(bgColor);
                g.fillRoundRect(x, baseY, varWidth, h, 8, 8);
                g.setColor(borderColor);
                g.drawRoundRect(x, baseY, varWidth, h, 8, 8);
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception ignored) {}
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