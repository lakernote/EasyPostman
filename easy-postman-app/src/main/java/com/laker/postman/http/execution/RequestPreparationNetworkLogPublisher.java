package com.laker.postman.http.execution;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSupport;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class RequestPreparationNetworkLogPublisher {

    public static void publish(PreparedRequest request) {
        if (!NetworkLogSupport.isEnabled(request)) {
            return;
        }
        NetworkLogSupport.append(request, NetworkLogEventStage.REQUEST_PREPARED, format(request));
    }

    private static String format(PreparedRequest request) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("Method: ").append(valueOrDash(request.method)).append("\n");
        sb.append("URL: ").append(valueOrDash(request.url)).append("\n");
        sb.append("Body Type: ").append(valueOrDash(request.bodyType)).append("\n");
        appendHeaderSummary(sb, request.headersList);
        appendParamSummary(sb, "Query Params", request.paramsList);
        appendParamSummary(sb, "Path Variables", request.pathVariablesList);
        sb.append("Form Data Rows: ").append(sizeOf(request.formDataList)).append("\n");
        sb.append("Urlencoded Rows: ").append(sizeOf(request.urlencodedList)).append("\n");
        sb.append("Pre-script: ").append(isBlank(request.prescript) ? "none" : "present").append("\n");
        sb.append("Post-script: ").append(isBlank(request.postscript) ? "none" : "present").append("\n");
        sb.append("Redirects: ").append(request.followRedirects ? "enabled" : "disabled").append("\n");
        sb.append("Cookie Jar: ").append(request.cookieJarEnabled ? "enabled" : "disabled").append("\n");
        sb.append("Proxy Policy: ").append(request.proxyPolicy == null ? "-" : request.proxyPolicy).append("\n");
        sb.append("SSL Verification: ").append(request.sslVerificationEnabled ? "enabled" : "disabled").append("\n");
        sb.append("HTTP Version: ").append(valueOrDash(request.httpVersion)).append("\n");
        sb.append("Timeout: ").append(request.requestTimeoutMs > 0 ? request.requestTimeoutMs + "ms" : "default").append("\n");
        return sb.toString();
    }

    private static void appendHeaderSummary(StringBuilder sb, List<HttpHeader> headers) {
        int total = sizeOf(headers);
        long enabled = headers == null ? 0L : headers.stream()
                .filter(header -> header != null && header.isEnabled())
                .count();
        sb.append("Headers: ").append(enabled).append(" enabled / ").append(total).append(" total").append("\n");
        if (headers != null && !headers.isEmpty()) {
            String names = headers.stream()
                    .filter(header -> header != null && !isBlank(header.getKey()))
                    .map(HttpHeader::getKey)
                    .collect(Collectors.joining(", "));
            if (!isBlank(names)) {
                sb.append("Header Names: ").append(names).append("\n");
            }
        }
    }

    private static void appendParamSummary(StringBuilder sb, String label, List<HttpParam> params) {
        int total = sizeOf(params);
        long enabled = params == null ? 0L : params.stream()
                .filter(param -> param != null && param.isEnabled())
                .count();
        sb.append(label).append(": ").append(enabled).append(" enabled / ").append(total).append(" total").append("\n");
    }

    private static int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private static String valueOrDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
