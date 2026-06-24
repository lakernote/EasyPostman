package com.laker.postman.http.request;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HttpRequestValidatorTest {

    @Test
    public void shouldReturnConfirmationResultForGetRequestWithBody() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://example.com";
        preparedRequest.method = "GET";
        preparedRequest.body = "{\"name\":\"easy\"}";

        HttpRequestItem item = new HttpRequestItem();
        item.setBody(preparedRequest.body);

        HttpRequestValidationResult result = HttpRequestValidator.validate(preparedRequest, item);

        assertTrue(result.isValid());
        assertTrue(result.requiresConfirmation());
        assertFalse(result.isWarning());
        assertEquals(result.getMessage(), I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_GET_BODY_CONFIRM));
    }

    @Test
    public void shouldRejectMissingBinaryBodyFile() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://example.com/upload";
        preparedRequest.method = "PUT";
        preparedRequest.bodyType = RequestBodyTypes.BODY_TYPE_BINARY;
        preparedRequest.body = "/path/to/missing.bin";

        HttpRequestValidationResult result = HttpRequestValidator.validate(preparedRequest, new HttpRequestItem());

        assertFalse(result.isValid());
        assertEquals(result.getMessage(), I18nUtil.getMessage(
                MessageKeys.REQUEST_VALIDATION_BINARY_FILE_NOT_FOUND,
                preparedRequest.body
        ));
    }

    @Test
    public void shouldRejectBlankBinaryBodyFile() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://example.com/upload";
        preparedRequest.method = "PUT";
        preparedRequest.bodyType = RequestBodyTypes.BODY_TYPE_BINARY;
        preparedRequest.body = " ";

        HttpRequestValidationResult result = HttpRequestValidator.validate(preparedRequest, new HttpRequestItem());

        assertFalse(result.isValid());
        assertEquals(result.getMessage(), I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_BINARY_FILE_REQUIRED));
    }
}
