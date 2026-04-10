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
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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

    @Test
    public void shouldSupportPmGlobalsMethodsViaScopedApi() {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-global-methods-request";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.globals.clear();
                        pm.globals.set('apiHost', 'https://global.example.com');
                        pm.variables.set('globalsHasApiHost', String(pm.globals.has('apiHost')));
                        pm.variables.set('globalsGetApiHost', pm.globals.get('apiHost'));
                        pm.variables.set('globalsReplaceIn', pm.globals.replaceIn('{{apiHost}}/users'));
                        pm.variables.set('globalsObjectApiHost', pm.globals.toObject().apiHost);
                        pm.globals.unset('apiHost');
                        pm.variables.set('globalsHasAfterUnset', String(pm.globals.has('apiHost')));
                        pm.globals.set('cleanupKey', 'cleanup');
                        pm.globals.clear();
                        pm.variables.set('globalsSizeAfterClear', String(Object.keys(pm.globals.toObject()).length));
                        """)
                .postScript("")
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
        assertEquals(VariableResolver.resolve("{{globalsHasApiHost}}"), "true");
        assertEquals(VariableResolver.resolve("{{globalsGetApiHost}}"), "https://global.example.com");
        assertEquals(VariableResolver.resolve("{{globalsReplaceIn}}"), "https://global.example.com/users");
        assertEquals(VariableResolver.resolve("{{globalsObjectApiHost}}"), "https://global.example.com");
        assertEquals(VariableResolver.resolve("{{globalsHasAfterUnset}}"), "false");
        assertEquals(VariableResolver.resolve("{{globalsSizeAfterClear}}"), "0");
        assertTrue(GlobalVariablesService.getInstance().getAll().isEmpty(), "Globals should be empty after clear()");
    }

    @Test
    public void shouldResolvePmVariablesAcrossScopesAndPersistEnvironmentChanges() throws Exception {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-variable-scopes-request";
        request.method = "GET";
        request.url = "{{shared}}/{{envOnly}}/{{globalOnly}}";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        GlobalVariablesService.getInstance().getGlobalVariables().set("shared", "global-value");
        GlobalVariablesService.getInstance().getGlobalVariables().set("globalOnly", "global-only");

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.environment.set('shared', 'env-value');
                        pm.environment.set('envOnly', 'env-only');
                        pm.variables.set('shared', 'local-value');
                        pm.variables.set('localOnly', 'local-only');
                        pm.variables.set('variablesGetShared', pm.variables.get('shared'));
                        pm.variables.set('variablesGetEnvOnly', pm.variables.get('envOnly'));
                        pm.variables.set('variablesGetGlobalOnly', pm.variables.get('globalOnly'));
                        pm.variables.set('variablesHasGlobalOnly', String(pm.variables.has('globalOnly')));
                        var allVariables = pm.variables.toObject();
                        pm.variables.set('variablesObjectShared', allVariables.shared);
                        pm.variables.set('variablesObjectEnvOnly', allVariables.envOnly);
                        pm.variables.set('variablesObjectGlobalOnly', allVariables.globalOnly);
                        pm.variables.set('variablesReplaceIn', pm.variables.replaceIn('{{shared}}/{{envOnly}}/{{globalOnly}}'));
                        pm.variables.unset('localOnly');
                        pm.variables.set('variablesHasLocalOnlyAfterUnset', String(pm.variables.has('localOnly')));
                        """)
                .postScript("")
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
        assertEquals(VariableResolver.resolve("{{shared}}"), "local-value");
        assertEquals(VariableResolver.resolve("{{envOnly}}"), "env-only");
        assertEquals(VariableResolver.resolve("{{globalOnly}}"), "global-only");
        assertEquals(VariableResolver.resolve("{{variablesGetShared}}"), "local-value");
        assertEquals(VariableResolver.resolve("{{variablesGetEnvOnly}}"), "env-only");
        assertEquals(VariableResolver.resolve("{{variablesGetGlobalOnly}}"), "global-only");
        assertEquals(VariableResolver.resolve("{{variablesHasGlobalOnly}}"), "true");
        assertEquals(VariableResolver.resolve("{{variablesObjectShared}}"), "local-value");
        assertEquals(VariableResolver.resolve("{{variablesObjectEnvOnly}}"), "env-only");
        assertEquals(VariableResolver.resolve("{{variablesObjectGlobalOnly}}"), "global-only");
        assertEquals(VariableResolver.resolve("{{variablesReplaceIn}}"), "local-value/env-only/global-only");
        assertEquals(VariableResolver.resolve("{{variablesHasLocalOnlyAfterUnset}}"), "false");
        assertFalse(VariableResolver.isVariableDefined("localOnly"), "Unset local variable should not be resolvable");

        PreparedRequestBuilder.replaceVariablesAfterPreScript(request);
        assertEquals(request.url, "local-value/env-only/global-only");

        List<Environment> persistedEnvironments = JSONUtil.toList(Files.readString(tempEnvFile), Environment.class);
        Map<String, String> persistedActiveEnv = persistedEnvironments.stream()
                .filter(Environment::isActive)
                .findFirst()
                .map(Environment::toObject)
                .orElseThrow();
        assertEquals(persistedActiveEnv.get("shared"), "env-value");
        assertEquals(persistedActiveEnv.get("envOnly"), "env-only");
    }
}
