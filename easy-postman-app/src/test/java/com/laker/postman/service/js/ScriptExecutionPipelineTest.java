package com.laker.postman.service.js;

import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.variable.VariableResolver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ScriptExecutionPipelineTest {

    private String originalDataFilePath;
    private String originalGlobalDataFilePath;
    private Path tempEnvFile;
    private Path tempGlobalFile;
    private Environment testEnv;

    @BeforeMethod
    public void setUp() {
        originalDataFilePath = EnvironmentService.getDataFilePath();

        try {
            tempEnvFile = Files.createTempFile("easy-postman-script-pipeline-", ".json");
            Files.writeString(tempEnvFile, "[]");
            tempGlobalFile = Files.createTempFile("easy-postman-script-pipeline-globals-", ".json");
            Files.writeString(tempGlobalFile, "{}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize temporary environment file", e);
        }

        EnvironmentService.setDataFilePath(tempEnvFile.toString());
        originalGlobalDataFilePath = GlobalVariablesService.getInstance().getDataFilePath();
        GlobalVariablesService.getInstance().setDataFilePath(tempGlobalFile.toString());
        VariableResolver.clearTemporaryVariables();

        testEnv = new Environment();
        testEnv.setId("script-pipeline-test-env");
        testEnv.setName("Script Pipeline Test Env");
        EnvironmentService.saveEnvironment(testEnv);
        EnvironmentService.setActiveEnvironment(testEnv.getId());
    }

    @AfterMethod
    public void tearDown() {
        try {
            if (testEnv != null && testEnv.getId() != null) {
                EnvironmentService.deleteEnvironment(testEnv.getId());
            }
            VariableResolver.clearTemporaryVariables();
        } finally {
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

    @Test
    public void shouldSyncPmVariablesToVariableResolverAfterPreScript() {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-test-request";
        request.method = "POST";
        request.url = "{{baseUrl}}/anything/demo";
        request.body = "{\"session\":\"{{sessionId}}\"}";
        request.headersList = new ArrayList<>(List.of(
                new HttpHeader(true, "X-Trace-Id", "{{traceId}}")
        ));
        request.paramsList = new ArrayList<>(List.of(
                new HttpParam(true, "mode", "{{mode}}")
        ));
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.environment.set('baseUrl', 'https://httpbin.org');
                        pm.variables.set('traceId', 'trace-123');
                        pm.variables.set('mode', 'temporary-mode');
                        pm.variables.set('sessionId', 'session-456');
                        """)
                .postScript("")
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
        assertEquals(VariableResolver.resolve("{{traceId}}"), "trace-123");
        assertEquals(VariableResolver.resolve("{{mode}}"), "temporary-mode");
        assertEquals(VariableResolver.resolve("{{sessionId}}"), "session-456");

        PreparedRequestBuilder.replaceVariablesAfterPreScript(request);

        assertTrue(request.url.startsWith("https://httpbin.org/anything/demo"));
        assertTrue(request.url.contains("mode=temporary-mode"));
        assertEquals(request.headersList.get(0).getValue(), "trace-123");
        assertEquals(request.body, "{\"session\":\"session-456\"}");
    }

    @Test
    public void shouldResolveGlobalsSetInPreScript() throws Exception {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-global-request";
        request.method = "GET";
        request.url = "{{globalBaseUrl}}/users";
        request.headersList = new ArrayList<>(List.of(
                new HttpHeader(true, "X-App-Name", "{{appName}}")
        ));
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.globals.set('globalBaseUrl', 'https://global.example.com');
                        pm.globals.set('appName', 'easy-postman');
                        """)
                .postScript("")
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
        assertEquals(VariableResolver.resolve("{{globalBaseUrl}}"), "https://global.example.com");
        assertEquals(VariableResolver.resolve("{{appName}}"), "easy-postman");

        PreparedRequestBuilder.replaceVariablesAfterPreScript(request);

        assertEquals(request.url, "https://global.example.com/users");
        assertEquals(request.headersList.get(0).getValue(), "easy-postman");

        Environment persistedGlobals = JSONUtil.toBean(Files.readString(tempGlobalFile), Environment.class);
        assertEquals(persistedGlobals.get("globalBaseUrl"), "https://global.example.com");
        assertEquals(persistedGlobals.get("appName"), "easy-postman");
    }
}
