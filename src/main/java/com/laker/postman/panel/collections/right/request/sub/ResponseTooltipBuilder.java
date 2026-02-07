package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpEventInfo;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * 响应大小Tooltip构建器
 * 负责构建响应大小的HTML tooltip
 */
@UtilityClass
public class ResponseTooltipBuilder {

    /**
     * 构建响应大小的tooltip HTML
     */
    public static String buildSizeTooltip(long uncompressedBytes, HttpEventInfo httpEventInfo,
                                          ResponseSizeCalculator.SizeInfo sizeInfo) {
        TooltipColors colors = new TooltipColors();

        if (sizeInfo.isCompressed()) {
            return buildCompressedTooltip(uncompressedBytes, httpEventInfo, sizeInfo, colors);
        } else {
            return buildNormalTooltip(uncompressedBytes, httpEventInfo, colors);
        }
    }

    /**
     * 构建压缩响应的tooltip
     */
    private static String buildCompressedTooltip(long uncompressedBytes, HttpEventInfo info,
                                                 ResponseSizeCalculator.SizeInfo sizeInfo, TooltipColors colors) {
        return String.format("<html>" +
                        "<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", \"Helvetica Neue\", Arial, sans-serif; font-size: 10px; width: 220px; padding: 4px;'>" +
                        "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔽 Response Size</div>" +
                        "<div style='margin-left: 8px; line-height: 1.4;'>" +
                        "<div style='color: %s; margin-bottom: 3px;'>🏷️ Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "<div style='color: %s; margin-bottom: 3px;'>📦 Body (Compressed): <span style='font-weight: 600; color: %s;'>%s</span></div>" +
                        "<div style='margin-left: 8px; color: %s; font-size: 9px; margin-bottom: 4px;'>🔓 Uncompressed: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "<div style='margin: 4px 0; padding: 6px 8px; background: %s; border-radius: 4px; border-left: 3px solid %s;'>" +
                        "<div style='color: %s; font-weight: 600; font-size: 10px; margin-bottom: 2px;'>✨ Compression Ratio: <span style='color: %s;'>%.1f%%</span></div>" +
                        "<div style='color: %s; font-weight: 600; font-size: 10px;'>💾 Saved: <span style='color: %s;'>%s</span></div>" +
                        "</div>" +
                        "</div>" +
                        "<div style='border-top: 1px solid %s; margin: 6px 0;'></div>" +
                        "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔼 Request Size</div>" +
                        "<div style='margin-left: 8px; line-height: 1.4;'>" +
                        "<div style='color: %s; margin-bottom: 3px;'>📋 Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "<div style='color: %s;'>📝 Body: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "</div>" +
                        "</div>" +
                        "</html>",
                colors.titlePrimary,
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getHeaderBytesReceived()),
                colors.textSecondary, colors.success, ResponseSizeCalculator.formatBytes(info.getBodyBytesReceived()),
                colors.textHint, colors.textSecondary, ResponseSizeCalculator.formatBytes(uncompressedBytes),
                colors.compressBg, colors.success,
                colors.successDark, colors.successDark, sizeInfo.getCompressionRatio(),
                colors.successDark, colors.successDark, ResponseSizeCalculator.formatBytes(sizeInfo.getSavedBytes()),
                colors.border,
                colors.titlePrimary,
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getHeaderBytesSent()),
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getBodyBytesSent())
        );
    }

    /**
     * 构建普通响应的tooltip
     */
    private static String buildNormalTooltip(long uncompressedBytes, HttpEventInfo info, TooltipColors colors) {
        return String.format("<html>" +
                        "<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", \"Helvetica Neue\", Arial, sans-serif; font-size: 10px; width: 180px; padding: 4px;'>" +
                        "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔽 Response Size</div>" +
                        "<div style='margin-left: 8px; line-height: 1.4;'>" +
                        "<div style='color: %s; margin-bottom: 3px;'>🏷️ Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "<div style='color: %s; margin-bottom: 3px;'>📦 Body: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "<div style='margin-left: 8px; color: %s; font-size: 9px;'>🔓 Uncompressed: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "</div>" +
                        "<div style='border-top: 1px solid %s; margin: 6px 0;'></div>" +
                        "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔼 Request Size</div>" +
                        "<div style='margin-left: 8px; line-height: 1.4;'>" +
                        "<div style='color: %s; margin-bottom: 3px;'>📋 Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "<div style='color: %s;'>📝 Body: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                        "</div>" +
                        "</div>" +
                        "</html>",
                colors.titlePrimary,
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getHeaderBytesReceived()),
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getBodyBytesReceived()),
                colors.textHint, colors.textSecondary, ResponseSizeCalculator.formatBytes(uncompressedBytes),
                colors.border,
                colors.titlePrimary,
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getHeaderBytesSent()),
                colors.textSecondary, colors.textPrimary, ResponseSizeCalculator.formatBytes(info.getBodyBytesSent())
        );
    }

    /**
     * Tooltip颜色配置
     */
    static class TooltipColors {
        final String titlePrimary;
        final String textSecondary;
        final String textPrimary;
        final String textHint;
        final String success;
        final String successDark;
        final String border;
        final String compressBg;

        TooltipColors() {
            this.titlePrimary = toHtmlColor(ModernColors.PRIMARY);
            this.textSecondary = toHtmlColor(ModernColors.getTextSecondary());
            this.textPrimary = toHtmlColor(ModernColors.getTextPrimary());
            this.textHint = toHtmlColor(ModernColors.getTextHint());
            this.success = toHtmlColor(ModernColors.SUCCESS);
            this.successDark = toHtmlColor(ModernColors.SUCCESS_DARK);
            this.border = toHtmlColor(ModernColors.getBorderLightColor());
            this.compressBg = ModernColors.isDarkTheme()
                    ? "rgba(34, 197, 94, 0.15)"
                    : "linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%)";
        }

        private static String toHtmlColor(Color color) {
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        }
    }
}
