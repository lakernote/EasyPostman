package com.laker.postman.service.js;

import com.laker.postman.model.Environment;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;


import cn.hutool.json.JSONUtil;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.http.request.PreparedRequestFinalizer;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataVariableService;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.service.variable.VariablesService;
import com.laker.postman.variable.VariableType;
import com.laker.postman.service.variable.VariableResolver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
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
        clearExecutionContext();

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
            clearExecutionContext();
            RequestExecutionContext.clearCurrentScope();
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
    public void shouldNotPrepareBindingsForBlankPreScriptWhenPostScriptIsBlank() {
        PreparedRequest request = new PreparedRequest();
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("")
                .postScript("")
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess());
        assertNull(pipeline.getBindings());
    }

    @Test
    public void shouldPrepareBindingsLazilyForPostScriptWhenPreScriptIsBlank() {
        PreparedRequest request = new PreparedRequest();
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("")
                .postScript("pm.test('status', function () { pm.response.to.have.status(200); });")
                .build();
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new java.util.LinkedHashMap<>();

        assertTrue(pipeline.executePreScript().isSuccess());
        ScriptExecutionResult postResult = pipeline.executePostScript(response);

        assertTrue(postResult.isSuccess());
        assertEquals(postResult.getTestResults().size(), 1);
        assertTrue(postResult.getTestResults().get(0).passed);
    }

    @Test
    public void shouldSupportLegacyPostmanVariableAndResponseAliasesInPostScript() {
        PreparedRequest request = new PreparedRequest();
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("")
                .postScript("""
                        var response = JSON.parse(pm.response.text())
                        pm.setEnvironmentVariable("smal2BearerToken", response.BearerToken)
                        pm.test('Status code is 200', function () {
                            pm.response.to.have.status(200);
                        });

                        var legacyResponse = JSON.parse(responseBody)
                        pm.setEnvironmentVariable("legacyBearerToken", legacyResponse.BearerToken)

                        var response = JSON.parse(pm.response.text())
                        postman.setEnvironmentVariable("access_token", response.access_token)

                        var accountExpiration = response.accountExpiration;

                        pm.test('password expiration', function () {
                            var jsonData = pm.response.json();
                            pm.expect(accountExpiration).to.be.above(30)
                        });

                        console.log('expires in ' + accountExpiration + ' days')
                        pm.setEnvironmentVariable("legacyStatusCode", String(statusCode))
                        """)
                .build();
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new java.util.LinkedHashMap<>();
        response.body = """
                {"BearerToken":"bearer-token-value","access_token":"access-token-value","accountExpiration":31}
                """;

        ScriptExecutionResult postResult = pipeline.executePostScript(response);

        assertTrue(postResult.isSuccess(), postResult.getErrorMessage());
        assertEquals(postResult.getTestResults().size(), 2);
        assertTrue(postResult.getTestResults().stream().allMatch(result -> result.passed));
        assertEquals(EnvironmentService.getActiveEnvironment().get("smal2BearerToken"), "bearer-token-value");
        assertEquals(EnvironmentService.getActiveEnvironment().get("legacyBearerToken"), "bearer-token-value");
        assertEquals(EnvironmentService.getActiveEnvironment().get("access_token"), "access-token-value");
        assertEquals(EnvironmentService.getActiveEnvironment().get("legacyStatusCode"), "200");
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
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{traceId}}"), "trace-123");
            assertEquals(VariableResolver.resolve("{{mode}}"), "temporary-mode");
            assertEquals(VariableResolver.resolve("{{sessionId}}"), "session-456");
        });

        pipeline.finalizeRequest();

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
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{globalBaseUrl}}"), "https://global.example.com");
            assertEquals(VariableResolver.resolve("{{appName}}"), "easy-postman");
        });

        pipeline.finalizeRequest();

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
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{globalsHasApiHost}}"), "true");
            assertEquals(VariableResolver.resolve("{{globalsGetApiHost}}"), "https://global.example.com");
            assertEquals(VariableResolver.resolve("{{globalsReplaceIn}}"), "https://global.example.com/users");
            assertEquals(VariableResolver.resolve("{{globalsObjectApiHost}}"), "https://global.example.com");
            assertEquals(VariableResolver.resolve("{{globalsHasAfterUnset}}"), "false");
            assertEquals(VariableResolver.resolve("{{globalsSizeAfterClear}}"), "0");
        });
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
        pipeline.withExecutionContext(() -> {
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
        });

        pipeline.finalizeRequest();
        assertEquals(request.url, "local-value/env-only/global-only");

        List<Environment> persistedEnvironments = JSONUtil.toList(Files.readString(tempEnvFile), Environment.class);
        Map<String, String> persistedActiveEnv = persistedEnvironments.stream()
                .filter(Environment::isActive)
                .findFirst()
                .map(Environment::getVariables)
                .orElseThrow();
        assertEquals(persistedActiveEnv.get("shared"), "env-value");
        assertEquals(persistedActiveEnv.get("envOnly"), "env-only");
    }

    @Test
    public void shouldSupportPmEnvAliasForHistoricalScripts() {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-pm-env-alias-request";
        request.method = "GET";
        request.url = "https://example.com/{{aliasValue}}/{{canonicalValue}}";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.env.set('aliasValue', 'from-alias');
                        pm.environment.set('canonicalValue', 'from-canonical');
                        pm.variables.set('aliasCanReadCanonical', pm.env.get('canonicalValue'));
                        pm.variables.set('canonicalCanReadAlias', pm.environment.get('aliasValue'));
                        """)
                .postScript("")
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Pre-request scripts using pm.env should remain compatible");
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{aliasCanReadCanonical}}"), "from-canonical");
            assertEquals(VariableResolver.resolve("{{canonicalCanReadAlias}}"), "from-alias");
        });

        pipeline.finalizeRequest();
        assertEquals(request.url, "https://example.com/from-alias/from-canonical");
    }

    @Test
    public void shouldNotFallBackToActiveEnvironmentWhenSupplierReturnsNull() {
        testEnv.addVariable("activeOnly", "active-value");
        EnvironmentService.saveEnvironment(testEnv);
        EnvironmentService.setActiveEnvironment(testEnv.getId());

        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-null-env-supplier-request";
        request.method = "GET";
        request.url = "https://example.com";

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.set('hasActiveOnly', String(pm.environment.has('activeOnly')));
                        """)
                .postScript("")
                .environmentSupplier(() -> null)
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
        pipeline.withExecutionContext(() ->
                assertEquals(VariableResolver.resolve("{{hasActiveOnly}}"), "false"));
    }

    @Test
    public void shouldPersistPmVariablesAcrossRequestsInSameExecutionContext() {
        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        PreparedRequest createOrderRequest = new PreparedRequest();
        createOrderRequest.id = "script-pipeline-create-order-request";
        createOrderRequest.method = "POST";
        createOrderRequest.url = "https://example.com/orders";
        createOrderRequest.headersList = new ArrayList<>();
        createOrderRequest.paramsList = new ArrayList<>();
        createOrderRequest.formDataList = new ArrayList<>();
        createOrderRequest.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline createOrderPipeline = ScriptExecutionPipeline.builder()
                .request(createOrderRequest)
                .preScript("")
                .postScript("""
                        pm.variables.set('orderId', pm.response.json().id);
                        """)
                .sharedExecutionContext(sharedContext)
                .build();

        assertTrue(createOrderPipeline.executePreScript().isSuccess());

        HttpResponse createOrderResponse = new HttpResponse();
        createOrderResponse.code = 200;
        createOrderResponse.headers = new java.util.LinkedHashMap<>();
        createOrderResponse.body = "{\"id\":\"order-123\"}";

        ScriptExecutionResult createOrderPostResult = createOrderPipeline.executePostScript(createOrderResponse);
        assertTrue(createOrderPostResult.isSuccess(), "Create order post-script should execute successfully");
        createOrderPipeline.withExecutionContext(() ->
                assertEquals(VariableResolver.resolve("{{orderId}}"), "order-123"));

        PreparedRequest queryOrderRequest = new PreparedRequest();
        queryOrderRequest.id = "script-pipeline-query-order-request";
        queryOrderRequest.method = "GET";
        queryOrderRequest.url = "https://example.com/orders/{{orderId}}";
        queryOrderRequest.headersList = new ArrayList<>();
        queryOrderRequest.paramsList = new ArrayList<>();
        queryOrderRequest.formDataList = new ArrayList<>();
        queryOrderRequest.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline queryOrderPipeline = ScriptExecutionPipeline.builder()
                .request(queryOrderRequest)
                .preScript("""
                        pm.variables.set('seenOrderId', pm.variables.get('orderId'));
                        """)
                .postScript("")
                .sharedExecutionContext(sharedContext)
                .build();

        ScriptExecutionResult queryOrderPreResult = queryOrderPipeline.executePreScript();
        assertTrue(queryOrderPreResult.isSuccess(), "Query order pre-script should execute successfully");
        queryOrderPipeline.withExecutionContext(() ->
                assertEquals(VariableResolver.resolve("{{seenOrderId}}"), "order-123"));

        queryOrderPipeline.finalizeRequest();
        assertEquals(queryOrderRequest.url, "https://example.com/orders/order-123");
    }

    @Test
    public void shouldCreateBasicAuthorizationHeaderFromSharedExecutionContextVariables() {
        ExecutionVariableContext sharedContext = new ExecutionVariableContext();

        PreparedRequest setupRequest = new PreparedRequest();
        setupRequest.id = "script-pipeline-basic-auth-shared-setup";
        setupRequest.method = "POST";
        setupRequest.url = "https://example.com/login";
        setupRequest.headersList = new ArrayList<>();
        setupRequest.paramsList = new ArrayList<>();
        setupRequest.formDataList = new ArrayList<>();
        setupRequest.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline setupPipeline = ScriptExecutionPipeline.builder()
                .request(setupRequest)
                .preScript("")
                .postScript("""
                        pm.variables.set('username', pm.response.json().username);
                        pm.variables.set('password', pm.response.json().password);
                        """)
                .sharedExecutionContext(sharedContext)
                .build();

        assertTrue(setupPipeline.executePreScript().isSuccess(), "Setup pre-request script should execute successfully");

        HttpResponse setupResponse = new HttpResponse();
        setupResponse.code = 200;
        setupResponse.headers = new java.util.LinkedHashMap<>();
        setupResponse.body = "{\"username\":\"runner\",\"password\":\"vu-secret\"}";

        assertTrue(setupPipeline.executePostScript(setupResponse).isSuccess(),
                "Setup post-request script should execute successfully");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-basic-auth-shared-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BASIC.getConstant());
        authItem.setAuthUsername("{{username}}");
        authItem.setAuthPassword("{{password}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        assertEquals(findAuthorizationHeader(authRequest), null,
                "Authorization header should not be materialized before the shared context is attached");

        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("")
                .postScript("")
                .sharedExecutionContext(sharedContext)
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        authPipeline.finalizeRequest();

        assertEquals(findAuthorizationHeader(authRequest),
                "Basic " + Base64.getEncoder().encodeToString("runner:vu-secret".getBytes()));
    }

    @Test
    public void shouldCreateBasicAuthorizationHeaderFromSameRequestPreScriptVariables() {
        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-basic-auth-same-request-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BASIC.getConstant());
        authItem.setAuthUsername("{{username}}");
        authItem.setAuthPassword("{{password}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("""
                        pm.variables.set('username', 'runner');
                        pm.variables.set('password', 'same-request-secret');
                        """)
                .postScript("")
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        authPipeline.finalizeRequest();

        assertEquals(findAuthorizationHeader(authRequest),
                "Basic " + Base64.getEncoder().encodeToString("runner:same-request-secret".getBytes()));
    }

    @Test
    public void shouldRefreshPreviewAuthorizationHeaderWhenPreScriptOverridesCredential() {
        testEnv.set("token", "env-token");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-bearer-auth-refresh-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BEARER.getConstant());
        authItem.setAuthToken("{{token}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        assertEquals(findAuthorizationHeader(authRequest), "Bearer env-token");

        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("""
                        pm.variables.set('token', 'runtime-token');
                        """)
                .postScript("")
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        authPipeline.finalizeRequest();

        assertEquals(findAuthorizationHeader(authRequest), "Bearer runtime-token");
    }

    @Test
    public void shouldExposeAuthTabAuthorizationToPreScriptWhenCredentialsAreAlreadyResolvable() {
        testEnv.set("username", "runner");
        testEnv.set("password", "env-secret");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-basic-auth-visible-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BASIC.getConstant());
        authItem.setAuthUsername("{{username}}");
        authItem.setAuthPassword("{{password}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("""
                        pm.variables.set('authHeaderBeforeScript', pm.request.headers.get('Authorization'));
                        pm.variables.set('authHeaderExistsBeforeScript', String(pm.request.headers.has('Authorization')));
                        """)
                .postScript("")
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        authPipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{authHeaderExistsBeforeScript}}"), "true");
            assertEquals(VariableResolver.resolve("{{authHeaderBeforeScript}}"),
                    "Basic " + Base64.getEncoder().encodeToString("runner:env-secret".getBytes()));
        });
    }

    @Test
    public void shouldUseConfiguredAuthWhenPreScriptRemovesAuthorizationHeader() {
        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        sharedContext.getVariables().put("username", "runner");
        sharedContext.getVariables().put("password", "vu-secret");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-basic-auth-remove-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BASIC.getConstant());
        authItem.setAuthUsername("{{username}}");
        authItem.setAuthPassword("{{password}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("""
                        pm.request.headers.remove('Authorization');
                        """)
                .postScript("")
                .sharedExecutionContext(sharedContext)
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        assertEquals(findAuthorizationHeader(authRequest), null,
                "Pre-request script should see the header removed before final request preparation");

        authPipeline.finalizeRequest();
        assertEquals(findAuthorizationHeader(authRequest),
                "Basic " + Base64.getEncoder().encodeToString("runner:vu-secret".getBytes()));
    }

    @Test
    public void shouldPreferScriptAddedAuthorizationHeaderOverPreviewHeader() {
        testEnv.set("token", "env-token");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-bearer-auth-script-add-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BEARER.getConstant());
        authItem.setAuthToken("{{token}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        assertEquals(countAuthorizationHeaders(authRequest), 1);
        assertEquals(findAuthorizationHeader(authRequest), "Bearer env-token");

        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("""
                        pm.request.headers.add({
                            key: 'Authorization',
                            value: 'Bearer script-token'
                        });
                        """)
                .postScript("")
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        authPipeline.finalizeRequest();

        assertEquals(countAuthorizationHeaders(authRequest), 1);
        assertEquals(findAuthorizationHeader(authRequest), "Bearer script-token");
    }

    @Test
    public void shouldRemovePreviewAuthorizationHeaderWhenCredentialsBecomeUnavailable() {
        testEnv.set("token", "env-token");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("script-pipeline-bearer-auth-clear-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BEARER.getConstant());
        authItem.setAuthToken("{{token}}");

        PreparedRequest authRequest = PreparedRequestFactory.build(authItem);
        assertEquals(findAuthorizationHeader(authRequest), "Bearer env-token");

        ScriptExecutionPipeline authPipeline = ScriptExecutionPipeline.builder()
                .request(authRequest)
                .preScript("""
                        pm.environment.unset('token');
                        """)
                .postScript("")
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorization(authItem, true))
                .build();

        assertTrue(authPipeline.executePreScript().isSuccess(), "Auth pre-request script should execute successfully");
        authPipeline.finalizeRequest();

        assertEquals(findAuthorizationHeader(authRequest), null);
    }

    @Test
    public void shouldMaterializeAutomaticAuthorizationOutsideScriptPipeline() {
        testEnv.set("username", "runner");
        testEnv.set("password", "curl-secret");

        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("prepared-request-basic-auth-materialize-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BASIC.getConstant());
        authItem.setAuthUsername("{{username}}");
        authItem.setAuthPassword("{{password}}");

        PreparedRequest request = PreparedRequestFactory.build(authItem);
        PreparedRequestFinalizer.finalizeForSend(request, authItem, true);

        assertEquals(findAuthorizationHeader(request),
                "Basic " + Base64.getEncoder().encodeToString("runner:curl-secret".getBytes()));
    }

    @Test
    public void shouldSkipDeferredAuthorizationWhenCredentialPlaceholderIsStillUnresolved() {
        HttpRequestItem authItem = new HttpRequestItem();
        authItem.setId("prepared-request-bearer-auth-unresolved-item");
        authItem.setMethod("GET");
        authItem.setUrl("https://example.com/profile");
        authItem.setAuthType(AuthType.BEARER.getConstant());
        authItem.setAuthToken("{{missingToken}}");

        PreparedRequest request = PreparedRequestFactory.build(authItem);
        PreparedRequestFinalizer.finalizeForSend(request, authItem, true);

        assertEquals(findAuthorizationHeader(request), null);
    }

    @Test
    public void shouldRetainVariablesRecreatedAfterClearAcrossLaterScriptPhases() {
        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-clear-and-reset-request";
        request.method = "GET";
        request.url = "https://example.com/orders/{{token}}";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.set('stale', 'old-value');
                        pm.variables.clear();
                        pm.variables.set('token', 'token-789');
                        """)
                .postScript("""
                        pm.variables.set('postToken', pm.variables.get('token'));
                        """)
                .sharedExecutionContext(sharedContext)
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();
        assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");

        pipeline.finalizeRequest();
        assertEquals(request.url, "https://example.com/orders/token-789");

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new java.util.LinkedHashMap<>();
        response.body = "{\"ok\":true}";

        ScriptExecutionResult postResult = pipeline.executePostScript(response);
        assertTrue(postResult.isSuccess(), "Post-request script should execute successfully");
        pipeline.withExecutionContext(() ->
                assertEquals(VariableResolver.resolve("{{postToken}}"), "token-789"));

        PreparedRequest nextRequest = new PreparedRequest();
        nextRequest.id = "script-pipeline-follow-up-request";
        nextRequest.method = "GET";
        nextRequest.url = "https://example.com/orders/{{followUpToken}}";
        nextRequest.headersList = new ArrayList<>();
        nextRequest.paramsList = new ArrayList<>();
        nextRequest.formDataList = new ArrayList<>();
        nextRequest.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline nextPipeline = ScriptExecutionPipeline.builder()
                .request(nextRequest)
                .preScript("""
                        pm.variables.set('followUpToken', pm.variables.get('token'));
                        """)
                .postScript("")
                .sharedExecutionContext(sharedContext)
                .build();

        ScriptExecutionResult nextPreResult = nextPipeline.executePreScript();
        assertTrue(nextPreResult.isSuccess(), "Follow-up pre-request script should execute successfully");

        nextPipeline.finalizeRequest();
        assertEquals(nextRequest.url, "https://example.com/orders/token-789");
    }

    @Test
    public void shouldKeepExecutionContextWhenPostScriptRunsOnAnotherThread() throws Exception {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-cross-thread-request";
        request.method = "GET";
        request.url = "https://example.com/orders/{{orderId}}";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.set('orderId', 'order-456');
                        pm.variables.set('requestKey', 'req-789');
                        """)
                .postScript("""
                        pm.variables.set('postOrderId', pm.variables.get('orderId'));
                        pm.variables.set('postRequestKey', pm.variables.get('requestKey'));
                        """)
                .sharedExecutionContext(sharedContext)
                .build();

        assertTrue(pipeline.executePreScript().isSuccess(), "Pre-request script should execute successfully");

        AtomicReference<ScriptExecutionResult> postResultRef = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            HttpResponse response = new HttpResponse();
            response.code = 200;
            response.headers = new java.util.LinkedHashMap<>();
            response.body = "{\"ok\":true}";
            postResultRef.set(pipeline.executePostScript(response));
        }, "script-pipeline-cross-thread-worker");

        worker.start();
        worker.join();

        ScriptExecutionResult postResult = postResultRef.get();
        assertTrue(postResult != null && postResult.isSuccess(), "Cross-thread post-script should execute successfully");
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{postOrderId}}"), "order-456");
            assertEquals(VariableResolver.resolve("{{postRequestKey}}"), "req-789");
        });
    }

    @Test
    public void shouldPreferPipelineOwnedContextOverStaleCallbackThreadContext() throws Exception {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-stale-thread-context-request";
        request.method = "GET";
        request.url = "https://example.com/orders/{{orderId}}";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        sharedContext.replaceIterationData(Map.of("csvUserId", "csv-123"));
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.set('orderId', 'order-456');
                        """)
                .postScript("""
                        pm.variables.set('resolvedOrderId', pm.variables.get('orderId'));
                        pm.variables.set('resolvedCsvUserId', pm.iterationData.get('csvUserId'));
                        """)
                .sharedExecutionContext(sharedContext)
                .build();

        assertTrue(pipeline.executePreScript().isSuccess(), "Pre-request script should execute successfully");

        AtomicReference<ScriptExecutionResult> postResultRef = new AtomicReference<>();
        AtomicReference<String> resolvedOrderIdRef = new AtomicReference<>();
        AtomicReference<String> resolvedCsvUserIdRef = new AtomicReference<>();
        Thread callbackThread = new Thread(() -> {
            VariablesService.getInstance().attachContextMap(new java.util.concurrent.ConcurrentHashMap<>(Map.of(
                    "orderId", "stale-order"
            )));
            IterationDataVariableService.getInstance().attachContextMap(new java.util.concurrent.ConcurrentHashMap<>(Map.of(
                    "csvUserId", "stale-csv"
            )));

            try {
                HttpResponse response = new HttpResponse();
                response.code = 200;
                response.headers = new java.util.LinkedHashMap<>();
                response.body = "{\"ok\":true}";
                postResultRef.set(pipeline.executePostScript(response));
                resolvedOrderIdRef.set(sharedContext.getVariables().get("resolvedOrderId"));
                resolvedCsvUserIdRef.set(sharedContext.getVariables().get("resolvedCsvUserId"));
            } finally {
                clearExecutionContext();
            }
        }, "script-pipeline-stale-callback-thread");

        callbackThread.start();
        callbackThread.join();

        ScriptExecutionResult postResult = postResultRef.get();
        assertTrue(postResult != null && postResult.isSuccess(), "Post-request script should execute successfully");
        assertEquals(resolvedOrderIdRef.get(), "order-456");
        assertEquals(resolvedCsvUserIdRef.get(), "csv-123");
    }

    @Test
    public void shouldIgnoreCurrentThreadContextWhenPipelineHasExplicitFreshContext() {
        VariablesService.getInstance().attachContextMap(new java.util.concurrent.ConcurrentHashMap<>(Map.of(
                "orderId", "stale-order"
        )));
        IterationDataVariableService.getInstance().attachContextMap(new java.util.concurrent.ConcurrentHashMap<>(Map.of(
                "csvUserId", "stale-csv"
        )));

        try {
            PreparedRequest request = new PreparedRequest();
            request.id = "script-pipeline-fresh-context-request";
            request.method = "GET";
            request.url = "https://example.com/orders";
            request.headersList = new ArrayList<>();
            request.paramsList = new ArrayList<>();
            request.formDataList = new ArrayList<>();
            request.urlencodedList = new ArrayList<>();

            ExecutionVariableContext sharedContext = new ExecutionVariableContext();
            ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                    .request(request)
                    .preScript("""
                            pm.variables.set('capturedOrderId', pm.variables.get('orderId') === null ? 'missing' : pm.variables.get('orderId'));
                            pm.variables.set('capturedCsvUserId', pm.iterationData.get('csvUserId') === null ? 'missing' : pm.iterationData.get('csvUserId'));
                            """)
                    .postScript("")
                    .sharedExecutionContext(sharedContext)
                    .build();

            ScriptExecutionResult preResult = pipeline.executePreScript();
            assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
            pipeline.withExecutionContext(() -> {
                assertEquals(VariableResolver.resolve("{{capturedOrderId}}"), "missing");
                assertEquals(VariableResolver.resolve("{{capturedCsvUserId}}"), "missing");
            });
        } finally {
            clearExecutionContext();
        }
    }

    @Test
    public void shouldUseFreshContextByDefaultInsteadOfCurrentThreadContext() {
        VariablesService.getInstance().attachContextMap(new java.util.concurrent.ConcurrentHashMap<>(Map.of(
                "orderId", "stale-order"
        )));
        IterationDataVariableService.getInstance().attachContextMap(new java.util.concurrent.ConcurrentHashMap<>(Map.of(
                "csvUserId", "stale-csv"
        )));

        try {
            PreparedRequest request = new PreparedRequest();
            request.id = "script-pipeline-default-fresh-context-request";
            request.method = "GET";
            request.url = "https://example.com/orders";
            request.headersList = new ArrayList<>();
            request.paramsList = new ArrayList<>();
            request.formDataList = new ArrayList<>();
            request.urlencodedList = new ArrayList<>();

            ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                    .request(request)
                    .preScript("""
                            pm.variables.set('capturedOrderId', pm.variables.get('orderId') === null ? 'missing' : pm.variables.get('orderId'));
                            pm.variables.set('capturedCsvUserId', pm.iterationData.get('csvUserId') === null ? 'missing' : pm.iterationData.get('csvUserId'));
                            """)
                    .postScript("")
                    .build();

            ScriptExecutionResult preResult = pipeline.executePreScript();
            assertTrue(preResult.isSuccess(), "Pre-request script should execute successfully");
            pipeline.withExecutionContext(() -> {
                assertEquals(VariableResolver.resolve("{{capturedOrderId}}"), "missing");
                assertEquals(VariableResolver.resolve("{{capturedCsvUserId}}"), "missing");
            });
        } finally {
            clearExecutionContext();
        }
    }

    @Test
    public void shouldResolveGroupVariablesWhenScriptRunsOnAnotherThread() throws Exception {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-group-context-request";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.set('capturedTenantId', pm.variables.get('tenantId'));
                        """)
                .postScript("")
                .requestExecutionScope(RequestExecutionScope.fromGroupVariables(Map.of("tenantId", "group-tenant")))
                .build();

        AtomicReference<ScriptExecutionResult> resultRef = new AtomicReference<>();
        Thread worker = new Thread(() -> resultRef.set(pipeline.executePreScript()), "script-pipeline-group-worker");
        worker.start();
        worker.join();

        ScriptExecutionResult preResult = resultRef.get();
        assertTrue(preResult != null && preResult.isSuccess(), "Pre-request script should execute successfully");
        pipeline.withExecutionContext(() ->
                assertEquals(VariableResolver.resolve("{{capturedTenantId}}"), "group-tenant"));
    }

    @Test
    public void shouldSupportNegatedExpectationChainInScripts() {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-negated-expectation-request";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.environment.set('tenantId', 'team-test');
                        """)
                .postScript("""
                        pm.test('Negated eql works', function () {
                            pm.expect(pm.environment.get('tenantId')).to.not.eql(null);
                            pm.expect(pm.environment.get('tenantId')).to.not.eql('other-team');
                        });
                        """)
                .build();

        assertTrue(pipeline.executePreScript().isSuccess(), "Pre-request script should execute successfully");

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new java.util.LinkedHashMap<>();
        response.body = "{\"ok\":true}";

        ScriptExecutionResult postResult = pipeline.executePostScript(response);

        assertTrue(postResult.isSuccess(), "Post-request script should execute successfully");
        assertTrue(postResult.hasTestResults(), "Negated assertion test should produce test results");
        assertTrue(postResult.allTestsPassed(), "Negated assertion chain should pass in scripts");
    }

    @Test
    public void shouldSupportPostmanStyleExpectationPatternsUsedByBuiltInSnippets() {
        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-snippet-expectation-patterns-request";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("")
                .postScript("""
                        pm.test('Snippet assertion patterns work', function () {
                            var jsonData = pm.response.json();
                            var schema = {
                                type: 'object',
                                required: ['code', 'data'],
                                properties: {
                                    code: { type: 'number' },
                                    data: { type: 'object' }
                                }
                            };
                            var errors = [];

                            pm.expect(jsonData).to.have.jsonSchema(schema);
                            pm.expect(errors).to.have.lengthOf(0);
                            pm.expect(jsonData.list).to.be.an('array').that.is.not.empty;
                            pm.expect(jsonData).to.have.property('success', true);
                            pm.expect(jsonData.status).to.be.oneOf(['ok', 'done']);
                            pm.expect(jsonData.tags).to.include('api');
                            pm.expect(jsonData).to.have.keys(['code', 'data', 'success', 'status', 'tags', 'list']);
                        });
                        """)
                .build();

        assertTrue(pipeline.executePreScript().isSuccess(), "Pre-request script should execute successfully");

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new java.util.LinkedHashMap<>();
        response.body = """
                {
                  "code": 0,
                  "data": {"id": "order-1"},
                  "success": true,
                  "status": "ok",
                  "tags": ["api", "smoke"],
                  "list": [1]
                }
                """;

        ScriptExecutionResult postResult = pipeline.executePostScript(response);

        assertTrue(postResult.isSuccess(), "Built-in snippet assertion patterns should execute successfully");
        assertTrue(postResult.hasTestResults(), "Snippet assertion pattern test should produce a result");
        assertTrue(postResult.allTestsPassed(), "Snippet assertion pattern test should pass");
    }

    @Test
    public void shouldExposeIterationDataWithoutMixingItIntoExecutionVariables() {
        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        sharedContext.replaceIterationData(Map.of(
                "csvOrderId", "csv-001",
                "shared", "csv-shared"
        ));

        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-iteration-data-request";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        testEnv.set("shared", "env-shared");

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.unset('csvOrderId');
                        pm.variables.clear();
                        pm.variables.set('iterationOrderIdAfterClear', pm.iterationData.get('csvOrderId'));
                        pm.variables.set('sharedAfterClear', pm.variables.get('shared'));
                        """)
                .postScript("")
                .sharedExecutionContext(sharedContext)
                .build();

        ScriptExecutionResult preResult = pipeline.executePreScript();

        assertTrue(preResult.isSuccess(), "Iteration data pre-script should execute successfully");
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{iterationOrderIdAfterClear}}"), "csv-001");
            assertEquals(VariableResolver.resolve("{{sharedAfterClear}}"), "env-shared");
            assertEquals(VariableResolver.resolve("{{shared}}"), "csv-shared");
            assertEquals(VariableResolver.getVariableType("shared"), VariableType.ITERATION_DATA);
            assertEquals(VariableResolver.resolve("{{csvOrderId}}"), "csv-001");
        });
    }

    @Test
    public void shouldExposePostmanStyleIterationInfoInScripts() {
        ExecutionVariableContext sharedContext = new ExecutionVariableContext();
        sharedContext.setIterationInfo(2, 5);
        sharedContext.replaceIterationData(Map.of("userId", "user-003"));

        PreparedRequest request = new PreparedRequest();
        request.id = "script-pipeline-pm-info-request";
        request.name = "PM Info Demo";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript("""
                        pm.variables.set('iteration', String(pm.info.iteration));
                        pm.variables.set('iterationCount', String(pm.info.iterationCount));
                        pm.variables.set('eventName', pm.info.eventName);
                        pm.variables.set('requestName', pm.info.requestName);
                        pm.variables.set('requestId', pm.info.requestId);
                        pm.variables.set('dataI', pm.iterationData.get('i') === null ? 'missing' : pm.iterationData.get('i'));
                        """)
                .postScript("""
                        pm.variables.set('postEventName', pm.info.eventName);
                        """)
                .sharedExecutionContext(sharedContext)
                .build();

        assertTrue(pipeline.executePreScript().isSuccess(), "Pre-request script should execute successfully");
        pipeline.withExecutionContext(() -> {
            assertEquals(VariableResolver.resolve("{{iteration}}"), "2");
            assertEquals(VariableResolver.resolve("{{iterationCount}}"), "5");
            assertEquals(VariableResolver.resolve("{{eventName}}"), "prerequest");
            assertEquals(VariableResolver.resolve("{{requestName}}"), "PM Info Demo");
            assertEquals(VariableResolver.resolve("{{requestId}}"), "script-pipeline-pm-info-request");
            assertEquals(VariableResolver.resolve("{{dataI}}"), "missing");
        });

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new java.util.LinkedHashMap<>();
        response.body = "{\"ok\":true}";

        ScriptExecutionResult postResult = pipeline.executePostScript(response);

        assertTrue(postResult.isSuccess(), "Post-request script should execute successfully");
        pipeline.withExecutionContext(() ->
                assertEquals(VariableResolver.resolve("{{postEventName}}"), "test"));
    }

    private void clearExecutionContext() {
        VariablesService.getInstance().detachContext();
        IterationDataVariableService.getInstance().detachContext();
        RequestExecutionContext.clearCurrentScope();
    }

    private String findAuthorizationHeader(PreparedRequest request) {
        if (request == null || request.headersList == null) {
            return null;
        }
        return request.headersList.stream()
                .filter(header -> header != null && header.isEnabled()
                        && "Authorization".equalsIgnoreCase(header.getKey()))
                .map(HttpHeader::getValue)
                .findFirst()
                .orElse(null);
    }

    private long countAuthorizationHeaders(PreparedRequest request) {
        if (request == null || request.headersList == null) {
            return 0L;
        }
        return request.headersList.stream()
                .filter(header -> header != null && header.isEnabled()
                        && "Authorization".equalsIgnoreCase(header.getKey()))
                .count();
    }

}
