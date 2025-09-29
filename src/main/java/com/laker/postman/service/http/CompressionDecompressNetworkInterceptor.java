package com.laker.postman.service.http;

import okhttp3.*;
import okio.BufferedSource;
import okio.GzipSource;
import okio.InflaterSource;
import okio.Okio;
import org.brotli.dec.BrotliInputStream;

import java.io.IOException;
import java.util.zip.Inflater;

/**
 * 网络拦截器：自动解压 gzip、deflate、br 三种压缩格式，流式解压，兼容 SSE/chunked/普通响应
 * 严格参考 OkHttp BridgeInterceptor 的 promisesBody 逻辑，只有在响应确实有 body 时才解压
 * 头部处理与 BridgeInterceptor 保持一致，移除 Content-Encoding/Content-Length，保留 Content-Type
 */
public class CompressionDecompressNetworkInterceptor implements Interceptor {
    private boolean promisesBody(Response response) {
        if ("HEAD".equalsIgnoreCase(response.request().method())) {
            return false;
        }
        int responseCode = response.code();
        if ((responseCode < 100 || responseCode >= 200) && responseCode != 204 && responseCode != 304) {
            return true;
        }
        if (response.header("Content-Length") != null ||
                "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
            return true;
        }
        return false;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        String encoding = response.header("Content-Encoding");
        String contentLength = response.header("Content-Length");
        MediaType respContentType = response.body() != null ? response.body().contentType() : null;
        // 只在 Content-Encoding 存在且 promisesBody 时处理，避免重复解压和流式响应卡死
        if (encoding != null && response.body() != null && promisesBody(response)) {
            encoding = encoding.toLowerCase();
            BufferedSource decompressed;
            if ("gzip".equals(encoding)) {
                decompressed = Okio.buffer(new GzipSource(response.body().source()));
            } else if ("deflate".equals(encoding)) {
                decompressed = Okio.buffer(new InflaterSource(response.body().source(), new Inflater(true)));
            } else if ("br".equals(encoding)) {
                BrotliInputStream brInputStream = new BrotliInputStream(response.body().byteStream());
                decompressed = Okio.buffer(Okio.source(brInputStream));
            } else {
                return response;
            }
            // 头部处理参考 BridgeInterceptor，流式解压，长度未知
            Response.Builder builder = response.newBuilder()
                    .removeHeader("Content-Encoding")
                    .removeHeader("Content-Length")
                    .header("Easy-Content-Encoding", encoding)
                    .body(ResponseBody.create(respContentType, -1, decompressed));

            // 如果原来有Content-Length，保存到Easy-Content-Length
            if (contentLength != null) {
                builder.header("Easy-Content-Length", contentLength);
            }

            return builder.build();
        }
        // 其他场景（无 body、流式响应、HEAD/204/304等），原样返回
        return response;
    }
}
