package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.transport.HttpExchangeTerminalResponseFactory;
import lombok.experimental.UtilityClass;

@UtilityClass
class HttpRequestCancellationResponseSupport {

    static HttpResponse resolveTerminalResponse(PreparedRequest request,
                                                HttpResponse response,
                                                boolean workerCanceled,
                                                long requestStartMs,
                                                long endTimeMs) {
        if (response != null || !workerCanceled) {
            return response;
        }
        return HttpExchangeTerminalResponseFactory.fromCancellation(request, requestStartMs, endTimeMs);
    }
}
