package com.laker.postman.model.script;

import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 响应断言对象 (pm.response)
 * <p>
 * 该类为 JavaScript 后置脚本提供对 HTTP 响应的访问和断言接口，模拟 Postman 的 pm.response 对象。
 * 支持链式断言语法，使测试脚本更加简洁和可读。
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li><b>状态码断言</b>: pm.response.to.have.status(200)</li>
 *   <li><b>响应头断言</b>: pm.response.to.have.header('Content-Type')</li>
 *   <li><b>响应时间断言</b>: pm.expect(pm.response.responseTime).to.be.below(1000)</li>
 *   <li><b>JSON 数据访问</b>: pm.response.json()</li>
 *   <li><b>响应体文本访问</b>: pm.response.text()</li>
 *   <li><b>响应头访问</b>: pm.response.headers.get('Content-Type')</li>
 * </ul>
 *
 * <h3>链式断言示例：</h3>
 * <pre>{@code
 * // 状态码断言
 * pm.response.to.have.status(200);
 *
 * // 响应头断言
 * pm.response.to.have.header('Content-Type');
 * pm.response.to.have.header('Authorization');
 *
 * // 响应时间断言
 * pm.expect(pm.response.responseTime).to.be.below(1000);
 *
 * // JSON 数据访问和断言
 * var jsonData = pm.response.json();
 * pm.expect(jsonData.status).to.equal("success");
 * pm.environment.set("token", jsonData.token);
 *
 * // 响应头访问
 * var contentType = pm.response.headers.get('Content-Type');
 * console.log("Content-Type:", contentType);
 * }</pre>
 *
 * <h3>支持的链式语法：</h3>
 * <ul>
 *   <li>pm.response.to.have.status(code) - 断言状态码</li>
 *   <li>pm.response.to.have.header(name) - 断言响应头存在</li>
 *   <li>pm.expect(value).to.be.below(max) - 断言数值小于指定值</li>
 * </ul>
 *
 * @see <a href="https://learning.postman.com/docs/writing-scripts/test-scripts/">Postman Test Scripts</a>
 * @author laker
 */
public class ResponseAssertion {
    /** HTTP 响应对象 */
    private HttpResponse response;

    /** 响应头访问器 - 对应 pm.response.headers */
    public Headers headers;

    /** 链式断言语法支持 - 对应 pm.response.to */
    public ResponseAssertion to = this;

    /** 链式断言语法支持 - 对应 pm.response.to.have */
    public ResponseAssertion have = this;

    /** 链式断言语法支持 - 对应 pm.response.to.be */
    public ResponseAssertion be = this;

    /** 响应时间（毫秒） - 对应 pm.response.responseTime */
    public long responseTime;

    /** 响应状态码 - 对应 pm.response.code */
    public int code;

    /**
     * 构造响应断言对象
     *
     * @param response HTTP 响应对象
     */
    public ResponseAssertion(HttpResponse response) {
        this.response = response;
        this.responseTime = response != null ? response.costMs : -1;
        this.code = response != null ? response.code : -1;
        this.headers = new Headers(this);
    }

    /**
     * 获取底层的 HTTP 响应对象
     *
     * @return HTTP 响应对象
     */
    public HttpResponse getHttpResponse() {
        return response;
    }

    /**
     * 返回当前对象以支持链式调用
     * 对应脚本中的: pm.response.to
     *
     * @return 当前 ResponseAssertion 对象
     */
    public ResponseAssertion to() {
        return this;
    }

