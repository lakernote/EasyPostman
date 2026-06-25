package com.laker.postman.plugin.capture;

import com.laker.postman.common.component.ChipLabel;
import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class CaptureStatusStyle {

    private static final int TLS_PROXY_FAILURE_STATUS = 495;

    enum Tone {
        PENDING,
        INFORMATIONAL,
        SUCCESS,
        REDIRECT,
        CLIENT_ERROR,
        FAILURE
    }

    static Tone toneFor(Object statusValue) {
        int statusCode = parseStatusCode(statusValue);
        if (statusCode <= 0) {
            return Tone.PENDING;
        }
        if (statusCode == TLS_PROXY_FAILURE_STATUS || statusCode >= 500) {
            return Tone.FAILURE;
        }
        if (statusCode >= 400) {
            return Tone.CLIENT_ERROR;
        }
        if (statusCode >= 300) {
            return Tone.REDIRECT;
        }
        if (statusCode >= 200) {
            return Tone.SUCCESS;
        }
        if (statusCode >= 100) {
            return Tone.INFORMATIONAL;
        }
        return Tone.PENDING;
    }

    static Color accentFor(Object statusValue) {
        return switch (toneFor(statusValue)) {
            case INFORMATIONAL -> ModernColors.getInfo();
            case SUCCESS -> ModernColors.getSuccess();
            case REDIRECT -> ModernColors.getSecondary();
            case CLIENT_ERROR -> ModernColors.getWarningDark();
            case FAILURE -> ModernColors.getError();
            case PENDING -> ModernColors.getNeutral();
        };
    }

    static Color tableForegroundFor(Object statusValue) {
        if (toneFor(statusValue) == Tone.PENDING) {
            return ModernColors.getTextSecondary();
        }
        return ChipLabel.foregroundFor(accentFor(statusValue));
    }

    private static int parseStatusCode(Object statusValue) {
        if (statusValue == null) {
            return 0;
        }
        if (statusValue instanceof Number number) {
            return number.intValue();
        }
        String text = statusValue.toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        int index = 0;
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        if (index == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(text.substring(0, index));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
