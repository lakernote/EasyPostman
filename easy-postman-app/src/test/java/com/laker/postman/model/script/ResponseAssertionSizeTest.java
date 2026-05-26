package com.laker.postman.model.script;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

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
