package com.laker.postman.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.Environment;
import com.laker.postman.model.Variable;
import com.laker.postman.service.variable.VariableProvider;
import com.laker.postman.service.variable.VariableType;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应用级全局变量服务。
 * <p>
 * 与环境变量不同，全局变量不跟随工作区切换，始终持久化到应用级数据目录。
 */
@Slf4j
public class GlobalVariablesService implements VariableProvider {

    private static final GlobalVariablesService INSTANCE = new GlobalVariablesService();
    private static final String GLOBALS_ID = "globals";
    private static final String GLOBALS_NAME = "Globals";

    private final Environment globalVariables = new PersistentGlobalEnvironment();
    private String currentDataFilePath;

    private GlobalVariablesService() {
        loadGlobalVariables();
    }

    public static GlobalVariablesService getInstance() {
        return INSTANCE;
    }

    public synchronized Environment getGlobalVariables() {
        return globalVariables;
    }

    public synchronized String getDataFilePath() {
        if (currentDataFilePath != null && !currentDataFilePath.isBlank()) {
            return currentDataFilePath;
        }
        return ConfigPathConstants.GLOBAL_VARIABLES;
    }

    /**
     * 仅用于测试隔离或特殊场景注入路径。
     */
    public synchronized void setDataFilePath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        currentDataFilePath = path;
        loadGlobalVariables();
    }

    public synchronized void resetDataFilePath() {
        currentDataFilePath = null;
        loadGlobalVariables();
    }

    public synchronized void saveGlobalVariables() {
        saveGlobalVariablesInternal();
    }

    private void saveGlobalVariablesInternal() {
        try {
            File file = new File(getDataFilePath());
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            ensureMetadata();
            FileUtil.writeString(JSONUtil.toJsonPrettyStr(globalVariables), file, StandardCharsets.UTF_8);
            log.debug("全局变量已保存到: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("保存全局变量失败", e);
        }
    }

    public synchronized void replaceGlobalVariables(List<Variable> variables) {
        globalVariables.setVariableList(copyVariables(variables));
        ensureMetadata();
        saveGlobalVariablesInternal();
    }

    public synchronized List<Variable> mergeVariables(List<Variable> originalVariables, List<Variable> editedVariables) {
        List<Variable> originalCopy = copyVariables(originalVariables);
        List<Variable> editedCopy = copyVariables(editedVariables);
        List<Variable> latestCopy = copyVariables(globalVariables.getVariableList());

        Map<String, Variable> originalMap = toVariableMap(originalCopy);
        Map<String, Variable> latestMap = toVariableMap(latestCopy);
        Map<String, Variable> mergedMap = new LinkedHashMap<>();

        for (Variable edited : editedCopy) {
            String key = normalizeKey(edited);
            if (key == null) {
                continue;
            }

            Variable original = originalMap.get(key);
            Variable latest = latestMap.get(key);
            if (original == null) {
                mergedMap.put(key, copyVariable(edited));
                continue;
            }

            if (isSameVariable(edited, original)) {
                if (latest != null) {
                    mergedMap.put(key, copyVariable(latest));
                }
            } else {
                mergedMap.put(key, copyVariable(edited));
            }
        }

        for (Variable latest : latestCopy) {
            String key = normalizeKey(latest);
            if (key == null || mergedMap.containsKey(key) || originalMap.containsKey(key)) {
                continue;
            }
            mergedMap.put(key, copyVariable(latest));
        }

        return new ArrayList<>(mergedMap.values());
    }

    private synchronized void loadGlobalVariables() {
        globalVariables.setId(GLOBALS_ID);
        globalVariables.setName(GLOBALS_NAME);
        globalVariables.setActive(false);

        File file = new File(getDataFilePath());
        if (!file.exists()) {
            globalVariables.setVariableList(new ArrayList<>());
            return;
        }

        try {
            String json = FileUtil.readString(file, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                globalVariables.setVariableList(new ArrayList<>());
                return;
            }

            Object parsed = JSONUtil.parse(json);
            if (parsed instanceof JSONObject obj) {
                Environment loaded = JSONUtil.toBean(obj, Environment.class);
                List<Variable> loadedVariables = loaded.getVariableList() != null
                        ? new ArrayList<>(loaded.getVariableList())
                        : new ArrayList<>();
                globalVariables.setVariableList(loadedVariables);
            } else if (parsed instanceof JSONArray array) {
                globalVariables.setVariableList(new ArrayList<>(JSONUtil.toList(array, Variable.class)));
            } else {
                globalVariables.setVariableList(new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("加载全局变量失败: {}", file.getAbsolutePath(), e);
            globalVariables.setVariableList(new ArrayList<>());
        }

        ensureMetadata();
    }

    private void ensureMetadata() {
        globalVariables.setId(GLOBALS_ID);
        globalVariables.setName(GLOBALS_NAME);
        globalVariables.setActive(false);
        if (globalVariables.getVariableList() == null) {
            globalVariables.setVariableList(new ArrayList<>());
        }
    }

    private List<Variable> copyVariables(List<Variable> variables) {
        List<Variable> result = new ArrayList<>();
        if (variables == null) {
            return result;
        }
        for (Variable variable : variables) {
            Variable copy = copyVariable(variable);
            if (copy != null) {
                result.add(copy);
            }
        }
        return result;
    }

    private Variable copyVariable(Variable variable) {
        if (variable == null) {
            return null;
        }
        return new Variable(variable.isEnabled(), variable.getKey(), variable.getValue());
    }

    private Map<String, Variable> toVariableMap(List<Variable> variables) {
        Map<String, Variable> map = new LinkedHashMap<>();
        if (variables == null) {
            return map;
        }
        for (Variable variable : variables) {
            String key = normalizeKey(variable);
            if (key != null) {
                map.put(key, copyVariable(variable));
            }
        }
        return map;
    }

    private String normalizeKey(Variable variable) {
        if (variable == null || variable.getKey() == null) {
            return null;
        }
        String key = variable.getKey().trim();
        return key.isEmpty() ? null : key;
    }

    private boolean isSameVariable(Variable left, Variable right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.isEnabled() == right.isEnabled()
                && Objects.equals(left.getKey(), right.getKey())
                && Objects.equals(left.getValue(), right.getValue());
    }

    private final class PersistentGlobalEnvironment extends Environment {

        private PersistentGlobalEnvironment() {
            super(GLOBALS_NAME);
        }

        @Override
        public void addVariable(String key, String value) {
            mutateAndPersist(() -> super.addVariable(key, value));
        }

        @Override
        public void set(String key, String value) {
            mutateAndPersist(() -> super.set(key, value));
        }

        @Override
        public void set(String key, Object value) {
            mutateAndPersist(() -> super.set(key, value));
        }

        @Override
        public void removeVariable(String key) {
            mutateAndPersist(() -> super.removeVariable(key));
        }

        @Override
        public void unset(String key) {
            mutateAndPersist(() -> super.unset(key));
        }

        @Override
        public void clear() {
            mutateAndPersist(super::clear);
        }

        private void mutateAndPersist(Runnable mutation) {
            synchronized (GlobalVariablesService.this) {
                mutation.run();
                ensureMetadata();
                saveGlobalVariablesInternal();
            }
        }
    }

    @Override
    public synchronized String get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return globalVariables.get(key);
    }

    @Override
    public synchronized boolean has(String key) {
        return get(key) != null;
    }

    @Override
    public synchronized Map<String, String> getAll() {
        Map<String, String> variables = globalVariables.getVariables();
        return variables.isEmpty() ? Collections.emptyMap() : variables;
    }

    @Override
    public int getPriority() {
        return VariableType.GLOBAL.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.GLOBAL;
    }
}
