package com.laker.postman.collection.cli;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.http.runtime.app.AppHttpRuntimeBootstrap;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.cookie.HttpCookieStore;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.model.Environment;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.service.collections.CollectionDocumentJsonCodec;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.workspace.cli.WorkspaceRunExecutor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class CollectionRunCliCommandTest {
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
    public void shouldRunNativeWorkspaceWithActiveEnvironmentAndWriteReport() throws Exception {
        server = startServer(2);
        Path workspace = Files.createTempDirectory("easy-postman-native-cli-");
        Files.writeString(workspace.resolve("sample-file.txt"), "native-workspace-payload", StandardCharsets.UTF_8);
        Path dataFile = workspace.resolve("users.csv");
        Path reportFile = workspace.resolve("result.json");
        Files.writeString(dataFile, "user\nalice\nbob\n", StandardCharsets.UTF_8);

        writeWorkspace(workspace, collection("Upload CLI",
                requestNode(uploadRequest("Upload file", "{{baseUrl}}/upload?user={{user}}", "sample-file.txt"))));
        writeEnvironments(workspace,
                environment("Unused Env", "http://127.0.0.1:1", false),
                environment("Active Env", baseUrl(), true));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(),
                        "-d", dataFile.toString(),
                        "--out", reportFile.toString()
                },
                new PrintStream(stdout),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest first = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest second = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getPath(), "/upload?user=alice");
        assertEquals(second.getPath(), "/upload?user=bob");
        assertTrue(first.getBody().readUtf8().contains("native-workspace-payload"));
        assertTrue(second.getBody().readUtf8().contains("native-workspace-payload"));

        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("schemaVersion").asText(), "2.1");
        assertEquals(report.get("status").asText(), "SUCCESS");
        assertEquals(report.get("workspacePath").asText(), workspace.toString());
        assertEquals(report.get("environment").asText(), "Active Env");
        assertEquals(report.get("collections").get(0).asText(), "Upload CLI");
        assertEquals(report.get("selectionMode").asText(), "COLLECTIONS");
        assertEquals(report.get("iterationDataSource").asText(), dataFile.toString());
        assertEquals(report.get("iterations").asInt(), 2);
        assertEquals(report.get("passedTests").asInt(), 2);
        assertTrue(stdout.toString().contains("Workspace:"));
        assertTrue(stdout.toString().contains("Environment: Active Env"));
    }

    @Test
    public void shouldSelectCollectionAndEnvironmentByName() throws Exception {
        server = startServer(1);
        Path workspace = Files.createTempDirectory("easy-postman-native-selection-");
        writeWorkspace(workspace,
                collection("Ignored", requestNode(request("Ignored request", baseUrl() + "/ignored"))),
                collection("Selected", requestNode(request("Selected request", "{{baseUrl}}/{{route}}"))));
        Environment dev = environment("Dev Env", "http://127.0.0.1:1", true);
        dev.addVariable("route", "dev");
        Environment test = environment("Test Env", baseUrl(), false);
        test.addVariable("route", "chosen");
        writeEnvironments(workspace, dev, test);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(),
                        "-c", "Selected",
                        "-e", "Test Env"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(request.getPath(), "/chosen");
        assertEquals(server.getRequestCount(), 1);
    }

    @Test
    public void shouldRejectFunctionalModeFlag() throws Exception {
        Path workspace = Files.createTempDirectory("easy-postman-collection-no-functional-");
        writeWorkspace(workspace, collection("Demo", requestNode(request("Demo", "http://127.0.0.1:1"))));

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(), "--functional"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("Unknown option: --functional"));
    }

    @Test
    public void shouldRequireWorkspaceDirectory() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = command().run(new String[]{"collection", "run"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("Workspace directory is required"));
    }

    @Test
    public void shouldRejectCollectionFileInsteadOfWorkspaceDirectory() throws Exception {
        Path workspace = Files.createTempDirectory("easy-postman-native-file-reject-");
        writeWorkspace(workspace, collection("Demo", requestNode(request("Demo request", "http://127.0.0.1:1"))));

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.resolve("collections.json").toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("expects a workspace directory"));
    }

    @Test
    public void shouldListAvailableCollectionsWhenSelectionIsUnknown() throws Exception {
        Path workspace = Files.createTempDirectory("easy-postman-native-missing-collection-");
        writeWorkspace(workspace, collection("Existing", requestNode(request("Demo", "http://127.0.0.1:1"))));

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(), "-c", "Missing"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("Collection not found: Missing"));
        assertTrue(stderr.toString().contains("Available collections: Existing"));
    }

    @Test
    public void shouldStopAfterFirstFailedTestWhenBailIsEnabled() throws Exception {
        server = startServer(2);
        Path workspace = Files.createTempDirectory("easy-postman-native-bail-");
        HttpRequestItem failing = request("Fails", baseUrl() + "/first");
        failing.setPostscript("pm.test('expected failure', function () { pm.response.to.have.status(201); });");
        writeWorkspace(workspace, collection("Bail CLI",
                folder("Selected", requestNode(failing), requestNode(request("Must not run", baseUrl() + "/second"))),
                folder("Not selected", requestNode(request("Ignored", baseUrl() + "/ignored")))));

        Path reportFile = workspace.resolve("bail-result.json");
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(),
                        "--folder", "Selected",
                        "-n", "3",
                        "--out", reportFile.toString(),
                        "--bail"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 1);
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        assertEquals(server.getRequestCount(), 1);
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("iterations").asInt(), 1);
        assertEquals(report.get("totalRequests").asInt(), 1);
    }

    @Test
    public void shouldNotTreatCollectionRootNameAsFolder() throws Exception {
        Path workspace = Files.createTempDirectory("easy-postman-native-root-filter-");
        writeWorkspace(workspace, collection("Root collection",
                folder("Nested", requestNode(request("Request", "http://127.0.0.1:1")))));

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(), "--folder", "Root collection"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("No requests matched folder(s): Root collection"));
    }

    @Test
    public void shouldUploadRelativeBinaryBodyFromWorkspaceDirectory() throws Exception {
        server = startServer(1);
        Path workspace = Files.createTempDirectory("easy-postman-native-binary-");
        byte[] payload = new byte[]{0, 1, 2, 3, (byte) 0xFE, (byte) 0xFF};
        Files.write(workspace.resolve("payload.bin"), payload);
        HttpRequestItem request = request("Binary upload", baseUrl() + "/binary");
        request.setMethod("POST");
        request.setBodyType(RequestBodyTypes.BODY_TYPE_BINARY);
        request.setBody("payload.bin");
        writeWorkspace(workspace, collection("Binary CLI", requestNode(request)));

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{"collection", "run", workspace.toString()},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recorded);
        assertEquals(recorded.getBody().readByteArray(), payload);
    }

    @Test
    public void shouldUseEnvironmentFallbackForDisabledCollectionVariable() throws Exception {
        server = startServer(1);
        Path workspace = Files.createTempDirectory("easy-postman-native-variable-");
        CollectionNode root = collection("Variable CLI",
                requestNode(request("Uses environment", baseUrl() + "/get?switch={{switch}}")));
        root.asGroup().setVariables(List.of(new Variable(false, "switch", "disabled-collection-value")));
        Environment environment = environment("Active Env", baseUrl(), true);
        environment.addVariable("switch", "environment-value");
        writeWorkspace(workspace, root);
        writeEnvironments(workspace, environment);

        int exitCode = command().run(new String[]{"collection", "run", workspace.toString()},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 0);
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recorded);
        assertEquals(recorded.getPath(), "/get?switch=environment-value");
    }

    @Test
    public void shouldKeepRunVariablesAcrossRequestsAndIterations() throws Exception {
        server = startServer(4);
        Path workspace = Files.createTempDirectory("easy-postman-native-run-variable-");
        HttpRequestItem setter = request("Set once", baseUrl() + "/set");
        setter.setPrescript("if (pm.info.iteration === 0) { pm.variables.set('carry', 'run-value'); }");
        HttpRequestItem reader = request("Read", baseUrl() + "/read?carry={{carry}}");
        reader.setPostscript("pm.test('run value remains', function () {"
                + " pm.expect(pm.variables.get('carry')).to.equal('run-value'); });");
        writeWorkspace(workspace, collection("Run variables", requestNode(setter), requestNode(reader)));

        Path reportFile = workspace.resolve("result.json");
        int exitCode = command().run(new String[]{
                        "collection", "run", workspace.toString(), "-n", "2", "--out", reportFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 0);
        assertEquals(server.getRequestCount(), 4);
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("passedTests").asInt(), 2);
        assertEquals(report.get("failedTests").asInt(), 0);
    }

    @Test
    public void shouldRejectMissingOptionValueAndUnsupportedDataFile() throws Exception {
        ByteArrayOutputStream missingValue = new ByteArrayOutputStream();
        int missingValueExit = command().run(new String[]{"collection", "run", "--collection", "--bail"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(missingValue));
        assertEquals(missingValueExit, 2);
        assertTrue(missingValue.toString().contains("--collection requires a value"));

        Path workspace = Files.createTempDirectory("easy-postman-native-data-extension-");
        writeWorkspace(workspace, collection("Demo", requestNode(request("Demo", "http://127.0.0.1:1"))));
        Path dataFile = workspace.resolve("users.txt");
        Files.writeString(dataFile, "user\nalice\n", StandardCharsets.UTF_8);
        ByteArrayOutputStream badExtension = new ByteArrayOutputStream();
        int badExtensionExit = command().run(new String[]{
                        "collection", "run", workspace.toString(), "-d", dataFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(badExtension));
        assertEquals(badExtensionExit, 2);
        assertTrue(badExtension.toString().contains("Iteration data file must use .csv or .json"));
    }

    private MockWebServer startServer(int responses) throws Exception {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();
        for (int i = 0; i < responses; i++) {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody("{\"ok\":true}"));
        }
        return mockServer;
    }

    private CollectionRunCliCommand command() {
        return new CollectionRunCliCommand(() -> {
        }, new WorkspaceRunExecutor());
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

    private static void writeEnvironments(Path workspace, Environment... environments) throws Exception {
        Files.writeString(
                workspace.resolve("environments.json"),
                JsonUtil.toJsonPrettyStr(Arrays.asList(environments)),
                StandardCharsets.UTF_8
        );
    }

    private static Environment environment(String name, String baseUrl, boolean active) {
        Environment environment = new Environment(name);
        environment.setId(name.toLowerCase().replace(' ', '-'));
        environment.setActive(active);
        environment.addVariable("baseUrl", baseUrl);
        return environment;
    }

    private static CollectionNode collection(String name, CollectionNode... children) {
        return group(name, children);
    }

    private static CollectionNode folder(String name, CollectionNode... children) {
        return group(name, children);
    }

    private static CollectionNode group(String name, CollectionNode... children) {
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

    private static HttpRequestItem uploadRequest(String name, String url, String uploadPath) {
        HttpRequestItem request = request(name, url);
        request.setMethod("POST");
        request.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_DATA);
        request.setFormDataList(List.of(
                new HttpFormData(true, "document", HttpFormData.TYPE_FILE, uploadPath),
                new HttpFormData(true, "user", HttpFormData.TYPE_TEXT, "{{user}}")
        ));
        request.setPostscript("pm.test('status is 200', function () { pm.response.to.have.status(200); });");
        return request;
    }
}
