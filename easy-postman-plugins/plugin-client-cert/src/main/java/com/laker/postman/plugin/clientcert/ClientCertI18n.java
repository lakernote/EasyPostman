package com.laker.postman.plugin.clientcert;

import com.laker.postman.util.I18nUtil;

public final class ClientCertI18n {

    private static final String BUNDLE_NAME = "client-cert-messages";

    private ClientCertI18n() {
    }

    public static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, ClientCertI18n.class.getClassLoader(), key, args);
    }
}
