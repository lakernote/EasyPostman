package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.VariableTooltipHtmlBuilder;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.variable.VariableType;
import lombok.experimental.UtilityClass;

/**
 * 请求体变量提示内容构建器。
 * <p>
 * 变量 badge、自动补全列表和悬浮提示都需要 HTML 转义和主题色，集中到这里避免编辑器面板继续膨胀。
 */
@UtilityClass
class RequestBodyVariableTooltipBuilder {

    static String valueTooltip(String value) {
        return "<html>" + formatLongText(value) + "</html>";
    }

    static String listItemTooltip(String varName, String varValue, VariableType varType) {
        StringBuilder tooltipBuilder = new StringBuilder("<html>");
        tooltipBuilder.append("<b>").append(escapeHtml(varName)).append("</b>");
        tooltipBuilder.append(" <span style='color:")
                .append(ModernColors.toHtmlColor(ModernColors.getTextHint()))
                .append("'>(").append(varType.getDisplayName()).append(")</span>");
        if (varValue != null && !varValue.isEmpty()) {
            tooltipBuilder.append("<br/>").append(escapeHtml(varValue));
        }
        tooltipBuilder.append("</html>");
        return tooltipBuilder.toString();
    }

    static String variableTooltip(String varName, String content, VariableType varType) {
        return VariableTooltipHtmlBuilder.variableTooltip(varName, content, varType);
    }

    static String pathVariableTooltip(String varName, String content) {
        return VariableTooltipHtmlBuilder.pathVariableTooltip(varName, content);
    }

    static String undefinedVariableTooltip(String varName) {
        return VariableTooltipHtmlBuilder.undefinedVariableTooltip(varName);
    }

    static String escapeHtml(String text) {
        return VariableTooltipHtmlBuilder.escapeHtml(text);
    }

    private static String formatLongText(String text) {
        if (text == null || text.length() <= 60) {
            return text;
        }

        StringBuilder formatted = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 60, text.length());
            formatted.append(text, start, end);
            if (end < text.length()) {
                formatted.append("<br/>");
            }
            start = end + 1;
        }
        return formatted.toString();
    }

}
