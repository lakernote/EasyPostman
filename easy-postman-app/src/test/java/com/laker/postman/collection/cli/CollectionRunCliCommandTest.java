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
        return """
                {
                  "name": "CLI local",
                  "values": [
                    {"key": "baseUrl", "value": "%s", "enabled": true}
                  ]
                }
                """.formatted(baseUrl);
    }

    private static String uploadCollection() {
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
                            {"key": "document", "type": "file", "src": "sample-file.txt"},
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
                """;
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
                        "file": {"src": "payload.bin"}
                      }
                    }
                  }]
                }
                """.formatted(baseUrl);
    }
}
