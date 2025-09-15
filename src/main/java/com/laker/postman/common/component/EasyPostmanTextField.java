package com.laker.postman.common.component;

import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EasyPostmanTextField extends JTextField {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    // Postman 风格颜色
    private static final Color DEFINED_VAR_BG = new Color(180, 210, 255, 120); // 半透明淡蓝
    private static final Color DEFINED_VAR_BORDER = new Color(80, 150, 255); // 蓝色边框
    private static final Color UNDEFINED_VAR_BG = new Color(255, 200, 200, 120); // 半透明红
    private static final Color UNDEFINED_VAR_BORDER = new Color(255, 100, 100); // 红色

    public EasyPostmanTextField(int columns) {
        super(columns);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");

    }

    public EasyPostmanTextField(String text, int columns) {
        super(text, columns);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");
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
                Color bgColor = isDefined ? DEFINED_VAR_BG : UNDEFINED_VAR_BG;
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
        } catch (Exception e) {
            log.error("paintComponent", e);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        String value = getText();
        List<VariableSegment> segments = getVariableSegments(value);
        if (segments.isEmpty()) return super.getToolTipText(event);
        try {
            int mouseX = event.getX();
            Rectangle startRect = modelToView2D(0).getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int last = 0;
            for (VariableSegment seg : segments) {
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                if (mouseX >= x && mouseX <= x + varWidth) {
                    // 鼠标悬浮在变量上
                    String varName = seg.name;
                    String varValue = getVariableValue(varName);
                    if (varValue != null) {
                        return varName + " = " + varValue;
                    } else {
                        return varName + " 未定义";
                    }
                }
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("getToolTipText", e);
        }
        return super.getToolTipText(event);
    }

    private String getVariableValue(String varName) {
        Object temp = EnvironmentService.getTemporaryVariable(varName);
        if (temp != null) return temp.toString();
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariable(varName) != null) {
            Object v = activeEnv.getVariable(varName);
            return v == null ? null : v.toString();
        }
        return null;
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
        int start;
        int end;
        String name;

        VariableSegment(int start, int end, String name) {
            this.start = start;
            this.end = end;
            this.name = name;
        }
    }
}