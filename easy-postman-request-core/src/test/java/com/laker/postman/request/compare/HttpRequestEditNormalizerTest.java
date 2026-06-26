package com.laker.postman.request.compare;

import com.laker.postman.request.defaults.HttpRequestDefaults;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class HttpRequestEditNormalizerTest {

    @Test
    public void shouldDropUiGeneratedDefaultHeadersOnlyFromCurrentRequest() {
        HttpRequestItem original = request();

        HttpRequestItem current = request();
        current.setHeadersList(List.of(
                new HttpHeader(true, HttpRequestDefaults.USER_AGENT, "EasyPostman/test"),
                new HttpHeader(true, "X-Trace", "1")
        ));

        HttpRequestEditNormalizer.NormalizedRequest normalizedOriginal = HttpRequestEditNormalizer.original(original);
        HttpRequestEditNormalizer.NormalizedRequest normalizedCurrent = HttpRequestEditNormalizer.currentComparedToOriginal(
                current,
                normalizedOriginal,
                List.of(new HttpHeader(true, HttpRequestDefaults.USER_AGENT, "EasyPostman/test"))
        );

        assertEquals(normalizedOriginal.headers(), List.of());
        assertEquals(normalizedCurrent.headers(), List.of(new HttpHeader(true, "X-Trace", "1", "")));
    }

    @Test
    public void shouldDropUiGeneratedUrlQueryParamsOnlyFromCurrentRequest() {
        HttpRequestItem original = request();
        original.setUrl("https://metrics.o.webex.com/api/ds/query?ds_type=prometheus&requestId=SQR106");

        HttpRequestItem current = request();
        current.setUrl("https://metrics.o.webex.com/api/ds/query?ds_type=prometheus&requestId=SQR106");
        current.setParamsList(List.of(
                new HttpParam(true, "ds_type", "prometheus"),
                new HttpParam(true, "requestId", "SQR106"),
                new HttpParam(true, "manual", "kept")
        ));

        HttpRequestEditNormalizer.NormalizedRequest normalizedOriginal = HttpRequestEditNormalizer.original(original);
        HttpRequestEditNormalizer.NormalizedRequest normalizedCurrent = HttpRequestEditNormalizer.currentComparedToOriginal(
                current,
                normalizedOriginal,
                List.of()
        );

        assertEquals(normalizedOriginal.params(), List.of());
        assertEquals(normalizedCurrent.params(), List.of(new HttpParam(true, "manual", "kept", "")));
    }

    @Test
    public void shouldKeepEditedUrlQueryParamAsCurrentRequestChange() {
        HttpRequestItem original = request();
        original.setUrl("https://metrics.o.webex.com/api/ds/query?ds_type=prometheus&requestId=SQR106");

        HttpRequestItem current = request();
        current.setUrl("https://metrics.o.webex.com/api/ds/query?ds_type=prometheus&requestId=SQR106");
        current.setParamsList(List.of(
                new HttpParam(true, "ds_type", "prometheus"),
                new HttpParam(true, "requestId", "SQR107")
        ));

        HttpRequestEditNormalizer.NormalizedRequest normalizedOriginal = HttpRequestEditNormalizer.original(original);
        HttpRequestEditNormalizer.NormalizedRequest normalizedCurrent = HttpRequestEditNormalizer.currentComparedToOriginal(
                current,
                normalizedOriginal,
                List.of()
        );

        assertEquals(normalizedCurrent.params(), List.of(new HttpParam(true, "requestId", "SQR107", "")));
    }

    private static HttpRequestItem request() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-1");
        item.setName("Request 1");
        item.setUrl("https://api.example.com");
        return item;
    }
}
