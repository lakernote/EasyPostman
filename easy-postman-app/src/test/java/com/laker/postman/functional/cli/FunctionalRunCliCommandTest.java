package com.laker.postman.functional.cli;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.http.runtime.app.AppHttpRuntimeBootstrap;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.cookie.HttpCookieStore;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.service.collections.CollectionDocumentJsonCodec;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.workspace.cli.WorkspaceRunExecutor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FunctionalRunCliCommandTest {
    private MockWebServer server;

    @BeforeMethod
    public void setUpRuntime() {
        HttpRuntimeSettingsProvider.reset();
        OkHttpClientManager.clearClientCache();
        AppHttpRuntimeBootstrap.configure();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
            server = null;
        }
        OkHttpClientManager.clearClientCache();
        HttpRuntimeSettingsProvider.reset();
        HttpCookieStore.clearAllCookies();
    }

    @Test
    public void shouldRunSelectedRequestsWithEmbeddedCsvRows() throws Exception {
        server = startServer(2);
        Path workspace = Files.createTempDirectory("easy-postman-functional-");
        HttpRequestItem selected = request("Selected functional request", baseUrl() + "/{{user}}");
        HttpRequestItem ignored = request("Ignored functional request", baseUrl() + "/ignored");
        writeWorkspace(workspace, collection("Functional CLI", requestNode(selected), requestNode(ignored)));
        writeFunctionalConfig(workspace, """
                {
                  "version": "1.0",
                  "rows": [
                    {"selected": true, "requestItemId": "%s"},
                    {"selected": false, "requestItemId": "%s"}
                  ],
                  "csvState": {
                    "sourceName": "users.csv",
                    "headers": ["user"],
                    "rows": [{"user": "alice"}, {"user": "bob"}]
                  }
                }
                """.formatted(selected.getId(), ignored.getId()));
        Path reportFile = workspace.resolve("functional-result.json");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "functional", "run", workspace.toString(), "--out", reportFile.toString()
                },
                new PrintStream(stdout),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        assertEquals(server.takeRequest(5, TimeUnit.SECONDS).getPath(), "/alice");
        assertEquals(server.takeRequest(5, TimeUnit.SECONDS).getPath(), "/bob");
        assertEquals(server.getRequestCount(), 2);
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("selectionMode").asText(), "FUNCTIONAL_CONFIG");
        assertEquals(report.get("iterationDataSource").asText(), "functional_config.json (users.csv)");
        assertEquals(report.get("iterations").asInt(), 2);
        assertTrue(stdout.toString().contains("Selection: FUNCTIONAL_CONFIG"));
    }

    @Test
    public void shouldLetExternalCsvOverrideEmbeddedRowsAndCycleForExplicitRounds() throws Exception {
        server = startServer(3);
        Path workspace = Files.createTempDirectory("easy-postman-functional-external-csv-");
        HttpRequestItem selected = request("CSV functional request", baseUrl() + "/{{user}}");
        writeWorkspace(workspace, collection("Functional CLI", requestNode(selected)));
        writeFunctionalConfig(workspace, """
                {
                  "version": "1.0",
                  "rows": [{"selected": true, "requestItemId": "%s"}],
                  "csvState": {
                    "sourceName": "desktop.csv",
                    "headers": ["user"],
                    "rows": [{"user": "desktop-only"}]
                  }
                }
                """.formatted(selected.getId()));
        Files.writeString(workspace.resolve("ci-users.csv"), "user\nalice\nbob\n", StandardCharsets.UTF_8);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "functional", "run", workspace.toString(),
                        "-d", "ci-users.csv",
                        "-n", "3"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        assertEquals(server.takeRequest(5, TimeUnit.SECONDS).getPath(), "/alice");
        assertEquals(server.takeRequest(5, TimeUnit.SECONDS).getPath(), "/bob");
        assertEquals(server.takeRequest(5, TimeUnit.SECONDS).getPath(), "/alice");
    }

    @Test
    public void shouldRejectCollectionOptionsAndStaleRequestIds() throws Exception {
        Path workspace = Files.createTempDirectory("easy-postman-functional-invalid-");
        writeWorkspace(workspace, collection("Demo", requestNode(request("Demo", "http://127.0.0.1:1"))));

        ByteArrayOutputStream optionError = new ByteArrayOutputStream();
        int optionExit = command().run(new String[]{
                        "functional", "run", workspace.toString(), "-c", "Demo"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(optionError));
        assertEquals(optionExit, 2);
        assertTrue(optionError.toString().contains("Unknown option: -c"));

        writeFunctionalConfig(workspace, """
                {
                  "version": "1.0",
                  "rows": [{"selected": true, "requestItemId": "missing-request-id"}]
                }
                """);
        ByteArrayOutputStream staleError = new ByteArrayOutputStream();
        int staleExit = command().run(new String[]{
                        "functional", "run", workspace.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(staleError));
        assertEquals(staleExit, 2);
        assertTrue(staleError.toString().contains("references missing request ID(s): missing-request-id"));
    }

    private FunctionalRunCliCommand command() {
        return new FunctionalRunCliCommand(() -> {
        }, new WorkspaceRunExecutor());
    }

    private MockWebServer startServer(int responses) throws Exception {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();
        for (int index = 0; index < responses; index++) {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody("{\"ok\":true}"));
        }
        return mockServer;
    }

    private String baseUrl() {
        String value = server.url("").toString();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static void writeWorkspace(Path workspace, CollectionNode... roots) throws Exception {
        CollectionDocumentJsonCodec.write(
                workspace.resolve("collections.json").toFile(),
                new CollectionDocument(Arrays.asList(roots))
        );
    }

    private static void writeFunctionalConfig(Path workspace, String json) throws Exception {
        Files.writeString(workspace.resolve("functional_config.json"), json, StandardCharsets.UTF_8);
    }

    private static CollectionNode collection(String name, CollectionNode... children) {
        RequestGroup group = new RequestGroup(name);
        CollectionNode node = CollectionNode.group(group);
        Arrays.stream(children).forEach(node::addChild);
        return node;
    }

    private static CollectionNode requestNode(HttpRequestItem request) {
        return CollectionNode.request(request);
    }

    private static HttpRequestItem request(String name, String url) {
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-" + name.toLowerCase().replace(' ', '-'));
        request.setName(name);
        request.setUrl(url);
        request.setMethod("GET");
        request.setBodyType(RequestBodyTypes.BODY_TYPE_NONE);
        return request;
    }
}
