package com.laker.postman.performance.execution;


import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;

final class DefaultPerformanceScriptRuntime implements PerformanceScriptRuntime {
    private final ScriptExecutionPipeline pipeline;

    DefaultPerformanceScriptRuntime(ScriptExecutionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public ScriptExecutionResult executePreScript() {
        return pipeline == null ? ScriptExecutionResult.success() : pipeline.executePreScript();
    }

    @Override
    public void finalizeRequest() {
        if (pipeline != null) {
            pipeline.finalizeRequest();
        }
    }

    @Override
    public <T> T withExecutionContextThrowing(ThrowingAction<T> action) throws Exception {
        if (pipeline == null) {
            return action.execute();
        }
        return pipeline.withExecutionContextThrowing(action::execute);
    }

    @Override
    public void withExecutionContext(Runnable action) {
        if (action == null) {
            return;
        }
        if (pipeline != null) {
            pipeline.withExecutionContext(action);
        } else {
            action.run();
        }
    }

    @Override
    public ScriptExecutionResult executePostScript(HttpResponse response) {
        return pipeline == null ? ScriptExecutionResult.success() : pipeline.executePostScript(response);
    }

    @Override
    public ScriptExecutionResult executeWebSocketSendScript(String script,
                                                           int sendIndex,
                                                           int sendCount,
                                                           String stepName) {
        return pipeline == null
                ? ScriptExecutionResult.success()
                : pipeline.executeWebSocketSendScript(script, sendIndex, sendCount, stepName);
    }
}
