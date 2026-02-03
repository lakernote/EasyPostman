package com.laker.postman.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.model.Workspace;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 环境变量管理服务，负责环境变量的持久化、加载和管理
 */
@Slf4j
@UtilityClass
public class EnvironmentService {

    private static final Map<String, Environment> environments = Collections.synchronizedMap(new LinkedHashMap<>());

    @Getter
    private static Environment activeEnvironment = null;


    // 当前数据文件路径
    private static String currentDataFilePath;

    static {
        loadEnvironments();
    }

    /**
     * 获取当前数据文件路径
     */
    public static String getDataFilePath() {
        if (currentDataFilePath != null) {
            return currentDataFilePath;
        }
        // 如果没有设置，返回当前工作区的环境文件路径
        Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
        return SystemUtil.getEnvPathForWorkspace(currentWorkspace);
    }

    /**
     * 切换环境变量数据文件路径，并重新加载
     */
    public static void setDataFilePath(String path) {
        if (path == null || path.isBlank()) return;
        currentDataFilePath = path;
        loadEnvironmentsFromPath(path);
    }

    /**
     * 从指定路径加载环境变量
     */
    private static void loadEnvironmentsFromPath(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("环境变量文件不存在: {}, 将创建默认环境", filePath);
            createDefaultEnvironments();
            return;
        }

        try {
            environments.clear();
            JSONArray array = JSONUtil.readJSONArray(file, StandardCharsets.UTF_8);
            for (Object obj : array) {
                Environment env = JSONUtil.toBean((JSONObject) obj, Environment.class);
                environments.put(env.getId(), env);
                if (env.isActive()) {
                    activeEnvironment = env;
                }
            }

            if (activeEnvironment == null && !environments.isEmpty()) {
                // 如果没有激活的环境，激活第一个
                Environment firstEnv = environments.values().iterator().next();
                firstEnv.setActive(true);
                activeEnvironment = firstEnv;
                saveEnvironments();
            }
        } catch (Exception e) {
            log.error("加载环境变量失败: {}", filePath, e);
            createDefaultEnvironments();
        }
    }

    /**
     * 加载所有环境变量
     */
    public static void loadEnvironments() {
        if (currentDataFilePath != null) {
            loadEnvironmentsFromPath(currentDataFilePath);
        } else {
            Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
            String filePath = SystemUtil.getEnvPathForWorkspace(currentWorkspace);
            loadEnvironmentsFromPath(filePath);
        }
    }

    /**
     * 创建默认环境
     */
    private static void createDefaultEnvironments() {
        environments.clear();

        // Create Development Environment
        Environment devEnv = new Environment("Dev Env");
        devEnv.setId("dev-" + System.currentTimeMillis());
        devEnv.setActive(true);
        devEnv.addVariable("baseUrl", "https://so.gitee.com");
        devEnv.addVariable("apiKey", "dev-api-key-123");

        // Create Testing Environment
        Environment testEnv = new Environment("Test Env");
        testEnv.setId("test-" + System.currentTimeMillis());
        testEnv.addVariable("baseUrl", "https://so.csdn.net/so/search");
        testEnv.addVariable("apiKey", "test-api-key-456");

        environments.put(devEnv.getId(), devEnv);
        environments.put(testEnv.getId(), testEnv);
        activeEnvironment = devEnv;

        saveEnvironments();
    }

    /**
     * 保存所有环境变量
     */
    public static void saveEnvironments() {
        try {
            String filePath;
            if (currentDataFilePath != null) {
                filePath = currentDataFilePath;
            } else {
                Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
                filePath = SystemUtil.getEnvPathForWorkspace(currentWorkspace);
            }

            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            List<Environment> envList = new ArrayList<>(environments.values());
            String jsonStr = JSONUtil.toJsonPrettyStr(envList);

            FileUtil.writeString(jsonStr, file, StandardCharsets.UTF_8);
            log.debug("环境变量已保存到: {}", filePath);
        } catch (Exception e) {
            log.error("保存环境变量失败", e);
        }
    }

    /**
     * 添加或更新环境
     */
    public static void saveEnvironment(Environment environment) {
        // 如果环境没有 ID，自动生成一个
        if (environment.getId() == null) {
            environment.setId("env-" + IdUtil.simpleUUID());
        }
        environments.put(environment.getId(), environment);
        saveEnvironments();
    }

    /**
     * 删除环境
     */
    public static void deleteEnvironment(String id) {
        Environment env = environments.remove(id);
        if (env != null && env.isActive() && !environments.isEmpty()) {
            // 如果删除的是当前激活的环境，激活第一个环境
            Environment firstEnv = environments.values().iterator().next();
            firstEnv.setActive(true);
            activeEnvironment = firstEnv;
        }
        // if environments is empty after removal, set activeEnvironment to null
        if (environments.isEmpty()) {
            activeEnvironment = null;
        }
        saveEnvironments();
    }

    /**
     * 获取所有环境
     */
    public static List<Environment> getAllEnvironments() {
        return new ArrayList<>(environments.values());
    }

    /**
     * 设置激活的环境
     */
    public static void setActiveEnvironment(String id) {
        if (activeEnvironment != null) {
            activeEnvironment.setActive(false);
        }

        Environment env = environments.get(id);
        if (env != null) {
            env.setActive(true);
            activeEnvironment = env;
            saveEnvironments();
        }
    }

    /**
     * 替换文本中的环境变量占位符 (委托给 VariableResolver)
     * 例如: {{baseUrl}}/api/users -> https://api.example.com/api/users
     * 优先级: 临时变量 > 环境变量 > 内置函数
     *
     * @deprecated 请直接使用 VariableResolver.resolve()
     */
    @Deprecated
    public static String replaceVariables(String text) {
        return VariableResolver.resolve(text);
    }

    /**
     * 根据指定的id顺序重排environments并持久化
     */
    public static void saveEnvironmentOrder(List<String> idOrder) {
        synchronized (environments) {
            LinkedHashMap<String, Environment> newOrder = new LinkedHashMap<>();
            for (String id : idOrder) {
                Environment env = environments.get(id);
                if (env != null) {
                    newOrder.put(id, env);
                }
            }
            // 补充未在idOrder中的环境（防止遗漏）
            for (Map.Entry<String, Environment> entry : environments.entrySet()) {
                if (!newOrder.containsKey(entry.getKey())) {
                    newOrder.put(entry.getKey(), entry.getValue());
                }
            }
            environments.clear();
            environments.putAll(newOrder);
            saveEnvironments();
        }
    }
}