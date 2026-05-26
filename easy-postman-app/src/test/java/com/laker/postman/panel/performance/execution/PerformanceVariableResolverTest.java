package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.RequestFinalizer;
import com.laker.postman.service.variable.VariableResolver;
import com.laker.postman.service.variable.VariablesService;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceVariableResolverTest {

    @Test
    public void shouldResolveLightweightFunctionsInPerformanceScope() {
        VariablesService.getInstance().set("raw", "a b");
        try {
            assertTrue(PerformanceVariableResolver.resolve("{{__uuid()}}")
                    .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
            assertEquals(PerformanceVariableResolver.resolve("{{__randomInt(5,5)}}"), "5");
            assertEquals(PerformanceVariableResolver.resolve("{{__randomString(8)}}").length(), 8);
            assertEquals(PerformanceVariableResolver.resolve("{{__urlEncode(\"a b\")}}"), "a+b");
            assertEquals(PerformanceVariableResolver.resolve("{{__urlEncode(raw)}}"), "a+b");
            assertEquals(PerformanceVariableResolver.resolve("{{__base64(\"a:b\")}}"), "YTpi");
            assertTrue(PerformanceVariableResolver.resolve("{{__time(\"yyyy\")}}").matches("\\d{4}"));
        } finally {
            VariablesService.getInstance().detachContext();
        }
    }

    @Test
    public void shouldKeepLightweightFunctionsOutOfGlobalVariableResolver() {
        assertEquals(VariableResolver.resolve("{{__uuid()}}"), "{{__uuid()}}");
        assertEquals(VariableResolver.resolve("{{__randomInt(5,5)}}"), "{{__randomInt(5,5)}}");
    }

    @Test
    public void shouldResolvePerformanceFunctionsWhenFinalizingPerformanceRequestFields() {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://example.com/{{__randomInt(5,5)}}";
        request.body = "{\"id\":\"{{__randomString(6)}}\"}";
        request.headersList = List.of(new HttpHeader(true, "X-Trace", "{{__base64(\"a:b\")}}"));

        RequestFinalizer.finalizeForSend(request, null, PerformanceVariableResolver::resolve);

        assertEquals(request.url, "https://example.com/5");
        assertTrue(request.body.matches("\\{\"id\":\"[A-Za-z0-9]{6}\"}"));
        assertEquals(request.headersList.get(0).getValue(), "YTpi");
    }
}
