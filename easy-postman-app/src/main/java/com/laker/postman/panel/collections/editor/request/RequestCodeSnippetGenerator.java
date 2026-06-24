package com.laker.postman.panel.collections.editor.request;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.http.request.AppRequestHeaderDefaults;
import com.laker.postman.request.model.AuthApiKeyPlacement;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestAuthTypes;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.util.HttpUrlUtil;
import com.laker.postman.util.FileMimeTypeUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@UtilityClass
class RequestCodeSnippetGenerator {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM_URLENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

    static String generate(HttpRequestItem request, RequestCodeSnippetLanguage language) {
        if (request == null) {
            return "";
        }
        return switch (language) {
            case CURL -> generateCurl(request);
            case JAVA_OKHTTP -> generateJavaOkHttp(request);
            case JAVASCRIPT_FETCH -> generateJavaScriptFetch(request);
            case PYTHON_REQUESTS -> generatePythonRequests(request);
        };
    }

    private static String generateCurl(HttpRequestItem request) {
        StringBuilder code = new StringBuilder("curl");
        String method = method(request);
        if (!"GET".equalsIgnoreCase(method)) {
            code.append(" \\\n  -X ").append(method);
        }
        String url = urlWithParams(request);
        if (CharSequenceUtil.isNotBlank(url)) {
            code.append(" \\\n  ").append(shellQuote(url));
        }
        for (HttpHeader header : effectiveHeaders(request)) {
            code.append(" \\\n  -H ").append(shellQuote(header.getKey() + ": " + nullToEmpty(header.getValue())));
        }

        if (hasBinaryBody(request)) {
            code.append(" \\\n  --data-binary ").append(shellQuote("@" + request.getBody()));
        } else if (isFormDataBody(request)) {
            for (HttpFormData item : enabledFormData(request)) {
                String value = item.isFile()
                        ? item.getKey() + "=@" + nullToEmpty(item.getValue())
                        : item.getKey() + "=" + nullToEmpty(item.getValue());
                code.append(" \\\n  -F ").append(shellQuote(value));
            }
        } else if (RequestBodyTypes.BODY_TYPE_FORM_URLENCODED.equals(request.getBodyType())) {
            for (HttpFormUrlencoded item : enabledUrlencoded(request)) {
                code.append(" \\\n  --data-urlencode ")
                        .append(shellQuote(item.getKey() + "=" + nullToEmpty(item.getValue())));
            }
        } else if (CharSequenceUtil.isNotBlank(request.getBody())) {
            code.append(" \\\n  --data-raw ").append(shellQuote(request.getBody()));
        }
        return code.toString();
    }

    private static String generateJavaOkHttp(HttpRequestItem request) {
        StringBuilder code = new StringBuilder();
        code.append("OkHttpClient client = new OkHttpClient().newBuilder()\n")
                .append("    .build();\n");

        String bodyVariable = "null";
        String bodyType = request.getBodyType();
        if (hasBinaryBody(request)) {
            code.append("MediaType mediaType = MediaType.parse(")
                    .append(javaString(contentType(request)))
                    .append(");\n")
                    .append("RequestBody body = RequestBody.create(new File(")
                    .append(javaString(request.getBody()))
                    .append("), mediaType);\n");
            bodyVariable = "body";
        } else if (RequestBodyTypes.BODY_TYPE_FORM_DATA.equals(bodyType) && !enabledFormData(request).isEmpty()) {
            code.append("RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)");
            for (HttpFormData item : enabledFormData(request)) {
                if (item.isFile()) {
                    code.append("\n    .addFormDataPart(")
                            .append(javaString(item.getKey()))
                            .append(", ")
                            .append(javaString(nullToEmpty(item.getValue())))
                            .append(", RequestBody.create(new File(")
                            .append(javaString(nullToEmpty(item.getValue())))
                            .append("), MediaType.parse(")
                            .append(javaString(FileMimeTypeUtil.detectMimeType(item.getValue())))
                            .append(")))");
                } else {
                    code.append("\n    .addFormDataPart(")
                            .append(javaString(item.getKey()))
                            .append(", ")
                            .append(javaString(nullToEmpty(item.getValue())))
                            .append(")");
                }
            }
            code.append("\n    .build();\n");
            bodyVariable = "body";
        } else {
            String payload = bodyPayload(request);
            if (CharSequenceUtil.isNotBlank(payload) || methodRequiresBody(method(request))) {
                code.append("MediaType mediaType = MediaType.parse(")
                        .append(javaString(contentType(request)))
                        .append(");\n")
                        .append("RequestBody body = RequestBody.create(mediaType, ")
                        .append(javaString(payload))
                        .append(");\n");
                bodyVariable = "body";
            }
        }

        code.append("Request request = new Request.Builder()\n")
                .append("    .url(")
                .append(javaString(urlWithParams(request)))
                .append(")\n")
                .append("    .method(")
                .append(javaString(method(request)))
                .append(", ")
                .append(bodyVariable)
                .append(")");
        for (HttpHeader header : effectiveHeaders(request)) {
            code.append("\n    .addHeader(")
                    .append(javaString(header.getKey()))
                    .append(", ")
                    .append(javaString(nullToEmpty(header.getValue())))
                    .append(")");
        }
        code.append("\n    .build();\n")
                .append("Response response = client.newCall(request).execute();");
        return code.toString();
    }

