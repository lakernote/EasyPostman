package com.laker.postman.service.curl;

import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpHeader;

import java.util.ArrayList;
import java.util.List;

class CurlCommandOptions {
    String url;
    String method;
    boolean forceGet;
    boolean followRedirects;
    String authType;
    String authUsername;
    String authPassword;
    String binaryDataFilePath;
    final List<HttpHeader> headersList = new ArrayList<>();
    final List<String> dataParams = new ArrayList<>();
    final List<String> dataUrlencodeParams = new ArrayList<>();
    final List<HttpFormData> formDataList = new ArrayList<>();
    final List<CurlParseWarning> warnings = new ArrayList<>();

    void addHeader(HttpHeader header) {
        if (header != null) {
            headersList.add(header);
        }
    }

    void setHeader(String headerName, String headerValue) {
        CurlHeaderSupport.setHeader(headersList, headerName, headerValue);
    }

    void addCookieHeader(String cookieValue) {
        headersList.add(new HttpHeader(true, "Cookie", cookieValue));
    }

    void applyUserOption(String userOption) {
        if (userOption == null) {
            return;
        }
        int separator = userOption.indexOf(':');
        if (separator < 0) {
            authUsername = userOption;
            authPassword = "";
            return;
        }
        authUsername = userOption.substring(0, separator);
        authPassword = userOption.substring(separator + 1);
    }

    void addWarning(String code, String message) {
        warnings.add(new CurlParseWarning(code, message));
    }

    boolean hasDataParams() {
        return !dataParams.isEmpty()
                || !dataUrlencodeParams.isEmpty()
                || (binaryDataFilePath != null && !binaryDataFilePath.isBlank());
    }

    boolean hasFormData() {
        return !formDataList.isEmpty();
    }
}
