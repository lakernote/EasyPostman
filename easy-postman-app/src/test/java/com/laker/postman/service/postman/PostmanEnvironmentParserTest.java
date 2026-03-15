package com.laker.postman.service.postman;

import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * PostmanEnvironmentParser 单元测试
 */
public class PostmanEnvironmentParserTest {

    @Test
    public void testParsePostmanEnvironments_SingleObject() {
        // 准备测试数据 - 单个环境对象
        String json = """
                {
                    "name": "测试环境",
                    "values": [
                        {"key": "baseUrl", "value": "https://api.example.com", "enabled": true},
                        {"key": "token", "value": "abc123", "enabled": true},
                        {"key": "disabled_var", "value": "value", "enabled": false}
                    ]
                }
                """;

        // 执行解析
        List<Environment> envs = PostmanEnvironmentParser.parsePostmanEnvironments(json);

        // 验证结果
        assertNotNull(envs);
        assertEquals(envs.size(), 1);

        Environment env = envs.get(0);
        assertEquals(env.getName(), "测试环境");
        assertNotNull(env.getId());

        // 验证变量
        assertEquals(env.getVariables().size(), 2); // disabled_var 应该被忽略
        assertEquals(env.getVariables().get("baseUrl"), "https://api.example.com");
        assertEquals(env.getVariables().get("token"), "abc123");
        assertNull(env.getVariables().get("disabled_var"));
    }

    @Test
    public void testParsePostmanEnvironments_Array() {
        // 准备测试数据 - 环境数组
        String json = """
                [
                    {
                        "name": "开发环境",
                        "values": [
                            {"key": "url", "value": "http://dev.example.com", "enabled": true}
                        ]
                    },
                    {
                        "name": "生产环境",
                        "values": [
                            {"key": "url", "value": "https://prod.example.com", "enabled": true}
                        ]
                    }
                ]
                """;

        // 执行解析
        List<Environment> envs = PostmanEnvironmentParser.parsePostmanEnvironments(json);

        // 验证结果
        assertNotNull(envs);
        assertEquals(envs.size(), 2);

        assertEquals(envs.get(0).getName(), "开发环境");
        assertEquals(envs.get(0).getVariables().get("url"), "http://dev.example.com");

        assertEquals(envs.get(1).getName(), "生产环境");
        assertEquals(envs.get(1).getVariables().get("url"), "https://prod.example.com");
    }

    @Test
    public void testParsePostmanEnvironments_EmptyValues() {
        // 准备测试数据 - 空变量列表
        String json = """
                {
                    "name": "空环境",
                    "values": []
                }
                """;

        // 执行解析
        List<Environment> envs = PostmanEnvironmentParser.parsePostmanEnvironments(json);

        // 验证结果
        assertNotNull(envs);
        assertEquals(envs.size(), 1);
        assertEquals(envs.get(0).getName(), "空环境");
        assertTrue(envs.get(0).getVariables().isEmpty());
    }

    @Test
    public void testParsePostmanEnvironments_NoName() {
        // 准备测试数据 - 无名称
        String json = """
                {
                    "values": [
                        {"key": "test", "value": "value", "enabled": true}
                    ]
                }
                """;

        // 执行解析
        List<Environment> envs = PostmanEnvironmentParser.parsePostmanEnvironments(json);

        // 验证结果
        assertNotNull(envs);
        assertEquals(envs.size(), 1);
        assertEquals(envs.get(0).getName(), "未命名环境");
    }

    @Test
    public void testToPostmanEnvironmentJson() {
        // 准备测试数据
        Environment env = new Environment("测试环境");
        env.setId("test-id");
        env.addVariable("apiUrl", "https://api.example.com");
        env.addVariable("apiKey", "secret-key");

        // 执行导出
        String json = PostmanEnvironmentParser.toPostmanEnvironmentJson(env);

        // 验证结果
        assertNotNull(json);
        assertTrue(JSONUtil.isTypeJSON(json));

        // 解析JSON验证内容
        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(json);
        assertNotNull(obj.getStr("id"));
        assertEquals(obj.getStr("name"), "测试环境");
        assertEquals(obj.getStr("_postman_variable_scope"), "environment");
        assertEquals(obj.getStr("_postman_exported_using"), "EasyPostman");
        assertNotNull(obj.getStr("_postman_exported_at"));

        // 验证变量
        cn.hutool.json.JSONArray values = obj.getJSONArray("values");
        assertNotNull(values);
        assertEquals(values.size(), 2);

        boolean foundApiUrl = false;
        boolean foundApiKey = false;
        for (Object v : values) {
            cn.hutool.json.JSONObject vObj = (cn.hutool.json.JSONObject) v;
            if ("apiUrl".equals(vObj.getStr("key"))) {
                foundApiUrl = true;
                assertEquals(vObj.getStr("value"), "https://api.example.com");
                assertTrue(vObj.getBool("enabled"));
            } else if ("apiKey".equals(vObj.getStr("key"))) {
                foundApiKey = true;
                assertEquals(vObj.getStr("value"), "secret-key");
                assertTrue(vObj.getBool("enabled"));
            }
        }
        assertTrue(foundApiUrl && foundApiKey);
    }

    @Test
    public void testToPostmanEnvironmentJson_EmptyVariables() {
        // 准备测试数据 - 空变量
        Environment env = new Environment("空环境");
        env.setId("empty-id");

        // 执行导出
        String json = PostmanEnvironmentParser.toPostmanEnvironmentJson(env);

        // 验证结果
        assertNotNull(json);
        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(json);
        cn.hutool.json.JSONArray values = obj.getJSONArray("values");
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testRoundTrip() {
        // 测试导出后再导入，数据应该保持一致
        Environment original = new Environment("往返测试");
        original.addVariable("var1", "value1");
        original.addVariable("var2", "value2");

        // 导出
        String json = PostmanEnvironmentParser.toPostmanEnvironmentJson(original);

        // 导入
        List<Environment> imported = PostmanEnvironmentParser.parsePostmanEnvironments(json);

        // 验证
        assertNotNull(imported);
        assertEquals(imported.size(), 1);
        Environment restored = imported.get(0);
        assertEquals(restored.getName(), original.getName());
        assertEquals(restored.getVariables().size(), original.getVariables().size());
        assertEquals(restored.getVariables().get("var1"), "value1");
        assertEquals(restored.getVariables().get("var2"), "value2");
    }
}

