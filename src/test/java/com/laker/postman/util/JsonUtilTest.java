package com.laker.postman.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * JsonUtil 测试类
 */
public class JsonUtilTest {

    // ==================== isTypeJSON 测试 ====================

    @Test(description = "测试无效的 JSON 字符串")
    public void testIsTypeJSON() {
        String json = "123123{}[]";
        boolean isJson = JsonUtil.isTypeJSON(json);
        Assert.assertFalse(isJson);
    }

    @Test(description = "测试有效的 JSON 对象")
    public void testIsTypeJSONValidObject() {
        String json = """
                {
                    "name": "test",
                    "age": 30
                }
                """;
        boolean isJson = JsonUtil.isTypeJSON(json);
        assertTrue(isJson);
    }

    @Test(description = "测试有效的 JSON 数组")
    public void testIsTypeJSONValidArray() {
        String json = """
                [
                    {"name": "Alice"},
                    {"name": "Bob"}
                ]
                """;
        boolean isJson = JsonUtil.isTypeJSON(json);
        assertTrue(isJson);
    }

    @Test(description = "测试空字符串")
    public void testIsTypeJSONEmpty() {
        assertFalse(JsonUtil.isTypeJSON(""));
        assertFalse(JsonUtil.isTypeJSON("   "));
    }

    // ==================== isTypeJSON5 测试 ====================

    @Test(description = "测试带单行注释的 JSON5")
    public void testIsTypeJSON5WithSingleLineComments() {
        String json5 = """
                {
                    // 这是注释
                    "name": "test",
                    "age": 30
                }
                """;
        boolean isJson5 = JsonUtil.isTypeJSON5(json5);
        assertTrue(isJson5, "带单行注释的 JSON5 应该被识别为有效");
    }

    @Test(description = "测试带多行注释的 JSON5")
    public void testIsTypeJSON5WithMultiLineComments() {
        String json5 = """
                {
                    /* 这是多行注释
                       包含多行内容 */
                    "name": "test",
                    /* 年龄 */
                    "age": 30
                }
                """;
        boolean isJson5 = JsonUtil.isTypeJSON5(json5);
        assertTrue(isJson5, "带多行注释的 JSON5 应该被识别为有效");
    }

    @Test(description = "测试混合注释的 JSON5")
    public void testIsTypeJSON5WithMixedComments() {
        String json5 = """
                {
                    // 用户信息
                    "user": {
                        /* 姓名 */
                        "name": "Alice",
                        "email": "alice@example.com" // 邮箱
                    }
                }
                """;
        boolean isJson5 = JsonUtil.isTypeJSON5(json5);
        assertTrue(isJson5, "混合注释的 JSON5 应该被识别为有效");
    }

    @Test(description = "测试纯 JSON（无注释）也应该被识别为 JSON5")
    public void testIsTypeJSON5PureJSON() {
        String json = """
                {
                    "name": "test",
                    "age": 30
                }
                """;
        boolean isJson5 = JsonUtil.isTypeJSON5(json);
        assertTrue(isJson5, "纯 JSON 也应该被识别为有效的 JSON5");
    }

    @Test(description = "测试无效的 JSON5")
    public void testIsTypeJSON5Invalid() {
        String invalid = "{ invalid json5 }";
        boolean isJson5 = JsonUtil.isTypeJSON5(invalid);
        assertFalse(isJson5, "无效的 JSON5 应该返回 false");
    }

    @Test(description = "测试 JSON5 数组")
    public void testIsTypeJSON5Array() {
        String json5 = """
                [
                    // 第一个元素
                    {"name": "Alice"},
                    /* 第二个元素 */
                    {"name": "Bob"}
                ]
                """;
        boolean isJson5 = JsonUtil.isTypeJSON5(json5);
        assertTrue(isJson5, "带注释的 JSON5 数组应该被识别为有效");
    }

    // ==================== cleanJsonComments 测试 ====================

    @Test(description = "测试清除单行注释")
    public void testCleanJsonComments() {
        String json = """
                {
                    "fileIds": "",
                    "name": "测试新增20251023",
                    "complexType": "1",
                    "taskPriority": "medium",
                    "taskType": "2",
                    "workerCode": "PER009574",
                    "note": "",
                    "deadLine": "2025-09-30",
                    "startTime": "2025-09-01",
                    "link": "http://www.baidu.com",
                    "urgencyLevel": "critical"
                }
                """;
        String cleanedJson = JsonUtil.cleanJsonComments(json);

        assertNotNull(cleanedJson);
        assertTrue(cleanedJson.contains("测试新增20251023"), "应该保留 JSON 字段值");
        assertTrue(cleanedJson.contains("fileIds"), "应该保留字段名");
        assertTrue(JsonUtil.isTypeJSON(cleanedJson), "清除后应该是有效的 JSON");

        System.out.println("清除注释后的 JSON:");
        System.out.println(cleanedJson);
    }