    /**
     * 断言响应状态码
     * 对应脚本中的: pm.response.to.have.status(200)
     *
     * @param code 期望的状态码
     * @throws AssertionError 如果实际状态码不匹配
     */
    public void status(int code) {
        if (response == null || response.code != code) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_STATUS_FAILED, code, response == null ? null : response.code));
        }
    }

    /**
     * 断言响应头存在
     * 对应脚本中的: pm.response.to.have.header('Content-Type')
     *
     * @param name 响应头名称（不区分大小写）
     * @throws AssertionError 如果响应头不存在
     */
    public void header(String name) {
        if (response == null || response.headers == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_HEADER_NOT_FOUND));
        }
        boolean found = false;
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_HEADER_NOT_FOUND_WITH_NAME, name));
        }
    }

    /**
     * 断言数值小于指定值（通常用于响应时间）
     * 对应脚本中的: pm.expect(pm.response.responseTime).to.be.below(1000)
     *
     * @param ms 期望的最大值（毫秒）
     * @throws AssertionError 如果实际值大于或等于指定值
     */
    public void below(long ms) {
        if (response == null || response.costMs >= ms) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_BELOW_FAILED, ms, response == null ? null : response.costMs));
        }
    }

    /**
     * 获取响应体文本
     * 对应脚本中的: pm.response.text()
     *
     * @return 响应体字符串，响应为空则返回 null
     */
    public String text() {
        return response != null ? response.body : null;
    }

    /**
     * 解析响应体为 JSON 对象
     * 对应脚本中的: pm.response.json()
     *
     * @return JSON 对象，可在 JavaScript 中访问属性
     * @throws AssertionError 如果响应体不是有效的 JSON
     */
    public Object json() {
        try {
            if (response != null && response.body != null) {
                return JSONUtil.parse(response.body);
            }
        } catch (Exception e) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_INVALID_JSON, e.getMessage()));
        }
        return null;
    }

    /**
     * 获取指定名称的响应头值
     * 对应脚本中的: pm.response.headers.get('Content-Type')
     *
     * @param name 响应头名称（不区分大小写）
     * @return 响应头值，不存在则返回 null
     */
    public String getHeader(String name) {
        if (response == null || response.headers == null) return null;
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }

    /**
     * 创建期望断言对象（Chai.js 风格）
     * 对应脚本中的: pm.expect(value)
     *
     * @param actual 要断言的实际值
     * @return 期望断言对象，支持链式断言
     */
    public Expectation expect(Object actual) {
        return new Expectation(actual);
    }

    /**
     * 响应头访问器 (pm.response.headers)
     * <p>
     * 提供便捷的响应头访问接口，支持不区分大小写的响应头名称查询。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * var contentType = pm.response.headers.get('Content-Type');
     * var authorization = pm.response.headers.get('authorization'); // 不区分大小写
     *
     * // 检查 header 是否存在
     * if (pm.response.headers.has('Set-Cookie')) {
     *     console.log('Cookie was set');
     * }
     *
     * // 获取 header 数量
     * var count = pm.response.headers.count();
     *
     * // 遍历所有 headers
     * pm.response.headers.each(function(header) {
     *     console.log(header.key + ': ' + header.value);
     * });
     * }</pre>
     */
    public static class Headers {
        private final ResponseAssertion responseAssertion;

        /**
         * 构造响应头访问器
         *
         * @param responseAssertion 响应断言对象
         */
        public Headers(ResponseAssertion responseAssertion) {
            this.responseAssertion = responseAssertion;
        }

        /**
         * 获取指定名称的响应头值
         *
         * @param name 响应头名称（不区分大小写）
         * @return 响应头值，不存在则返回 null
         */
        public String get(String name) {
            return responseAssertion.getHeader(name);
        }

        /**
         * 检查指定名称的响应头是否存在
         *
         * @param name 响应头名称（不区分大小写）
         * @return 如果响应头存在返回 true，否则返回 false
         */
        public boolean has(String name) {
            return responseAssertion.getHeader(name) != null;
        }

        /**
         * 获取响应头的数量
         *
         * @return 响应头数量
         */
        public int count() {
            if (responseAssertion.response == null || responseAssertion.response.headers == null) {
                return 0;
            }
            return responseAssertion.response.headers.size();
        }

        /**
         * 遍历所有响应头
         *
         * @param callback 回调函数，接收 header 对象（包含 key 和 value 属性）
         */
        public void each(org.graalvm.polyglot.Value callback) {
            if (responseAssertion.response == null || responseAssertion.response.headers == null) {
                return;
            }
            if (callback == null || !callback.canExecute()) {
                return;
            }

            for (Map.Entry<String, List<String>> entry : responseAssertion.response.headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    // 创建 header 对象
                    HeaderEntry headerEntry = new HeaderEntry(entry.getKey(), entry.getValue().get(0));
                    callback.executeVoid(headerEntry);
                }
            }
        }

        /**
         * 获取所有响应头
         *
         * @return 响应头数组
         */
        public HeaderEntry[] all() {
            if (responseAssertion.response == null || responseAssertion.response.headers == null) {
                return new HeaderEntry[0];
            }

           List<HeaderEntry> headerList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : responseAssertion.response.headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    headerList.add(new HeaderEntry(entry.getKey(), entry.getValue().get(0)));
                }
            }
            return headerList.toArray(new HeaderEntry[0]);
        }
    }

    /**
     * Header 条目类，用于表示单个响应头
     */
    public static class HeaderEntry {
        public String key;
        public String value;

        public HeaderEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }
    }
}