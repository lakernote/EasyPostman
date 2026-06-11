package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.service.js.ScriptExecutionPipeline;

import java.util.List;

interface WebSocketResponseHandler {
    List<TestResult> handleStreamMessage(ScriptExecutionPipeline pipeline, String message);

    void saveHistory(PreparedRequest request, HttpResponse response, String label);
}
