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

        // Postman 核心 API 对象 - 使用 RESERVED_WORD（蓝色 - 最重要）
        postmanWordsToHighlight.put("pm", TokenTypes.RESERVED_WORD);

        // 内置库对象 - 使用 FUNCTION（黄色/橙色 - 次重要）
        postmanWordsToHighlight.put("console", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("CryptoJS", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("moment", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("_", TokenTypes.FUNCTION);  // Lodash
        postmanWordsToHighlight.put("require", TokenTypes.FUNCTION);

        // Postman 常用属性 - 使用 ANNOTATION（黄色/特殊颜色 - 属性标识）
        postmanWordsToHighlight.put("request", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("response", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("environment", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("globals", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("variables", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("cookies", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("iterationData", TokenTypes.ANNOTATION);
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
