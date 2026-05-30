package com.laker.postman.service.js;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.PostmanApiContext;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.RequestFinalizer;
import com.laker.postman.service.variable.ExecutionContextScope;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.RequestContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.service.variable.RunScopedVariableContext;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 脚本执行流水线
 * 提供完整的脚本执行生命周期管理，包括前置脚本、后置脚本、测试结果收集等
 * <p>
 * 使用示例：
 * <pre>{@code
 * ScriptExecutionResult result = ScriptExecutionPipeline.builder()
 *     .request(req)
 *     .preScript(item.getPrescript())
 *     .postScript(item.getPostscript())
 *     .build()
 *     .executePreScript()
 *     .executePostScript(response);
 * }</pre>
 */
@Slf4j
@Builder
@Getter
public class ScriptExecutionPipeline {
    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              boolean useCache) {
        return forRequestExecution(item, request, sharedExecutionContext, useCache, null);
    }

    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              boolean useCache,
                                                              JsScriptExecutor.OutputCallback outputCallback) {
        return forRequestExecution(item, request, sharedExecutionContext, useCache, outputCallback, null);
    }

    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              boolean useCache,
                                                              JsScriptExecutor.OutputCallback outputCallback,
                                                              Supplier<Environment> environmentSupplier) {
        return forRequestExecution(
                item,
                request,
                sharedExecutionContext,
                useCache,
                outputCallback,
                environmentSupplier,
                RequestContext.captureCurrentExecutionScope()
        );
    }

    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              boolean useCache,
                                                              JsScriptExecutor.OutputCallback outputCallback,
                                                              Supplier<Environment> environmentSupplier,
                                                              RequestExecutionScope requestExecutionScope) {
        return ScriptExecutionPipeline.builder()
                .request(request)
                .preScript(request.prescript)
                .postScript(request.postscript)
                .sharedExecutionContext(sharedExecutionContext)
                .requestExecutionScope(requestExecutionScope != null
                        ? requestExecutionScope
                        : RequestContext.captureCurrentExecutionScope())
                .deferredAuthorization(PreparedRequestBuilder.resolveDeferredAuthorization(item, useCache))
                .outputCallback(outputCallback)
                .environmentSupplier(environmentSupplier)
                .build();
    }

    /**
     * 准备好的请求对象
     */
    private final PreparedRequest request;

    /**
     * 前置脚本内容
     */
    private final String preScript;

    /**
     * 后置脚本内容
     */
    private final String postScript;

    /**
     * 变量绑定（可选，如果不提供则自动创建）
     */
    private Map<String, Object> bindings;

    /**
     * 是否在前置脚本失败时显示错误对话框
     */
    @Builder.Default
    private final boolean showPreScriptErrorDialog = true;

    /**
     * 输出回调（可选）
     */
    private final JsScriptExecutor.OutputCallback outputCallback;

    /**
     * 可选的环境变量供应器。Headless / 集群 worker 可按 run 注入环境；
     * GUI 默认不传时仍使用 EnvironmentService 的 active environment。
     */
    private final Supplier<Environment> environmentSupplier;

    /**
     * 脚本执行器。性能压测会注入 run-scoped executor，普通 GUI 请求默认使用全局 executor。
     */
    private final JsScriptExecutor.ScriptExecutor scriptExecutor;

    /**
     * 当前脚本所属请求的变量作用域，用于 headless / GUI 共用的分组变量解析。
     */
    private final RequestExecutionScope requestExecutionScope;

    /**
     * 可选的共享上下文。Functional / Performance 这类“同一轮多请求”
     * 场景会显式传入它，避免再依赖线程残留来共享 pm.variables。
     */
    private final ExecutionVariableContext sharedExecutionContext;

    /**
     * 延迟到执行上下文挂载后再生成的自动认证配置。
     * 这样同一轮共享变量可用于 Basic/Bearer 认证，同时 pre-script 仍可删除/覆盖该请求头。
     */
    private PreparedRequestBuilder.DeferredAuthorization deferredAuthorization;

    private transient ExecutionVariableContext runtimeExecutionContext;

    /**
     * 执行前置脚本
     *
     * @return 执行结果
     */
    public ScriptExecutionResult executePreScript() {
        return withExecutionContext(() -> {
            if (CharSequenceUtil.isBlank(preScript)) {
                return ScriptExecutionResult.success();
            }
            if (bindings == null) {
                bindings = preparePreRequestBindings(request);
            }

            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null && pm.info != null) {
                pm.info.eventName = "prerequest";
            }
            clearTestResults();

            try {
                ScriptExecutionContext context = ScriptExecutionContext.builder()
                        .script(preScript)
                        .scriptType(ScriptExecutionContext.ScriptType.PRE_REQUEST)
                        .bindings(bindings)
                        .outputCallback(getEffectiveOutputCallback("[PreScript Console]\n"))
                        .build();

                executeScript(context);
                return ScriptExecutionResult.success();

            } catch (ScriptExecutionException ex) {
                log.error("Pre-script execution failed: {}", ex.getMessage(), ex);
                appendScriptOutput("[PreScript Error]\n" + ex.getMessage(), JsScriptExecutor.ConsoleType.ERROR);
                return ScriptExecutionResult.failure(ex.getMessage(), ex);
            }
        });
    }

    /**
     * 执行后置脚本
     *
     * @param response HTTP 响应对象
     * @return 执行结果（包含测试结果）
     */
    public ScriptExecutionResult executePostScript(HttpResponse response) {
        return withExecutionContext(() -> {
            if (bindings == null) {
                bindings = preparePreRequestBindings(request);
            }

            addResponseBindings(bindings, response);
            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null && pm.info != null) {
                pm.info.eventName = "test";
            }
            clearTestResults();

            try {
                ScriptExecutionContext context = ScriptExecutionContext.builder()
                        .script(postScript)
                        .scriptType(ScriptExecutionContext.ScriptType.POST_REQUEST)
                        .bindings(bindings)
                        .outputCallback(getEffectiveOutputCallback("[PostScript Console]\n"))
                        .build();

                executeScript(context);

                if (pm != null && pm.testResults != null) {
                    return ScriptExecutionResult.success(pm.testResults);
                }
                return ScriptExecutionResult.success();

            } catch (ScriptExecutionException ex) {
                log.error("Post-script execution failed: {}", ex.getMessage(), ex);
                appendScriptOutput("[PostScript Error]\n" + ex.getMessage(), JsScriptExecutor.ConsoleType.ERROR);

                if (pm != null && pm.testResults != null) {
                    return ScriptExecutionResult.failure(ex.getMessage(), ex, pm.testResults);
                }
                return ScriptExecutionResult.failure(ex.getMessage(), ex);
            }
        });
    }

    public ScriptExecutionResult executeWebSocketSendScript(String script,
                                                            int sendIndex,
                                                            int sendCount,
                                                            String stepName) {
        return withExecutionContext(() -> {
            if (bindings == null) {
                bindings = preparePreRequestBindings(request);
            }

            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null && pm.info != null) {
                pm.info.eventName = "websocket_send";
                pm.info.setWebSocketSendInfo(sendIndex, sendCount, stepName);
            }
            clearTestResults();

            try {
                ScriptExecutionContext context = ScriptExecutionContext.builder()
                        .script(script)
                        .scriptType(ScriptExecutionContext.ScriptType.CUSTOM)
                        .bindings(bindings)
                        .outputCallback(getEffectiveOutputCallback("[WebSocket Send Script Console]\n"))
                        .build();

                executeScript(context);
                return ScriptExecutionResult.success();
            } catch (ScriptExecutionException ex) {
                log.error("WebSocket send script execution failed: {}", ex.getMessage(), ex);
                appendScriptOutput("[WebSocket Send Script Error]\n" + ex.getMessage(), JsScriptExecutor.ConsoleType.ERROR);
                return ScriptExecutionResult.failure(ex.getMessage(), ex);
            }
        });
    }

    /**
     * 完整执行流程：前置脚本 -> 后置脚本
     * 如果前置脚本失败，则不执行后置脚本
     *
     * @param response HTTP 响应对象
     * @return 后置脚本的执行结果
     */
    public ScriptExecutionResult executeFullPipeline(HttpResponse response) {
        // 执行前置脚本
        ScriptExecutionResult preResult = executePreScript();
        if (!preResult.isSuccess()) {
            return preResult;
        }

        // 执行后置脚本
        return executePostScript(response);
    }

    /**
     * 清空测试结果
     */
    private void clearTestResults() {
        if (bindings != null) {
            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null && pm.testResults != null) {
                pm.testResults.clear();
            }
        }
    }

    /**
     * 获取有效的输出回调
     */
    private JsScriptExecutor.OutputCallback getEffectiveOutputCallback(String prefix) {
        if (outputCallback != null) {
            return outputCallback;
        }
        // 返回一个带颜色支持的回调
        return new JsScriptExecutor.OutputCallback() {
            @Override
            public void onOutput(String output) {
                ConsolePanel.appendLog(prefix + output);
            }

            @Override
            public void onOutput(String output, JsScriptExecutor.ConsoleType consoleType) {
                ConsolePanel.appendLog(prefix + output, mapConsoleType(consoleType));
            }
        };
    }

    private void appendScriptOutput(String output, JsScriptExecutor.ConsoleType consoleType) {
        if (outputCallback != null) {
            outputCallback.onOutput(output, consoleType);
            return;
        }
        ConsolePanel.appendLog(output, mapConsoleType(consoleType));
    }

    /**
     * 将 ConsoleType 映射到 ConsolePanel.LogType
     */
    private ConsolePanel.LogType mapConsoleType(JsScriptExecutor.ConsoleType consoleType) {
        return switch (consoleType) {
            case ERROR -> ConsolePanel.LogType.ERROR;
            case WARN -> ConsolePanel.LogType.WARN;
            case INFO -> ConsolePanel.LogType.INFO;
            case DEBUG -> ConsolePanel.LogType.DEBUG;
            case LOG -> ConsolePanel.LogType.INFO;
        };
    }

    /**
     * 前置脚本执行成功后，统一进入最终收尾阶段。
     * 这里不再散落处理变量替换 / Authorization 兜底规则，
     * 而是全部委托给 RequestFinalizer，保证各发送入口行为一致。
     */
    public void finalizeRequest() {
        withExecutionContext(() -> RequestFinalizer.finalizeForSend(request, deferredAuthorization));
        deferredAuthorization = null;
    }

    public void withExecutionContext(Runnable action) {
        withExecutionContext(() -> {
            action.run();
            return null;
        });
    }

    public <T> T withExecutionContext(Supplier<T> action) {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(getOrCreateExecutionContext(), requestExecutionScope)) {
            return action.get();
        }
    }

    public <T> T withExecutionContextThrowing(ThrowingSupplier<T> action) throws Exception {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(getOrCreateExecutionContext(), requestExecutionScope)) {
            return action.get();
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private ExecutionVariableContext getOrCreateExecutionContext() {
        if (runtimeExecutionContext != null) {
            return runtimeExecutionContext;
        }
        runtimeExecutionContext = sharedExecutionContext != null
                ? sharedExecutionContext
                : new ExecutionVariableContext();
        return runtimeExecutionContext;
    }

    /**
     * 执行脚本（通用方法）
     *
     * @param context 脚本执行上下文
     * @throws ScriptExecutionException 脚本执行异常
     */
    private void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        if (context == null || CharSequenceUtil.isBlank(context.getScript())) {
            return;
        }
        JsScriptExecutor.ScriptExecutor executor = scriptExecutor == null
                ? JsScriptExecutor::executeScript
                : scriptExecutor;
        executor.execute(context);
    }


    /**
     * 准备前置脚本的变量绑定
     * 包含请求、环境、PostmanAPI 上下文等，并初始化空响应对象
     *
     * @param req 准备好的请求对象
     * @return 变量绑定 Map
     */
    private Map<String, Object> preparePreRequestBindings(PreparedRequest req) {
        Environment activeEnv = resolveActiveEnvironment();
        PostmanApiContext postman = createPostmanApiContext(activeEnv);
        postman.setRequest(req);

        Map<String, Object> bindings = new java.util.LinkedHashMap<>();
        bindings.put("request", req);
        bindings.put("env", activeEnv);
        bindings.put("globals", postman.globals);
        bindings.put("iterationData", postman.iterationData);
        bindings.put("postman", postman);
        bindings.put("pm", postman);

        // 为前置脚本提供一个空的响应对象，防止 pm.response 为 undefined
        try {
            HttpResponse emptyResponse = new HttpResponse();
            emptyResponse.code = 0;
            emptyResponse.headers = new java.util.LinkedHashMap<>();
            emptyResponse.body = "{}";
            postman.setResponse(emptyResponse);

            bindings.put("response", emptyResponse);
            bindings.put("responseBody", "{}");
            bindings.put("responseHeaders", emptyResponse.headers);
            bindings.put("statusCode", 0);
        } catch (Exception e) {
            log.warn("Failed to initialize empty response object for pre-request script", e);
        }

        return bindings;
    }

    private PostmanApiContext createPostmanApiContext(Environment activeEnv) {
        if (environmentSupplier != null) {
            return PostmanApiContext.scoped(activeEnv, RunScopedVariableContext.currentGlobals());
        }
        return new PostmanApiContext(activeEnv);
    }

    private Environment resolveActiveEnvironment() {
        if (environmentSupplier != null) {
            return environmentSupplier.get();
        }
        return EnvironmentService.getActiveEnvironment();
    }

    /**
     * 为后置脚本添加响应相关的变量绑定
     * 在现有 bindings 基础上添加响应数据
     *
     * @param bindings 现有的变量绑定
     * @param response HTTP 响应对象
     */
    private static void addResponseBindings(Map<String, Object> bindings, HttpResponse response) {
        if (bindings == null || response == null) {
            return;
        }

        PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
        if (pm != null) {
            pm.setResponse(response);
        }

        bindings.put("response", response);
        bindings.put("responseBody", response.body);
        bindings.put("responseHeaders", response.headers);
        bindings.put("statusCode", response.code);
    }
}
