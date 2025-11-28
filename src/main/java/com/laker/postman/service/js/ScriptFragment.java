package com.laker.postman.service.js;

import lombok.Getter;

/**
 * 脚本片段
 * <p>
 * 表示一个可执行的脚本片段，包含脚本内容和来源标识。
 * 主要用于脚本继承场景，区分脚本来自请求本身还是从父分组继承。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>分组级别的脚本继承</li>
 *   <li>合并多个来源的脚本</li>
 *   <li>脚本来源追踪和调试</li>
 * </ul>
 *
 * <h3>示例：</h3>
 * <pre>{@code
 * // 创建请求自身的脚本片段
 * ScriptFragment requestScript = new ScriptFragment(
 *     "pm.environment.set('token', 'abc123');",
 *     "Request: /api/login"
 * );
 *
 * // 创建继承的脚本片段
 * ScriptFragment inheritedScript = new ScriptFragment(
 *     "pm.request.headers.add({key: 'X-API-Key', value: pm.environment.get('apiKey')});",
 *     "Group: API Requests"
 * );
 *
 * // 合并脚本
 * List<ScriptFragment> fragments = Arrays.asList(inheritedScript, requestScript);
 * String mergedScript = ScriptMerger.merge(fragments);
 * }</pre>
 *
 * @see ScriptMerger
 * @author laker
 */
@Getter
public class ScriptFragment {
    /**
     * 脚本内容（JavaScript 代码）
     */
    private final String content;

    /**
     * 脚本来源标识
     * <p>
     * 用于标识脚本来源，便于调试和追踪。例如：
     * <ul>
     *   <li>"Request: /api/users"</li>
     *   <li>"Group: API Requests"</li>
     *   <li>"Inherited: Root Collection"</li>
     * </ul>
     * </p>
     */
    private final String source;

    /**
     * 构造脚本片段（带来源标识）
     *
     * @param source 脚本来源标识
     * @param content 脚本内容
     */
    public ScriptFragment(String source, String content) {
        this.source = source;
        this.content = content;
    }

    /**
     * 构造脚本片段（无来源标识）
     *
     * @param content 脚本内容
     */
    public ScriptFragment(String content) {
        this(null, content);
    }

    /**
     * 工厂方法：创建带来源标识的脚本片段
     *
     * @param source 脚本来源标识
     * @param content 脚本内容
     * @return 脚本片段实例
     */
    public static ScriptFragment of(String source, String content) {
        return new ScriptFragment(source, content);
    }

    /**
     * 工厂方法：创建无来源标识的脚本片段
     *
     * @param content 脚本内容
     * @return 脚本片段实例
     */
    public static ScriptFragment of(String content) {
        return new ScriptFragment(content);
    }
}