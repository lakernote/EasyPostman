package com.laker.postman.panel.performance.execution;


import com.laker.postman.model.PreparedRequest;

record PerformancePreparedRequest(String requestId,
                                  String requestName,
                                  PreparedRequest request,
                                  String requestBodyTemplate,
                                  PerformanceScriptRuntime scriptRuntime) {
}
