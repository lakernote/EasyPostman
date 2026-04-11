package com.laker.postman.service.variable;

import com.laker.postman.model.script.ScriptVariablesApi;
import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.GlobalVariablesService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * 变量解析器测试类 - 测试嵌套变量解析功能
 */
public class VariableResolverTest {

    private Environment testEnv;
    private String originalDataFilePath;
    private String originalGlobalDataFilePath;
    private Path tempEnvFile;
    private Path tempGlobalFile;

    @BeforeMethod
    public void setUp() {
        // 隔离测试数据：切换到临时环境文件，避免污染本机真实环境变量
        originalDataFilePath = EnvironmentService.getDataFilePath();
        try {
            tempEnvFile = Files.createTempFile("easy-postman-env-test-", ".json");
            Files.writeString(tempEnvFile, "[]");
            tempGlobalFile = Files.createTempFile("easy-postman-global-test-", ".json");
            Files.writeString(tempGlobalFile, "{}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize temporary environment file", e);
        }
        EnvironmentService.setDataFilePath(tempEnvFile.toString());
        originalGlobalDataFilePath = GlobalVariablesService.getInstance().getDataFilePath();
        GlobalVariablesService.getInstance().setDataFilePath(tempGlobalFile.toString());

        // 清空执行上下文，避免测试之间共享 pm.variables / pm.iterationData
        clearExecutionContext();

        // 创建测试环境
        testEnv = new Environment();
        testEnv.setId("test-env-id");
        testEnv.setName("Test Environment");

        // 设置嵌套变量
        testEnv.set("baseUrl", "https://api.example.com");
        testEnv.set("apiPath", "{{baseUrl}}/api");
        testEnv.set("userModule", "{{apiPath}}/user");

        // 将环境添加到 EnvironmentService 并设置为活动环境
        EnvironmentService.saveEnvironment(testEnv);
        EnvironmentService.setActiveEnvironment(testEnv.getId());
    }

