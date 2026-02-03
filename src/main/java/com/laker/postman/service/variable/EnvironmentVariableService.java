package com.laker.postman.service.variable;

import com.laker.postman.model.Environment;
import com.laker.postman.service.EnvironmentService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * 环境变量提供者
 * <p>
 * 负责从当前激活的环境中获取环境变量
 * <ul>
 *   <li>优先级中等（优先级 = 2）</li>
 *   <li>低于临时变量，高于内置函数</li>
 * </ul>
 */
@Slf4j
public class EnvironmentVariableService implements VariableProvider {

    /**
     * 单例实例
     */
    private static final EnvironmentVariableService INSTANCE = new EnvironmentVariableService();

    /**
     * 私有构造函数，防止外部实例化
     */
    private EnvironmentVariableService() {
    }

    /**
     * 获取单例实例
     */
    public static EnvironmentVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv == null) {
            return null;
        }

        return activeEnv.getVariable(key);
    }

    @Override
    public boolean has(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv == null) {
            return false;
        }

        return activeEnv.getVariable(key) != null;
    }

    @Override
    public Map<String, String> getAll() {
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv == null || activeEnv.getVariables() == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(activeEnv.getVariables());
    }

    @Override
    public int getPriority() {
        return 2; // 中等优先级
    }

    @Override
    public String getName() {
        return "Environment Variables";
    }
}
