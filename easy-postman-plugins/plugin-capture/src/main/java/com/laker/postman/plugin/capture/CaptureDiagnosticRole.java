package com.laker.postman.plugin.capture;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

enum CaptureDiagnosticRole {
    SOURCE_APP(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ROLE_SOURCE_APP),
    CLIENT_CONNECTION(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ROLE_CLIENT_CONNECTION),
    EASY_POSTMAN_PROXY(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ROLE_PROXY),
    HTTPS_MITM_PROXY(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ROLE_MITM_PROXY),
    TARGET_SERVER(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ROLE_TARGET_SERVER),
    TRUST_STORE(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ROLE_TRUST_STORE);

    private final String messageKey;

    CaptureDiagnosticRole(String messageKey) {
        this.messageKey = messageKey;
    }

    String displayText() {
        return t(messageKey);
    }
}
