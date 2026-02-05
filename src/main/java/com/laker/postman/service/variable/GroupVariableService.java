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
 * 注意：此服务需要在请求执行前设置当前请求节点
 */
@Slf4j
public class GroupVariableService implements VariableProvider {

    /**
     * 单例实例
     */
    private static final GroupVariableService INSTANCE = new GroupVariableService();

    /**
     * 当前请求所在的树节点（线程局部变量，避免并发问题）
     */
    private final ThreadLocal<DefaultMutableTreeNode> currentRequestNode = new ThreadLocal<>();

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

    /**
     * 设置当前请求节点
     * <p>
     * 在请求执行前调用此方法，以便变量解析器能够获取正确的分组变量
     *
     * @param requestNode 请求节点
     */
    public void setCurrentRequestNode(DefaultMutableTreeNode requestNode) {
        currentRequestNode.set(requestNode);
    }

    /**
     * 清除当前请求节点
     * <p>
     * 在请求执行完成后调用此方法，释放资源
     */
    public void clearCurrentRequestNode() {
        currentRequestNode.remove();
    }

    @Override
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        DefaultMutableTreeNode requestNode = currentRequestNode.get();
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

        DefaultMutableTreeNode requestNode = currentRequestNode.get();
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
        // 优先级：临时变量(10) > 分组变量(20) > 环境变量(30) > 内置函数(100)
        return 20;
    }

    @Override
    public String getName() {
        return "Group Variables";
    }
}
