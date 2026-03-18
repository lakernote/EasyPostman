package com.laker.postman.model.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 测试 API (pm.test)
 * <p>
 * 提供 Postman 兼容的测试接口，支持：
 * - pm.test(name, function) - 执行测试
 * - pm.test.index() - 获取所有测试结果
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 执行测试
 * pm.test("Status code is 200", function() {
 *     pm.expect(pm.response.code).to.equal(200);
 * });
 *
 * // 获取所有测试结果
 * var results = pm.test.index();
 * results.forEach(function(result) {
 *     console.log(result.name + ": " + (result.passed ? "PASS" : "FAIL"));
 * });
 * }</pre>
 */
@Slf4j
public class TestApi {
    private final PostmanApiContext context;

    public TestApi(PostmanApiContext context) {
        this.context = context;
    }


    /**
     * 执行测试断言
     * 对应脚本中的: pm.test(name, function)
     *
     * @param name         测试名称
     * @param testFunction 测试函数
     */
    public void run(String name, Value testFunction) {
        String errorMessage = null;
        boolean passed = true;

        try {
            if (testFunction != null && testFunction.canExecute()) {
                testFunction.execute();
            }
        } catch (Exception e) {
            passed = false;
            errorMessage = e.getMessage();
            log.debug("测试失败: {}, 错误: {}", name, e.getMessage());
        }

        TestResult result = new TestResult(name, passed, errorMessage);
        result.id = UUID.randomUUID().toString();
        context.testResults.add(result);
    }

    /**
     * 获取所有测试结果
     * 对应脚本中的: pm.test.index()
     *
     * @return 测试结果数组
     */
    public TestResult[] index() {
        if (context.testResults == null) {
            return new TestResult[0];
        }
        return context.testResults.toArray(new TestResult[0]);
    }

    /**
     * 获取测试结果列表（ArrayList）
     * 方便在 Java 代码中使用
     *
     * @return 测试结果列表
     */
    public List<TestResult> getResults() {
        if (context.testResults == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(context.testResults);
    }

    /**
     * 清空所有测试结果
     */
    public void clear() {
        if (context.testResults != null) {
            context.testResults.clear();
        }
    }

    /**
     * 获取测试总数
     *
     * @return 测试总数
     */
    public int count() {
        return context.testResults != null ? context.testResults.size() : 0;
    }

    /**
     * 获取通过的测试数量
     *
     * @return 通过的测试数量
     */
    public int passedCount() {
        if (context.testResults == null) {
            return 0;
        }
        return (int) context.testResults.stream().filter(t -> t.passed).count();
    }

    /**
     * 获取失败的测试数量
     *
     * @return 失败的测试数量
     */
    public int failedCount() {
        if (context.testResults == null) {
            return 0;
        }
        return (int) context.testResults.stream().filter(t -> !t.passed).count();
    }
}

