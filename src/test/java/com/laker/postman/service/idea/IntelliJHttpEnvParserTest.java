package com.laker.postman.service.idea;

import com.laker.postman.model.Environment;
import com.laker.postman.service.ideahttp.IntelliJHttpEnvParser;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * IntelliJHttpEnvironmentParser 单元测试
 */
public class IntelliJHttpEnvParserTest {

    @Test
    public void testParseIntelliJEnvironments() {
        String json = """
                {
                    "local": {
                        "broker_host": "localhost.example.com",
                        "org_id": "00000000-0000-0000-0000-000000000001",
                        "account_name": "test_account_local",
                        "grant_type": "urn:ietf:params:oauth:grant-type:saml2-bearer",
                        "client_id": "test_client_id_local_xxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                        "client_secret": "test_client_secret_local_xxxxxxxxxxxxxxxxxxxxxxxx",
                        "apiUrl": "https://api-local.example.com",
                        "serviceUrl": "http://localhost:9221"
                    },
                    "dev": {
                        "broker_host": "dev.example.com",
                        "org_id": "00000000-0000-0000-0000-000000000002",
                        "serviceUrl": "https://api-dev.example.com/service/api/v1",
                        "apiUrl": "https://api-dev.example.com"
                    },
                    "prod": {
                        "broker_host": "prod.example.com",
                        "org_id": "00000000-0000-0000-0000-000000000003",
                        "apiUrl": "https://api-prod.example.com"
                    }
                }
                """;

        List<Environment> envs = IntelliJHttpEnvParser.parseIntelliJEnvironments(json);

        assertNotNull(envs);
        assertEquals(envs.size(), 3);

        // 验证第一个环境 - local
        Environment localEnv = envs.stream()
                .filter(e -> "local".equals(e.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(localEnv);
        assertEquals(localEnv.getVariable("broker_host"), "localhost.example.com");
        assertEquals(localEnv.getVariable("org_id"), "00000000-0000-0000-0000-000000000001");
        assertEquals(localEnv.getVariable("account_name"), "test_account_local");
        assertEquals(localEnv.getVariable("apiUrl"), "https://api-local.example.com");
        assertEquals(localEnv.getVariable("serviceUrl"), "http://localhost:9221");

        // 验证第二个环境 - dev
        Environment devEnv = envs.stream()
                .filter(e -> "dev".equals(e.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(devEnv);
        assertEquals(devEnv.getVariable("serviceUrl"), "https://api-dev.example.com/service/api/v1");
        assertEquals(devEnv.getVariable("apiUrl"), "https://api-dev.example.com");

        // 验证第三个环境 - prod
        Environment prodEnv = envs.stream()
                .filter(e -> "prod".equals(e.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(prodEnv);
        assertEquals(prodEnv.getVariable("broker_host"), "prod.example.com");
        assertEquals(prodEnv.getVariable("apiUrl"), "https://api-prod.example.com");
    }

    @Test
    public void testParseEmptyEnvironment() {
        String json = """
                {
                    "empty": {}
                }
                """;

        List<Environment> envs = IntelliJHttpEnvParser.parseIntelliJEnvironments(json);

        assertNotNull(envs);
        assertEquals(envs.size(), 1);
        assertEquals(envs.get(0).getName(), "empty");
        assertTrue(envs.get(0).getVariables() == null || envs.get(0).getVariables().isEmpty());
    }

    @Test
    public void testToIntelliJEnvironmentJson() {
        Environment env = new Environment("dev");
        env.addVariable("host", "localhost");
        env.addVariable("port", "8080");
        env.addVariable("protocol", "http");

        String json = IntelliJHttpEnvParser.toIntelliJEnvironmentJson(env);

        assertNotNull(json);
        assertTrue(json.contains("\"dev\""));
        assertTrue(json.contains("\"host\""));
        assertTrue(json.contains("\"localhost\""));
        assertTrue(json.contains("\"port\""));
        assertTrue(json.contains("\"8080\""));
        assertTrue(json.contains("\"protocol\""));
        assertTrue(json.contains("\"http\""));
    }

    @Test
    public void testToIntelliJEnvironmentsJson() {
        Environment dev = new Environment("dev");
        dev.addVariable("host", "localhost");
        dev.addVariable("port", "8080");

        Environment prod = new Environment("prod");
        prod.addVariable("host", "example.com");
        prod.addVariable("port", "443");

        List<Environment> envs = List.of(dev, prod);
        String json = IntelliJHttpEnvParser.toIntelliJEnvironmentsJson(envs);

        assertNotNull(json);
        assertTrue(json.contains("\"dev\""));
        assertTrue(json.contains("\"prod\""));
        assertTrue(json.contains("\"localhost\""));
        assertTrue(json.contains("\"example.com\""));
        assertTrue(json.contains("\"8080\""));
        assertTrue(json.contains("\"443\""));
    }

    @Test
    public void testRoundTrip() {
        // 原始环境
        Environment original = new Environment("test");
        original.addVariable("key1", "value1");
        original.addVariable("key2", "value2");
        original.addVariable("key3", "value3");

        // 导出为JSON
        String json = IntelliJHttpEnvParser.toIntelliJEnvironmentJson(original);

        // 重新导入
        List<Environment> imported = IntelliJHttpEnvParser.parseIntelliJEnvironments(json);

        // 验证
        assertEquals(imported.size(), 1);
        Environment reimported = imported.get(0);
        assertEquals(reimported.getName(), original.getName());
        assertEquals(reimported.getVariable("key1"), "value1");
        assertEquals(reimported.getVariable("key2"), "value2");
        assertEquals(reimported.getVariable("key3"), "value3");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testParseInvalidJson() {
        String invalidJson = "not a valid json";
        IntelliJHttpEnvParser.parseIntelliJEnvironments(invalidJson);
    }

    @Test
    public void testParseJsonWithTrailingSemicolons() {
        // 测试处理带有尾随分号的JSON（常见的语法错误）
        String jsonWithSemicolons = """
                {
                    "dev": {
                        "host": "localhost";
                        "port": "8080";
                        "protocol": "http"
                    },
                    "prod": {
                        "host": "api.example.com";
                        "port": "443"
                    }
                }
                """;

        List<Environment> envs = IntelliJHttpEnvParser.parseIntelliJEnvironments(jsonWithSemicolons);

        assertNotNull(envs);
        assertEquals(envs.size(), 2);

        Environment devEnv = envs.stream()
                .filter(e -> "dev".equals(e.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(devEnv);
        assertEquals(devEnv.getVariable("host"), "localhost");
        assertEquals(devEnv.getVariable("port"), "8080");
        assertEquals(devEnv.getVariable("protocol"), "http");

        Environment prodEnv = envs.stream()
                .filter(e -> "prod".equals(e.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(prodEnv);
        assertEquals(prodEnv.getVariable("host"), "api.example.com");
        assertEquals(prodEnv.getVariable("port"), "443");
    }
}
