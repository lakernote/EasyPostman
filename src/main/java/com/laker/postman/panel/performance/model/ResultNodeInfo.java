package com.laker.postman.panel.performance.model;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;

import java.util.List;

public class ResultNodeInfo {

    /** 接口名称 */
    public final String name;

    /** 错误信息 */
    public final String errorMsg;

    /** 请求 */
    public final PreparedRequest req;

    /** 响应 */
    public final HttpResponse resp;

    /** 断言结果 */
    public final List<TestResult> testResults;

    /** 耗时（毫秒）——在构造时就算好 */
    public final int costMs;

    /** HTTP响应状态码 */
    public final int responseCode;

    public ResultNodeInfo(
            String name,
            String errorMsg,
            PreparedRequest req,
            HttpResponse resp,
            List<TestResult> testResults
    ) {
        this.name = name;
        this.errorMsg = errorMsg;
        this.req = req;
        this.resp = resp;
        this.testResults = testResults;

        // ✅ 统一在这里算 ms（不依赖 costNs）
        this.costMs = resp != null ? (int) resp.costMs : 0;

        // 提取响应码
        this.responseCode = resp != null ? resp.code : 0;
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
     * 3. 如果没有响应，返回 false
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

        // 3. 兜底：没有响应则返回 false
        return false;
    }
}
