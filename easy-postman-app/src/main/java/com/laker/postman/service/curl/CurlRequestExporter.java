package com.laker.postman.service.curl;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.TransportAuth;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class CurlRequestExporter {

    static String toCurl(PreparedRequest preparedRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("curl");
        if (preparedRequest.method != null && !"GET".equalsIgnoreCase(preparedRequest.method)) {
            sb.append(" -X ").append(preparedRequest.method.toUpperCase());
        }

        TransportAuth auth = preparedRequest.transportAuth;
        if (auth != null && auth.isDigest() && !isBlank(auth.username)) {
            sb.append(" --digest");
            sb.append(" --user ").append(ShellArgumentEscaper.escape(auth.username + ":" + (auth.password == null ? "" : auth.password)));
        }

        if (preparedRequest.url != null) {
            sb.append(" ").append(ShellArgumentEscaper.escape(preparedRequest.url));
        }
        if (preparedRequest.headersList != null) {
            for (var header : preparedRequest.headersList) {
                if (header.isEnabled()) {
                    sb.append(" -H ").append(ShellArgumentEscaper.escape(header.getKey() + ": " + header.getValue()));
                }
            }
        }
        if (preparedRequest.body != null && !preparedRequest.body.isEmpty()) {
            sb.append(" --data ").append(ShellArgumentEscaper.escape(preparedRequest.body));
        }
        if (preparedRequest.urlencodedList != null && !preparedRequest.urlencodedList.isEmpty()) {
            for (var encoded : preparedRequest.urlencodedList) {
                if (encoded.isEnabled()) {
                    sb.append(" --data-urlencode ").append(ShellArgumentEscaper.escape(encoded.getKey() + "=" + encoded.getValue()));
                }
            }
        }
        if (preparedRequest.formDataList != null && !preparedRequest.formDataList.isEmpty()) {
            for (var formData : preparedRequest.formDataList) {
                if (formData.isEnabled()) {
                    if (formData.isText()) {
                        sb.append(" -F ").append(ShellArgumentEscaper.escape(formData.getKey() + "=" + formData.getValue()));
                    } else if (formData.isFile()) {
                        sb.append(" -F ").append(ShellArgumentEscaper.escape(formData.getKey() + "=@" + formData.getValue()));
                    }
                }
            }
        }
        return sb.toString();
    }

    static String toActualCurl(PreparedRequest preparedRequest) {
        if (!hasSentSnapshot(preparedRequest)) {
            throw new IllegalStateException("No actual sent request snapshot is available");
        }
        boolean hasBodySnapshot = preparedRequest.sentRequestBody != null && !preparedRequest.sentRequestBody.isEmpty();
        boolean hasReplayableBody = hasBodySnapshot && preparedRequest.sentRequestBodyReplayable;
        StringBuilder sb = new StringBuilder();
        if (hasBodySnapshot && !hasReplayableBody) {
            sb.append("# Request body omitted: captured body snapshot is diagnostic-only and cannot be replayed safely.\n");
        }
        sb.append("curl");
        appendProtocolOption(sb, preparedRequest);
        appendMethod(sb, actualMethod(preparedRequest), hasReplayableBody);
        String actualUrl = actualUrl(preparedRequest);
        if (actualUrl != null) {
            sb.append(" ").append(ShellArgumentEscaper.escape(actualUrl));
        }
        appendHeaders(sb, preparedRequest.sentHeadersList, hasBodySnapshot && !hasReplayableBody);
        if (hasReplayableBody) {
            sb.append(" --data-raw ").append(ShellArgumentEscaper.escape(preparedRequest.sentRequestBody));
        }
        return sb.toString();
    }

    static boolean hasSentSnapshot(PreparedRequest preparedRequest) {
        if (preparedRequest == null) {
            return false;
        }
        return !isBlank(preparedRequest.sentUrl)
                || !isBlank(preparedRequest.sentMethod)
                || hasHeaders(preparedRequest.sentHeadersList)
                || preparedRequest.sentRequestBody != null;
    }

    private static void appendMethod(StringBuilder sb, String method, boolean hasBody) {
        if (method != null && (!"GET".equalsIgnoreCase(method) || hasBody)) {
            sb.append(" -X ").append(method.toUpperCase());
        }
    }

    private static String actualUrl(PreparedRequest preparedRequest) {
        return isBlank(preparedRequest.sentUrl) ? preparedRequest.url : preparedRequest.sentUrl;
    }

    private static String actualMethod(PreparedRequest preparedRequest) {
        return isBlank(preparedRequest.sentMethod) ? preparedRequest.method : preparedRequest.sentMethod;
    }

    private static void appendHeaders(StringBuilder sb, List<HttpHeader> headers, boolean bodyOmitted) {
        if (headers == null) {
            return;
        }
        for (HttpHeader header : headers) {
            if (header != null && header.isEnabled() && !shouldOmitHeader(header, bodyOmitted)) {
                sb.append(" -H ").append(ShellArgumentEscaper.escape(header.getKey() + ": " + header.getValue()));
            }
        }
    }

    private static boolean shouldOmitHeader(HttpHeader header, boolean bodyOmitted) {
        if (!bodyOmitted || header.getKey() == null) {
            return false;
        }
        return "Content-Length".equalsIgnoreCase(header.getKey())
                || "Transfer-Encoding".equalsIgnoreCase(header.getKey());
    }

    private static boolean hasHeaders(List<HttpHeader> headers) {
        if (headers == null) {
            return false;
        }
        for (HttpHeader header : headers) {
            if (header != null && header.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private static void appendProtocolOption(StringBuilder sb, PreparedRequest preparedRequest) {
        String protocol = preparedRequest.exchangeEventInfo == null ? null : preparedRequest.exchangeEventInfo.getProtocol();
        if (isBlank(protocol)) {
            return;
        }
        if ("h2".equalsIgnoreCase(protocol) || "http/2".equalsIgnoreCase(protocol)) {
            sb.append(" --http2");
        } else if ("http/1.1".equalsIgnoreCase(protocol)) {
            sb.append(" --http1.1");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
