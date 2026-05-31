package com.laker.postman.service.js.api;

import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.CookieService;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PostmanApiContextSendRequestTest {

    private String originalDataFilePath;
    private Path tempEnvFile;
    private MockWebServer server;

    @BeforeMethod
    public void setUp() throws IOException {
        originalDataFilePath = EnvironmentService.getDataFilePath();
        tempEnvFile = Files.createTempFile("easy-postman-pm-send-request-", ".json");
        Files.writeString(tempEnvFile, "[]");
        EnvironmentService.setDataFilePath(tempEnvFile.toString());
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
            server = null;
        }
        OkHttpClientManager.clearClientCache();
        CookieService.clearAllCookies();
        if (originalDataFilePath != null && !originalDataFilePath.isBlank()) {
            EnvironmentService.setDataFilePath(originalDataFilePath);
        }
        if (tempEnvFile != null) {
            Files.deleteIfExists(tempEnvFile);
        }
    }

    @Test
    public void pmSendRequestShouldFinalizeVariablesBeforeTransport() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true}"));

        Environment env = new Environment();
        env.setId("pm-send-request-env");
        env.setName("pm sendRequest env");
        env.set("scriptBaseUrl", trimTrailingSlash(server.url("").toString()));
        env.set("scriptToken", "token-123");
        EnvironmentService.saveEnvironment(env);
        EnvironmentService.setActiveEnvironment(env.getId());

        PostmanApiContext pm = new PostmanApiContext(env);
        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> false)
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {
            context.getBindings("js").putMember("pm", pm);
            String result = context.eval("js", """
                    var result = {};
                    pm.sendRequest({
                        url: '{{scriptBaseUrl}}/script?token={{scriptToken}}',
                        method: 'POST',
                        header: {
                            'X-Token': '{{scriptToken}}',
                            'Content-Type': 'text/plain'
                        },
                        body: {
                            mode: 'raw',
                            raw: 'token={{scriptToken}}'
                        }
                    }, function (err, response) {
                        result.error = err ? err.message : null;
                        result.code = response ? response.code : -1;
                    });
                    JSON.stringify(result);
                    """).asString();

            assertTrue(result.contains("\"error\":null"), result);
            assertTrue(result.contains("\"code\":200"), result);
        }

        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals(recordedRequest.getPath(), "/script?token=token-123");
        assertEquals(recordedRequest.getHeader("X-Token"), "token-123");
        assertEquals(recordedRequest.getBody().readUtf8(), "token=token-123");
    }

    private String trimTrailingSlash(String value) {
        if (value == null || !value.endsWith("/")) {
            return value;
        }
        return value.substring(0, value.length() - 1);
    }
}
