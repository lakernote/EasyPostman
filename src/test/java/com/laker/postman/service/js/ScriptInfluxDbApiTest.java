package com.laker.postman.service.js;

import com.laker.postman.model.Environment;
import com.laker.postman.model.script.PostmanApiContext;
import com.laker.postman.model.script.ScriptInfluxDbApi;
import okhttp3.*;
import okio.Buffer;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

public class ScriptInfluxDbApiTest {

    @Test
    public void testInfluxV1Query() throws Exception {
        AtomicReference<Request> requestRef = new AtomicReference<>();
        String body = "{\"results\":[{\"series\":[{\"name\":\"cpu\",\"columns\":[\"time\",\"value\"],\"values\":[[1710000000000,12.3]]}]}]}";
        ScriptInfluxDbApi api = buildApi(requestRef, 200, "application/json", body);

        ScriptInfluxDbApi.InfluxResponse response = api.query(Map.of(
                "baseUrl", "http://localhost:8086",
                "version", "v1",
                "db", "metrics",
                "query", "SELECT * FROM cpu LIMIT 1",
                "username", "demo",
                "password", "secret"
        ));

        assertEquals(response.code, 200);
        assertNotNull(response.json);
        assertEquals(response.meta.get("mode"), "v1");
        assertEquals(requestRef.get().method(), "GET");
        assertEquals(requestRef.get().url().encodedPath(), "/query");
        assertEquals(requestRef.get().url().queryParameter("db"), "metrics");
        assertEquals(requestRef.get().url().queryParameter("q"), "SELECT * FROM cpu LIMIT 1");
        assertEquals(requestRef.get().url().queryParameter("u"), "demo");
        assertEquals(requestRef.get().url().queryParameter("p"), "secret");
        assertEquals(requestRef.get().url().queryParameter("epoch"), "ms");
    }

    @Test
    public void testInfluxV2Write() throws Exception {
        AtomicReference<Request> requestRef = new AtomicReference<>();
        String lineProtocol = "api_requests,service=order count=1i 1710000000000";
        ScriptInfluxDbApi api = buildApi(requestRef, 204, "text/plain", "");

        ScriptInfluxDbApi.InfluxResponse response = api.write(Map.of(
                "baseUrl", "http://localhost:8086",
                "version", "v2",
                "org", "team-a",
                "bucket", "metrics",
                "token", "token-123",
                "precision", "ms",
                "lineProtocol", lineProtocol
        ));

        assertEquals(response.code, 204);
        assertEquals(response.meta.get("mode"), "v2");
        assertEquals(requestRef.get().method(), "POST");
        assertEquals(requestRef.get().url().encodedPath(), "/api/v2/write");
        assertEquals(requestRef.get().header("Authorization"), "Token token-123");
        assertTrue(requestRef.get().header("Content-Type").startsWith("text/plain"));
        assertEquals(readRequestBody(requestRef.get()), lineProtocol);
        assertEquals(requestRef.get().url().queryParameter("org"), "team-a");
        assertEquals(requestRef.get().url().queryParameter("bucket"), "metrics");
        assertEquals(requestRef.get().url().queryParameter("precision"), "ms");
    }

    @Test
    public void testPmInfluxQueryAndAliasInJavaScript() throws Exception {
        AtomicReference<Request> requestRef = new AtomicReference<>();
        String csv = """
                #datatype,string,long,dateTime:RFC3339,double
                #group,false,false,false,false
                #default,_result,,,
                ,result,table,_time,_value
                ,,0,2024-01-01T00:00:00Z,12.5
                """;
        ScriptInfluxDbApi api = buildApi(requestRef, 200, "text/csv", csv);

        Environment env = new Environment();
        PostmanApiContext pm = new PostmanApiContext(env);
        pm.influxdb = api;
        pm.influx = api;
        assertSame(pm.influx, pm.influxdb);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {
            JsPolyfillInjector.injectAll(context);
            context.getBindings("js").putMember("pm", pm);

            String script = """
                    const resp = pm.influx.query({
                      baseUrl: 'http://localhost:8086',
                      version: 'v2',
                      org: 'demo-org',
                      token: 'token-123',
                      query: 'from(bucket: "metrics") |> range(start: -5m) |> limit(n: 1)'
                    });
                    pm.test("Influx query success", function () {
                      pm.expect(resp.code).to.equal(200);
                      pm.expect(resp.body).to.include("_value");
                    });
                    pm.test("Influx mode is v2", function () {
                      pm.expect(resp.meta.mode).to.equal("v2");
                    });
                    JSON.stringify(pm.test.index());
                    """;
            Value result = context.eval("js", script);

            String json = result.asString();
            assertTrue(json.contains("\"passed\":true"));
            assertEquals(pm.test.count(), 2);
            assertEquals(pm.test.failedCount(), 0);
        }

        assertEquals(requestRef.get().method(), "POST");
        assertEquals(requestRef.get().url().encodedPath(), "/api/v2/query");
        assertEquals(requestRef.get().header("Authorization"), "Token token-123");
        assertEquals(requestRef.get().header("Accept"), "text/csv");
        assertEquals(requestRef.get().url().queryParameter("org"), "demo-org");
        assertTrue(readRequestBody(requestRef.get()).contains("from(bucket: \"metrics\")"));
    }

    private ScriptInfluxDbApi buildApi(
            AtomicReference<Request> requestRef,
            int responseCode,
            String contentType,
            String responseBody) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    requestRef.set(request);
                    ResponseBody body = ResponseBody.create(
                            responseBody == null ? new byte[0] : responseBody.getBytes(StandardCharsets.UTF_8),
                            MediaType.get(contentType));
                    return new Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(responseCode)
                            .message("mock")
                            .header("Content-Type", contentType)
                            .body(body)
                            .build();
                })
                .build();
        return new ScriptInfluxDbApi(client);
    }

    private String readRequestBody(Request request) throws IOException {
        if (request == null || request.body() == null) {
            return "";
        }
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}
