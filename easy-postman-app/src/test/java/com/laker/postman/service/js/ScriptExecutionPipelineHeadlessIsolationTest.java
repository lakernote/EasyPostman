package com.laker.postman.service.js;

import com.laker.postman.model.Environment;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.variable.RunScopedVariableContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ScriptExecutionPipelineHeadlessIsolationTest {
    private static final String ENV_KEY = "__ep_headless_runtime_env__";
    private static final String GLOBAL_KEY = "__ep_headless_runtime_global__";

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        GlobalVariablesService.getInstance().getGlobalVariables().unset(GLOBAL_KEY);
    }

    @Test
    public void headlessSupplierShouldUseRunScopedVariablesWithoutLoadingWorkspaceService() throws Exception {
        resetWorkspaceServiceInstance();
        Environment environment = new Environment("plan-env");
        Environment globals = new Environment("plan-globals");

        PreparedRequest request = new PreparedRequest();
        request.id = "headless-script-isolation";
        request.method = "GET";
        request.url = "https://example.com";
        request.headersList = new ArrayList<>();
        request.paramsList = new ArrayList<>();
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();

        try (RunScopedVariableContext ignored = RunScopedVariableContext.open(environment, globals)) {
            ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                    .request(request)
                    .preScript("""
                            pm.environment.set('__ep_headless_runtime_env__', 'env-value');
                            pm.globals.set('__ep_headless_runtime_global__', 'global-value');
                            """)
                    .postScript("")
                    .environmentSupplier(() -> environment)
                    .build();

            ScriptExecutionResult result = pipeline.executePreScript();

            assertTrue(result.isSuccess(), "Headless pre-request script should execute successfully");
        }

        assertEquals(environment.get(ENV_KEY), "env-value");
        assertEquals(globals.get(GLOBAL_KEY), "global-value");
        assertNull(workspaceServiceInstance(), "Headless script execution must not initialize WorkspaceService");
    }

    private static void resetWorkspaceServiceInstance() throws Exception {
        Field instance = workspaceServiceInstanceField();
        instance.set(null, null);
    }

    private static Object workspaceServiceInstance() throws Exception {
        return workspaceServiceInstanceField().get(null);
    }

    private static Field workspaceServiceInstanceField() throws Exception {
        Class<?> workspaceServiceClass = Class.forName(
                "com.laker.postman.service.WorkspaceService",
                false,
                Thread.currentThread().getContextClassLoader()
        );
        Field instance = workspaceServiceClass.getDeclaredField("instance");
        instance.setAccessible(true);
        return instance;
    }
}
