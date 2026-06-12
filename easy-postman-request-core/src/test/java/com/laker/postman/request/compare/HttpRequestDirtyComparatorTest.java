package com.laker.postman.request.compare;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.defaults.HttpRequestDefaults;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import com.laker.postman.request.model.RequestBodyTypes;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HttpRequestDirtyComparatorTest {
    private static final String USER_AGENT = "EasyPostman/test";
    private static final List<HttpHeader> DEFAULT_HEADERS = HttpRequestDefaults.standardHttpHeaders(USER_AGENT);

    @Test
    public void shouldTreatUiAppliedDefaultSettingsAsUnmodified() {
        HttpRequestItem original = baseRequest();

        HttpRequestItem current = baseRequest();
        current.setCookieJarEnabled(Boolean.TRUE);
        current.setHttpVersion(HttpRequestItem.HTTP_VERSION_AUTO);

        assertFalse(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatGeneratedDefaultHeadersAbsentFromOriginalAsUnmodified() {
        HttpRequestItem original = baseRequest();

        HttpRequestItem current = baseRequest();
        current.setHeadersList(DEFAULT_HEADERS);

        assertFalse(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatChangedDefaultHeaderAsModified() {
        HttpRequestItem original = baseRequest();

        HttpRequestItem current = baseRequest();
        current.setHeadersList(List.of(new HttpHeader(true, HttpRequestDefaults.USER_AGENT, "custom-client")));

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatDeletingPersistedDefaultHeaderAsModified() {
        HttpRequestItem original = baseRequest();
        original.setHeadersList(List.of(new HttpHeader(true, HttpRequestDefaults.ACCEPT, HttpRequestDefaults.ACCEPT_ANY)));

        HttpRequestItem current = baseRequest();

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatUiSortedHeadersAsUnmodified() {
        HttpRequestItem original = baseRequest();
        original.setHeadersList(List.of(
                new HttpHeader(true, HttpRequestDefaults.ACCEPT, "application/json, text/plain, */*"),
                new HttpHeader(true, "Accept-Language", "zh-CN,zh;q=0.9"),
                new HttpHeader(true, HttpRequestDefaults.CONNECTION, HttpRequestDefaults.CONNECTION_VALUE),
                new HttpHeader(true, "Referer", "http://aaxx.uk.cn:29000/mallpc/"),
                new HttpHeader(true, HttpRequestDefaults.USER_AGENT, "Mozilla/5.0"),
                new HttpHeader(true, "token", "0471570d4cf243f881e17aa148859aad")
        ));

        HttpRequestItem current = baseRequest();
        current.setHeadersList(List.of(
                new HttpHeader(true, HttpRequestDefaults.USER_AGENT, "Mozilla/5.0"),
                new HttpHeader(true, HttpRequestDefaults.ACCEPT, "application/json, text/plain, */*"),
                new HttpHeader(true, HttpRequestDefaults.ACCEPT_ENCODING, HttpRequestDefaults.ACCEPT_ENCODING_VALUE),
                new HttpHeader(true, HttpRequestDefaults.CONNECTION, HttpRequestDefaults.CONNECTION_VALUE),
                new HttpHeader(true, "Accept-Language", "zh-CN,zh;q=0.9"),
                new HttpHeader(true, "Referer", "http://aaxx.uk.cn:29000/mallpc/"),
                new HttpHeader(true, "token", "0471570d4cf243f881e17aa148859aad")
        ));

        assertFalse(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatInferredRawBodyTypeAsUnmodified() {
        HttpRequestItem original = baseRequest();
        original.setMethod("POST");
        original.setBodyType("");
        original.setBody("{\"pageNum\":1}");
        original.setHeadersList(List.of(new HttpHeader(true, "Content-Type", "application/json")));

        HttpRequestItem current = baseRequest();
        current.setMethod("POST");
        current.setBodyType(RequestBodyTypes.BODY_TYPE_RAW);
        current.setBody("{\"pageNum\":1}");
        current.setHeadersList(List.of(new HttpHeader(true, "Content-Type", "application/json")));

        assertFalse(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldIgnoreSavedResponses() {
        HttpRequestItem original = baseRequest();
        original.setResponse(null);

        HttpRequestItem current = baseRequest();

        assertFalse(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldKeepExplicitRequestSettingsAsModified() {
        HttpRequestItem original = baseRequest();

        HttpRequestItem current = baseRequest();
        current.setFollowRedirects(Boolean.TRUE);
        current.setProxyPolicy(HttpRequestProxyPolicy.NO_PROXY);
        current.setRequestTimeoutMs(5000);

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatExplicitProxyPolicyAsModified() {
        HttpRequestItem original = baseRequest();

        HttpRequestItem current = baseRequest();
        current.setProxyPolicy(HttpRequestProxyPolicy.NO_PROXY);

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatChangedHeaderDescriptionAsModified() {
        HttpRequestItem original = baseRequest();
        original.setHeadersList(List.of(new HttpHeader(true, "X-Trace", "1", "old description")));

        HttpRequestItem current = baseRequest();
        current.setHeadersList(List.of(new HttpHeader(true, "X-Trace", "1", "new description")));

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatChangedParamDescriptionAsModified() {
        HttpRequestItem original = baseRequest();
        original.setParamsList(List.of(new HttpParam(true, "page", "1", "old description")));

        HttpRequestItem current = baseRequest();
        current.setParamsList(List.of(new HttpParam(true, "page", "1", "new description")));

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatChangedPathVariableAsModified() {
        HttpRequestItem original = baseRequest();
        original.setUrl("https://api.example.com/users/:userId");
        original.setPathVariablesList(List.of(new HttpParam(true, "userId", "42")));

        HttpRequestItem current = baseRequest();
        current.setUrl("https://api.example.com/users/:userId");
        current.setPathVariablesList(List.of(new HttpParam(true, "userId", "43")));

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatChangedFormDataDescriptionAsModified() {
        HttpRequestItem original = baseRequest();
        original.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_DATA);
        original.setFormDataList(List.of(new HttpFormData(
                true,
                "file",
                HttpFormData.TYPE_FILE,
                "/tmp/a.txt",
                "old description"
        )));

        HttpRequestItem current = baseRequest();
        current.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_DATA);
        current.setFormDataList(List.of(new HttpFormData(
                true,
                "file",
                HttpFormData.TYPE_FILE,
                "/tmp/a.txt",
                "new description"
        )));

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    @Test
    public void shouldTreatChangedUrlencodedDescriptionAsModified() {
        HttpRequestItem original = baseRequest();
        original.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_URLENCODED);
        original.setUrlencodedList(List.of(new HttpFormUrlencoded(true, "token", "abc", "old description")));

        HttpRequestItem current = baseRequest();
        current.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_URLENCODED);
        current.setUrlencodedList(List.of(new HttpFormUrlencoded(true, "token", "abc", "new description")));

        assertTrue(HttpRequestDirtyComparator.isDirty(original, current, DEFAULT_HEADERS));
    }

    private static HttpRequestItem baseRequest() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-1");
        item.setName("Request 1");
        item.setUrl("https://api.example.com");
        item.setMethod("GET");
        return item;
    }
}
