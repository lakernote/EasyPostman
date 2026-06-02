package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class CookieHeaderMergeNetworkInterceptor implements Interceptor {
    private final PreparedRequest preparedRequest;

    CookieHeaderMergeNetworkInterceptor(PreparedRequest preparedRequest) {
        this.preparedRequest = preparedRequest;
    }

    static boolean hasEnabledExplicitCookieHeader(PreparedRequest preparedRequest) {
        return !isBlank(explicitCookieHeader(preparedRequest));
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(mergeExplicitCookieHeader(chain.request()));
    }

    private Request mergeExplicitCookieHeader(Request request) {
        String explicitCookie = explicitCookieHeader(preparedRequest);
        if (isBlank(explicitCookie)) {
            return request;
        }

        String actualCookie = String.join("; ", request.headers("Cookie"));
        String mergedCookie = mergeCookieHeaders(explicitCookie, actualCookie);
        if (isBlank(mergedCookie) || mergedCookie.equals(actualCookie)) {
            return request;
        }
        return request.newBuilder()
                .removeHeader("Cookie")
                .header("Cookie", mergedCookie)
                .build();
    }

    private static String explicitCookieHeader(PreparedRequest preparedRequest) {
        if (preparedRequest == null || preparedRequest.headersList == null || preparedRequest.headersList.isEmpty()) {
            return "";
        }
        StringBuilder cookie = new StringBuilder();
        for (HttpHeader header : preparedRequest.headersList) {
            if (header == null || !header.isEnabled() || header.getKey() == null
                    || !"Cookie".equalsIgnoreCase(header.getKey())) {
                continue;
            }
            if (!cookie.isEmpty()) {
                cookie.append("; ");
            }
            cookie.append(header.getValue() == null ? "" : header.getValue());
        }
        return cookie.toString();
    }

    private String mergeCookieHeaders(String explicitCookie, String actualCookie) {
        List<CookiePair> explicitPairs = parseCookiePairs(explicitCookie);
        List<CookiePair> actualPairs = parseCookiePairs(actualCookie);
        Set<String> actualNames = new HashSet<>();
        for (CookiePair pair : actualPairs) {
            actualNames.add(pair.name().toLowerCase(Locale.ROOT));
        }

        List<CookiePair> cookies = new ArrayList<>(explicitPairs.size() + actualPairs.size());
        for (CookiePair pair : explicitPairs) {
            if (!actualNames.contains(pair.name().toLowerCase(Locale.ROOT))) {
                cookies.add(pair);
            }
        }
        cookies.addAll(actualPairs);

        StringBuilder merged = new StringBuilder();
        for (CookiePair pair : cookies) {
            if (!merged.isEmpty()) {
                merged.append("; ");
            }
            merged.append(pair.name()).append("=").append(pair.value());
        }
        return merged.toString();
    }

    private List<CookiePair> parseCookiePairs(String cookieHeader) {
        List<CookiePair> cookies = new ArrayList<>();
        if (isBlank(cookieHeader)) {
            return cookies;
        }
        for (String part : cookieHeader.split(";")) {
            String pair = part.trim();
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String name = pair.substring(0, separatorIndex).trim();
            if (name.isEmpty()) {
                continue;
            }
            cookies.add(new CookiePair(name, pair.substring(separatorIndex + 1).trim()));
        }
        return cookies;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record CookiePair(String name, String value) {
    }
}
