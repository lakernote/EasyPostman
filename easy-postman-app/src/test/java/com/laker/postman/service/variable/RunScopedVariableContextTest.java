package com.laker.postman.service.variable;

import com.laker.postman.model.Environment;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class RunScopedVariableContextTest {

    @Test
    public void shouldResolveScopedEnvironmentAndGlobalsInChildThreads() throws Exception {
        Environment environment = new Environment("run-env");
        environment.set("__ep_run_scoped_base_url__", "https://run.example");
        Environment globals = new Environment("run-globals");
        globals.set("__ep_run_scoped_token__", "run-token");
        AtomicReference<String> resolved = new AtomicReference<>();

        try (RunScopedVariableContext ignored = RunScopedVariableContext.open(environment, globals)) {
            Thread thread = new Thread(() -> resolved.set(VariableResolver.resolve(
                    "{{__ep_run_scoped_base_url__}}/{{__ep_run_scoped_token__}}"
            )));
            thread.start();
            thread.join();
        }

        assertEquals(resolved.get(), "https://run.example/run-token");
        assertNull(EnvironmentVariableService.getInstance().get("__ep_run_scoped_base_url__"));
    }
}
