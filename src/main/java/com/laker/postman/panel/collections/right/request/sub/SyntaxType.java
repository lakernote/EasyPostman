package com.laker.postman.panel.collections.right.request.sub;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 * 语法类型枚举，统一管理语法高亮类型
 * 添加新的语法类型只需在此枚举中添加一项即可
 */
public enum SyntaxType {
    AUTO_DETECT("Auto Detect", null),
    JSON("JSON", SyntaxConstants.SYNTAX_STYLE_JSON),
    XML("XML", SyntaxConstants.SYNTAX_STYLE_XML),
    HTML("HTML", SyntaxConstants.SYNTAX_STYLE_HTML),
    JAVASCRIPT("JavaScript", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
    CSS("CSS", SyntaxConstants.SYNTAX_STYLE_CSS),
    PLAIN_TEXT("Plain Text", SyntaxConstants.SYNTAX_STYLE_NONE);

    private final String displayName;
    private final String syntaxStyle;

    SyntaxType(String displayName, String syntaxStyle) {
        this.displayName = displayName;
        this.syntaxStyle = syntaxStyle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSyntaxStyle() {
        return syntaxStyle;
    }

    /**
     * 获取所有显示名称数组（用于下拉框）
     */
    public static String[] getDisplayNames() {
        SyntaxType[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }

    /**
     * 根据索引获取语法类型
     */
    public static SyntaxType getByIndex(int index) {
        SyntaxType[] values = values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return AUTO_DETECT;
    }

    /**
     * 根据语法样式获取语法类型
     */
    public static SyntaxType getBySyntaxStyle(String syntaxStyle) {
        if (syntaxStyle == null) {
            return AUTO_DETECT;
        }
        for (SyntaxType type : values()) {
            if (syntaxStyle.equals(type.syntaxStyle)) {
                return type;
            }
        }
        return AUTO_DETECT;
    }

    /**
     * 获取索引位置
     */
    public int getIndex() {
        return ordinal();
    }
}

