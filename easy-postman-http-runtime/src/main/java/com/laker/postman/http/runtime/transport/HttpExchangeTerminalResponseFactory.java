package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.error.NetworkErrorMessageResolver;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSupport;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpExchangeTerminalResponseFactory {
    private static final String REQUEST_CANCELED = "Request canceled";

    public static HttpResponse fromFailure(PreparedRequest request,
                                           Throwable throwable,
                                           long requestStartMs,
                                           long endTimeMs) {
        String errorMessage = resolveErrorMessage(throwable);
        HttpResponse response = buildBaseResponse(request, requestStartMs, endTimeMs);
        HttpEventInfo eventInfo = response.httpEventInfo;
        eventInfo.setErrorMessage(errorMessage);
        eventInfo.setError(throwable);
        if (eventInfo.getCallFailed() <= 0) {
            eventInfo.setCallFailed(endTimeMs);
        }

        NetworkLogSupport.append(request, NetworkLogEventStage.FAILED,
                buildLogMessage(request, "failed", errorMessage), response.costMs);
        return response;
    }

    public static HttpResponse fromCancellation(PreparedRequest request,
                                                long requestStartMs,
                                                long endTimeMs) {
        HttpResponse response = buildBaseResponse(request, requestStartMs, endTimeMs);
        HttpEventInfo eventInfo = response.httpEventInfo;
        eventInfo.setError(null);
        eventInfo.setErrorMessage(REQUEST_CANCELED);
        eventInfo.setCallFailed(0L);
        boolean firstCancellation = eventInfo.getCanceled() <= 0;
        if (firstCancellation) {
            eventInfo.setCanceled(endTimeMs);
            NetworkLogSupport.append(request, NetworkLogEventStage.CANCELED,
                    buildLogMessage(request, "canceled", REQUEST_CANCELED), response.costMs);
        }
        return response;
    }

    private static HttpResponse buildBaseResponse(PreparedRequest request,
                                                  long requestStartMs,
                                                  long endTimeMs) {
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
        if (eventInfo.getThreadName() == null || eventInfo.getThreadName().isBlank()) {
            eventInfo.setThreadName(Thread.currentThread().getName());
        }

        response.httpEventInfo = eventInfo;
        response.threadName = eventInfo.getThreadName();
        response.protocol = eventInfo.getProtocol();
        response.costMs = resolveCostMs(eventInfo, endTimeMs);
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

    private static String buildLogMessage(PreparedRequest request, String action, String message) {
        if (request == null) {
            return message;
        }
        String method = request.method == null || request.method.isBlank() ? "HTTP" : request.method;
        String url = request.url == null ? "" : request.url;
        return method + " " + url + " " + action + ": " + message;
    }
}
