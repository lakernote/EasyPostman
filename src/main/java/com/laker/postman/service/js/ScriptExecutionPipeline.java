package com.laker.postman.service.js;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.PostmanApiContext;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.VariableResolver;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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
     * 执行前置脚本
     *
     * @return 执行结果
     */
    public ScriptExecutionResult executePreScript() {
        // 准备绑定变量
        if (bindings == null) {
            bindings = preparePreRequestBindings(request);
        }

        // 清空之前的测试结果
        clearTestResults();

        // 执行前置脚本
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
            ConsolePanel.appendLog("[PreScript Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);
            return ScriptExecutionResult.failure(ex.getMessage(), ex);
        }
    }

    /**
     * 执行后置脚本
     *
     * @param response HTTP 响应对象
     * @return 执行结果（包含测试结果）
     */
    public ScriptExecutionResult executePostScript(HttpResponse response) {
        if (bindings == null) {
            throw new IllegalStateException("Bindings not initialized. Please call executePreScript() first or provide bindings.");
        }

        // 添加响应绑定
        addResponseBindings(bindings, response);

        // 清空之前的测试结果
        clearTestResults();

        // 执行后置脚本
        try {
            ScriptExecutionContext context = ScriptExecutionContext.builder()
                    .script(postScript)
                    .scriptType(ScriptExecutionContext.ScriptType.POST_REQUEST)
                    .bindings(bindings)
                    .outputCallback(getEffectiveOutputCallback("[PostScript Console]\n"))
                    .build();

            executeScript(context);

            // 收集测试结果
            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null && pm.testResults != null) {
                return ScriptExecutionResult.success(pm.testResults);
            }
            return ScriptExecutionResult.success();

        } catch (ScriptExecutionException ex) {
            log.error("Post-script execution failed: {}", ex.getMessage(), ex);
            ConsolePanel.appendLog("[PostScript Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);

            // 即使失败也返回已有的测试结果
            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null && pm.testResults != null) {
                return ScriptExecutionResult.failure(ex.getMessage(), ex, pm.testResults);
            }
            return ScriptExecutionResult.failure(ex.getMessage(), ex);
        }
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
        return output -> ConsolePanel.appendLog(prefix + output);
    }

    /**
     * 为 CSV 数据添加变量绑定
     *
     * @param csvData CSV 数据行
     */
    public void addCsvDataBindings(Map<String, String> csvData) {
        if (bindings == null) {
            bindings = preparePreRequestBindings(request);
        }

        if (csvData != null && !csvData.isEmpty()) {
            PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
            if (pm != null) {
                // 设置 CSV 数据到 pm.variables
                for (Map.Entry<String, String> entry : csvData.entrySet()) {
                    pm.variables.set(entry.getKey(), entry.getValue());
                }

                // 同步到 VariableResolver，以便在 HTTP 请求变量替换时使用
                VariableResolver.setAllTemporaryVariables(csvData);
                log.debug("已同步 {} 个 CSV 变量到 VariableResolver", csvData.size());
            }
        }
    }


    /**
     * 执行脚本（通用方法）
     *
     * @param context 脚本执行上下文
     * @throws ScriptExecutionException 脚本执行异常
     */
    private static void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        if (context == null || CharSequenceUtil.isBlank(context.getScript())) {
            return;
        }
        JsScriptExecutor.executeScript(context);
    }


    /**
     * 准备前置脚本的变量绑定
     * 包含请求、环境、PostmanAPI 上下文等，并初始化空响应对象
     *
     * @param req 准备好的请求对象
     * @return 变量绑定 Map
     */
    private static Map<String, Object> preparePreRequestBindings(PreparedRequest req) {
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        PostmanApiContext postman = new PostmanApiContext(activeEnv);
        postman.setRequest(req);

        Map<String, Object> bindings = new java.util.LinkedHashMap<>();
        bindings.put("request", req);
        bindings.put("env", activeEnv);
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

