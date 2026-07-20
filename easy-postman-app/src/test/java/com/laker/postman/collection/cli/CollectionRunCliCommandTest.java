package com.laker.postman.collection.cli;

import com.laker.postman.http.runtime.app.AppHttpRuntimeBootstrap;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.cookie.HttpCookieStore;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.util.JsonUtil;
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
    public void shouldUploadRelativeFileForEachCsvIterationAndWriteReport() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-cli-");
        Path uploadFile = directory.resolve("sample-file.txt");
        Path collectionFile = directory.resolve("upload.postman_collection.json");
        Path environmentFile = directory.resolve("local.postman_environment.json");
        Path dataFile = directory.resolve("users.csv");
        Path reportFile = directory.resolve("result.json");
        Files.writeString(uploadFile, "file-from-collection-directory", StandardCharsets.UTF_8);
        Files.writeString(collectionFile, uploadCollection(), StandardCharsets.UTF_8);
        Files.writeString(environmentFile, environmentJson(baseUrl()), StandardCharsets.UTF_8);
        Files.writeString(dataFile, "user\nalice\nbob\n", StandardCharsets.UTF_8);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-e", environmentFile.toString(),
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
        assertTrue(first.getBody().readUtf8().contains("file-from-collection-directory"));
        assertTrue(second.getBody().readUtf8().contains("file-from-collection-directory"));

        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("status").asText(), "SUCCESS");
        assertEquals(report.get("iterations").asInt(), 2);
        assertEquals(report.get("totalRequests").asInt(), 2);
        assertEquals(report.get("passedTests").asInt(), 2);
        assertTrue(stdout.toString().contains("Collection run completed: status=SUCCESS"));
    }

    @Test
    public void shouldUploadAbsoluteFilePathFromEnvironmentVariable() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-absolute-form-");
        Path workingDirectory = Files.createDirectory(directory.resolve("working"));
        Path uploadFile = directory.resolve("outside-working-directory.txt").toAbsolutePath();
        Path collectionFile = directory.resolve("upload.postman_collection.json");
        Path environmentFile = directory.resolve("local.postman_environment.json");
        Files.writeString(uploadFile, "absolute-form-data-payload", StandardCharsets.UTF_8);
        Files.writeString(collectionFile, uploadCollection("{{uploadPath}}"), StandardCharsets.UTF_8);
        Files.writeString(
                environmentFile,
                environmentJson(baseUrl(), uploadFile.toString()),
                StandardCharsets.UTF_8
        );

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-e", environmentFile.toString(),
                        "--working-dir", workingDirectory.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getBody().readUtf8().contains("absolute-form-data-payload"));
    }

    @Test
    public void shouldResolveAFilePathForEachIterationDataRow() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-iteration-files-");
        Path firstUpload = directory.resolve("first.txt").toAbsolutePath();
        Path secondUpload = directory.resolve("second.txt").toAbsolutePath();
        Path collectionFile = directory.resolve("upload.postman_collection.json");
        Path environmentFile = directory.resolve("local.postman_environment.json");
        Path dataFile = directory.resolve("files.csv");
        Files.writeString(firstUpload, "first-iteration-payload", StandardCharsets.UTF_8);
        Files.writeString(secondUpload, "second-iteration-payload", StandardCharsets.UTF_8);
        Files.writeString(collectionFile, uploadCollection("{{uploadPath}}"), StandardCharsets.UTF_8);
        Files.writeString(environmentFile, environmentJson(baseUrl()), StandardCharsets.UTF_8);
        Files.writeString(
                dataFile,
                "uploadPath,user\n%s,alice\n%s,bob\n".formatted(firstUpload, secondUpload),
                StandardCharsets.UTF_8
        );

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-e", environmentFile.toString(),
                        "-d", dataFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest firstRequest = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest secondRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(firstRequest);
        assertNotNull(secondRequest);
        assertTrue(firstRequest.getBody().readUtf8().contains("first-iteration-payload"));
        assertTrue(secondRequest.getBody().readUtf8().contains("second-iteration-payload"));
    }

    @Test
    public void shouldStopAfterFirstFailedTestWhenBailIsEnabled() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-bail-");
        Path collectionFile = directory.resolve("bail.postman_collection.json");
        Path reportFile = directory.resolve("bail-result.json");
        Files.writeString(collectionFile, bailCollection(baseUrl()), StandardCharsets.UTF_8);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "--folder", "Selected",
                        "--iteration-count", "3",
                        "--out", reportFile.toString(),
                        "--bail"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 1, stderr.toString());
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        assertEquals(server.getRequestCount(), 1);
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("iterations").asInt(), 1);
        assertEquals(report.get("totalRequests").asInt(), 1);
    }

    @Test
    public void shouldNotTreatCollectionRootNameAsFolder() throws Exception {
        Path directory = Files.createTempDirectory("easy-postman-collection-root-filter-");
        Path collectionFile = directory.resolve("root-filter.postman_collection.json");
        Files.writeString(
                collectionFile,
                bailCollection("http://127.0.0.1:1"),
                StandardCharsets.UTF_8
        );

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "--folder", "Bail CLI"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("No requests matched folder(s): Bail CLI"));
    }

    @Test
    public void shouldUploadRelativeBinaryBody() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-binary-");
        Path uploadFile = directory.resolve("payload.bin");
        Path collectionFile = directory.resolve("binary.postman_collection.json");
        byte[] payload = new byte[]{0, 1, 2, 3, (byte) 0xFE, (byte) 0xFF};
        Files.write(uploadFile, payload);
        Files.writeString(collectionFile, binaryCollection(baseUrl()), StandardCharsets.UTF_8);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(request.getBody().readByteArray(), payload);
    }

    @Test
    public void shouldUploadAbsoluteBinaryPathFromEnvironmentVariable() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-absolute-binary-");
        Path workingDirectory = Files.createDirectory(directory.resolve("working"));
        Path uploadFile = directory.resolve("outside-working-directory.bin").toAbsolutePath();
        Path collectionFile = directory.resolve("binary.postman_collection.json");
        Path environmentFile = directory.resolve("local.postman_environment.json");
        byte[] payload = new byte[]{7, 6, 5, 4, 3, 2, 1};
        Files.write(uploadFile, payload);
        Files.writeString(collectionFile, binaryCollection(baseUrl(), "{{uploadPath}}"), StandardCharsets.UTF_8);
        Files.writeString(
                environmentFile,
                environmentJson(baseUrl(), uploadFile.toString()),
                StandardCharsets.UTF_8
        );

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-e", environmentFile.toString(),
                        "--working-dir", workingDirectory.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 0, stderr.toString());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(request.getBody().readByteArray(), payload);
    }

    @Test
    public void shouldReturnUsageErrorWhenUploadFileIsMissing() throws Exception {
        Path directory = Files.createTempDirectory("easy-postman-collection-missing-file-");
        Path collectionFile = directory.resolve("upload.postman_collection.json");
        Files.writeString(collectionFile, uploadCollection(), StandardCharsets.UTF_8);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("Upload file does not exist or is not readable"));
    }

    @Test
    public void shouldRejectUnresolvedUploadPathBeforeSendingRequest() throws Exception {
        Path directory = Files.createTempDirectory("easy-postman-collection-unresolved-file-");
        Path collectionFile = directory.resolve("upload.postman_collection.json");
        Files.writeString(collectionFile, uploadCollection("{{uploadPath}}"), StandardCharsets.UTF_8);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("Upload file path contains unresolved variables"));
    }

    @Test
    public void shouldRejectKnownOptionWhenFolderValueIsMissing() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", "--folder", "--bail"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("--folder requires a value"));
    }

    @Test
    public void shouldRejectUnsupportedIterationDataExtension() throws Exception {
        Path directory = Files.createTempDirectory("easy-postman-collection-data-extension-");
        Path collectionFile = directory.resolve("collection.postman_collection.json");
        Path dataFile = directory.resolve("users.txt");
        Files.writeString(collectionFile, bailCollection("http://127.0.0.1:1"), StandardCharsets.UTF_8);
        Files.writeString(dataFile, "user\nalice\n", StandardCharsets.UTF_8);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-d", dataFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("Iteration data file must use .csv or .json"));
    }

    @Test
    public void shouldFailWhenPreRequestAssertionFails() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-pre-tests-");
        Path collectionFile = directory.resolve("pre-tests.postman_collection.json");
        Path reportFile = directory.resolve("result.json");
        Files.writeString(collectionFile, """
                {
                  "info": {"name": "Pre-request assertions"},
                  "item": [{
                    "name": "Fails before request",
                    "event": [{
                      "listen": "prerequest",
                      "script": {"exec": [
                        "pm.test('pre assertion', function () { pm.expect(1).to.equal(2); });"
                      ]}
                    }],
                    "request": {"method": "GET", "url": "%s/pre-test"}
                  }]
                }
                """.formatted(baseUrl()), StandardCharsets.UTF_8);

        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "--out", reportFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 1);
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("status").asText(), "FAILED");
        assertEquals(report.get("failedRequests").asInt(), 1);
        assertEquals(report.get("totalTests").asInt(), 1);
        assertEquals(report.get("failedTests").asInt(), 1);
    }

    @Test
    public void shouldFailWhenPostRequestScriptThrows() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-post-error-");
        Path collectionFile = directory.resolve("post-error.postman_collection.json");
        Path reportFile = directory.resolve("result.json");
        Files.writeString(collectionFile, """
                {
                  "info": {"name": "Post-request error"},
                  "item": [{
                    "name": "Fails after request",
                    "request": {"method": "GET", "url": "%s/post-error"},
                    "event": [{
                      "listen": "test",
                      "script": {"exec": ["throw new Error('post script boom');"]}
                    }]
                  }]
                }
                """.formatted(baseUrl()), StandardCharsets.UTF_8);

        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "--out", reportFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 1);
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("status").asText(), "FAILED");
        assertEquals(report.get("failedRequests").asInt(), 1);
        assertTrue(report.get("requests").get(0).get("error").asText().contains("post script boom"));
    }

    @Test
    public void shouldIgnoreDisabledCollectionVariableDuringExecution() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-disabled-variable-");
        Path collectionFile = directory.resolve("disabled.postman_collection.json");
        Path environmentFile = directory.resolve("local.postman_environment.json");
        Files.writeString(collectionFile, """
                {
                  "info": {"name": "Disabled variable"},
                  "variable": [{
                    "key": "switch",
                    "value": "disabled-collection-value",
                    "disabled": true
                  }],
                  "item": [{
                    "name": "Uses environment fallback",
                    "request": {"method": "GET", "url": "%s/get?switch={{switch}}"}
                  }]
                }
                """.formatted(baseUrl()), StandardCharsets.UTF_8);
        Files.writeString(environmentFile, """
                {
                  "name": "CLI local",
                  "values": [{
                    "key": "switch",
                    "value": "environment-value",
                    "enabled": true
                  }]
                }
                """, StandardCharsets.UTF_8);

        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-e", environmentFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 0);
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(request.getPath(), "/get?switch=environment-value");
    }

    @Test
    public void shouldExposeIterationDataThroughPmVariables() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-pm-variables-");
        Path collectionFile = directory.resolve("variables.postman_collection.json");
        Path dataFile = directory.resolve("data.json");
        Files.writeString(collectionFile, """
                {
                  "info": {"name": "pm.variables precedence"},
                  "variable": [{"key": "shared", "value": "collection-value"}],
                  "item": [{
                    "name": "Uses iteration value",
                    "request": {"method": "GET", "url": "%s/get?shared={{shared}}"},
                    "event": [{
                      "listen": "test",
                      "script": {"exec": [
                        "pm.test('iteration wins', function () {",
                        "  pm.expect(pm.variables.get('shared')).to.equal('iteration-value');",
                        "});"
                      ]}
                    }]
                  }]
                }
                """.formatted(baseUrl()), StandardCharsets.UTF_8);
        Files.writeString(dataFile, "[{\"shared\":\"iteration-value\"}]", StandardCharsets.UTF_8);

        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-d", dataFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 0);
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(request.getPath(), "/get?shared=iteration-value");
    }

    @Test
    public void shouldKeepPmVariablesForTheWholeCollectionRun() throws Exception {
        server = new MockWebServer();
        server.start();
        for (int i = 0; i < 4; i++) {
            server.enqueue(okResponse());
        }

        Path directory = Files.createTempDirectory("easy-postman-collection-run-variables-");
        Path collectionFile = directory.resolve("run-variables.postman_collection.json");
        Path reportFile = directory.resolve("result.json");
        Files.writeString(collectionFile, """
                {
                  "info": {"name": "Run variable lifetime"},
                  "item": [
                    {
                      "name": "Set once",
                      "event": [{
                        "listen": "prerequest",
                        "script": {"exec": [
                          "if (pm.info.iteration === 0) { pm.variables.set('carry', 'run-value'); }"
                        ]}
                      }],
                      "request": {"method": "GET", "url": "%s/set"}
                    },
                    {
                      "name": "Read every iteration",
                      "request": {"method": "GET", "url": "%s/read?carry={{carry}}"},
                      "event": [{
                        "listen": "test",
                        "script": {"exec": [
                          "pm.test('run value remains', function () {",
                          "  pm.expect(pm.variables.get('carry')).to.equal('run-value');",
                          "});"
                        ]}
                      }]
                    }
                  ]
                }
                """.formatted(baseUrl(), baseUrl()), StandardCharsets.UTF_8);

        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString(),
                        "-n", "2",
                        "--out", reportFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 0);
        assertEquals(server.getRequestCount(), 4);
        JsonNode report = JsonUtil.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
        assertEquals(report.get("totalRequests").asInt(), 4);
        assertEquals(report.get("passedTests").asInt(), 2);
        assertEquals(report.get("failedTests").asInt(), 0);
    }

    @Test
    public void shouldUploadEveryFileFromPostmanSourceArray() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(okResponse());

        Path directory = Files.createTempDirectory("easy-postman-collection-multi-file-");
        Path firstUpload = directory.resolve("first.txt");
        Path secondUpload = directory.resolve("second.txt");
        Path collectionFile = directory.resolve("multi-file.postman_collection.json");
        Files.writeString(firstUpload, "first-file-payload", StandardCharsets.UTF_8);
        Files.writeString(secondUpload, "second-file-payload", StandardCharsets.UTF_8);
        Files.writeString(collectionFile, """
                {
                  "info": {"name": "Multi-file upload"},
                  "item": [{
                    "name": "Uploads both files",
                    "request": {
                      "method": "POST",
                      "url": "%s/upload",
                      "body": {
                        "mode": "formdata",
                        "formdata": [{
                          "key": "documents",
                          "type": "file",
                          "src": ["first.txt", "second.txt"]
                        }]
                      }
                    }
                  }]
                }
                """.formatted(baseUrl()), StandardCharsets.UTF_8);

        int exitCode = command().run(new String[]{
                        "collection", "run", collectionFile.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 0);
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("first-file-payload"));
        assertTrue(requestBody.contains("second-file-payload"));
    }

    private CollectionRunCliCommand command() {
        return new CollectionRunCliCommand(() -> {
        }, new CollectionRunExecutor());
    }

    private MockResponse okResponse() {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true}");
    }

    private String baseUrl() {
        String value = server.url("").toString();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String environmentJson(String baseUrl) {
        return environmentJson(baseUrl, null);
    }

    private static String environmentJson(String baseUrl, String uploadPath) {
        String uploadVariable = uploadPath == null
                ? ""
                : ",\n    {\"key\": \"uploadPath\", \"value\": %s, \"enabled\": true}"
                        .formatted(JsonUtil.toJsonStr(uploadPath));
        return """
                {
                  "name": "CLI local",
                  "values": [
                    {"key": "baseUrl", "value": %s, "enabled": true}%s
                  ]
                }
                """.formatted(JsonUtil.toJsonStr(baseUrl), uploadVariable);
    }

    private static String uploadCollection() {
        return uploadCollection("sample-file.txt");
    }

    private static String uploadCollection(String uploadPath) {
        return """
                {
                  "info": {
                    "name": "Upload CLI",
                    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
                  },
                  "item": [
                    {
                      "name": "Upload file",
                      "request": {
                        "method": "POST",
                        "url": "{{baseUrl}}/upload?user={{user}}",
                        "body": {
                          "mode": "formdata",
                          "formdata": [
                            {"key": "document", "type": "file", "src": %s},
                            {"key": "user", "type": "text", "value": "{{user}}"}
                          ]
                        }
                      },
                      "event": [
                        {
                          "listen": "test",
                          "script": {
                            "exec": [
                              "pm.test('status is 200', function () {",
                              "  pm.response.to.have.status(200);",
                              "});"
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """.formatted(JsonUtil.toJsonStr(uploadPath));
    }

    private static String bailCollection(String baseUrl) {
        return """
                {
                  "info": {"name": "Bail CLI"},
                  "item": [
                    {
                      "name": "Selected",
                      "item": [
                        {
                          "name": "Fails",
                          "request": {"method": "GET", "url": "%s/first"},
                          "event": [{
                            "listen": "test",
                            "script": {"exec": [
                              "pm.test('expected failure', function () {",
                              "  pm.response.to.have.status(201);",
                              "});"
                            ]}
                          }]
                        },
                        {
                          "name": "Must not run",
                          "request": {"method": "GET", "url": "%s/second"}
                        }
                      ]
                    },
                    {
                      "name": "Not selected",
                      "item": [{
                        "name": "Ignored",
                        "request": {"method": "GET", "url": "%s/ignored"}
                      }]
                    }
                  ]
                }
                """.formatted(baseUrl, baseUrl, baseUrl);
    }

    private static String binaryCollection(String baseUrl) {
        return binaryCollection(baseUrl, "payload.bin");
    }

    private static String binaryCollection(String baseUrl, String uploadPath) {
        return """
                {
                  "info": {"name": "Binary CLI"},
                  "item": [{
                    "name": "Upload binary",
                    "request": {
                      "method": "POST",
                      "header": [{
                        "key": "Content-Type",
                        "value": "application/octet-stream",
                        "type": "text"
                      }],
                      "url": "%s/binary",
                      "body": {
                        "mode": "file",
                        "file": {"src": %s}
                      }
                    }
                  }]
                }
                """.formatted(baseUrl, JsonUtil.toJsonStr(uploadPath));
    }
}
