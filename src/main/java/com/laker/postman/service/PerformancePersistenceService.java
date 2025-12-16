package com.laker.postman.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PostConstruct;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.timer.TimerData;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 性能测试配置持久化服务
 * 用于保存和加载性能测试面板中的测试计划配置
 */
@Slf4j
@Component
public class PerformancePersistenceService {
    private static final String FILE_PATH = SystemUtil.getUserHomeEasyPostmanPath() + "performance_config.json";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

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
     * 保存性能测试配置树结构
     * 只保存请求ID引用，不保存完整请求配置，确保与集合中的请求保持同步
     */
    public void save(DefaultMutableTreeNode rootNode) {
        try {
            JSONObject jsonRoot = new JSONObject();
            jsonRoot.set("version", "1.0");
            jsonRoot.set("tree", serializeTreeNode(rootNode));

            // 写入文件
            String jsonString = JSONUtil.toJsonPrettyStr(jsonRoot);
            Files.writeString(Paths.get(FILE_PATH), jsonString, StandardCharsets.UTF_8);

            log.info("Successfully saved performance test configuration");
        } catch (IOException e) {
            log.error("Failed to save performance test config: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步保存配置
     */
    public void saveAsync(DefaultMutableTreeNode rootNode) {
        Thread saveThread = new Thread(() -> save(rootNode));
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * 序列化树节点（递归）
     */
    private JSONObject serializeTreeNode(DefaultMutableTreeNode treeNode) {
        JSONObject jsonNode = new JSONObject();

        Object userObj = treeNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jmNode)) {
            return jsonNode;
        }

        // 保存节点基本信息
        jsonNode.set("name", jmNode.name);
        jsonNode.set("type", jmNode.type.name());
        jsonNode.set("enabled", jmNode.enabled);

        // 根据节点类型保存数据
        switch (jmNode.type) {
            case THREAD_GROUP -> {
                if (jmNode.threadGroupData != null) {
                    jsonNode.set("threadGroupData", serializeThreadGroupData(jmNode.threadGroupData));
                }
            }
            case REQUEST -> {
                // 只保存请求ID，不保存完整配置
                if (jmNode.httpRequestItem != null) {
                    jsonNode.set("requestItemId", jmNode.httpRequestItem.getId());
                }
            }
            case ASSERTION -> {
                if (jmNode.assertionData != null) {
                    jsonNode.set("assertionData", serializeAssertionData(jmNode.assertionData));
                }
            }
            case TIMER -> {
                if (jmNode.timerData != null) {
                    jsonNode.set("timerData", serializeTimerData(jmNode.timerData));
                }
            }
        }

        // 递归序列化子节点
        JSONArray children = new JSONArray();
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            children.add(serializeTreeNode(childNode));
        }
        if (!children.isEmpty()) {
            jsonNode.set("children", children);
        }

        return jsonNode;
    }

    /**
     * 序列化线程组数据
     */
    private JSONObject serializeThreadGroupData(ThreadGroupData data) {
        JSONObject json = new JSONObject();
        json.set("threadMode", data.threadMode.name());
        json.set("numThreads", data.numThreads);
        json.set("duration", data.duration);
        json.set("loops", data.loops);
        json.set("useTime", data.useTime);
        json.set("rampUpStartThreads", data.rampUpStartThreads);
        json.set("rampUpEndThreads", data.rampUpEndThreads);
        json.set("rampUpTime", data.rampUpTime);
        json.set("rampUpDuration", data.rampUpDuration);
        json.set("spikeMinThreads", data.spikeMinThreads);
        json.set("spikeMaxThreads", data.spikeMaxThreads);
        json.set("spikeRampUpTime", data.spikeRampUpTime);
        json.set("spikeHoldTime", data.spikeHoldTime);
        json.set("spikeRampDownTime", data.spikeRampDownTime);
        json.set("spikeDuration", data.spikeDuration);
        json.set("stairsStartThreads", data.stairsStartThreads);
        json.set("stairsEndThreads", data.stairsEndThreads);
        json.set("stairsStep", data.stairsStep);
        json.set("stairsHoldTime", data.stairsHoldTime);
        json.set("stairsDuration", data.stairsDuration);
        return json;
    }

    /**
     * 序列化断言数据
     */
    private JSONObject serializeAssertionData(AssertionData data) {
        JSONObject json = new JSONObject();
        json.set("type", data.type);
        json.set("content", data.content);
        json.set("operator", data.operator);
        json.set("value", data.value);
        return json;
    }

    /**
     * 序列化定时器数据
     */
    private JSONObject serializeTimerData(TimerData data) {
        JSONObject json = new JSONObject();
        json.set("delayMs", data.delayMs);
        return json;
    }

    /**
     * 加载性能测试配置
     * 通过ID从集合中获取最新的请求配置，确保与集合保持同步
     */
    public DefaultMutableTreeNode load(String rootName) {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            log.info("No performance test config file found, starting fresh");
            return null;
        }

        try {
            // 检查文件大小
            long fileSizeInBytes = file.length();
            if (fileSizeInBytes > MAX_FILE_SIZE) {
                log.warn("Config file is too large ({} bytes), deleting and starting fresh", fileSizeInBytes);
                deleteFile(file);
                return null;
            }

            if (fileSizeInBytes == 0) {
                return null;
            }

            // 读取文件
            String jsonString = Files.readString(Paths.get(FILE_PATH), StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return null;
            }

            JSONObject jsonRoot = JSONUtil.parseObj(jsonString);
            JSONObject treeJson = jsonRoot.getJSONObject("tree");

            if (treeJson == null) {
                return null;
            }

            DefaultMutableTreeNode rootNode = deserializeTreeNode(treeJson);
            if (rootNode != null) {
                // 更新根节点名称为当前的rootName
                Object userObj = rootNode.getUserObject();
                if (userObj instanceof JMeterTreeNode jmNode) {
                    jmNode.name = rootName;
                }
            }

            log.info("Successfully loaded performance test configuration");
            return rootNode;

        } catch (Exception e) {
            log.error("Failed to load performance test config: {}", e.getMessage(), e);
            deleteFile(file);
        }

        return null;
    }

