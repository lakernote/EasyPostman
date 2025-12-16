package com.laker.postman.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PostConstruct;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.functional.table.RunnerRowData;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.util.SystemUtil;
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
    private static final String FILE_PATH = SystemUtil.getUserHomeEasyPostmanPath() + "functional_config.json";
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024; // 2MB

    @PostConstruct
    public void init() {
        ensureDirExists();
    }

    private void ensureDirExists() {
        try {
            Path configDir = Paths.get(SystemUtil.getUserHomeEasyPostmanPath());
            if (!Files.exists(configDir)) {
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
        try {
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

            // 写入文件
            String jsonString = JSONUtil.toJsonPrettyStr(jsonArray);
            Files.writeString(Paths.get(FILE_PATH), jsonString, StandardCharsets.UTF_8);

            log.info("Successfully saved {} functional test configurations", rows.size());
        } catch (IOException e) {
            log.error("Failed to save functional test config: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步保存配置
     */
    public void saveAsync(List<RunnerRowData> rows) {
        Thread saveThread = new Thread(() -> save(rows));
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * 加载功能测试配置
     * 通过ID从集合中获取最新的请求配置，确保与集合保持同步
     */
    public List<RunnerRowData> load() {
        List<RunnerRowData> rows = new ArrayList<>();
        File file = new File(FILE_PATH);

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
            String jsonString = Files.readString(Paths.get(FILE_PATH), StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return rows;
            }

            JSONArray jsonArray = JSONUtil.parseArray(jsonString);

            for (int i = 0; i < jsonArray.size(); i++) {
                try {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    String requestItemId = jsonItem.getStr("requestItemId");
                    boolean selected = jsonItem.getBool("selected", true);

                    // 通过ID从集合中查找最新的请求配置
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

            log.info("Successfully loaded {} functional test configurations", rows.size());

        } catch (Exception e) {
            log.error("Failed to load functional test config: {}", e.getMessage(), e);
            deleteFile(file);
        }

        return rows;
    }

    /**
     * 清空配置
     */
    public void clear() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            deleteFile(file);
        }
    }

    /**
     * 通过ID从集合中查找请求项
     */
    private HttpRequestItem findRequestItemById(String requestId) {
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
}

