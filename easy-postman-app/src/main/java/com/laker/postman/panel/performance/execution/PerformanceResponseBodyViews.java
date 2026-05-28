package com.laker.postman.panel.performance.execution;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
final class PerformanceResponseBodyViews {

    String bodyForBodyBasedNode(HttpResponse response) {
        String responseBody = response != null && response.body != null ? response.body : "";
        if (response != null && response.isSse) {
            return extractLastSseDataPayload(responseBody);
        }
        return responseBody;
    }

    String extractLastSseDataPayload(String responseBody) {
        if (CharSequenceUtil.isBlank(responseBody)) {
            return responseBody;
        }
        StringBuilder currentEventData = null;
        String lastEventData = null;
        String[] lines = responseBody.split("\\R", -1);
        for (String line : lines) {
            if (line.isBlank()) {
                if (currentEventData != null) {
                    lastEventData = currentEventData.toString();
                    currentEventData = null;
                }
                continue;
            }
            if (!line.startsWith("data:")) {
                continue;
            }
            String dataLine = line.length() > 5 && line.charAt(5) == ' '
                    ? line.substring(6)
                    : line.substring(5);
            if (currentEventData == null) {
                currentEventData = new StringBuilder();
            } else {
                currentEventData.append('\n');
            }
            currentEventData.append(dataLine);
        }
        if (currentEventData != null) {
            lastEventData = currentEventData.toString();
        }
        return lastEventData == null ? responseBody : lastEventData;
    }
}
