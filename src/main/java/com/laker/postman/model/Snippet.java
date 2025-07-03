package com.laker.postman.model;

/**
 * 代码片段数据结构
 */
public class Snippet {
    public String title;
    public String code;
    public String desc;

    public Snippet(String title, String code, String desc) {
        this.title = title;
        this.code = code;
        this.desc = desc;
    }

    public String toString() {
        return title + (desc != null && !desc.isEmpty() ? (" - " + desc) : "");
    }
}