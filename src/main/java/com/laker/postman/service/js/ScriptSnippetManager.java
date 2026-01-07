package com.laker.postman.service.js;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
 * 脚本 API 提示管理器
 * 仅提供 API 自动补全提示，不包含代码片段
 * 代码片段由 SnippetDialog 统一管理
 */
@UtilityClass
public class ScriptSnippetManager {

    /**
     * 创建自动补全提供器
     */
    public static CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        // 添加 API 提示
        addApiCompletions(provider);

        return provider;
    }

    /**
     * 添加 API 补全提示
     */
    private static void addApiCompletions(DefaultCompletionProvider provider) {
        // ========== 核心对象 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM)));
        provider.addCompletion(new BasicCompletion(provider, "console",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONSOLE)));

        // ========== pm 对象核心方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.test",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_TEST)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT)));
        provider.addCompletion(new BasicCompletion(provider, "pm.uuid",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_UUID)));
        provider.addCompletion(new BasicCompletion(provider, "pm.generateUUID",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GENERATE_UUID)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getTimestamp",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_TIMESTAMP)));
        provider.addCompletion(new BasicCompletion(provider, "pm.setVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_SET_VARIABLE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_VARIABLE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.setGlobalVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_SET_GLOBAL_VARIABLE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getGlobalVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_GLOBAL_VARIABLE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getResponseCookie",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_RESPONSE_COOKIE)));

        // ========== pm.environment - 环境变量 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.environment",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENVIRONMENT)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.set",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_SET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_GET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.unset",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_UNSET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.clear",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_CLEAR)));

        // ========== pm.variables - 临时变量 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.variables",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VARIABLES)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.set",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_SET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_GET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.unset",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_UNSET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.clear",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_CLEAR)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.toObject",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_TO_OBJECT)));

        // ========== pm.request - 请求对象 (Pre-request) ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.request",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.url",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_URL)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.method",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_METHOD)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.headers",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.params",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_PARAMS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.formData",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_FORMDATA)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.urlencoded",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_URLENCODED)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.body",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_BODY)));

        // ========== pm.response - 响应对象 (Post-request) ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.response",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.code",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_CODE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.status",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.headers",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.text",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TEXT)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.json",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_JSON)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.responseTime",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TIME)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.size",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_SIZE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.to.have.status",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TO_HAVE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.to.have.header",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TO_HAVE_HEADER)));

        // ========== pm.cookies - Cookie 管理 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_GET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.getAll",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_GET_ALL)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.jar",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_JAR)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.toObject",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_TO_OBJECT)));

        // ========== pm.expect - 断言方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.expect().to.equal",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_EQUAL)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect().to.eql",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_EQL)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect().to.include",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_INCLUDE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect().to.have.property",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_HAVE_PROPERTY)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect().to.match",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_MATCH)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect().to.be.below",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_BELOW)));

        // ========== 内置库 - CryptoJS ==========
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.AES",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_AES)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.DES",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_DES)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.MD5",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_MD5)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.SHA1",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_SHA1)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.SHA256",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_SHA256)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.HmacSHA256",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_HMAC_SHA256)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.enc",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_ENC)));
        provider.addCompletion(new BasicCompletion(provider, "CryptoJS.lib.WordArray",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CRYPTOJS_WORD_ARRAY)));

        // ========== 内置库 - Lodash ==========
        provider.addCompletion(new BasicCompletion(provider, "_",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH)));
        provider.addCompletion(new BasicCompletion(provider, "_.map",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_MAP)));
        provider.addCompletion(new BasicCompletion(provider, "_.filter",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_FILTER)));
        provider.addCompletion(new BasicCompletion(provider, "_.find",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_FIND)));
        provider.addCompletion(new BasicCompletion(provider, "_.uniq",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_UNIQ)));
        provider.addCompletion(new BasicCompletion(provider, "_.pick",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_PICK)));
        provider.addCompletion(new BasicCompletion(provider, "_.omit",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_OMIT)));
        provider.addCompletion(new BasicCompletion(provider, "_.groupBy",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_GROUP_BY)));
        provider.addCompletion(new BasicCompletion(provider, "_.sortBy",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_SORT_BY)));
        provider.addCompletion(new BasicCompletion(provider, "_.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_GET)));
        provider.addCompletion(new BasicCompletion(provider, "_.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "_.sumBy",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_SUM_BY)));
        provider.addCompletion(new BasicCompletion(provider, "_.meanBy",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_MEAN_BY)));
        provider.addCompletion(new BasicCompletion(provider, "_.random",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_RANDOM)));
        provider.addCompletion(new BasicCompletion(provider, "_.sample",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_SAMPLE)));
        provider.addCompletion(new BasicCompletion(provider, "_.cloneDeep",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_CLONE_DEEP)));
        provider.addCompletion(new BasicCompletion(provider, "_.merge",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LODASH_MERGE)));

        // ========== 内置库 - Moment ==========
        provider.addCompletion(new BasicCompletion(provider, "moment",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT)));
        provider.addCompletion(new BasicCompletion(provider, "moment()",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_CALL)));
        provider.addCompletion(new BasicCompletion(provider, "moment().format",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_FORMAT)));
        provider.addCompletion(new BasicCompletion(provider, "moment().add",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_ADD)));
        provider.addCompletion(new BasicCompletion(provider, "moment().subtract",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_SUBTRACT)));
        provider.addCompletion(new BasicCompletion(provider, "moment().isBefore",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_IS_BEFORE)));
        provider.addCompletion(new BasicCompletion(provider, "moment().isAfter",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_IS_AFTER)));
        provider.addCompletion(new BasicCompletion(provider, "moment().isSame",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_IS_SAME)));
        provider.addCompletion(new BasicCompletion(provider, "moment().diff",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_DIFF)));
        provider.addCompletion(new BasicCompletion(provider, "moment().unix",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_UNIX)));
        provider.addCompletion(new BasicCompletion(provider, "moment().valueOf",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_VALUE_OF)));
        provider.addCompletion(new BasicCompletion(provider, "moment().toISOString",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_TO_ISO_STRING)));
        provider.addCompletion(new BasicCompletion(provider, "moment().startOf",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_START_OF)));
        provider.addCompletion(new BasicCompletion(provider, "moment().endOf",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MOMENT_END_OF)));

        // ========== JavaScript 内置对象 ==========
        provider.addCompletion(new BasicCompletion(provider, "JSON",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_JSON)));
        provider.addCompletion(new BasicCompletion(provider, "JSON.parse",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_JSON_PARSE)));
        provider.addCompletion(new BasicCompletion(provider, "JSON.stringify",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_JSON_STRINGIFY)));
        provider.addCompletion(new BasicCompletion(provider, "Date",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DATE)));
        provider.addCompletion(new BasicCompletion(provider, "Date.now",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DATE_NOW)));
        provider.addCompletion(new BasicCompletion(provider, "Math",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MATH)));
        provider.addCompletion(new BasicCompletion(provider, "Math.random",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MATH_RANDOM)));
        provider.addCompletion(new BasicCompletion(provider, "Math.floor",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MATH_FLOOR)));
        provider.addCompletion(new BasicCompletion(provider, "Math.ceil",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MATH_CEIL)));
        provider.addCompletion(new BasicCompletion(provider, "Math.round",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MATH_ROUND)));

        // ========== 编码/解码函数 ==========
        provider.addCompletion(new BasicCompletion(provider, "btoa",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_BTOA)));
        provider.addCompletion(new BasicCompletion(provider, "atob",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ATOB)));
        provider.addCompletion(new BasicCompletion(provider, "encodeURIComponent",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENCODE_URI)));
        provider.addCompletion(new BasicCompletion(provider, "decodeURIComponent",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DECODE_URI)));
        provider.addCompletion(new BasicCompletion(provider, "encodeURI",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENCODE_URI_FULL)));
        provider.addCompletion(new BasicCompletion(provider, "decodeURI",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DECODE_URI_FULL)));

        // ========== 控制台方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "console.log",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONSOLE_LOG)));
    }
}
