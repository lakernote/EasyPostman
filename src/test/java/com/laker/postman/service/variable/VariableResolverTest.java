package com.laker.postman.service.variable;

import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * 变量解析器测试类 - 测试嵌套变量解析功能
 */
public class VariableResolverTest {

    private Environment testEnv;

    @BeforeMethod
    public void setUp() {
        // 清空临时变量
        VariableResolver.clearTemporaryVariables();

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
        // 清理环境
        if (testEnv != null && testEnv.getId() != null) {
            EnvironmentService.deleteEnvironment(testEnv.getId());
        }
        VariableResolver.clearTemporaryVariables();
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
     * 测试临时变量优先级
     */
    @Test
    public void testTemporaryVariablePriority() {
        // 设置临时变量，覆盖环境变量
        Map<String, String> tempVars = new HashMap<>();
        tempVars.put("baseUrl", "https://temp.example.com");
        VariableResolver.setAllTemporaryVariables(tempVars);

        // 临时变量优先级更高，应该使用临时变量的值
        String result = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://temp.example.com/api/user/list", result);
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

    /**
     * 测试获取所有可用变量
     */
    @Test
    public void testGetAllAvailableVariables() {
        Map<String, String> allVars = VariableResolver.getAllAvailableVariables();

        // 应包含环境变量
        assertTrue(allVars.containsKey("baseUrl"));
        assertTrue(allVars.containsKey("apiPath"));
        assertTrue(allVars.containsKey("userModule"));

        // 应包含内置函数
        assertTrue(allVars.containsKey("$guid"));
        assertTrue(allVars.containsKey("$timestamp"));
    }

    /**
     * 测试变量过滤
     */
    @Test
    public void testFilterVariables() {
        Map<String, String> filtered = VariableResolver.filterVariables("base");
        assertTrue(filtered.containsKey("baseUrl"));
        assertFalse(filtered.containsKey("apiPath"));

        Map<String, String> all = VariableResolver.filterVariables("");
        assertTrue(all.size() > 0);
    }
}
