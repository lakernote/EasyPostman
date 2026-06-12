package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class HttpRequestEditorDraftMapperTest {

    @Test
    public void shouldMapCommonEditableFieldsToRequestItem() {
        SavedResponse response = new SavedResponse();
        response.setId("response-1");

        HttpRequestEditorDraft draft = HttpRequestEditorDraft.builder()
                .id("request-1")
                .name("Create User")
                .description("docs")
                .url("https://api.example.com/users")
                .method("POST")
                .protocol(RequestItemProtocolEnum.HTTP)
                .headers(List.of(new HttpHeader(true, "Content-Type", "application/json")))
                .pathVariables(List.of(new HttpParam(true, "userId", "42", "User id")))
                .params(List.of(new HttpParam(true, "verbose", "true")))
                .bodyType(RequestBodyTypes.BODY_TYPE_RAW)
                .body("{\"name\":\"easy\"}")
                .authType("Bearer Token")
                .authToken("token")
                .followRedirects(Boolean.FALSE)
                .cookieJarEnabled(Boolean.FALSE)
                .proxyPolicy(HttpRequestProxyPolicy.NO_PROXY)
                .httpVersion(HttpRequestItem.HTTP_VERSION_HTTP_2)
                .requestTimeoutMs(3000)
                .prescript("pre")
                .postscript("post")
                .responses(List.of(response))
                .build();

        HttpRequestItem item = HttpRequestEditorDraftMapper.toRequestItem(draft);

        assertEquals(item.getId(), "request-1");
        assertEquals(item.getName(), "Create User");
        assertEquals(item.getDescription(), "docs");
        assertEquals(item.getUrl(), "https://api.example.com/users");
        assertEquals(item.getMethod(), "POST");
        assertEquals(item.getProtocol(), RequestItemProtocolEnum.HTTP);
        assertEquals(item.getHeadersList().size(), 1);
        assertEquals(item.getPathVariablesList().size(), 1);
        assertEquals(item.getPathVariablesList().get(0).getKey(), "userId");
        assertEquals(item.getParamsList().size(), 1);
        assertEquals(item.getBodyType(), RequestBodyTypes.BODY_TYPE_RAW);
        assertEquals(item.getBody(), "{\"name\":\"easy\"}");
        assertEquals(item.getAuthType(), "Bearer Token");
        assertEquals(item.getAuthToken(), "token");
        assertEquals(item.getFollowRedirects(), Boolean.FALSE);
        assertEquals(item.getCookieJarEnabled(), Boolean.FALSE);
        HttpRequestProxyPolicy storedPolicy = item.getProxyPolicy();
        assertEquals(item.getProxyPolicy(), HttpRequestProxyPolicy.NO_PROXY);
        assertEquals(item.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_2);
        assertEquals(item.getRequestTimeoutMs(), Integer.valueOf(3000));
        assertEquals(item.getPrescript(), "pre");
        assertEquals(item.getPostscript(), "post");
        assertEquals(item.getResponse().get(0).getId(), "response-1");
    }

    @Test
    public void shouldKeepOnlyFormDataWhenBodyTypeIsFormData() {
        HttpRequestEditorDraft draft = HttpRequestEditorDraft.builder()
                .bodyType(RequestBodyTypes.BODY_TYPE_FORM_DATA)
                .body("ignored")
                .formData(List.of(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, "/tmp/a.txt")))
                .urlencoded(List.of(new HttpFormUrlencoded(true, "ignored", "ignored")))
                .build();

        HttpRequestItem item = HttpRequestEditorDraftMapper.toRequestItem(draft);

        assertEquals(item.getBody(), "");
        assertEquals(item.getFormDataList().size(), 1);
        assertTrue(item.getUrlencodedList().isEmpty());
    }

    @Test
    public void shouldKeepOnlyUrlencodedWhenBodyTypeIsUrlencoded() {
        HttpRequestEditorDraft draft = HttpRequestEditorDraft.builder()
                .bodyType(RequestBodyTypes.BODY_TYPE_FORM_URLENCODED)
                .body("ignored")
                .formData(List.of(new HttpFormData(true, "ignored", HttpFormData.TYPE_TEXT, "ignored")))
                .urlencoded(List.of(new HttpFormUrlencoded(true, "name", "easy")))
                .build();

        HttpRequestItem item = HttpRequestEditorDraftMapper.toRequestItem(draft);

        assertEquals(item.getBody(), "");
        assertTrue(item.getFormDataList().isEmpty());
        assertEquals(item.getUrlencodedList().size(), 1);
    }

    @Test
    public void shouldCreateDraftFromRequestWithoutMutatingSource() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-2");
        item.setName("Submit Form");
        item.setUrl("https://api.example.com/search?q=easy&page=1");
        item.setMethod("POST");
        item.setPathVariablesList(List.of(new HttpParam(true, "userId", "42")));
        item.setProxyPolicy(HttpRequestProxyPolicy.USE_PROXY);
        item.setBody("name=easy");
        item.setHeadersList(List.of(new HttpHeader(true, "Content-Type", "application/x-www-form-urlencoded")));

        HttpRequestEditorDraft draft = HttpRequestEditorDraftMapper.fromRequestItem(item);

        assertEquals(draft.getId(), "request-2");
        assertEquals(draft.getProxyPolicy(), HttpRequestProxyPolicy.USE_PROXY);
        assertEquals(draft.getPathVariables().size(), 1);
        assertEquals(draft.getPathVariables().get(0).getKey(), "userId");
        assertEquals(draft.getParams().size(), 2);
        assertEquals(draft.getBodyType(), RequestBodyTypes.BODY_TYPE_FORM_URLENCODED);
        assertTrue(item.getParamsList().isEmpty(), "Opening a request must not materialize URL params into the source item");
        assertEquals(item.getBodyType(), "", "Opening a request must not write inferred body type back to the source item");
    }

    @Test
    public void shouldPreferStoredBodyTypeWhenCreatingDraft() {
        HttpRequestItem item = new HttpRequestItem();
        item.setMethod("POST");
        item.setBodyType(RequestBodyTypes.BODY_TYPE_RAW);
        item.setHeadersList(List.of(new HttpHeader(true, "Content-Type", "multipart/form-data")));

        HttpRequestEditorDraft draft = HttpRequestEditorDraftMapper.fromRequestItem(item);

        assertEquals(draft.getBodyType(), RequestBodyTypes.BODY_TYPE_RAW);
    }

    @Test
    public void shouldCreateDraftFromSavedResponseOriginalRequest() {
        SavedResponse.OriginalRequest originalRequest = new SavedResponse.OriginalRequest();
        originalRequest.setMethod("POST");
        originalRequest.setUrl("https://api.example.com/search?q=hello%20world");
        originalRequest.setHeaders(List.of(new HttpHeader(true, "Content-Type", "application/json")));
        originalRequest.setParams(List.of(new HttpParam(true, "page", "1")));
        originalRequest.setBodyType(RequestBodyTypes.BODY_TYPE_RAW);
        originalRequest.setBody("{\"ok\":true}");

        HttpRequestEditorDraft draft = HttpRequestEditorDraftMapper.fromSavedResponseOriginalRequest(originalRequest);

        assertEquals(draft.getUrl(), "https://api.example.com/search?q=hello world");
        assertEquals(draft.getMethod(), "POST");
        assertEquals(draft.getHeaders().size(), 1);
        assertEquals(draft.getParams().size(), 1);
        assertEquals(draft.getBodyType(), RequestBodyTypes.BODY_TYPE_RAW);
        assertEquals(draft.getBody(), "{\"ok\":true}");
        assertTrue(draft.getFormData().isEmpty());
        assertTrue(draft.getUrlencoded().isEmpty());
    }
}
