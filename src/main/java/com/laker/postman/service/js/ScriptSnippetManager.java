package com.laker.postman.service.js;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

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
        // 使用自定义 Provider 来实现自动激活和点号分隔的补全
        DefaultCompletionProvider provider = new DefaultCompletionProvider() {
            @Override
            public boolean isAutoActivateOkay(JTextComponent tc) {
                Document doc = tc.getDocument();
                int docLength = doc.getLength();

                if (docLength == 0) {
                    return false;
                }

                try {
                    // 检查文档中的最后一个字符（因为此方法可能在光标位置更新之前被调用）
                    char ch = doc.getText(docLength - 1, 1).charAt(0);
                    // 字母、数字、下划线、点号都触发自动补全
                    return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
                } catch (BadLocationException e) {
                    return false;
                }
            }

            @Override
            protected boolean isValidChar(char ch) {
                // 点号也是有效字符，这样 pm. 会被当作一个整体
                return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
            }

            @Override
            public String getAlreadyEnteredText(JTextComponent comp) {
                int caret = comp.getCaretPosition();
                if (caret == 0) {
                    return "";
                }

                try {
                    Document doc = comp.getDocument();
                    // 向前查找到非标识符字符（空格、换行、括号等）
                    int start = caret - 1;
                    while (start >= 0) {
                        char ch = doc.getText(start, 1).charAt(0);
                        if (!isValidChar(ch)) {
                            break;
                        }
                        start--;
                    }
                    start++; // 移到第一个有效字符位置
                    return doc.getText(start, caret - start);
                } catch (BadLocationException e) {
                    return "";
                }
            }
        };

        // 添加 API 提示
        addApiCompletions(provider);

        return provider;
    }

    /**
     * 添加 API 补全提示
     */
    private static void addApiCompletions(DefaultCompletionProvider provider) {
        // ========== JavaScript 关键字 ==========
        addJavaScriptKeywords(provider);

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
        // 基础断言 - 使用 ShorthandCompletion 提供完整代码模板
        provider.addCompletion(new ShorthandCompletion(provider, "expect.equal",
                "pm.expect(value).to.equal(expected)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_EQUAL)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.eql",
                "pm.expect(value).to.eql(expected)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_EQL)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.include",
                "pm.expect(text).to.include(\"substring\")",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_INCLUDE)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.property",
                "pm.expect(object).to.have.property(\"propertyName\")",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_HAVE_PROPERTY)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.match",
                "pm.expect(text).to.match(/regex/)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_MATCH)));

        // 数值比较断言
        provider.addCompletion(new ShorthandCompletion(provider, "expect.below",
                "pm.expect(value).to.be.below(max)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_BELOW)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.above",
                "pm.expect(value).to.be.above(min)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_ABOVE)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.least",
                "pm.expect(value).to.be.at.least(min)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_AT_LEAST)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.most",
                "pm.expect(value).to.be.at.most(max)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_AT_MOST)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.within",
                "pm.expect(value).to.be.within(min, max)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_WITHIN)));

        // 长度和类型断言
        provider.addCompletion(new ShorthandCompletion(provider, "expect.length",
                "pm.expect(array).to.have.length(expectedLength)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_HAVE_LENGTH)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.a",
                "pm.expect(value).to.be.a(\"string\")",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_A)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.an",
                "pm.expect(value).to.be.an(\"array\")",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_AN)));

        // 状态断言
        provider.addCompletion(new ShorthandCompletion(provider, "expect.ok",
                "pm.expect(value).to.be.ok",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_OK)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.exist",
                "pm.expect(value).to.exist",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_EXIST)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.empty",
                "pm.expect(value).to.be.empty",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_EMPTY)));

        // 布尔值和特殊值断言
        provider.addCompletion(new ShorthandCompletion(provider, "expect.true",
                "pm.expect(value).to.be.true",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_TRUE)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.false",
                "pm.expect(value).to.be.false",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_FALSE)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.null",
                "pm.expect(value).to.be.null",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_NULL)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.undefined",
                "pm.expect(value).to.be.undefined",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_UNDEFINED)));

        provider.addCompletion(new ShorthandCompletion(provider, "expect.NaN",
                "pm.expect(value).to.be.NaN",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_TO_BE_NAN)));

        // 否定断言
        provider.addCompletion(new ShorthandCompletion(provider, "expect.not",
                "pm.expect(value).to.not.equal(expected)",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT_NOT)));


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

    /**
     * 添加 JavaScript 关键字和常用代码模板
     */
    private static void addJavaScriptKeywords(DefaultCompletionProvider provider) {
        // ========== JavaScript 关键字 ==========
        provider.addCompletion(new BasicCompletion(provider, "function", "JavaScript function keyword"));
        provider.addCompletion(new BasicCompletion(provider, "var", "Variable declaration (ES5)"));
        provider.addCompletion(new BasicCompletion(provider, "let", "Block-scoped variable (ES6)"));
        provider.addCompletion(new BasicCompletion(provider, "const", "Block-scoped constant (ES6)"));
        provider.addCompletion(new BasicCompletion(provider, "if", "Conditional statement"));
        provider.addCompletion(new BasicCompletion(provider, "else", "Else clause"));
        provider.addCompletion(new BasicCompletion(provider, "for", "For loop"));
        provider.addCompletion(new BasicCompletion(provider, "while", "While loop"));
        provider.addCompletion(new BasicCompletion(provider, "do", "Do-while loop"));
        provider.addCompletion(new BasicCompletion(provider, "switch", "Switch statement"));
        provider.addCompletion(new BasicCompletion(provider, "case", "Case clause"));
        provider.addCompletion(new BasicCompletion(provider, "break", "Break statement"));
        provider.addCompletion(new BasicCompletion(provider, "continue", "Continue statement"));
        provider.addCompletion(new BasicCompletion(provider, "return", "Return statement"));
        provider.addCompletion(new BasicCompletion(provider, "try", "Try-catch block"));
        provider.addCompletion(new BasicCompletion(provider, "catch", "Catch clause"));
        provider.addCompletion(new BasicCompletion(provider, "finally", "Finally clause"));
        provider.addCompletion(new BasicCompletion(provider, "throw", "Throw exception"));
        provider.addCompletion(new BasicCompletion(provider, "new", "Create new object"));
        provider.addCompletion(new BasicCompletion(provider, "this", "Current context reference"));
        provider.addCompletion(new BasicCompletion(provider, "typeof", "Get type of variable"));
        provider.addCompletion(new BasicCompletion(provider, "instanceof", "Check instance type"));
        provider.addCompletion(new BasicCompletion(provider, "true", "Boolean true"));
        provider.addCompletion(new BasicCompletion(provider, "false", "Boolean false"));
        provider.addCompletion(new BasicCompletion(provider, "null", "Null value"));
        provider.addCompletion(new BasicCompletion(provider, "undefined", "Undefined value"));

        // ========== 常用代码模板 ==========
        provider.addCompletion(new ShorthandCompletion(provider, "func",
                "function ${name}(${params}) {\n    ${cursor}\n}",
                "Function declaration"));

        provider.addCompletion(new ShorthandCompletion(provider, "arrow",
                "(${params}) => {\n    ${cursor}\n}",
                "Arrow function (ES6)"));

        provider.addCompletion(new ShorthandCompletion(provider, "ifel",
                "if (${condition}) {\n    ${cursor}\n} else {\n    \n}",
                "If-else statement"));

        provider.addCompletion(new ShorthandCompletion(provider, "forin",
                "for (let ${key} in ${object}) {\n    ${cursor}\n}",
                "For-in loop"));

        provider.addCompletion(new ShorthandCompletion(provider, "forof",
                "for (let ${item} of ${array}) {\n    ${cursor}\n}",
                "For-of loop (ES6)"));

        provider.addCompletion(new ShorthandCompletion(provider, "trycatch",
                "try {\n    ${cursor}\n} catch (error) {\n    console.log(error);\n}",
                "Try-catch block"));

        // ========== 常用方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "forEach", "Array.prototype.forEach()"));
        provider.addCompletion(new BasicCompletion(provider, "map", "Array.prototype.map()"));
        provider.addCompletion(new BasicCompletion(provider, "filter", "Array.prototype.filter()"));
        provider.addCompletion(new BasicCompletion(provider, "reduce", "Array.prototype.reduce()"));
        provider.addCompletion(new BasicCompletion(provider, "find", "Array.prototype.find()"));
        provider.addCompletion(new BasicCompletion(provider, "some", "Array.prototype.some()"));
        provider.addCompletion(new BasicCompletion(provider, "every", "Array.prototype.every()"));
        provider.addCompletion(new BasicCompletion(provider, "includes", "Array/String.prototype.includes()"));
        provider.addCompletion(new BasicCompletion(provider, "indexOf", "Array/String.prototype.indexOf()"));
        provider.addCompletion(new BasicCompletion(provider, "join", "Array.prototype.join()"));
        provider.addCompletion(new BasicCompletion(provider, "split", "String.prototype.split()"));
        provider.addCompletion(new BasicCompletion(provider, "slice", "Array/String.prototype.slice()"));
        provider.addCompletion(new BasicCompletion(provider, "splice", "Array.prototype.splice()"));
        provider.addCompletion(new BasicCompletion(provider, "push", "Array.prototype.push()"));
        provider.addCompletion(new BasicCompletion(provider, "pop", "Array.prototype.pop()"));
        provider.addCompletion(new BasicCompletion(provider, "shift", "Array.prototype.shift()"));
        provider.addCompletion(new BasicCompletion(provider, "unshift", "Array.prototype.unshift()"));
        provider.addCompletion(new BasicCompletion(provider, "substring", "String.prototype.substring()"));
        provider.addCompletion(new BasicCompletion(provider, "substr", "String.prototype.substr()"));
        provider.addCompletion(new BasicCompletion(provider, "toLowerCase", "String.prototype.toLowerCase()"));
        provider.addCompletion(new BasicCompletion(provider, "toUpperCase", "String.prototype.toUpperCase()"));
        provider.addCompletion(new BasicCompletion(provider, "trim", "String.prototype.trim()"));
        provider.addCompletion(new BasicCompletion(provider, "replace", "String.prototype.replace()"));
        provider.addCompletion(new BasicCompletion(provider, "match", "String.prototype.match()"));
        provider.addCompletion(new BasicCompletion(provider, "search", "String.prototype.search()"));
        provider.addCompletion(new BasicCompletion(provider, "toString", "Object.prototype.toString()"));
        provider.addCompletion(new BasicCompletion(provider, "valueOf", "Object.prototype.valueOf()"));
        provider.addCompletion(new BasicCompletion(provider, "hasOwnProperty", "Object.prototype.hasOwnProperty()"));

        // ========== Object/Array 静态方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "Object.keys", "Get object keys"));
        provider.addCompletion(new BasicCompletion(provider, "Object.values", "Get object values"));
        provider.addCompletion(new BasicCompletion(provider, "Object.entries", "Get object entries"));
        provider.addCompletion(new BasicCompletion(provider, "Object.assign", "Merge objects"));
        provider.addCompletion(new BasicCompletion(provider, "Array.isArray", "Check if value is array"));
        provider.addCompletion(new BasicCompletion(provider, "Array.from", "Create array from iterable"));
    }
}
