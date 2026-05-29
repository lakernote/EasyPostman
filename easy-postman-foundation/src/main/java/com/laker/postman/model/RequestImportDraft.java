package com.laker.postman.model;

import java.util.List;

public record RequestImportDraft(
        String id,
        String name,
        String url,
        String method,
        RequestImportProtocol protocol,
        List<RequestImportHeader> headers,
        String description,
        String bodyType,
        String body
) {

    public RequestImportDraft {
        id = normalize(id);
        name = normalize(name);
        url = normalize(url);
        method = normalize(method);
        if (method.isBlank()) {
            method = "GET";
        }
        protocol = protocol == null ? RequestImportProtocol.HTTP : protocol;
        headers = headers == null ? List.of() : List.copyOf(headers);
        description = normalize(description);
        bodyType = normalize(bodyType);
        if (bodyType.isBlank()) {
            bodyType = RequestImportBodyTypes.NONE;
        }
        body = normalize(body);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
