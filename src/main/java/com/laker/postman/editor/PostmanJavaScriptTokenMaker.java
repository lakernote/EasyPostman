package com.laker.postman.editor;

import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.JavaScriptTokenMaker;

import javax.swing.text.Segment;

/**
 * 扩展 JavaScript TokenMaker，为 Postman 特有的 API 添加语法高亮
 */
public class PostmanJavaScriptTokenMaker extends JavaScriptTokenMaker {

    /**
     * Postman 特有的关键字映射表
     * 这个映射表在 addToken 方法中使用，用于识别 Postman 特有的标识符
     */
    private final TokenMap postmanWordsToHighlight;

    public PostmanJavaScriptTokenMaker() {
        super();

        // 初始化 Postman 关键字映射
        postmanWordsToHighlight = new TokenMap();

        // ===== Postman 核心 API 对象 - 使用 RESERVED_WORD（蓝色/橙色 - 最重要）=====
        postmanWordsToHighlight.put("pm", TokenTypes.RESERVED_WORD);

        // ===== 内置库对象 - 使用 FUNCTION（黄色/橙色 - 次重要）=====
        postmanWordsToHighlight.put("console", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("CryptoJS", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("moment", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("cheerio", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("xml2Json", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("atob", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("btoa", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("_", TokenTypes.FUNCTION);  // Lodash
        postmanWordsToHighlight.put("require", TokenTypes.FUNCTION);

        // ===== Postman 核心方法 - 使用 FUNCTION =====
        postmanWordsToHighlight.put("test", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("expect", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("uuid", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("generateUUID", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getTimestamp", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("setVariable", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getVariable", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("setGlobalVariable", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getGlobalVariable", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("setEnvironmentVariable", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getEnvironmentVariable", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getResponseCookie", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("sendRequest", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("setNextRequest", TokenTypes.FUNCTION);

        // ===== Chai 断言链式关键字 - 使用 RESERVED_WORD_2（与关键字同级）=====
        postmanWordsToHighlight.put("to", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("be", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("been", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("is", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("that", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("which", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("and", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("has", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("have", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("with", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("at", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("of", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("same", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("not", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("deep", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("any", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("all", TokenTypes.RESERVED_WORD_2);

        // ===== Chai 断言方法 - 使用 FUNCTION =====
        postmanWordsToHighlight.put("equal", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("eql", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("above", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("least", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("below", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("most", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("within", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("instanceof", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("property", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("length", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("lengthOf", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("match", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("string", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("keys", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("include", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("includes", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("contain", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("contains", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("members", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("oneOf", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("change", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("increase", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("decrease", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("throw", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("respondTo", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("satisfy", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("closeTo", TokenTypes.FUNCTION);

        // ===== Chai 属性断言 - 使用 DATA_TYPE（类型相关）=====
        postmanWordsToHighlight.put("ok", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("true", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("false", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("null", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("undefined", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("NaN", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("exist", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("empty", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("arguments", TokenTypes.DATA_TYPE);

        // ===== Postman 常用属性/对象 - 使用 ANNOTATION（黄色/特殊颜色 - 属性标识）=====
        postmanWordsToHighlight.put("request", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("response", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("environment", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("globals", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("variables", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("cookies", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("iterationData", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("collectionVariables", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("info", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("execution", TokenTypes.ANNOTATION);

        // ===== 常用的响应/请求属性 - 使用 VARIABLE =====
        postmanWordsToHighlight.put("status", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("code", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("headers", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("body", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("text", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("json", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("responseTime", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("size", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("url", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("method", TokenTypes.VARIABLE);
    }

    /**
     * 重写 addToken 方法，在添加 token 前检查是否是 Postman 关键字
     * <p>
     * 这是 RSyntaxTextArea 推荐的正确实现方式：
     * 1. 在 token 被添加到列表之前进行类型转换
     * 2. 而不是在 token 创建之后尝试修改它
     *
     * @param segment     文本段
     * @param start       起始位置
     * @param end         结束位置
     * @param tokenType   token 类型
     * @param startOffset 起始偏移量
     */
    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        // 如果这个 token 被识别为普通标识符，检查它是否是 Postman 关键字
        if (tokenType == TokenTypes.IDENTIFIER) {
            // 使用 TokenMap.get() 方法查找是否在我们的关键字表中
            int newTokenType = postmanWordsToHighlight.get(segment, start, end);

            // 如果找到了（返回值不是 -1），则使用新的 token 类型
            if (newTokenType != -1) {
                tokenType = newTokenType;
            }
        }

        // 调用父类方法添加 token
        super.addToken(segment, start, end, tokenType, startOffset);
    }

    /**
     * 重写 addToken 的 char[] 版本
     * 有些 TokenMaker 实现会调用这个版本
     *
     * @param array       字符数组
     * @param start       起始位置
     * @param end         结束位置
     * @param tokenType   token 类型
     * @param startOffset 起始偏移量
     * @param hyperlink   是否是超链接
     */
    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset, boolean hyperlink) {
        // 如果这个 token 被识别为普通标识符，检查它是否是 Postman 关键字
        if (tokenType == TokenTypes.IDENTIFIER) {
            // 使用 TokenMap.get() 方法查找是否在我们的关键字表中
            int newTokenType = postmanWordsToHighlight.get(array, start, end);

            // 如果找到了（返回值不是 -1），则使用新的 token 类型
            if (newTokenType != -1) {
                tokenType = newTokenType;
            }
        }

        // 调用父类方法添加 token
        super.addToken(array, start, end, tokenType, startOffset, hyperlink);
    }
}
