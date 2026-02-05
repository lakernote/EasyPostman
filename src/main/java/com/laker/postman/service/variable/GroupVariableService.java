package com.laker.postman.service.variable;

import com.laker.postman.model.Variable;
import com.laker.postman.service.collections.GroupInheritanceHelper;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组级别变量服务（单例模式）
 * <p>
 * 提供从请求所在分组继承的变量
 * 变量优先级：内层分组 > 外层分组
 * <p>
 * 注意：使用全局 RequestContext 获取当前请求节点
 */
@Slf4j
public class GroupVariableService implements VariableProvider {

    /**
     * 单例实例
     */
    private static final GroupVariableService INSTANCE = new GroupVariableService();

    /**
     * 私有构造函数，防止外部实例化
     */
    private GroupVariableService() {
    }

    /**
     * 获取单例实例
     *
     * @return GroupVariableService 单例
     */
    public static GroupVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        DefaultMutableTreeNode requestNode = RequestContext.getCurrentRequestNode();
        if (requestNode == null) {
            return null;
        }

        List<Variable> variables = GroupInheritanceHelper.getMergedGroupVariables(requestNode);
        for (Variable variable : variables) {
            if (key.equals(variable.getKey())) {
                return variable.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean has(String key) {
        return get(key) != null;
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();

        DefaultMutableTreeNode requestNode = RequestContext.getCurrentRequestNode();
        if (requestNode == null) {
            return result;
        }

        List<Variable> variables = GroupInheritanceHelper.getMergedGroupVariables(requestNode);
        for (Variable variable : variables) {
            if (variable.getKey() != null && !variable.getKey().trim().isEmpty()) {
                result.put(variable.getKey(), variable.getValue());
            }
        }
        return result;
    }

    @Override
    public int getPriority() {
        return VariableType.GROUP.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.GROUP;
    }
}
