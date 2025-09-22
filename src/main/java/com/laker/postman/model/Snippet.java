package com.laker.postman.model;

import com.laker.postman.util.I18nUtil;

/**
 * 代码片段数据结构
 */
public class Snippet {
    public String title;
    public String code;
    public String desc;
    public SnippetType type;

    public Snippet(SnippetType type) {
        this.type = type;
        this.title = I18nUtil.getMessage(type.titleKey);
        this.desc = I18nUtil.getMessage(type.descKey);
        this.code = type.code;
    }

    public String toString() {
        return title + (desc != null && !desc.isEmpty() ? (" - " + desc) : "");
    }
}