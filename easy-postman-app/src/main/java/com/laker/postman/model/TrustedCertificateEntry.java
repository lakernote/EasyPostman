package com.laker.postman.model;

import lombok.Data;

import java.io.File;
import java.util.Locale;

@Data
public class TrustedCertificateEntry {
    private boolean enabled = true;
    private String path = "";
    private String password = "";

    public boolean hasUsablePath() {
        return path != null && !path.trim().isEmpty();
    }

    public String getDisplayName() {
        if (!hasUsablePath()) {
            return "";
        }
        return new File(path.trim()).getName();
    }

    public String getDisplayType() {
        if (!hasUsablePath()) {
            return "";
        }
        String fileName = getDisplayName().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".jks")) {
            return "JKS";
        }
        if (fileName.endsWith(".p12") || fileName.endsWith(".pfx")) {
            return "PKCS12";
        }
        return "X.509";
    }

    public String getMaskedPasswordHint() {
        return password != null && !password.isEmpty() ? "••••••" : "";
    }
}
