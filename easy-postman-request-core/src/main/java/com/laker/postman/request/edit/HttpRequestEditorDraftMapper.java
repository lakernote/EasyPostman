package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class HttpRequestEditorDraftMapper {
    public static HttpRequestEditorDraft fromRequestItem(HttpRequestItem item) {
        if (item == null) {
            return null;
        }

        return HttpRequestEditorDraft.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .url(item.getUrl())
                .method(item.getMethod())
                .protocol(item.getProtocol())
                .headers(copyList(item.getHeadersList()))
                .params(resolveEditableParams(item))
                .bodyType(HttpRequestBodyTypeResolver.resolveEditableBodyType(item))
                .body(item.getBody())
                .formData(copyList(item.getFormDataList()))
                .urlencoded(copyList(item.getUrlencodedList()))
                .authType(item.getAuthType())
                .authUsername(item.getAuthUsername())
                .authPassword(item.getAuthPassword())
                .authToken(item.getAuthToken())
                .followRedirects(item.getFollowRedirects())
                .cookieJarEnabled(item.getCookieJarEnabled())
                .httpVersion(item.getHttpVersion())
                .requestTimeoutMs(item.getRequestTimeoutMs())
                .prescript(item.getPrescript())
                .postscript(item.getPostscript())
                .responses(copyList(item.getResponse()))
                .build();
    }

    public static HttpRequestEditorDraft fromSavedResponseOriginalRequest(SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null) {
            return null;
        }

        return HttpRequestEditorDraft.builder()
                .url(HttpUrlUtil.decodeQueryForDisplay(originalRequest.getUrl()))
                .method(originalRequest.getMethod())
                .headers(copyList(originalRequest.getHeaders()))
                .params(copyList(originalRequest.getParams()))
                .bodyType(originalRequest.getBodyType())
                .body(originalRequest.getBody())
                .formData(copyList(originalRequest.getFormDataList()))
                .urlencoded(copyList(originalRequest.getUrlencodedList()))
                .build();
    }

    public static HttpRequestItem toRequestItem(HttpRequestEditorDraft draft) {
        if (draft == null) {
            return null;
        }

        HttpRequestItem item = new HttpRequestItem();
        item.setId(string(draft.getId()));
        item.setName(string(draft.getName()));
        item.setDescription(string(draft.getDescription()));
        item.setUrl(string(draft.getUrl()));
        item.setMethod(string(draft.getMethod()));
        item.setProtocol(draft.getProtocol() == null ? RequestItemProtocolEnum.HTTP : draft.getProtocol());
        item.setHeadersList(copyList(draft.getHeaders()));
        item.setParamsList(copyList(draft.getParams()));

        String bodyType = normalizeBodyType(draft.getBodyType());
        item.setBodyType(bodyType);
        applyBody(item, draft, bodyType);

        item.setAuthType(draft.getAuthType());
        item.setAuthUsername(string(draft.getAuthUsername()));
        item.setAuthPassword(string(draft.getAuthPassword()));
        item.setAuthToken(string(draft.getAuthToken()));
        item.setFollowRedirects(draft.getFollowRedirects());
        item.setCookieJarEnabled(draft.getCookieJarEnabled());
        if (draft.getHttpVersion() != null) {
            item.setHttpVersion(draft.getHttpVersion());
        }
        item.setRequestTimeoutMs(draft.getRequestTimeoutMs());
        item.setPrescript(string(draft.getPrescript()));
        item.setPostscript(string(draft.getPostscript()));
        item.setResponse(copyList(draft.getResponses()));
        return item;
    }

    private static List<HttpParam> resolveEditableParams(HttpRequestItem item) {
        if (item.getParamsList() != null && !item.getParamsList().isEmpty()) {
            return copyList(item.getParamsList());
        }
        return HttpUrlUtil.parseQueryParams(item.getUrl());
    }

    private static void applyBody(HttpRequestItem item, HttpRequestEditorDraft draft, String bodyType) {
        if (RequestBodyTypes.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            item.setBody("");
            item.setFormDataList(copyList(draft.getFormData()));
            item.setUrlencodedList(new ArrayList<>());
            return;
        }
        if (RequestBodyTypes.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            item.setBody("");
            item.setFormDataList(new ArrayList<>());
            item.setUrlencodedList(copyList(draft.getUrlencoded()));
            return;
        }
        item.setBody(string(draft.getBody()));
        item.setFormDataList(new ArrayList<>());
        item.setUrlencodedList(new ArrayList<>());
    }

    private static String normalizeBodyType(String bodyType) {
        return bodyType == null || bodyType.trim().isEmpty()
                ? RequestBodyTypes.BODY_TYPE_NONE
                : bodyType.trim();
    }

    private static String string(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyList(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
