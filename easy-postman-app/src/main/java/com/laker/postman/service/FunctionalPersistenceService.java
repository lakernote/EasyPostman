package com.laker.postman.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PostConstruct;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.functional.table.RunnerRowData;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.common.constants.ConfigPathConstants;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能测试配置持久化服务
 * 用于保存和加载功能测试面板中的请求配置
 */
@Slf4j
@Component
public class FunctionalPersistenceService {
    private static final String FILE_PATH = ConfigPathConstants.FUNCTIONAL_CONFIG;
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024; // 2MB

    @PostConstruct
    public void init() {
        ensureDirExists();
    }

    private void ensureDirExists() {
        ensureDirExists(getConfigFilePath());
    }

    private void ensureDirExists(Path configPath) {
        try {
            Path configDir = configPath.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            log.error("Failed to create config directory: {}", e.getMessage());
        }
    }

    /**
     * 保存功能测试配置
     * 只保存请求ID引用，不保存完整配置，确保与集合中的请求保持同步
     */
    public void save(List<RunnerRowData> rows) {
        save(rows, null);
    }

    /**
     * 保存功能测试配置
     */
    public void save(List<RunnerRowData> rows, CsvDataPanel.CsvState csvState) {
        save(getConfigFilePath(), rows, csvState);
    }

