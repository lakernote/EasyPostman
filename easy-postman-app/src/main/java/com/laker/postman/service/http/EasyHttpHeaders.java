package com.laker.postman.service.http;

/**
 * HTTP 头部名称常量，包含标准头部与 Easy-Postman 内部自定义头部。
 * <p>
 * 内部自定义头部（{@code Easy-*}）由
 * {@link CompressionDecompressNetworkInterceptor} 在解压响应时注入，
 * 用于在拦截器链中保留被移除的原始头部信息。
 * </p>
 */
public final class EasyHttpHeaders {

    private EasyHttpHeaders() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ── 标准 HTTP 头部 ────────────────────────────────────────────────────────

    /** 标准头部：{@code Content-Encoding} */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /** 标准头部：{@code Content-Length} */
    public static final String CONTENT_LENGTH = "Content-Length";

    // ── Easy-Postman 内部自定义头部 ───────────────────────────────────────────

    /**
     * 保存原始 {@code Content-Length} 的值。
     * <p>
     * 当 {@link CompressionDecompressNetworkInterceptor} 解压响应体后，
     * 原始 {@code Content-Length}（压缩体的大小）会被移除，
     * 其值保存到此头部作为大小估算依据。
     * </p>
     */
    public static final String EASY_CONTENT_LENGTH = "Easy-Content-Length";

    /**
     * 保存原始 {@code Content-Encoding} 的值（小写）。
     * <p>
     * 当 {@link CompressionDecompressNetworkInterceptor} 解压响应体后，
     * 原始 {@code Content-Encoding}（如 gzip / deflate / br）会被移除，
     * 其值保存到此头部，供下游展示使用。
     * </p>
     */
    public static final String EASY_CONTENT_ENCODING = "Easy-Content-Encoding";
}
