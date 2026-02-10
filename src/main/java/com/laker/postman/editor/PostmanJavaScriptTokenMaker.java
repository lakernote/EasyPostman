package com.laker.postman.editor;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.JavaScriptTokenMaker;

import javax.swing.text.Segment;

/**
 * 扩展 JavaScript TokenMaker，为 Postman 特有的 API 添加语法高亮
 */
public class PostmanJavaScriptTokenMaker extends JavaScriptTokenMaker {

    /**
     * Postman 特有的全局对象和关键字
     */
    private static final String[] POSTMAN_KEYWORDS = {
            "pm",
            "console",
            "CryptoJS",
            "moment",
            "_",  // Lodash
    };

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        // 先使用标准 JavaScript 解析
        Token tokenList = super.getTokenList(text, initialTokenType, startOffset);

        // 然后标记 Postman 特有的关键字
        Token current = tokenList;
        while (current != null && current.isPaintable()) {
            if (current.getType() == TokenTypes.IDENTIFIER) {
                String lexeme = current.getLexeme();
                if (isPostmanKeyword(lexeme)) {
                    // 将标识符标记为关键字类型
                    current.setType(TokenTypes.RESERVED_WORD);
                }
            }
            current = current.getNextToken();
        }

        return tokenList;
    }

    /**
     * 检查是否是 Postman 关键字
     */
    private boolean isPostmanKeyword(String word) {
        for (String keyword : POSTMAN_KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }
}