    private void save(Path configPath, List<RunnerRowData> rows, CsvDataPanel.CsvState csvState) {
        try {
            ensureDirExists(configPath);
            JSONObject root = new JSONObject();
            root.set("version", "1.0");
            root.set("rows", serializeRows(rows));
            if (csvState != null) {
                root.set("csvState", serializeCsvState(csvState));
            }

            // 写入文件
            String jsonString = JSONUtil.toJsonPrettyStr(root);
            Files.writeString(configPath, jsonString, StandardCharsets.UTF_8);

            log.info("Successfully saved {} functional test configurations", rows.size());
        } catch (IOException e) {
            log.error("Failed to save functional test config: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步保存配置
     */
    public void saveAsync(List<RunnerRowData> rows) {
        saveAsync(rows, null);
    }

    /**
     * 异步保存配置
     */
    public void saveAsync(List<RunnerRowData> rows, CsvDataPanel.CsvState csvState) {
        // 异步线程启动前先固定路径，防止用户切换 workspace 后把旧数据写到新 workspace。
        Path configPath = getConfigFilePath();
        Thread saveThread = new Thread(() -> save(configPath, rows, csvState), "functional-config-save");
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * 加载功能测试配置
     * 通过ID从集合中获取最新的请求配置，确保与集合保持同步
     */
    public List<RunnerRowData> load() {
        List<RunnerRowData> rows = new ArrayList<>();
        Path configPath = getConfigFilePath();
        File file = configPath.toFile();

        if (!file.exists()) {
            log.info("No functional test config file found, starting fresh");
            return rows;
        }

        try {
            // 检查文件大小
            long fileSizeInBytes = file.length();
            if (fileSizeInBytes > MAX_FILE_SIZE) {
                log.warn("Config file is too large ({} bytes), deleting and starting fresh", fileSizeInBytes);
                deleteFile(file);
                return rows;
            }

            if (fileSizeInBytes == 0) {
                return rows;
            }

            // 读取文件
            String jsonString = Files.readString(configPath, StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return rows;
            }

            JSONObject root = JSONUtil.parseObj(jsonString);
            JSONArray jsonArray = root.getJSONArray("rows");
            rows.addAll(deserializeRows(jsonArray));

            log.info("Successfully loaded {} functional test configurations", rows.size());

        } catch (Exception e) {
            log.error("Failed to load functional test config: {}", e.getMessage(), e);
            deleteFile(file);
        }

        return rows;
    }

    public CsvDataPanel.CsvState loadCsvState() {
        Path configPath = getConfigFilePath();
        File file = configPath.toFile();

        if (!file.exists()) {
            return null;
        }

        try {
            long fileSizeInBytes = file.length();
            if (fileSizeInBytes == 0 || fileSizeInBytes > MAX_FILE_SIZE) {
                return null;
            }

            String jsonString = Files.readString(configPath, StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return null;
            }

            JSONObject root = JSONUtil.parseObj(jsonString);
            JSONObject csvStateJson = root.getJSONObject("csvState");
            if (csvStateJson == null) {
                return null;
            }
            return deserializeCsvState(csvStateJson);
        } catch (Exception e) {
            log.error("Failed to load functional csvState: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清空配置
     */
    public void clear() {
        File file = getConfigFilePath().toFile();
        if (file.exists()) {
            deleteFile(file);
        }
    }

    /**
     * 通过ID从集合中查找请求项
     */
    public HttpRequestItem findRequestItemById(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return null;
        }

        try {
            RequestCollectionsLeftPanel collectionsPanel =
                    SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);

            DefaultMutableTreeNode rootNode = collectionsPanel.getRootTreeNode();
            DefaultMutableTreeNode requestNode =
                    RequestCollectionsService.findRequestNodeById(rootNode, requestId);

            if (requestNode != null) {
                Object userObj = requestNode.getUserObject();
                if (userObj instanceof Object[] obj) {
                    if (obj.length > 1 && obj[1] instanceof HttpRequestItem) {
                        return (HttpRequestItem) obj[1];
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find request item by ID {}: {}", requestId, e.getMessage());
        }

        return null;
    }

    /**
     * 删除配置文件
     */
    private void deleteFile(File file) {
        try {
            if (file.exists() && !file.delete()) {
                log.warn("Failed to delete config file: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error deleting config file: {}", e.getMessage());
        }
    }

    private JSONArray serializeRows(List<RunnerRowData> rows) {
        JSONArray jsonArray = new JSONArray();
        for (RunnerRowData row : rows) {
            if (row == null || row.requestItem == null) {
                continue;
            }

            JSONObject jsonItem = new JSONObject();
            jsonItem.set("selected", row.selected);
            jsonItem.set("requestItemId", row.requestItem.getId());
            jsonArray.add(jsonItem);
        }
        return jsonArray;
    }

    private List<RunnerRowData> deserializeRows(JSONArray jsonArray) {
        List<RunnerRowData> rows = new ArrayList<>();
        if (jsonArray == null) {
            return rows;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                JSONObject jsonItem = jsonArray.getJSONObject(i);
                String requestItemId = jsonItem.getStr("requestItemId");
                boolean selected = jsonItem.getBool("selected", true);

                HttpRequestItem requestItem = findRequestItemById(requestItemId);
                if (requestItem == null) {
                    log.warn("Request with ID {} not found in collections, skipping", requestItemId);
                    continue;
                }

                PreparedRequest preparedRequest = PreparedRequestBuilder.build(requestItem);
                RunnerRowData row = new RunnerRowData(requestItem, preparedRequest);
                row.selected = selected;
                rows.add(row);
            } catch (Exception e) {
                log.warn("Failed to restore config item at index {}: {}", i, e.getMessage());
            }
        }
        return rows;
    }

    private JSONObject serializeCsvState(CsvDataPanel.CsvState csvState) {
        JSONObject json = new JSONObject();
        json.set("sourceName", csvState.getSourceName());

        JSONArray headers = new JSONArray();
        for (String header : csvState.getHeaders()) {
            headers.add(header);
        }
        json.set("headers", headers);

        JSONArray rows = new JSONArray();
        for (java.util.Map<String, String> row : csvState.getRows()) {
            JSONObject rowJson = new JSONObject();
            if (row != null) {
                for (java.util.Map.Entry<String, String> entry : row.entrySet()) {
                    rowJson.set(entry.getKey(), entry.getValue());
                }
            }
            rows.add(rowJson);
        }
        json.set("rows", rows);
        return json;
    }

    private CsvDataPanel.CsvState deserializeCsvState(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONArray headersJson = json.getJSONArray("headers");
        JSONArray rowsJson = json.getJSONArray("rows");
        if (headersJson == null || rowsJson == null || rowsJson.isEmpty()) {
            return null;
        }

        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headersJson.size(); i++) {
            headers.add(headersJson.getStr(i));
        }

        List<java.util.Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < rowsJson.size(); i++) {
            JSONObject rowJson = rowsJson.getJSONObject(i);
            java.util.Map<String, String> row = new java.util.LinkedHashMap<>();
            if (rowJson != null) {
                for (String header : headers) {
                    row.put(header, rowJson.getStr(header, ""));
                }
            }
            rows.add(row);
        }

        return new CsvDataPanel.CsvState(json.getStr("sourceName"), headers, rows);
    }

    protected Path getConfigFilePath() {
        Workspace workspace = getCurrentWorkspace();
        Path workspaceConfigPath = Paths.get(ConfigPathConstants.getFunctionalConfigPath(workspace));
        return WorkspaceScopedConfigSupport.resolveConfigPath(
                workspace,
                workspaceConfigPath,
                getLegacyConfigFilePath(),
                "functional",
                log
        );
    }

    protected Workspace getCurrentWorkspace() {
        try {
            return WorkspaceService.getInstance().getCurrentWorkspace();
        } catch (Exception e) {
            log.debug("Failed to resolve current workspace for functional config path", e);
            return null;
        }
    }

    protected Path getLegacyConfigFilePath() {
        return Paths.get(FILE_PATH);
    }
}
