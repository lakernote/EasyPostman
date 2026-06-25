package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.variable.VariableType;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * Shared compact tooltip markup for request variables.
 * <p>
 * Keep this as HTML because Swing's default tooltip renderer already handles it
 * consistently across text fields, editors, and table cells.
 */
@UtilityClass
public class VariableTooltipHtmlBuilder {
    private static final int TOOLTIP_WIDTH = 210;
    private static final int MAX_VALUE_LENGTH = 72;

    public static String variableTooltip(String varName, String content, VariableType varType) {
        if (varType == null) {
            return undefinedVariableTooltip(varName);
        }
        return typedTooltip(
                varName,
                safe(content),
                ModernColors.toHtmlColor(varType.getColor()),
                varType.getDisplayName(),
                varType.getIconSymbol(),
                varType,
                true
        );
    }

    public static String undefinedVariableTooltip(String varName) {
        return typedTooltip(
                varName,
                I18nUtil.getMessage(MessageKeys.VARIABLE_TYPE_UNDEFINED),
                ModernColors.toHtmlColor(ModernColors.getError()),
                I18nUtil.getMessage(MessageKeys.VARIABLE_TYPE_UNDEFINED),
                "x",
                null,
                false
        );
    }

    public static String pathVariableTooltip(String varName, String content) {
        String displayContent = content == null || content.isEmpty()
                ? I18nUtil.getMessage(MessageKeys.REQUEST_PATH_VARIABLE_EMPTY_VALUE)
                : content;
        return typedTooltip(
                varName,
                displayContent,
                ModernColors.toHtmlColor(ModernColors.getPrimary()),
                I18nUtil.getMessage(MessageKeys.REQUEST_PATH_VARIABLE_TOOLTIP_TYPE),
                ":",
                null,
                true
        );
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static String typedTooltip(String varName,
                                       String content,
                                       String titleColor,
                                       String typeLabel,
                                       String typeIcon,
                                       VariableType varType,
                                       boolean defined) {
        int metaFontSize = tooltipFontSize(Font.PLAIN, -3);
        int titleFontSize = tooltipFontSize(Font.BOLD, -2);
        int bodyFontSize = tooltipFontSize(Font.PLAIN, -2);

        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><body style='padding: 7px 10px; font-family: Dialog, Arial, sans-serif; color: ")
                .append(ModernColors.toHtmlColor(ModernColors.getTextPrimary()))
                .append(";'>");

        tooltip.append("<table width='").append(TOOLTIP_WIDTH)
                .append("' cellpadding='0' cellspacing='0' border='0'>");
        tooltip.append("<tr><td>");
        tooltip.append("<span style='font-size: ").append(metaFontSize)
                .append("px; font-weight: bold; color: ").append(titleColor).append(";'>");
        tooltip.append(escapeHtml(typeIcon)).append(" ").append(escapeHtml(typeLabel));
        tooltip.append("</span>");
        tooltip.append("&nbsp;&nbsp;");
        tooltip.append("<b style='font-size: ").append(titleFontSize)
                .append("px; color: ").append(titleColor).append(";'>");
        tooltip.append(escapeHtml(varName));
        tooltip.append("</b>");
        tooltip.append("</td></tr>");

        tooltip.append("<tr><td height='8'></td></tr>");

        tooltip.append("<tr><td style='color: ")
                .append(ModernColors.toHtmlColor(ModernColors.getTextPrimary()))
                .append("; font-size: ").append(bodyFontSize).append("px;'>");
        appendContent(tooltip, content, varType, defined);
        tooltip.append("</td></tr>");
        tooltip.append("</table>");
        tooltip.append("</body></html>");
        return tooltip.toString();
    }

    private static void appendContent(StringBuilder tooltip, String content, VariableType varType, boolean defined) {
        if (defined && varType == VariableType.BUILT_IN) {
            tooltip.append("<span style='color: ")
                    .append(ModernColors.toHtmlColor(ModernColors.getTextHint()))
                    .append("; font-style: italic;'>");
            tooltip.append(escapeHtml(content));
            tooltip.append("</span>");
            return;
        }

        if (defined) {
            appendValueContent(tooltip, content);
            return;
        }

        tooltip.append("<span style='color: ")
                .append(ModernColors.toHtmlColor(ModernColors.getTextSecondary()))
                .append(";'>");
        tooltip.append(escapeHtml(content));
        tooltip.append("</span>");
    }

    private static void appendValueContent(StringBuilder tooltip, String content) {
        tooltip.append("<span style='color: ")
                .append(ModernColors.toHtmlColor(ModernColors.getTextHint()))
                .append(";'>")
                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE)))
                .append(":</span>");
        tooltip.append("&nbsp;");
        tooltip.append("<span style='font-family: Consolas, monospace; background-color: ")
                .append(ModernColors.toHtmlColor(ModernColors.getHoverBackgroundColor()))
                .append("; color: ")
                .append(ModernColors.toHtmlColor(ModernColors.getTextPrimary()))
                .append("; padding: 3px 6px; border-radius: 3px; display: inline-block;'>");

        String displayContent = trimValue(safe(content));
        tooltip.append(escapeHtml(displayContent));
        tooltip.append("</span>");
    }

    private static int tooltipFontSize(int style, int offset) {
        return Math.max(10, FontsUtil.getDefaultFontWithOffset(style, offset).getSize());
    }

    private static String trimValue(String content) {
        return content.length() > MAX_VALUE_LENGTH ? content.substring(0, MAX_VALUE_LENGTH) + "..." : content;
    }

    private static String safe(String content) {
        return content == null ? "" : content;
    }
}
