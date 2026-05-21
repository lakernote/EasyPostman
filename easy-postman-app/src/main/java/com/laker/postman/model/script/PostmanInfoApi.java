package com.laker.postman.model.script;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.variable.IterationInfoService;

/**
 * Request metadata exposed as pm.info.
 */
public class PostmanInfoApi {

    public String eventName;
    public int iteration;
    public int iterationCount;
    public String requestName;
    public String requestId;
    public int wsSendIndex;
    public int wsSendCount;
    public String wsStepName;

    public PostmanInfoApi(IterationInfoService.IterationInfo iterationInfo) {
        IterationInfoService.IterationInfo info = iterationInfo != null
                ? iterationInfo
                : IterationInfoService.getInstance().getCurrentInfo();
        this.iteration = info.iteration();
        this.iterationCount = info.iterationCount();
        this.eventName = "prerequest";
        this.requestName = "";
        this.requestId = "";
        clearWebSocketSendInfo();
    }

    public void setRequest(PreparedRequest request) {
        if (request == null) {
            this.requestId = "";
            this.requestName = "";
            return;
        }
        this.requestId = request.id == null ? "" : request.id;
        this.requestName = request.name == null ? "" : request.name;
    }

    public void setWebSocketSendInfo(int sendIndex, int sendCount, String stepName) {
        this.wsSendIndex = Math.max(0, sendIndex);
        this.wsSendCount = Math.max(0, sendCount);
        this.wsStepName = stepName == null ? "" : stepName;
    }

    public void clearWebSocketSendInfo() {
        this.wsSendIndex = -1;
        this.wsSendCount = 0;
        this.wsStepName = "";
    }
}
