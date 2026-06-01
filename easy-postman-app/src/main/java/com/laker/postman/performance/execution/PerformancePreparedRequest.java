package com.laker.postman.performance.execution;


import com.laker.postman.http.runtime.model.PreparedRequest;

record PerformancePreparedRequest(String requestId,
                                  String requestName,
                                  PreparedRequest request,
                                  String requestBodyTemplate,
                                  PerformanceScriptRuntime scriptRuntime) {
}