    @Test(description = "测试清除单行注释 - 带注释版本")
    public void testCleanJsonCommentsWithComments() {
        String json = """
                {
                    "fileIds": "",
                    "name": "测试新增20251023",  // 测试任务
                    "complexType": "1",
                    "taskPriority": "medium"
                }
                """;
        String cleanedJson = JsonUtil.cleanJsonComments(json);

        assertNotNull(cleanedJson);
        assertFalse(cleanedJson.contains("测试任务"), "清除后不应包含注释内容");
        assertTrue(cleanedJson.contains("测试新增20251023"), "应该保留 JSON 字段值");
        assertTrue(cleanedJson.contains("fileIds"), "应该保留字段名");

        System.out.println("清除单行注释后的 JSON:");
        System.out.println(cleanedJson);
    }

    @Test(description = "测试清除多行注释")
    public void testCleanJsonCommentsMultiLine() {
        String json5 = """
                {
                    /* 这是一个多行注释
                       包含多行内容
                       用于描述字段 */
                    "name": "John",
                    /* 年龄字段 */
                    "age": 30,
                    "address": "Beijing" /* 地址信息 */
                }
                """;
        String cleanedJson = JsonUtil.cleanJsonComments(json5);

        assertNotNull(cleanedJson);
        assertFalse(cleanedJson.contains("/*"), "清除后不应包含多行注释开始符号");
        assertFalse(cleanedJson.contains("*/"), "清除后不应包含多行注释结束符号");
        assertFalse(cleanedJson.contains("这是一个多行注释"), "清除后不应包含注释内容");
        assertTrue(cleanedJson.contains("John"), "应该保留字段值");
        assertTrue(cleanedJson.contains("Beijing"), "应该保留字段值");

        System.out.println("清除多行注释后的 JSON:");
        System.out.println(cleanedJson);
    }

    @Test(description = "测试清除混合注释")
    public void testCleanJsonCommentsMixed() {
        String json5 = """
                {
                    // 用户信息部分
                    "user": {
                        /* 姓名 */
                        "name": "Alice",
                        "email": "alice@example.com" // 联系邮箱
                    },
                    /* 设置信息
                       包含主题等配置 */
                    "settings": {
                        "theme": "dark", // 深色主题
                        "language": "zh-CN" /* 中文 */
                    }
                }
                """;
        String cleanedJson = JsonUtil.cleanJsonComments(json5);

        assertNotNull(cleanedJson);
        assertFalse(cleanedJson.contains("//"), "清除后不应包含单行注释");
        assertFalse(cleanedJson.contains("/*"), "清除后不应包含多行注释");
        assertTrue(cleanedJson.contains("Alice"), "应该保留字段值");
        assertTrue(cleanedJson.contains("settings"), "应该保留字段名");

        System.out.println("清除混合注释后的 JSON:");
        System.out.println(cleanedJson);
    }

    @Test(description = "测试清除注释后的 JSON 仍然有效")
    public void testCleanJsonCommentsResultIsValid() {
        String json5 = """
                {
                    // 注释1
                    "key1": "value1",
                    /* 注释2 */
                    "key2": "value2"
                }
                """;
        String cleanedJson = JsonUtil.cleanJsonComments(json5);

        // 清除注释后的 JSON 应该仍然是有效的
        assertTrue(JsonUtil.isTypeJSON(cleanedJson), "清除注释后应该是有效的 JSON");
    }

    @Test(description = "测试清除空 JSON 的注释")
    public void testCleanJsonCommentsEmpty() {
        String json5 = """
                {
                    // 空对象
                }
                """;
        String cleanedJson = JsonUtil.cleanJsonComments(json5);

        assertNotNull(cleanedJson);
        assertTrue(JsonUtil.isTypeJSON(cleanedJson), "清除注释后应该是有效的 JSON");
    }

    // ==================== formatJson5 测试 ====================

