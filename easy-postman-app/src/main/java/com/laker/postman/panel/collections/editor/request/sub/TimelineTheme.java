package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class TimelineTheme {
    private final Color CONTENT_DOWNLOAD_LIGHT = new Color(0x0D, 0x94, 0x88);
    private final Color CONTENT_DOWNLOAD_DARK = new Color(0x4D, 0xB6, 0xAC);

    Color panelBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color infoBackground() {
        return sectionBackground();
    }

    Color infoBorder() {
        return ModernColors.withAlpha(ModernColors.getBorderLightColor(), ModernColors.isDarkTheme() ? 150 : 135);
    }

    Color barAreaBackground() {
        return sectionBackground();
    }

    Color barTrackBackground() {
        Color base = ModernColors.isDarkTheme()
                ? ModernColors.getBackgroundColor()
                : ModernColors.getCardBackgroundColor();
        Color border = ModernColors.getDividerBorderColor();
        return ModernColors.blendColors(base, border, ModernColors.isDarkTheme() ? 0.18f : 0.12f);
    }

    Color barTrackBorder() {
        return ModernColors.withAlpha(ModernColors.getDividerBorderColor(), ModernColors.isDarkTheme() ? 115 : 90);
    }

    Color gridLine() {
        return ModernColors.withAlpha(ModernColors.getDividerBorderColor(), ModernColors.isDarkTheme() ? 55 : 45);
    }

    Color labelText() {
        return ModernColors.getTextPrimary();
    }

    Color infoText() {
        return ModernColors.getTextSecondary();
    }

    Color descriptionText() {
        return ModernColors.getTextHint();
    }

    Color separator() {
        return ModernColors.withAlpha(ModernColors.getDividerBorderColor(), ModernColors.isDarkTheme() ? 105 : 90);
    }

    Color certificateWarning() {
        return ModernColors.getError();
    }

    Color[] barColors() {
        return new Color[]{
                dnsLookupColor(),
                socketConnectionColor(),
                tlsHandshakeColor(),
                requestSendColor(),
                timeToFirstByteColor(),
                contentDownloadColor()
        };
    }

    Color dnsLookupColor() {
        return ModernColors.getPrimary();
    }

    Color socketConnectionColor() {
        return ModernColors.getSecondary();
    }

    Color tlsHandshakeColor() {
        return ModernColors.getAccent();
    }

    Color requestSendColor() {
        return ModernColors.getNeutral();
    }

    Color timeToFirstByteColor() {
        return ModernColors.getWarningDark();
    }

    Color contentDownloadColor() {
        Color transferTeal = ModernColors.isDarkTheme()
                ? CONTENT_DOWNLOAD_DARK
                : CONTENT_DOWNLOAD_LIGHT;
        return ModernColors.blendColors(
                transferTeal,
                ModernColors.getTextSecondary(),
                ModernColors.isDarkTheme() ? 0.10f : 0.06f
        );
    }

    Color hoveredBarBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color hoveredLabelText() {
        return ModernColors.getTextPrimary();
    }

    Color hoveredBarOutline() {
        return ModernColors.withAlpha(ModernColors.getPrimary(), ModernColors.isDarkTheme() ? 130 : 105);
    }

    Color barHighlight(boolean hovered) {
        int alpha = ModernColors.isDarkTheme()
                ? (hovered ? 34 : 24)
                : (hovered ? 58 : 42);
        return ModernColors.whiteWithAlpha(alpha);
    }

    Color barText() {
        return ModernColors.getTextInverse();
    }

    private Color sectionBackground() {
        return ModernColors.blendColors(
                ModernColors.getCardBackgroundColor(),
                ModernColors.getBackgroundColor(),
                ModernColors.isDarkTheme() ? 0.32f : 0.18f
        );
    }
}
