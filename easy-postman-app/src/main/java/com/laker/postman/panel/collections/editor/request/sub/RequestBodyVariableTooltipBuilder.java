package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.variable.VariableType;
import lombok.experimental.UtilityClass;

import java.awt.*;

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
        tooltipBuilder.append(" <span style='color:gray'>(").append(varType.getDisplayName()).append(")</span>");
        if (varValue != null && !varValue.isEmpty()) {
            tooltipBuilder.append("<br/>").append(escapeHtml(varValue));
        }
        tooltipBuilder.append("</html>");
        return tooltipBuilder.toString();
    }

    static String variableTooltip(String varName, String content, VariableType varType) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><body style='padding: 8px; font-family: Arial, sans-serif; color: ")
                .append(toHex(RequestBodyTheme.tooltipText()))
                .append(";'>");

        boolean defined = varType != null;
        String titleColor;
        String typeLabel;
        String typeIcon;

        if (defined) {
            titleColor = toHex(varType.getColor());
            typeLabel = varType.getDisplayName();
            typeIcon = varType.getIconSymbol();
        } else {
            titleColor = "#D32F2F";
            typeLabel = I18nUtil.getMessage(MessageKeys.VARIABLE_TYPE_UNDEFINED);
            typeIcon = "x";
        }

        tooltip.append("<div style='margin-bottom: 6px;'>");
        tooltip.append("<span style='font-size: 10px; color: ").append(titleColor).append(";'>");
        tooltip.append(typeIcon).append(" ").append(typeLabel);
        tooltip.append("</span></div>");

        tooltip.append("<div style='margin-bottom: 1px;'>");
        tooltip.append("<b style='font-size: 10px; color: ").append(titleColor).append(";'>");
        tooltip.append(escapeHtml(varName));
        tooltip.append("</b></div>");

        tooltip.append("<hr style='border: none; border-top: 1px solid ")
                .append(toHex(RequestBodyTheme.tooltipDivider()))
                .append("; margin: 1px 0;'/>");

        tooltip.append("<div style='margin-top: 1px; color: ")
                .append(toHex(RequestBodyTheme.tooltipText()))
                .append("; font-size: 10px;'>");

        appendContent(tooltip, content, varType, defined);
        tooltip.append("</div>");
        tooltip.append("</body></html>");
        return tooltip.toString();
    }

    static String undefinedVariableTooltip(String varName) {
        return variableTooltip(varName, I18nUtil.getMessage(MessageKeys.VARIABLE_TYPE_UNDEFINED), null);
    }

    static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static void appendContent(StringBuilder tooltip, String content, VariableType varType, boolean defined) {
        if (defined && varType == VariableType.BUILT_IN) {
            tooltip.append("<span style='color: ")
                    .append(toHex(RequestBodyTheme.tooltipMutedText()))
                    .append("; font-style: italic;'>");
            tooltip.append(escapeHtml(content));
            tooltip.append("</span>");
            return;
        }

        if (defined) {
            appendValueContent(tooltip, content);
            return;
        }

        tooltip.append("<span style='color: #D32F2F; font-weight: bold;'>");
        tooltip.append(escapeHtml(content));
        tooltip.append("</span>");
    }

    private static void appendValueContent(StringBuilder tooltip, String content) {
        tooltip.append("<span style='color: ")
                .append(toHex(RequestBodyTheme.tooltipMutedText()))
                .append(";'>Value:</span><br/>");
        tooltip.append("<span style='font-family: Consolas, monospace; background-color: ")
                .append(toHex(RequestBodyTheme.tooltipCodeBackground()))
                .append("; color: ")
                .append(toHex(RequestBodyTheme.tooltipText()))
                .append("; ");
        tooltip.append("padding: 4px 6px; border-radius: 3px; display: inline-block; margin-top: 1px;'>");

        String displayContent = content.length() > 150 ? content.substring(0, 150) + "..." : content;
        tooltip.append(escapeHtml(displayContent));
        tooltip.append("</span>");
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

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
