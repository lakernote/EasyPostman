package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.OkHttpRequestBuilder;
import lombok.experimental.UtilityClass;
import okhttp3.Request;

@UtilityClass
class PreparedOkHttpRequestFactory {

    static Request build(PreparedRequest req) {
        if (req.isMultipart) {
            return OkHttpRequestBuilder.buildMultipartRequest(req);
        }
        if (req.urlencodedList != null && !req.urlencodedList.isEmpty()) {
            return OkHttpRequestBuilder.buildFormRequest(req);
        }
        return OkHttpRequestBuilder.buildRequest(req);
    }
}
