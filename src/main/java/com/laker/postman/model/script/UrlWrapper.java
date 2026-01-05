package com.laker.postman.model.script;

import com.laker.postman.model.HttpParam;

import java.util.ArrayList;
import java.util.List;

/**
 * URL 包装器 - 用于在 JavaScript 中访问 pm.request.url
 * <p>
 * 提供对 URL 查询参数的访问，模拟 Postman 的 pm.request.url 对象。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // JavaScript 脚本中
 * var password = pm.request.url.query.all()[1].value;
 * pm.request.url.query.all()[1].value = "newValue";
 * }</pre>
 */
public class UrlWrapper {

    /**
     * URL 字符串
     */
    public final String url;

    /**
     * 查询参数包装器
     */
    public final QueryWrapper query;

    public UrlWrapper(String url, List<HttpParam> params) {
        this.url = url;
        this.query = new QueryWrapper(params);
    }

    /**
     * 查询参数包装器类
     */
    public static class QueryWrapper {
        private final List<HttpParam> params;
        private List<ParamProxy> cachedProxies;

        public QueryWrapper(List<HttpParam> params) {
            this.params = params != null ? params : new ArrayList<>();
        }

        /**
         * 返回所有查询参数的 JavaScript 友好包装列表
         * 注意：返回的是缓存的代理列表，对代理字段的修改会保留
         */
        public List<ParamProxy> all() {
            if (cachedProxies == null) {
                cachedProxies = new ArrayList<>();
                for (HttpParam param : params) {
                    cachedProxies.add(new ParamProxy(param));
                }
            }
            return cachedProxies;
        }

        /**
         * 同步所有代理的修改回底层 HttpParam 对象
         */
        public void sync() {
            if (cachedProxies != null) {
                for (ParamProxy proxy : cachedProxies) {
                    proxy.sync();
                }
            }
        }
    }

    /**
     * 参数代理类 - 提供公共字段以便 JavaScript 访问
     * <p>
     * 这个类作为 HttpParam 的代理，暴露公共字段 key 和 value，
     * 使得 JavaScript 可以直接使用 param.value 而不是 param.getValue()
     * </p>
     *
     * <p>重要：为了让 GraalVM JavaScript 能够访问和修改字段，
     * 我们在构造时直接从底层 HttpParam 同步值到公共字段，
     * 然后通过拦截器或手动同步来确保修改被反映回去。</p>
     */
    public static class ParamProxy {
        private final HttpParam param;

        /**
         * 参数键（公共字段，可直接从 JavaScript 访问和修改）
         */
        @SuppressWarnings("checkstyle:VisibilityModifier")
        public String key;

        /**
         * 参数值（公共字段，可直接从 JavaScript 访问和修改）
         */
        @SuppressWarnings("checkstyle:VisibilityModifier")
        public String value;

        /**
         * 参数是否启用
         */
        @SuppressWarnings("checkstyle:VisibilityModifier")
        public boolean enabled;

        public ParamProxy(HttpParam param) {
            this.param = param;
            // 初始化时同步值
            this.key = param.getKey();
            this.value = param.getValue();
            this.enabled = param.isEnabled();
        }

        /**
         * 获取当前值（从底层 HttpParam 读取并同步到公共字段）
         */
        public String getValue() {
            this.value = param.getValue();
            return this.value;
        }

        /**
         * 设置值（同步到底层 HttpParam 和公共字段）
         */
        public void setValue(String value) {
            this.value = value;
            this.param.setValue(value);
        }

        /**
         * 获取键（从底层 HttpParam 读取并同步到公共字段）
         */
        public String getKey() {
            this.key = param.getKey();
            return this.key;
        }

        /**
         * 设置键（同步到底层 HttpParam 和公共字段）
         */
        public void setKey(String key) {
            this.key = key;
            this.param.setKey(key);
        }

        /**
         * 获取启用状态
         */
        public boolean isEnabled() {
            this.enabled = param.isEnabled();
            return this.enabled;
        }

        /**
         * 设置启用状态
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            this.param.setEnabled(enabled);
        }

        /**
         * 同步公共字段的修改回底层 HttpParam
         * 这个方法应该在 JavaScript 修改字段后被调用
         */
        public void sync() {
            this.param.setKey(this.key);
            this.param.setValue(this.value);
            this.param.setEnabled(this.enabled);
        }
    }
}

