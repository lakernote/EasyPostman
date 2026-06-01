package com.laker.postman.request.compare;

import com.laker.postman.request.edit.HttpRequestBodyTypeResolver;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

record HttpRequestEditSnapshot(
        String id,
        String name,
        String description,
        String url,
        String method,
        RequestItemProtocolEnum protocol,
        List<HeaderEntry> headers,
        String bodyType,
        String body,
        List<KeyValueEntry> params,
        List<FormDataEntry> formData,
        List<KeyValueEntry> urlencoded,
        String authType,
        String authUsername,
        String authPassword,
        String authToken,
        Boolean followRedirects,
        Boolean cookieJarEnabled,
        String httpVersion,
        Integer requestTimeoutMs,
        String prescript,
        String postscript
) {
    static HttpRequestEditSnapshot from(HttpRequestItem item, List<HttpHeader> headers) {
        return new HttpRequestEditSnapshot(
                string(item.getId()),
                string(item.getName()),
                string(item.getDescription()),
                string(item.getUrl()),
                string(item.getMethod()),
                item.getProtocol() == null ? RequestItemProtocolEnum.HTTP : item.getProtocol(),
                headerEntries(headers),
                normalizeBodyType(item),
                string(item.getBody()),
                paramEntries(item.getParamsList()),
                formDataEntries(item.getFormDataList()),
                urlencodedEntries(item.getUrlencodedList()),
                normalizeAuthType(item.getAuthType()),
                string(item.getAuthUsername()),
                string(item.getAuthPassword()),
                string(item.getAuthToken()),
                item.getFollowRedirects(),
                normalizeCookieJarEnabled(item.getCookieJarEnabled()),
                normalizeHttpVersion(item.getHttpVersion()),
                item.getRequestTimeoutMs(),
                string(item.getPrescript()),
                string(item.getPostscript())
        );
    }

    private static List<HeaderEntry> headerEntries(List<HttpHeader> headers) {
        List<HeaderEntry> entries = new ArrayList<>();
        if (headers == null) {
            return entries;
        }
        for (HttpHeader header : headers) {
            if (header == null) {
                continue;
            }
            String key = string(header.getKey()).trim();
            if (key.isEmpty()) {
                continue;
            }
            entries.add(new HeaderEntry(header.isEnabled(), key.toLowerCase(Locale.ROOT), string(header.getValue()).trim()));
        }
        entries.sort(Comparator
                .comparing(HeaderEntry::key)
                .thenComparing(HeaderEntry::value)
                .thenComparing(HeaderEntry::enabled));
        return entries;
    }

    private static List<KeyValueEntry> paramEntries(List<HttpParam> params) {
        List<KeyValueEntry> entries = new ArrayList<>();
        if (params == null) {
            return entries;
        }
        for (HttpParam param : params) {
            if (param == null) {
                continue;
            }
            entries.add(new KeyValueEntry(param.isEnabled(), string(param.getKey()), string(param.getValue())));
        }
        return entries;
    }

    private static List<FormDataEntry> formDataEntries(List<HttpFormData> formData) {
        List<FormDataEntry> entries = new ArrayList<>();
        if (formData == null) {
            return entries;
        }
        for (HttpFormData item : formData) {
            if (item == null) {
                continue;
            }
            entries.add(new FormDataEntry(
                    item.isEnabled(),
                    string(item.getKey()),
                    HttpFormData.normalizeType(item.getType()),
                    string(item.getValue())
            ));
        }
        return entries;
    }

    private static List<KeyValueEntry> urlencodedEntries(List<HttpFormUrlencoded> urlencoded) {
        List<KeyValueEntry> entries = new ArrayList<>();
        if (urlencoded == null) {
            return entries;
        }
        for (HttpFormUrlencoded item : urlencoded) {
            if (item == null) {
                continue;
            }
            entries.add(new KeyValueEntry(item.isEnabled(), string(item.getKey()), string(item.getValue())));
        }
        return entries;
    }

    private static String normalizeBodyType(HttpRequestItem item) {
        String bodyType = HttpRequestBodyTypeResolver.resolveEditableBodyType(item);
        return bodyType == null || bodyType.trim().isEmpty()
                ? RequestBodyTypes.BODY_TYPE_NONE
                : bodyType.trim();
    }

    private static String normalizeAuthType(String authType) {
        return authType == null || authType.trim().isEmpty()
                ? AuthType.INHERIT.getConstant()
                : authType;
    }

    private static Boolean normalizeCookieJarEnabled(Boolean cookieJarEnabled) {
        return Boolean.TRUE.equals(cookieJarEnabled) ? null : cookieJarEnabled;
    }

    private static String normalizeHttpVersion(String httpVersion) {
        return httpVersion == null || httpVersion.trim().isEmpty()
                ? HttpRequestItem.HTTP_VERSION_AUTO
                : httpVersion.trim();
    }

    private static String string(String value) {
        return value == null ? "" : value;
    }

    record HeaderEntry(boolean enabled, String key, String value) {
    }

    record KeyValueEntry(boolean enabled, String key, String value) {
    }

    record FormDataEntry(boolean enabled, String key, String type, String value) {
    }
}
