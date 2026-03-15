package com.laker.postman.util;

import com.laker.postman.model.VariableSegment;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析工具类
 * <p>
 * 负责解析字符串中的变量占位符 {{variableName}}
 */
@UtilityClass
public class VariableParser {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    /**
     * 获取字符串中的变量段
     *
     * @param value 包含变量的字符串
     * @return 变量段列表
     */
    public static List<VariableSegment> getVariableSegments(String value) {
        List<VariableSegment> segments = new ArrayList<>();
        if (value == null) {
            return segments;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        while (matcher.find()) {
            segments.add(new VariableSegment(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return segments;
    }
}