    /**
     * 反序列化树节点（递归）
     */
    private DefaultMutableTreeNode deserializeTreeNode(JSONObject jsonNode) {
        try {
            String name = jsonNode.getStr("name");
            String typeStr = jsonNode.getStr("type");
            Boolean enabled = jsonNode.getBool("enabled", true);

            if (name == null || typeStr == null) {
                return null;
            }

            NodeType nodeType = NodeType.valueOf(typeStr);
            JMeterTreeNode jmNode = new JMeterTreeNode(name, nodeType);
            jmNode.enabled = enabled;

            // 根据节点类型恢复数据
            switch (nodeType) {
                case THREAD_GROUP -> {
                    JSONObject tgData = jsonNode.getJSONObject("threadGroupData");
                    if (tgData != null) {
                        jmNode.threadGroupData = deserializeThreadGroupData(tgData);
                    }
                }
                case REQUEST -> {
                    String requestItemId = jsonNode.getStr("requestItemId");
                    if (requestItemId != null) {
                        // 通过ID从集合中查找最新的请求配置
                        HttpRequestItem requestItem = findRequestItemById(requestItemId);
                        if (requestItem == null) {
                            log.warn("Request with ID {} not found in collections, skipping", requestItemId);
                            return null; // 跳过不存在的请求
                        }
                        jmNode.httpRequestItem = requestItem;
                    }
                }
                case ASSERTION -> {
                    JSONObject assertionData = jsonNode.getJSONObject("assertionData");
                    if (assertionData != null) {
                        jmNode.assertionData = deserializeAssertionData(assertionData);
                    }
                }
                case TIMER -> {
                    JSONObject timerData = jsonNode.getJSONObject("timerData");
                    if (timerData != null) {
                        jmNode.timerData = deserializeTimerData(timerData);
                    }
                }
            }

            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(jmNode);

            // 递归反序列化子节点
            JSONArray children = jsonNode.getJSONArray("children");
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    JSONObject childJson = children.getJSONObject(i);
                    DefaultMutableTreeNode childNode = deserializeTreeNode(childJson);
                    if (childNode != null) {
                        treeNode.add(childNode);
                    }
                }
            }

