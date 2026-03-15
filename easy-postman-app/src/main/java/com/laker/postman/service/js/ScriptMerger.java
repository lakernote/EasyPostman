package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本合并工具类
 * 用于合并来自不同层级的脚本（分组脚本、请求脚本等）
 */
@UtilityClass
public class ScriptMerger {

    private static final String SEPARATOR = "\n\n";
    private static final String SECTION_COMMENT_TEMPLATE = "// === %s ===\n\n";

    /**
     * 合并多个脚本片段
     *
     * @param scripts 脚本片段列表（按执行顺序）
     * @return 合并后的脚本
     */
    public static String merge(List<ScriptFragment> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return null;
        }

        // 过滤空脚本
        List<ScriptFragment> validScripts = scripts.stream()
                .filter(s -> s != null && s.getContent() != null && !s.getContent().trim().isEmpty())
                .toList();

        if (validScripts.isEmpty()) {
            return null;
        }

        if (validScripts.size() == 1) {
            return validScripts.get(0).getContent();
        }

        // 合并多个脚本
        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < validScripts.size(); i++) {
            ScriptFragment fragment = validScripts.get(i);

            // 添加注释标识（如果提供了来源名称）
            if (fragment.getSource() != null && !fragment.getSource().isEmpty()) {
                merged.append(String.format(SECTION_COMMENT_TEMPLATE, fragment.getSource()));
            }

            // 添加脚本内容
            merged.append(fragment.getContent());

            // 如果不是最后一个脚本，添加分隔符
            if (i < validScripts.size() - 1) {
                merged.append(SEPARATOR);
            }
        }

        return merged.toString();
    }

    /**
     * 合并前置脚本（外层到内层的顺序）
     *
     * @param outerScripts  外层脚本（分组层级，从外到内）
     * @param requestScript 请求自身的脚本
     * @return 合并后的前置脚本
     */
    public static String mergePreScripts(List<ScriptFragment> outerScripts, String requestScript) {
        List<ScriptFragment> allScripts = new ArrayList<>();

        // 先添加外层脚本（按从外到内的顺序）
        if (outerScripts != null) {
            allScripts.addAll(outerScripts);
        }

        // 最后添加请求脚本
        if (requestScript != null && !requestScript.trim().isEmpty()) {
            allScripts.add(new ScriptFragment("请求级脚本", requestScript));
        }

        return merge(allScripts);
    }

    /**
     * 合并后置脚本（内层到外层的顺序）
     *
     * @param requestScript 请求自身的脚本
     * @param outerScripts  外层脚本（分组层级，从内到外）
     * @return 合并后的后置脚本
     */
    public static String mergePostScripts(String requestScript, List<ScriptFragment> outerScripts) {
        List<ScriptFragment> allScripts = new ArrayList<>();

        // 先添加请求脚本
        if (requestScript != null && !requestScript.trim().isEmpty()) {
            allScripts.add(new ScriptFragment("请求级脚本", requestScript));
        }

        // 再添加外层脚本（按从内到外的顺序）
        if (outerScripts != null) {
            allScripts.addAll(outerScripts);
        }

        return merge(allScripts);
    }


}

