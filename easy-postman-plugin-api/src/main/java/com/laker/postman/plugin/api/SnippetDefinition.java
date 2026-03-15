package com.laker.postman.plugin.api;

/**
 * 代码片段定义。
 */
public record SnippetDefinition(
        String categoryId,
        String title,
        String description,
        String code
) {
}
