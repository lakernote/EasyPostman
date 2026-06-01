package com.laker.postman.service.js.api;

import com.laker.postman.script.model.TestResult;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.UUID;

/**
 * 测试 API (pm.test)
 * <p>
 * 提供 Postman 风格的测试接口，支持：
 * - pm.test(name, function) - 执行测试
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 执行测试
 * pm.test("Status code is 200", function() {
 *     pm.expect(pm.response.code).to.equal(200);
 * });
 * }</pre>
 */
@Slf4j
public class TestApi implements ProxyExecutable {
    private final PostmanApiContext context;

    public TestApi(PostmanApiContext context) {
        this.context = context;
    }

    @Override
    public Object execute(Value... arguments) {
        String name = arguments != null && arguments.length > 0 && !arguments[0].isNull()
                ? arguments[0].asString()
                : "";
        Value testFunction = arguments != null && arguments.length > 1 ? arguments[1] : null;
        run(name, testFunction);
        return null;
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
}