    private static String generateJavaScriptFetch(HttpRequestItem request) {
        StringBuilder code = new StringBuilder();
        if (hasFormDataBody(request)) {
            code.append("const formData = new FormData();\n");
            for (HttpFormData item : enabledFormData(request)) {
                if (item.isFile()) {
                    code.append("// Attach a File or Blob for ")
                            .append(item.getKey())
                            .append(" before sending.\n");
                }
                code.append("formData.append(")
                        .append(jsString(item.getKey()))
                        .append(", ")
                        .append(jsString(nullToEmpty(item.getValue())))
                        .append(");\n");
            }
            code.append("\n");
        } else if (hasBinaryBody(request)) {
            code.append("// Node.js: import { readFile } from \"node:fs/promises\";\n")
                    .append("const binaryBody = await readFile(")
                    .append(jsString(request.getBody()))
                    .append(");\n\n");
        }

        code.append("const response = await fetch(")
                .append(jsString(urlWithParams(request)))
                .append(", {\n")
                .append("  method: ")
                .append(jsString(method(request)));
        List<HttpHeader> headers = effectiveHeaders(request);
        if (!headers.isEmpty()) {
            code.append(",\n  headers: {\n");
            for (int i = 0; i < headers.size(); i++) {
                HttpHeader header = headers.get(i);
                code.append("    ")
                        .append(jsString(header.getKey()))
                        .append(": ")
                        .append(jsString(nullToEmpty(header.getValue())));
                if (i < headers.size() - 1) {
                    code.append(",");
                }
                code.append("\n");
            }
            code.append("  }");
        }
        if (hasFormDataBody(request)) {
            code.append(",\n  body: formData");
        } else if (hasBinaryBody(request)) {
            code.append(",\n  body: binaryBody");
        } else if (CharSequenceUtil.isNotBlank(bodyPayload(request))) {
            code.append(",\n  body: ").append(jsString(bodyPayload(request)));
        }
        code.append("\n});\n\nconst data = await response.text();");
        return code.toString();
    }

