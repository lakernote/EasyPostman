package com.laker.postman.functional.cli;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
class FunctionalRunConfigLoader {
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;

    FunctionalRunConfig load(Path path) {
        Path configPath = requireReadableFile(path);
        try {
            if (Files.size(configPath) > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Functional config is larger than 2 MB: " + configPath);
            }
            JSONObject root = JSONUtil.parseObj(Files.readString(configPath, StandardCharsets.UTF_8));
            List<String> requestIds = readSelectedRequestIds(root.getJSONArray("rows"));
            JSONObject csvState = root.getJSONObject("csvState");
            List<Map<String, String>> iterationData = readIterationData(csvState);
            String sourceName = csvState == null ? "" : csvState.getStr("sourceName", "");
            return new FunctionalRunConfig(requestIds, iterationData, sourceName);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to read EasyPostman functional config: " + configPath + ": " + describe(ex),
                    ex
            );
        }
    }

    private List<String> readSelectedRequestIds(JSONArray rows) {
        List<String> requestIds = new ArrayList<>();
        if (rows == null) {
            return requestIds;
        }
        for (Object value : rows) {
            if (!(value instanceof JSONObject row) || !row.getBool("selected", true)) {
                continue;
            }
            String requestId = row.getStr("requestItemId", "");
            if (!requestId.isBlank()) {
                requestIds.add(requestId);
            }
        }
        return requestIds;
    }

    private List<Map<String, String>> readIterationData(JSONObject csvState) {
        if (csvState == null) {
            return List.of();
        }
        JSONArray rows = csvState.getJSONArray("rows");
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<String> headers = readHeaders(csvState.getJSONArray("headers"));
        List<Map<String, String>> result = new ArrayList<>();
        for (Object value : rows) {
            if (!(value instanceof JSONObject row)) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            if (headers.isEmpty()) {
                for (String key : row.keySet()) {
                    values.put(key, row.getStr(key, ""));
                }
            } else {
                for (String header : headers) {
                    values.put(header, row.getStr(header, ""));
                }
            }
            result.add(values);
        }
        return result;
    }

    private List<String> readHeaders(JSONArray headers) {
        if (headers == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object value : headers) {
            if (value != null && !value.toString().isBlank()) {
                result.add(value.toString());
            }
        }
        return result;
    }

    private Path requireReadableFile(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || !Files.isReadable(normalized)) {
            throw new IllegalArgumentException(
                    "EasyPostman functional config does not exist or is not readable: " + normalized
            );
        }
        return normalized;
    }

    private String describe(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank()
                ? failure == null ? "unknown error" : failure.getClass().getSimpleName()
                : message;
    }
}
