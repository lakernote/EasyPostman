package com.laker.postman.model.script;

import com.laker.postman.model.*;

import java.util.ArrayList;

/**
 * 脚本请求访问器 (pm.request)
 * <p>
 * 该类为 JavaScript 脚本提供对 HTTP 请求的访问接口，模拟 Postman 的 pm.request 对象。
 * 主要用于在前置脚本中读取或修改即将发送的请求。
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>访问和修改请求 URL、方法、头部、Body 等信息</li>
 *   <li>在前置脚本中动态修改请求参数</li>
 *   <li>支持操作 headers、formData、urlencoded 等集合</li>
 *   <li>提供 JavaScript 友好的访问接口</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在 PreRequest Script 中
 * console.log("Request URL:", pm.request.url);
 * console.log("Request Method:", pm.request.method);
 *
 * // 添加请求头
 * pm.request.headers.add({
 *     key: "Authorization",
 *     value: "Bearer " + pm.environment.get("token")
 * });
 *
 * // 修改请求体
 * pm.request.body = JSON.stringify({userId: 123});
 *
 * // 添加表单数据
 * pm.request.formData.add({
 *     key: "username",
 *     value: "john"
 * });
 *
 * // 添加 URL 查询参数
 * pm.request.params.add({
 *     key: "timestamp",
 *     value: Date.now()
 * });
 * }</pre>
 *
 * @author laker
 * @see PreparedRequest
 */
public class ScriptRequestAccessor {

    /**
     * 原始请求对象，包含所有请求信息
     */
    public final PreparedRequest raw;

    /**
     * 请求头集合包装器，支持 JavaScript 操作
     */
    public JsListWrapper<HttpHeader> headers;

    /**
     * 表单数据（multipart/form-data）集合包装器
     */
    public JsListWrapper<HttpFormData> formData;

    /**
     * URL 编码表单数据（application/x-www-form-urlencoded）集合包装器
     */
    public JsListWrapper<HttpFormUrlencoded> urlencoded;

    /**
     * URL 查询参数集合包装器
     */
    public JsListWrapper<HttpParam> params;

    /**
     * 请求唯一标识
     */
    public String id;

    /**
     * 请求 URL 对象（支持 pm.request.url.query.all() 访问）
     */
    public UrlWrapper url;

    /**
     * 请求 URL 字符串（用于向后兼容）
     */
    public String urlStr;

    /**
     * 请求方法 (GET, POST, PUT, DELETE 等)
     */
    public String method;

    /**
     * 请求体内容
     */
    public String body;

    /**
     * 是否为 multipart 请求
     */
    public boolean isMultipart;

    /**
     * 是否跟随重定向
     */
    public boolean followRedirects;

    /**
     * 是否记录事件日志
     */
    public boolean logEvent;

    /**
     * 构造脚本请求访问器
     * <p>
     * 会确保所有集合字段非空，并创建可在 JavaScript 中操作的包装器。
     * 前置脚本对这些包装器的修改会直接反映到原始请求对象中。
     * </p>
     *
     * @param req 准备好的请求对象
     */
    public ScriptRequestAccessor(PreparedRequest req) {
        this.raw = req;

        // 确保 List 不为 null，并关联到 PreparedRequest
        if (req.headersList == null) {
            req.headersList = new ArrayList<>();
        }
        if (req.formDataList == null) {
            req.formDataList = new ArrayList<>();
        }
        if (req.urlencodedList == null) {
            req.urlencodedList = new ArrayList<>();
        }
        if (req.paramsList == null) {
            req.paramsList = new ArrayList<>();
        }

        // 直接包装 PreparedRequest 中的 List，确保前置脚本修改能生效
        this.headers = new JsListWrapper<>(req.headersList, JsListWrapper.ListType.HEADER);
        this.formData = new JsListWrapper<>(req.formDataList, JsListWrapper.ListType.FORM_DATA);
        this.urlencoded = new JsListWrapper<>(req.urlencodedList, JsListWrapper.ListType.URLENCODED);
        this.params = new JsListWrapper<>(req.paramsList, JsListWrapper.ListType.PARAM);

        this.id = req.id;
        this.urlStr = req.url;
        this.url = new UrlWrapper(req.url, req.paramsList);
        this.method = req.method;
        this.body = req.body;
        this.isMultipart = req.isMultipart;
        this.followRedirects = req.followRedirects;
        this.logEvent = req.logEvent;
    }
}
