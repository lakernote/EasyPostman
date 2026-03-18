package com.laker.postman.model;

import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.util.I18nUtil;

/**
 * 代码片段数据结构
 */
public class Snippet {
    public String title;
    public String code;
    public String desc;
    public Category category;

    public Snippet(SnippetType type) {
        this.category = type.type;
        this.title = I18nUtil.getMessage(type.titleKey);
        this.desc = I18nUtil.getMessage(type.descKey);
        this.code = type.code;
    }

    public Snippet(SnippetDefinition definition) {
        this.category = resolveCategory(definition.categoryId());
        this.title = definition.title();
        this.desc = definition.description();
        this.code = definition.code();
    }

    private Category resolveCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return Category.OTHER;
        }
        try {
            return Category.valueOf(categoryId);
        } catch (IllegalArgumentException ignore) {
            return Category.OTHER;
        }
    }

    public String toString() {
        return title + (desc != null && !desc.isEmpty() ? (" - " + desc) : "");
    }
}
