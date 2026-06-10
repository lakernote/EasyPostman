package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.error.NetworkErrorMessageResolver;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSupport;
import com.laker.postman.http.runtime.transport.HttpExchangeTraceSupport;
import lombok.experimental.UtilityClass;

@UtilityClass
class HttpRequestFailureResponseFactory {

    static HttpResponse fromException(PreparedRequest request,
                                      Throwable throwable,
                                      long requestStartMs,
                                      long endTimeMs) {
        String errorMessage = resolveErrorMessage(throwable);
        HttpResponse response = new HttpResponse();
        response.code = 0;
        response.body = "";
        response.bodySize = 0L;
        response.endTime = endTimeMs;

        HttpEventInfo eventInfo = HttpExchangeTraceSupport.resolveFromRequest(request);
        if (eventInfo == null) {
            eventInfo = new HttpEventInfo();
            eventInfo.setQueueStart(requestStartMs);
        } else if (eventInfo.getQueueStart() <= 0 && requestStartMs > 0) {
            eventInfo.setQueueStart(requestStartMs);
        }
        eventInfo.setErrorMessage(errorMessage);
        eventInfo.setError(throwable);
        if (eventInfo.getCallFailed() <= 0) {
            eventInfo.setCallFailed(endTimeMs);
        }
        if (eventInfo.getThreadName() == null || eventInfo.getThreadName().isBlank()) {
            eventInfo.setThreadName(Thread.currentThread().getName());
        }

        response.httpEventInfo = eventInfo;
        response.threadName = eventInfo.getThreadName();
        response.protocol = eventInfo.getProtocol();
        response.costMs = resolveCostMs(eventInfo, endTimeMs);

        NetworkLogSupport.append(request, NetworkLogEventStage.FAILED, buildLogMessage(request, errorMessage), response.costMs);
        return response;
    }

    private static String resolveErrorMessage(Throwable throwable) {
        String message = NetworkErrorMessageResolver.toUserFriendlyMessage(throwable);
        if (message != null && !message.isBlank()) {
            return message;
        }
        return throwable == null ? "Request failed" : throwable.getClass().getSimpleName();
    }

    private static long resolveCostMs(HttpEventInfo eventInfo, long endTimeMs) {
        long start = eventInfo.getQueueStart() > 0 ? eventInfo.getQueueStart() : eventInfo.getCallStart();
        return start > 0 ? Math.max(0L, endTimeMs - start) : 0L;
    }

    private static String buildLogMessage(PreparedRequest request, String errorMessage) {
        if (request == null) {
            return errorMessage;
        }
        String method = request.method == null || request.method.isBlank() ? "HTTP" : request.method;
        String url = request.url == null ? "" : request.url;
        return method + " " + url + " failed: " + errorMessage;
    }
}
