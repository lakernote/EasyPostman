package com.laker.postman.performance.core.run;

import lombok.Value;

@Value
public class PerformanceRunAsset {
    public static final String TYPE_CSV = "CSV";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_CERTIFICATE = "CERTIFICATE";

    String id;
    String type;
    String path;
    String sha256;

    public PerformanceRunAsset(String id, String type, String path, String sha256) {
        this.id = id == null ? "" : id;
        this.type = type == null ? TYPE_FILE : type;
        this.path = path == null ? "" : path;
        this.sha256 = sha256;
    }
}