    private static String generatePythonRequests(HttpRequestItem request) {
        StringBuilder code = new StringBuilder("import requests\n\n");
        code.append("url = ").append(pyString(urlWithParams(request))).append("\n");
        List<HttpHeader> headers = effectiveHeaders(request);
        if (!headers.isEmpty()) {
            code.append("headers = {\n");
            for (HttpHeader header : headers) {
                code.append("    ")
                        .append(pyString(header.getKey()))
                        .append(": ")
                        .append(pyString(nullToEmpty(header.getValue())))
                        .append(",\n");
            }
            code.append("}\n");
        }

        boolean hasBinaryBody = hasBinaryBody(request);
        boolean hasFormData = hasFormDataBody(request);
        if (hasFormData) {
            List<HttpFormData> textParts = enabledFormData(request).stream().filter(HttpFormData::isText).toList();
            List<HttpFormData> fileParts = enabledFormData(request).stream().filter(HttpFormData::isFile).toList();
            if (!textParts.isEmpty()) {
                code.append("data = {\n");
                for (HttpFormData item : textParts) {
                    code.append("    ")
                            .append(pyString(item.getKey()))
                            .append(": ")
                            .append(pyString(nullToEmpty(item.getValue())))
                            .append(",\n");
                }
                code.append("}\n");
            }
            if (!fileParts.isEmpty()) {
                code.append("files = {\n");
                for (HttpFormData item : fileParts) {
                    code.append("    ")
                            .append(pyString(item.getKey()))
                            .append(": open(")
                            .append(pyString(nullToEmpty(item.getValue())))
                            .append(", \"rb\"),\n");
                }
                code.append("}\n");
            }
        } else if (hasBinaryBody) {
            code.append("payload = open(")
                    .append(pyString(request.getBody()))
                    .append(", \"rb\")\n");
        } else if (CharSequenceUtil.isNotBlank(bodyPayload(request))) {
            code.append("payload = ").append(pyString(bodyPayload(request))).append("\n");
        }

        code.append("\nresponse = requests.request(\n")
                .append("    ")
                .append(pyString(method(request)))
                .append(",\n")
                .append("    url");
        if (!headers.isEmpty()) {
            code.append(",\n    headers=headers");
        }
        if (hasFormData) {
            if (enabledFormData(request).stream().anyMatch(HttpFormData::isText)) {
                code.append(",\n    data=data");
            }
            if (enabledFormData(request).stream().anyMatch(HttpFormData::isFile)) {
                code.append(",\n    files=files");
            }
        } else if (hasBinaryBody) {
            code.append(",\n    data=payload");
        } else if (CharSequenceUtil.isNotBlank(bodyPayload(request))) {
            code.append(",\n    data=payload");
        }
        code.append("\n)\n\nprint(response.text)");
        return code.toString();
    }

    private static List<HttpHeader> effectiveHeaders(HttpRequestItem request) {
        List<HttpHeader> headers = new ArrayList<>();
        if (request.getHeadersList() != null) {
            for (HttpHeader header : request.getHeadersList()) {
                if (header != null && header.isEnabled() && CharSequenceUtil.isNotBlank(header.getKey())) {
                    if (isGeneratedDefaultHeader(header)) {
                        continue;
                    }
                    headers.add(new HttpHeader(true, header.getKey(), nullToEmpty(header.getValue()), header.getDescription()));
                }
            }
        }
        HttpHeader authHeader = authHeader(request);
        if (authHeader != null
                && headers.stream().noneMatch(header -> authHeader.getKey().equalsIgnoreCase(header.getKey()))) {
            headers.add(authHeader);
        }
        if (headers.stream().noneMatch(header -> CONTENT_TYPE.equalsIgnoreCase(header.getKey()))) {
            String payload = bodyPayload(request);
            if (CharSequenceUtil.isNotBlank(payload)
                    || RequestBodyTypes.BODY_TYPE_FORM_URLENCODED.equals(request.getBodyType())
                    || hasBinaryBody(request)) {
                headers.add(new HttpHeader(true, CONTENT_TYPE, contentType(request), ""));
            }
        }
        return headers;
    }

    private static HttpHeader authHeader(HttpRequestItem request) {
        String authType = request.getAuthType();
        if (RequestAuthTypes.AUTH_TYPE_BEARER.equals(authType) && CharSequenceUtil.isNotBlank(request.getAuthToken())) {
            return new HttpHeader(true, "Authorization", "Bearer " + request.getAuthToken(), "");
        }
        if (RequestAuthTypes.AUTH_TYPE_BASIC.equals(authType) && CharSequenceUtil.isNotBlank(request.getAuthUsername())) {
            String credentials = request.getAuthUsername() + ":" + nullToEmpty(request.getAuthPassword());
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return new HttpHeader(true, "Authorization", "Basic " + encoded, "");
        }
        if (RequestAuthTypes.AUTH_TYPE_API_KEY.equals(authType)
                && AuthApiKeyPlacement.HEADER == AuthApiKeyPlacement.fromConstant(request.getAuthApiKeyPlacement())
                && CharSequenceUtil.isNotBlank(request.getAuthApiKeyName())
                && CharSequenceUtil.isNotBlank(request.getAuthApiKeyValue())) {
            return new HttpHeader(true, request.getAuthApiKeyName(), request.getAuthApiKeyValue(), "");
        }
        return null;
    }

