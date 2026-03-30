package com.laker.postman.service.http;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

public final class NetworkErrorMessageResolver {
    private static final String SOCKS_MALFORMED_REPLY = "Malformed reply from SOCKS server";

    private NetworkErrorMessageResolver() {
    }

    public static String toUserFriendlyMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        return toUserFriendlyMessage(throwable.getMessage());
    }

    public static String toUserFriendlyMessage(String rawMessage) {
        String normalized = rawMessage == null ? "" : rawMessage.trim();
        if (normalized.contains(SOCKS_MALFORMED_REPLY)) {
            return I18nUtil.getMessage(MessageKeys.NETWORK_ERROR_PROXY_SOCKS_MALFORMED, normalized);
        }
        return normalized;
    }
}
