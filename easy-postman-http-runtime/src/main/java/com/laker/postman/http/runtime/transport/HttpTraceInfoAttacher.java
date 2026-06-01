package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.okhttp.OkHttpExchangeEventListener;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpTraceInfoAttacher {

    public static void attachTraceInfo(HttpResponse httpResponse, long startTime) {
        HttpEventInfo httpEventInfo = OkHttpExchangeEventListener.getAndRemove();
        if (httpEventInfo != null) {
            httpEventInfo.setQueueStart(startTime);
            if (httpEventInfo.getCallStart() > 0) {
                httpEventInfo.setQueueingCost(httpEventInfo.getCallStart() - startTime);
            }
            if (httpEventInfo.getConnectStart() > 0 && httpEventInfo.getCallStart() > 0) {
                httpEventInfo.setStalledCost(httpEventInfo.getConnectStart() - httpEventInfo.getCallStart());
            }
        }
        httpResponse.httpEventInfo = httpEventInfo;
    }

    static long resolveResponseReceivedEndTime(HttpResponse httpResponse, long fallbackEndTime) {
        if (httpResponse == null || httpResponse.httpEventInfo == null) {
            return fallbackEndTime;
        }
        long responseEnd = Math.max(
                httpResponse.httpEventInfo.getResponseBodyEnd(),
                Math.max(httpResponse.httpEventInfo.getCallEnd(), httpResponse.httpEventInfo.getResponseHeadersEnd())
        );
        return responseEnd > 0 ? Math.max(httpResponse.httpEventInfo.getQueueStart(), responseEnd) : fallbackEndTime;
    }
}