    @Test(description = "测试格式化带单行注释的 JSON5")
    public void testFormatJson5WithSingleLineComments() {
        String json5 = """
                {
                  // 用户名
                  "username": "test",
                  // 密码
                  "password": "123456"
                }
                """;
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(formatted.contains("username"), "格式化后应该包含字段");
        assertTrue(formatted.contains("test"), "格式化后应该包含值");
        // 注意：注释会被去除
        assertFalse(formatted.contains("//"), "格式化后注释会被去除");

        System.out.println("格式化带单行注释的 JSON5:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化带多行注释的 JSON5")
    public void testFormatJson5WithMultiLineComments() {
        String json5 = """
                {
                  /* 这是用户信息
                     包含姓名和年龄 */
                  "name": "John",
                  /* 年龄 */
                  "age": 30
                }
                """;
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(formatted.contains("name"), "格式化后应该包含字段");
        assertTrue(formatted.contains("John"), "格式化后应该包含值");
        assertFalse(formatted.contains("/*"), "格式化后多行注释会被去除");

        System.out.println("格式化带多行注释的 JSON5:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化紧凑的 JSON5")
    public void testFormatJson5Compact() {
        String json5 = "{/* comment */\"name\":\"test\",// comment\n\"value\":123}";
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(formatted.contains("name"), "格式化后应该包含字段");
        assertTrue(formatted.contains("test"), "格式化后应该包含值");
        // 格式化后应该是多行的
        long lineCount = formatted.lines().count();
        assertTrue(lineCount > 1, "格式化后应该是多行格式，实际行数: " + lineCount);

        System.out.println("格式化紧凑的 JSON5:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化普通 JSON（无注释）")
    public void testFormatJson5WithoutComments() {
        String json = """
                {"name":"Bob","age":25,"active":true}
                """;
        String formatted = JsonUtil.formatJson5(json);

        assertNotNull(formatted);
        assertTrue(formatted.contains("name"), "格式化后应该包含字段");
        assertTrue(formatted.contains("Bob"), "格式化后应该包含值");
        // 应该被格式化为多行
        assertTrue(formatted.lines().count() > 1, "格式化后应该是多行格式");

        System.out.println("格式化普通 JSON:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化混合注释的 JSON5")
    public void testFormatJson5Mixed() {
        String json5 = """
                {
                  // 用户信息
                  "user": {
                    /* 姓名 */
                    "name": "Alice",
                    "email": "alice@example.com" // 邮箱
                  },
                  /* 设置 */
                  "settings": {
                    "theme": "dark"
                  }
                }
                """;
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(formatted.contains("user"), "格式化后应该包含字段");
        assertTrue(formatted.contains("Alice"), "格式化后应该包含值");
        assertTrue(formatted.contains("settings"), "格式化后应该包含字段");

        System.out.println("格式化混合注释的 JSON5:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化 JSON5 数组")
    public void testFormatJson5Array() {
        String json5 = """
                [
                  // 第一个元素
                  {"name": "Alice"},
                  /* 第二个元素 */
                  {"name": "Bob"}
                ]
                """;
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(formatted.contains("Alice"), "格式化后应该包含值");
        assertTrue(formatted.contains("Bob"), "格式化后应该包含值");

        System.out.println("格式化 JSON5 数组:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化无效的 JSON")
    public void testFormatJson5Invalid() {
        String invalid = "{ invalid json }";
        String result = JsonUtil.formatJson5(invalid);

        // 如果格式化失败，应该返回原始字符串或处理错误
        assertNotNull(result);

        System.out.println("格式化无效 JSON 的结果:");
        System.out.println(result);
    }

    @Test(description = "测试格式化空 JSON")
    public void testFormatJson5Empty() {
        String json5 = """
                {
                  // 空对象
                }
                """;
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(JsonUtil.isTypeJSON(formatted), "格式化后应该是有效的 JSON");

        System.out.println("格式化空 JSON:");
        System.out.println(formatted);
    }

    @Test(description = "测试格式化嵌套的 JSON5")
    public void testFormatJson5Nested() {
        String json5 = """
                {
                  // 用户配置
                  "config": {
                    /* 数据库配置 */
                    "database": {
                      "host": "localhost", // 主机
                      "port": 3306 /* 端口 */
                    },
                    // API 配置
                    "api": {
                      "timeout": 5000
                    }
                  }
                }
                """;
        String formatted = JsonUtil.formatJson5(json5);

        assertNotNull(formatted);
        assertTrue(formatted.contains("config"), "格式化后应该包含字段");
        assertTrue(formatted.contains("database"), "格式化后应该包含嵌套字段");
        assertTrue(formatted.contains("localhost"), "格式化后应该包含值");

        System.out.println("格式化嵌套的 JSON5:");
        System.out.println(formatted);
    }
}