            return treeNode;
        } catch (Exception e) {
            log.warn("Failed to deserialize tree node: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 反序列化线程组数据
     */
    private ThreadGroupData deserializeThreadGroupData(JSONObject json) {
        ThreadGroupData data = new ThreadGroupData();
        try {
            String threadMode = json.getStr("threadMode");
            if (threadMode != null) {
                data.threadMode = ThreadGroupData.ThreadMode.valueOf(threadMode);
            }
            data.numThreads = json.getInt("numThreads", 20);
            data.duration = json.getInt("duration", 60);
            data.loops = json.getInt("loops", 1);
            data.useTime = json.getBool("useTime", true);
            data.rampUpStartThreads = json.getInt("rampUpStartThreads", 1);
            data.rampUpEndThreads = json.getInt("rampUpEndThreads", 20);
            data.rampUpTime = json.getInt("rampUpTime", 30);
            data.rampUpDuration = json.getInt("rampUpDuration", 60);
            data.spikeMinThreads = json.getInt("spikeMinThreads", 1);
            data.spikeMaxThreads = json.getInt("spikeMaxThreads", 20);
            data.spikeRampUpTime = json.getInt("spikeRampUpTime", 20);
            data.spikeHoldTime = json.getInt("spikeHoldTime", 15);
            data.spikeRampDownTime = json.getInt("spikeRampDownTime", 20);
            data.spikeDuration = json.getInt("spikeDuration", 60);
            data.stairsStartThreads = json.getInt("stairsStartThreads", 5);
            data.stairsEndThreads = json.getInt("stairsEndThreads", 20);
            data.stairsStep = json.getInt("stairsStep", 5);
            data.stairsHoldTime = json.getInt("stairsHoldTime", 15);
            data.stairsDuration = json.getInt("stairsDuration", 60);
        } catch (Exception e) {
            log.warn("Failed to deserialize thread group data: {}", e.getMessage());
        }
        return data;
    }

    /**
     * 反序列化断言数据
     */
    private AssertionData deserializeAssertionData(JSONObject json) {
        AssertionData data = new AssertionData();
        data.type = json.getStr("type", "Response Code");
        data.content = json.getStr("content", "");
        data.operator = json.getStr("operator", "=");
        data.value = json.getStr("value", "200");
        return data;
    }

    /**
     * 反序列化定时器数据
     */
    private TimerData deserializeTimerData(JSONObject json) {
        TimerData data = new TimerData();
        data.delayMs = json.getInt("delayMs", 1000);
        return data;
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
     * 注意：返回的是深拷贝，避免性能测试面板中的修改影响集合中的原始数据
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
                    if (obj.length > 1 && obj[1] instanceof HttpRequestItem originalItem) {
                        // 使用 JSON 序列化/反序列化进行深拷贝
                        // 这样可以确保性能测试面板中的修改不会影响集合中的原始请求
                        return deepCopyRequestItem(originalItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find request item by ID {}: {}", requestId, e.getMessage());
        }

        return null;
    }

    /**
     * 深拷贝 HttpRequestItem 对象
     * 使用 JSON 序列化/反序列化实现深拷贝
     */
    private HttpRequestItem deepCopyRequestItem(HttpRequestItem original) {
        try {
            // 将对象转换为 JSON
            String json = JSONUtil.toJsonStr(original);
            // 从 JSON 反序列化为新对象
            return JSONUtil.toBean(json, HttpRequestItem.class);
        } catch (Exception e) {
            log.error("Failed to deep copy request item: {}", e.getMessage());
            // 如果深拷贝失败，返回原始对象（降级处理）
            return original;
        }
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

