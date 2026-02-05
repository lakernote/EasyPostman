package com.laker.postman.service.variable;

import java.util.Map;

/**
 * 变量提供者接口
 * <p>
 * 定义了变量提供者的通用行为，所有变量源（临时变量、环境变量、内置函数）都需要实现此接口
 * <p>
 * 设计原则：
 * <ul>
 *   <li>高内聚：每个实现类只负责一种变量源</li>
 *   <li>低耦合：通过接口隔离具体实现</li>
 *   <li>策略模式：可以轻松替换或扩展变量源</li>
 * </ul>
 */
public interface VariableProvider {

    /**
     * 获取变量的值
     *
     * @param key 变量名
     * @return 变量值，不存在则返回 null
     */
    String get(String key);

    /**
     * 检查变量是否存在
     *
     * @param key 变量名
     * @return 是否存在
     */
    boolean has(String key);

    /**
     * 获取所有变量
     *
     * @return 所有变量的 Map，可能为空但不为 null
     */
    Map<String, String> getAll();

    /**
     * 获取变量提供者的优先级
     * 数值越小优先级越高
     *
     * @return 优先级值
     */
    int getPriority();

    /**
     * 获取变量提供者的类型
     *
     * @return 变量类型枚举
     */
    VariableType getType();
}
