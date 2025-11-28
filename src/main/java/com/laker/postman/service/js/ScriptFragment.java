package com.laker.postman.service.js;

import lombok.Getter;

/**
 * 脚本片段
 */
@Getter
public class ScriptFragment {
    private final String sourceName;
    private final String content;

    public ScriptFragment(String sourceName, String content) {
        this.sourceName = sourceName;
        this.content = content;
    }

    public ScriptFragment(String content) {
        this(null, content);
    }

    public static ScriptFragment of(String sourceName, String content) {
        return new ScriptFragment(sourceName, content);
    }

    public static ScriptFragment of(String content) {
        return new ScriptFragment(content);
    }
}