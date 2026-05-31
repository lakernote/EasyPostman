package com.laker.postman.http.request;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpHeaders {

    public static String getIgnoreCase(HttpRequestItem item, String name) {
        if (item == null || item.getHeadersList() == null || name == null) {
            return null;
        }
        for (HttpHeader header : item.getHeadersList()) {
            if (header.isEnabled() && name.equalsIgnoreCase(header.getKey())) {
                return header.getValue();
            }
        }
        return null;
    }
}