    private static String bodyPayload(HttpRequestItem request) {
        if (RequestBodyTypes.BODY_TYPE_FORM_URLENCODED.equals(request.getBodyType())) {
            StringBuilder payload = new StringBuilder();
            for (HttpFormUrlencoded item : enabledUrlencoded(request)) {
                if (!payload.isEmpty()) {
                    payload.append("&");
                }
                payload.append(HttpUrlUtil.encodeComponent(nullToEmpty(item.getKey())))
                        .append("=")
                        .append(HttpUrlUtil.encodeComponent(nullToEmpty(item.getValue())));
            }
            return payload.toString();
        }
        if (RequestBodyTypes.BODY_TYPE_RAW.equals(request.getBodyType())) {
            return nullToEmpty(request.getBody());
        }
        return "";
    }

    private static boolean hasBinaryBody(HttpRequestItem request) {
        return RequestBodyTypes.BODY_TYPE_BINARY.equals(request.getBodyType())
                && CharSequenceUtil.isNotBlank(request.getBody());
    }

    private static boolean isFormDataBody(HttpRequestItem request) {
        return RequestBodyTypes.BODY_TYPE_FORM_DATA.equals(request.getBodyType());
    }

    private static boolean hasFormDataBody(HttpRequestItem request) {
        return isFormDataBody(request) && !enabledFormData(request).isEmpty();
    }

    private static String contentType(HttpRequestItem request) {
        String existing = effectiveHeadersWithoutDefaults(request).stream()
                .filter(header -> CONTENT_TYPE.equalsIgnoreCase(header.getKey()))
                .map(HttpHeader::getValue)
                .filter(CharSequenceUtil::isNotBlank)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        if (RequestBodyTypes.BODY_TYPE_FORM_URLENCODED.equals(request.getBodyType())) {
            return FORM_URLENCODED_CONTENT_TYPE;
        }
        if (RequestBodyTypes.BODY_TYPE_BINARY.equals(request.getBodyType())) {
            return FileMimeTypeUtil.detectMimeType(request.getBody());
        }
        return "application/json";
    }

    private static List<HttpHeader> effectiveHeadersWithoutDefaults(HttpRequestItem request) {
        if (request.getHeadersList() == null) {
            return List.of();
        }
        return request.getHeadersList().stream()
                .filter(header -> header != null && header.isEnabled() && CharSequenceUtil.isNotBlank(header.getKey()))
                .toList();
    }

    private static List<HttpFormData> enabledFormData(HttpRequestItem request) {
        if (request.getFormDataList() == null) {
            return List.of();
        }
        return request.getFormDataList().stream()
                .filter(item -> item != null && item.isEnabled() && CharSequenceUtil.isNotBlank(item.getKey()))
                .toList();
    }

    private static List<HttpFormUrlencoded> enabledUrlencoded(HttpRequestItem request) {
        if (request.getUrlencodedList() == null) {
            return List.of();
        }
        return request.getUrlencodedList().stream()
                .filter(item -> item != null && item.isEnabled() && CharSequenceUtil.isNotBlank(item.getKey()))
                .toList();
    }

    private static String urlWithParams(HttpRequestItem request) {
        return HttpUrlUtil.buildEncodedUrl(nullToEmpty(request.getUrl()), request.getParamsList());
    }

    private static boolean isGeneratedDefaultHeader(HttpHeader header) {
        for (HttpHeader generatedHeader : AppRequestHeaderDefaults.generatedHeaders()) {
            if (sameHeaderName(header.getKey(), generatedHeader.getKey())
                    && nullToEmpty(header.getValue()).equals(nullToEmpty(generatedHeader.getValue()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameHeaderName(String left, String right) {
        return nullToEmpty(left).trim().equalsIgnoreCase(nullToEmpty(right).trim());
    }

    private static String method(HttpRequestItem request) {
        return CharSequenceUtil.isBlank(request.getMethod()) ? "GET" : request.getMethod().trim().toUpperCase();
    }

    private static boolean methodRequiresBody(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private static String shellQuote(String value) {
        return "'" + nullToEmpty(value).replace("'", "'\\''") + "'";
    }

    private static String javaString(String value) {
        return "\"" + escapeCommon(value).replace("\n", "\\n") + "\"";
    }

    private static String jsString(String value) {
        return "\"" + escapeCommon(value).replace("\n", "\\n") + "\"";
    }

    private static String pyString(String value) {
        String escaped = nullToEmpty(value)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n");
        return "'" + escaped + "'";
    }

    private static String escapeCommon(String value) {
        return nullToEmpty(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
