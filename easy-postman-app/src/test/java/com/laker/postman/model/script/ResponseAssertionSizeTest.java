package com.laker.postman.model.script;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.service.js.api.ResponseAssertion;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

public class ResponseAssertionSizeTest {

    @Test
    public void shouldUseCollectedBodySizeWhenBodyTextIsSkipped() {
        HttpResponse response = new HttpResponse();
        response.body = "response body skipped";
        response.bodySize = 128;

        ResponseAssertion.ResponseSize size = new ResponseAssertion(response).size();

        assertEquals(size.body, 128);
    }

    @Test
    public void shouldUseZeroCollectedBodySizeWhenSkippedBodyPlaceholderIsPresent() {
        HttpResponse response = new HttpResponse();
        response.bodySize = 0;
        response.body = I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_SKIPPED_PERFORMANCE, response.bodySize);

        ResponseAssertion.ResponseSize size = new ResponseAssertion(response).size();

        assertEquals(size.body, 0);
    }
}