    @AfterMethod
    public void tearDown() {
        try {
            // 清理测试环境
            if (testEnv != null && testEnv.getId() != null) {
                EnvironmentService.deleteEnvironment(testEnv.getId());
            }
            clearExecutionContext();
            RequestContext.clearCurrentRequestNode();
        } finally {
            // 恢复原始环境文件路径
            if (originalDataFilePath != null && !originalDataFilePath.isBlank()) {
                EnvironmentService.setDataFilePath(originalDataFilePath);
            }
            if (originalGlobalDataFilePath != null && !originalGlobalDataFilePath.isBlank()) {
                GlobalVariablesService.getInstance().setDataFilePath(originalGlobalDataFilePath);
            }
            if (tempEnvFile != null) {
                try {
                    Files.deleteIfExists(tempEnvFile);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
            if (tempGlobalFile != null) {
                try {
                    Files.deleteIfExists(tempGlobalFile);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
        }
    }

    /**
     * 测试嵌套变量解析 - 核心功能
     */
    @Test
    public void testNestedVariableResolution() {
        // 测试一级嵌套
        String result1 = VariableResolver.resolve("{{apiPath}}");
        assertEquals("https://api.example.com/api", result1,
                "apiPath should resolve to https://api.example.com/api");

        // 测试二级嵌套
        String result2 = VariableResolver.resolve("{{userModule}}");
        assertEquals("https://api.example.com/api/user", result2,
                "userModule should resolve to https://api.example.com/api/user");

        // 测试完整的接口 URL
        String result3 = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://api.example.com/api/user/list", result3,
                "{{userModule}}/list should resolve to https://api.example.com/api/user/list");
    }

    /**
     * 测试多个嵌套变量在同一字符串中
     */
    @Test
    public void testMultipleNestedVariables() {
        String input = "{{apiPath}}/admin and {{userModule}}/profile";
        String result = VariableResolver.resolve(input);
        assertEquals("https://api.example.com/api/admin and https://api.example.com/api/user/profile", result);
    }

    /**
     * 测试深层嵌套（3层以上）
     */
    @Test
    public void testDeepNesting() {
        testEnv.set("level3", "{{userModule}}/admin");
        testEnv.set("level4", "{{level3}}/settings");

        String result = VariableResolver.resolve("{{level4}}/profile");
        assertEquals(result, "https://api.example.com/api/user/admin/settings/profile");
    }

    /**
     * 测试执行上下文变量优先级
     */
    @Test
    public void testExecutionScopedVariablePriority() {
        // 设置执行上下文变量，覆盖环境变量
        Map<String, String> tempVars = new HashMap<>();
        tempVars.put("baseUrl", "https://temp.example.com");
        VariablesService.getInstance().setAll(tempVars);

        // 执行上下文变量优先级更高，应该使用它的值
        String result = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://temp.example.com/api/user/list", result);
    }

    @Test
    public void testIterationDataPriority() {
        IterationDataVariableService.getInstance().replaceAll(Map.of("baseUrl", "https://csv.example.com"));

        String result = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://csv.example.com/api/user/list", result);
        assertEquals(VariableResolver.getVariableType("baseUrl"), VariableType.ITERATION_DATA);
    }

    @Test
    public void testClearVariablesClearsValuesButKeepsExecutionContextUsable() {
        VariablesService variablesService = VariablesService.getInstance();

        variablesService.set("token", "old-token");
        Map<String, String> originalContext = variablesService.getCurrentContextMap();
        assertNotNull(originalContext);

        variablesService.clearValues();

        assertTrue(variablesService.getAll().isEmpty(), "Execution-scoped values should be cleared");
        assertSame(variablesService.getCurrentContextMap(), originalContext,
                "clearVariables should keep the current execution context attached");

        variablesService.set("token", "new-token");
        assertEquals(VariableResolver.resolve("{{token}}"), "new-token");
        assertSame(variablesService.getCurrentContextMap(), originalContext,
                "Values recreated after clearVariables should stay on the same context map");
    }

    @Test
    public void testClearExecutionContextDetachesVariablesAndIterationData() {
        VariablesService variablesService = VariablesService.getInstance();
        IterationDataVariableService iterationDataService = IterationDataVariableService.getInstance();

        variablesService.set("requestId", "req-001");
        iterationDataService.replaceAll(Map.of("csvUserId", "user-001"));

        assertNotNull(variablesService.getCurrentContextMap());
        assertNotNull(iterationDataService.getCurrentContextMap());

        clearExecutionContext();

        assertNull(variablesService.getCurrentContextMap(),
                "Execution-scoped variable context should be detached after clearExecutionContext");
        assertNull(iterationDataService.getCurrentContextMap(),
                "Iteration-data context should be detached after clearExecutionContext");
        assertFalse(VariableResolver.isVariableDefined("requestId"));
        assertFalse(VariableResolver.isVariableDefined("csvUserId"));
    }

    @Test
    public void testScriptVariablesDoesNotExposeBuiltInFunctionsAsStoredVariables() {
        ScriptVariablesApi scriptVariablesApi = new ScriptVariablesApi();

        assertFalse(scriptVariablesApi.has("$guid"));
        assertFalse(scriptVariablesApi.has("$timestamp"));
        assertNull(scriptVariablesApi.get("$guid"));
        assertNull(scriptVariablesApi.get("$timestamp"));
    }

    /**
     * 测试全局变量在环境变量未命中时可被解析
     */
    @Test
    public void testGlobalVariableResolution() {
        GlobalVariablesService.getInstance().getGlobalVariables().set("globalBaseUrl", "https://global.example.com");
        GlobalVariablesService.getInstance().saveGlobalVariables();

        String result = VariableResolver.resolve("{{globalBaseUrl}}/api");
        assertEquals(result, "https://global.example.com/api");
    }

    /**
     * 测试内置函数与嵌套变量混合
     */
    @Test
    public void testBuiltInFunctionWithNesting() {
        testEnv.set("urlWithTimestamp", "{{baseUrl}}/api?t={{$timestamp}}");

        String result = VariableResolver.resolve("{{urlWithTimestamp}}");
        assertTrue(result.startsWith("https://api.example.com/api?t="),
                "Should resolve both baseUrl and $timestamp");
        assertTrue(result.matches("https://api.example.com/api\\?t=\\d+"),
                "Timestamp should be a number");
    }

    /**
     * 测试未定义变量保持原样
     */
    @Test
    public void testUndefinedVariableStaysUnchanged() {
        String input = "{{baseUrl}}/{{undefined}}/list";
        String result = VariableResolver.resolve(input);
        assertEquals("https://api.example.com/{{undefined}}/list", result,
                "Undefined variable should remain unchanged");
    }

    /**
     * 测试空字符串和 null
     */
    @Test
    public void testNullAndEmptyString() {
        assertNull(VariableResolver.resolve(null));
        assertEquals("", VariableResolver.resolve(""));
    }

    /**
     * 测试循环引用检测
     */
    @Test
    public void testCircularReferenceDetection() {
        Map<String, String> vars = testEnv.getVariables();
        vars.put("var1", "{{var2}}");
        vars.put("var2", "{{var1}}");

        // 应该在达到最大迭代次数后停止，不会无限循环
        String result = VariableResolver.resolve("{{var1}}");
        assertNotNull(result);
        // 由于循环引用，结果仍然会包含变量占位符
        assertTrue(result.contains("{{"), "Circular reference should stop after max iterations");
    }

    /**
     * 测试特殊字符处理
     */
    @Test
    public void testSpecialCharacters() {
        testEnv.set("special", "value$with\\backslash");
        testEnv.set("nested", "{{special}}/path");

        String result = VariableResolver.resolve("{{nested}}");
        assertEquals(result, "value$with\\backslash/path",
                "Special characters $ and \\ should be preserved");
    }

    /**
     * 测试变量查询功能
     */
    @Test
    public void testIsVariableDefined() {
        assertTrue(VariableResolver.isVariableDefined("baseUrl"));
        assertTrue(VariableResolver.isVariableDefined("apiPath"));
        assertTrue(VariableResolver.isVariableDefined("userModule"));
        assertFalse(VariableResolver.isVariableDefined("undefined"));
        assertFalse(VariableResolver.isVariableDefined(null));
        assertFalse(VariableResolver.isVariableDefined(""));
    }

    private void clearExecutionContext() {
        VariablesService.getInstance().detachContext();
        IterationDataVariableService.getInstance().detachContext();
    }

}
