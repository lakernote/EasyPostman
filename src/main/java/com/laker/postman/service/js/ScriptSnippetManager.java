package com.laker.postman.service.js;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

/**
 * 脚本代码片段管理器
 * 管理自动补全的代码片段和提示信息
 */
@UtilityClass
public class ScriptSnippetManager {

    /**
     * 创建自动补全提供器
     */
    public static CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        // 添加基础对象补全
        addBasicCompletions(provider);

        // 添加代码片段补全
        addSnippetCompletions(provider);

        return provider;
    }

    /**
     * 添加基础对象补全
     */
    private static void addBasicCompletions(DefaultCompletionProvider provider) {
        // 核心对象
        provider.addCompletion(new BasicCompletion(provider, "pm",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM)));
        provider.addCompletion(new BasicCompletion(provider, "postman",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_POSTMAN)));
        provider.addCompletion(new BasicCompletion(provider, "request",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_REQUEST)));
        provider.addCompletion(new BasicCompletion(provider, "response",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE)));
        provider.addCompletion(new BasicCompletion(provider, "env",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENV)));

        // 响应相关
        provider.addCompletion(new BasicCompletion(provider, "responseBody",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE_BODY)));
        provider.addCompletion(new BasicCompletion(provider, "responseHeaders",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "status",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "statusCode",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_STATUS_CODE)));

        // 环境变量方法
        provider.addCompletion(new BasicCompletion(provider, "setEnvironmentVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SET_ENV)));
        provider.addCompletion(new BasicCompletion(provider, "getEnvironmentVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_GET_ENV)));

        // JavaScript关键字
        provider.addCompletion(new BasicCompletion(provider, "if",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_IF)));
        provider.addCompletion(new BasicCompletion(provider, "else",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ELSE)));
        provider.addCompletion(new BasicCompletion(provider, "for",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_FOR)));
        provider.addCompletion(new BasicCompletion(provider, "while",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_WHILE)));
        provider.addCompletion(new BasicCompletion(provider, "function",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_FUNCTION)));
        provider.addCompletion(new BasicCompletion(provider, "return",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RETURN)));
    }

    /**
     * 添加代码片段补全
     */
    private static void addSnippetCompletions(DefaultCompletionProvider provider) {
        // 环境变量操作
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.environment.set",
                "pm.environment.set('key', 'value');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_SET_ENV)));

        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.environment.get",
                "pm.environment.get('key');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_GET_ENV)));

        // 编码函数
        provider.addCompletion(new ShorthandCompletion(provider,
                "btoa",
                "btoa('String');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_BTOA)));

        provider.addCompletion(new ShorthandCompletion(provider,
                "atob",
                "atob('Base64');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ATOB)));

        provider.addCompletion(new ShorthandCompletion(provider,
                "encodeURIComponent",
                "encodeURIComponent('String');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ENCODE_URI)));

        provider.addCompletion(new ShorthandCompletion(provider,
                "decodeURIComponent",
                "decodeURIComponent('String');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_DECODE_URI)));

        // 控制台输出
        provider.addCompletion(new ShorthandCompletion(provider,
                "console.log",
                "console.log('内容');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_CONSOLE_LOG)));

        // JSON操作
        provider.addCompletion(new ShorthandCompletion(provider,
                "JSON.parse(responseBody)",
                "JSON.parse(responseBody);",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_PARSE)));

        provider.addCompletion(new ShorthandCompletion(provider,
                "JSON.stringify",
                "JSON.stringify(obj);",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_STRINGIFY)));
    }

}
