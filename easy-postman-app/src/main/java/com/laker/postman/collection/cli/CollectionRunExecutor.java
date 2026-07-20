package com.laker.postman.collection.cli;

import com.laker.postman.collection.CollectionInheritance;
import com.laker.postman.collection.importer.postman.PostmanCollectionParser;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.CollectionParseResult;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.functional.execution.FunctionalRequestExecutionResult;
import com.laker.postman.functional.execution.FunctionalRequestExecutor;
import com.laker.postman.functional.model.AssertionResult;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.model.Environment;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.service.postman.PostmanEnvironmentParser;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.service.variable.RunScopedVariableContext;
import com.laker.postman.util.CsvDataUtil;
import com.laker.postman.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CollectionRunExecutor {
    private final FunctionalRequestExecutor requestExecutor;

    public CollectionRunExecutor() {
        this(new FunctionalRequestExecutor(null));
    }

    public CollectionRunReport execute(CollectionRunCliOptions options, PrintStream out) throws Exception {
        Path collectionPath = requireFile(options.getCollectionPath(), "Collection file");
        Path workingDirectory = resolveWorkingDirectory(options, collectionPath);
        CollectionParseResult collection = parseCollection(collectionPath);
        List<SelectedRequest> allRequests = flattenRequests(collection);
        List<SelectedRequest> selectedRequests = selectFolders(
                allRequests,
                options.getFolders(),
                collection.getGroup()
        );
        if (!options.getFolders().isEmpty() && selectedRequests.isEmpty()) {
            throw new IllegalArgumentException("No requests matched folder(s): "
                    + String.join(", ", options.getFolders()));
        }

        Environment environment = loadVariables(options.getEnvironmentPath(), "CLI Environment");
        Environment globals = loadVariables(options.getGlobalsPath(), "CLI Globals");
        List<Map<String, String>> dataRows = loadIterationData(options.getIterationDataPath());
        int iterationCount = resolveIterationCount(options.getIterationCount(), dataRows);

        long startTimeMs = System.currentTimeMillis();
        List<CollectionRunReport.RequestResult> requestReports = new ArrayList<>();
        int passedRequests = 0;
        int failedRequests = 0;
        int passedTests = 0;
        int failedTests = 0;
        int startedIterations = 0;
        boolean stoppedByBail = false;
        ExecutionVariableContext runContext = new ExecutionVariableContext();

        try (RunScopedVariableContext ignored = RunScopedVariableContext.open(environment, globals)) {
            for (int iteration = 0; iteration < iterationCount && !stoppedByBail; iteration++) {
                startedIterations++;
                runContext.setIterationInfo(iteration, iterationCount);
                runContext.replaceIterationData(IterationDataRuntimeSupport.prepare(
                        dataRows.isEmpty() ? Map.of() : dataRows.get(iteration % dataRows.size())
                ));
                if (out != null && iterationCount > 1) {
                    out.printf("Iteration %d/%d%n", iteration + 1, iterationCount);
                }

                for (SelectedRequest selected : selectedRequests) {
                    HttpRequestItem effectiveItem = CollectionInheritance.apply(
                            selected.request(),
                            selected.groupChain()
                    );
                    RequestExecutionScope requestScope = RequestExecutionScope.fromVariables(
                            CollectionInheritance.mergeEnabledGroupVariables(selected.groupChain())
                    );

                    if (out != null) {
                        out.printf("→ %s %s%n", effectiveItem.getMethod(), selected.path());
                    }
                    FunctionalRequestExecutionResult result = requestExecutor.executeEffective(
                            effectiveItem,
                            runContext,
                            () -> true,
                            () -> environment,
                            requestScope,
                            scriptOutput(out),
                            request -> resolveFilePaths(request, workingDirectory)
                    );
                    CollectionRunReport.RequestResult requestReport = toRequestReport(
                            iteration + 1,
                            selected,
                            result
                    );
                    requestReports.add(requestReport);
                    if (requestReport.passed()) {
                        passedRequests++;
                    } else {
                        failedRequests++;
                    }
                    for (CollectionRunReport.TestCase test : requestReport.tests()) {
                        if (test.passed()) {
                            passedTests++;
                        } else {
                            failedTests++;
                        }
                    }
                    printRequestResult(out, requestReport);
                    if (options.isBail() && !requestReport.passed()) {
                        stoppedByBail = true;
                        break;
                    }
                }
            }
        }

        long endTimeMs = System.currentTimeMillis();
        return new CollectionRunReport(
                "1.0",
                failedRequests == 0 ? CollectionRunReport.STATUS_SUCCESS : CollectionRunReport.STATUS_FAILED,
                collection.getGroup() == null ? "" : collection.getGroup().getName(),
                collectionPath.toString(),
                startTimeMs,
                endTimeMs,
                Math.max(0L, endTimeMs - startTimeMs),
                startedIterations,
                requestReports.size(),
                passedRequests,
                failedRequests,
                passedTests + failedTests,
                passedTests,
                failedTests,
                requestReports
        );
    }

    private static CollectionParseResult parseCollection(Path collectionPath) throws Exception {
        try {
            String json = Files.readString(collectionPath, StandardCharsets.UTF_8);
            CollectionParseResult result = PostmanCollectionParser.parsePostmanCollection(json);
            if (result == null) {
                throw new IllegalArgumentException("Unsupported or invalid Postman Collection: " + collectionPath);
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to read Postman Collection: " + collectionPath + ": " + describe(ex),
                    ex
            );
        }
    }

    private static List<SelectedRequest> flattenRequests(CollectionParseResult result) {
        List<SelectedRequest> requests = new ArrayList<>();
        List<RequestGroup> rootChain = result.getGroup() == null
                ? List.of()
                : List.of(result.getGroup());
        for (CollectionNode child : result.getChildren()) {
            collectRequests(child, rootChain, requests);
        }
        return requests;
    }

    private static void collectRequests(CollectionNode node,
                                        List<RequestGroup> groupChain,
                                        List<SelectedRequest> requests) {
        if (node == null) {
            return;
        }
        if (node.isRequest() && node.getRequest() != null) {
            String requestPath = buildRequestPath(groupChain, node.getRequest().getName());
            requests.add(new SelectedRequest(node.getRequest(), groupChain, requestPath));
            return;
        }

        List<RequestGroup> childChain = groupChain;
        if (node.isGroup() && node.getGroup() != null) {
            childChain = new ArrayList<>(groupChain);
            childChain.add(node.getGroup());
        }
        for (CollectionNode child : node.getChildren()) {
            collectRequests(child, childChain, requests);
        }
    }

    private static String buildRequestPath(List<RequestGroup> groupChain, String requestName) {
        List<String> segments = new ArrayList<>();
        for (RequestGroup group : groupChain) {
            if (group != null && group.getName() != null && !group.getName().isBlank()) {
                segments.add(group.getName());
            }
        }
        segments.add(requestName == null || requestName.isBlank() ? "Unnamed request" : requestName);
        return String.join(" / ", segments);
    }

    private static List<SelectedRequest> selectFolders(List<SelectedRequest> requests,
                                                       List<String> folders,
                                                       RequestGroup collectionRoot) {
        if (folders == null || folders.isEmpty()) {
            return requests;
        }
        return requests.stream()
                .filter(request -> request.groupChain().stream()
                        .filter(group -> group != collectionRoot)
                        .map(RequestGroup::getName)
                        .anyMatch(folders::contains))
                .toList();
    }

    private static Environment loadVariables(Path path, String defaultName) throws Exception {
        Environment empty = new Environment(defaultName);
        if (path == null) {
            return empty;
        }
        Path variableFile = requireFile(path, defaultName + " file");
        List<Environment> parsed;
        try {
            parsed = PostmanEnvironmentParser.parsePostmanEnvironments(
                    Files.readString(variableFile, StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to read variable file: " + variableFile + ": " + describe(ex),
                    ex
            );
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Variable file contains no environment: " + variableFile);
        }
        return parsed.get(0);
    }

    private static List<Map<String, String>> loadIterationData(Path path) throws Exception {
        if (path == null) {
            return List.of();
        }
        Path dataFile = requireFile(path, "Iteration data file");
        String lowerName = dataFile.getFileName().toString().toLowerCase();
        List<Map<String, String>> rows;
        if (lowerName.endsWith(".json")) {
            rows = readJsonData(dataFile);
        } else if (lowerName.endsWith(".csv")) {
            rows = CsvDataUtil.readCsvData(dataFile.toFile());
        } else {
            throw new IllegalArgumentException("Iteration data file must use .csv or .json: " + dataFile);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Iteration data contains no rows: " + dataFile);
        }
        return rows;
    }

    private static List<Map<String, String>> readJsonData(Path dataFile) throws Exception {
        JsonNode root;
        try {
            root = JsonUtil.readTree(Files.readString(dataFile, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to read JSON iteration data: " + dataFile + ": " + describe(ex),
                    ex
            );
        }
        if (root == null || !root.isArray()) {
            throw new IllegalArgumentException("JSON iteration data must be an array of objects: " + dataFile);
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (JsonNode rowNode : root) {
            if (!rowNode.isObject()) {
                throw new IllegalArgumentException("JSON iteration data rows must be objects: " + dataFile);
            }
            Map<String, String> row = new LinkedHashMap<>();
            rowNode.properties().forEach(entry -> row.put(
                    entry.getKey(),
                    jsonValue(entry.getValue())
            ));
            rows.add(row);
        }
        return rows;
    }

    private static String jsonValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }

    private static int resolveIterationCount(Integer configuredCount, List<Map<String, String>> dataRows) {
        if (configuredCount != null) {
            return configuredCount;
        }
        return dataRows.isEmpty() ? 1 : dataRows.size();
    }

    private static Path resolveWorkingDirectory(CollectionRunCliOptions options, Path collectionPath) {
        Path workingDirectory = options.getWorkingDirectory();
        if (workingDirectory == null) {
            workingDirectory = collectionPath.getParent();
        }
        if (workingDirectory == null) {
            workingDirectory = Path.of(".");
        }
        workingDirectory = workingDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(workingDirectory)) {
            throw new IllegalArgumentException("Working directory does not exist: " + workingDirectory);
        }
        return workingDirectory;
    }

    private static void resolveFilePaths(PreparedRequest request, Path workingDirectory) {
        if (request == null) {
            return;
        }
        if (RequestBodyTypes.BODY_TYPE_BINARY.equals(request.bodyType)) {
            request.body = resolveFilePath(request.body, workingDirectory);
            validateUploadFile(request.body);
        }
        if (request.formDataList != null) {
            for (HttpFormData part : request.formDataList) {
                if (part != null && part.isEnabled() && part.isFile()) {
                    part.setValue(resolveFilePath(part.getValue(), workingDirectory));
                    validateUploadFile(part.getValue());
                }
            }
        }
    }

    private static String resolveFilePath(String value, Path workingDirectory) {
        if (value == null || value.isBlank()) {
            return value;
        }
        Path path = Path.of(value);
        return (path.isAbsolute() ? path : workingDirectory.resolve(path))
                .normalize()
                .toString();
    }

    private static void validateUploadFile(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Upload file path is required");
        }
        if (value.contains("{{")) {
            throw new IllegalArgumentException("Upload file path contains unresolved variables: " + value);
        }
        Path path = Path.of(value);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Upload file does not exist or is not readable: " + path);
        }
    }

    private static Path requireFile(Path path, String label) {
        if (path == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || !Files.isReadable(normalized)) {
            throw new IllegalArgumentException(label + " does not exist or is not readable: " + normalized);
        }
        return normalized;
    }

    private static String describe(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank()
                ? failure == null ? "unknown error" : failure.getClass().getSimpleName()
                : message;
    }

    private static JsScriptExecutor.OutputCallback scriptOutput(PrintStream out) {
        return output -> {
            if (out == null || output == null || output.isBlank()) {
                return;
            }
            out.println(output.stripTrailing());
            out.flush();
        };
    }

    private static CollectionRunReport.RequestResult toRequestReport(int iteration,
                                                                     SelectedRequest selected,
                                                                     FunctionalRequestExecutionResult result) {
        List<CollectionRunReport.TestCase> tests = new ArrayList<>();
        if (result.getTestResults() != null) {
            for (TestResult test : result.getTestResults()) {
                if (test != null) {
                    tests.add(new CollectionRunReport.TestCase(test.name, test.passed, safe(test.message)));
                }
            }
        }
        boolean passed = result.getAssertion() != AssertionResult.FAIL
                && (result.getErrorMessage() == null || result.getErrorMessage().isBlank());
        String url = result.getRequest() == null
                ? selected.request().getUrl()
                : firstNonBlank(result.getRequest().sentUrl, result.getRequest().url);
        return new CollectionRunReport.RequestResult(
                iteration,
                selected.request().getName(),
                selected.path(),
                selected.request().getMethod(),
                url,
                result.getStatus(),
                result.getCost(),
                passed,
                safe(result.getErrorMessage()),
                tests
        );
    }

    private static void printRequestResult(PrintStream out, CollectionRunReport.RequestResult result) {
        if (out == null) {
            return;
        }
        out.printf("  %s %dms %s%n", result.status(), result.durationMs(), result.passed() ? "PASS" : "FAIL");
        for (CollectionRunReport.TestCase test : result.tests()) {
            out.printf("    %s %s", test.passed() ? "✓" : "✗", test.name());
            if (!test.passed() && !test.message().isBlank()) {
                out.printf(": %s", test.message());
            }
            out.println();
        }
        if (!result.error().isBlank()) {
            out.printf("    Error: %s%n", result.error());
        }
        out.flush();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : safe(second);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record SelectedRequest(HttpRequestItem request, List<RequestGroup> groupChain, String path) {
        private SelectedRequest {
            groupChain = groupChain == null ? List.of() : List.copyOf(groupChain);
        }
    }
}
