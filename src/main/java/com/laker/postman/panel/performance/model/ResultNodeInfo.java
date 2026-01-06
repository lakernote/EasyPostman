package com.laker.postman.panel.performance.model;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;

import java.util.List;

public class ResultNodeInfo {

    /** 接口名称 */
    public final String name;

    /** 是否成功 */
    public final boolean success;

    /** 错误信息 */
    public final String errorMsg;

    /** 请求 */
    public final PreparedRequest req;

    /** 响应 */
    public final HttpResponse resp;

    /** 断言结果 */
    public final List<TestResult> testResults;

    /** 耗时（毫秒）——在构造时就算好 */
    public final long costMs;

    public ResultNodeInfo(
            String name,
            boolean success,
            String errorMsg,
            PreparedRequest req,
            HttpResponse resp,
            List<TestResult> testResults
    ) {
        this.name = name;
        this.success = success;
        this.errorMsg = errorMsg;
        this.req = req;
        this.resp = resp;
        this.testResults = testResults;

        // ✅ 统一在这里算 ms（不依赖 costNs）
        this.costMs = resp != null ? resp.costMs : 0L;
    }

    /** 是否有断言失败 */
    public boolean hasAssertionFailed() {
        if (testResults == null) return false;
        for (TestResult r : testResults) {
            if (!r.passed) return true;
        }
        return false;
    }

    /**
     * 智能判断是否成功
     * 优先级：
     * 1. 如果有断言，以断言结果为准
     * 2. 如果没有断言，以 HTTP 状态码为准（2xx/3xx 为成功）
     * 3. 如果没有响应，以构造函数传入的 success 为准
     */
    public boolean isActuallySuccessful() {
        // 1. 如果有断言结果，以断言为准
        if (testResults != null && !testResults.isEmpty()) {
            return !hasAssertionFailed();
        }

        // 2. 如果没有断言，以 HTTP 状态码为准
        if (resp != null && resp.code > 0) {
            return resp.code >= 200 && resp.code < 400;
        }

        // 3. 兜底：使用构造函数传入的 success
        return success;
    }
}
