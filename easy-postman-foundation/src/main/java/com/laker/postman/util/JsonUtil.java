package com.laker.postman.util;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Json 工具类
 */
@UtilityClass
public class JsonUtil {
    /**
     * JsonMapper 自定义 json 美化格式，4 个空格缩进，属性名后不加空格，属性值前加空格
     * 支持解析带注释的 JSON（JSON5）
     */
    private static final DefaultIndenter DEFAULT_INDENTER = new DefaultIndenter("    ", "\n");
    private static final DefaultPrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter(
            Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))
            .withObjectIndenter(DEFAULT_INDENTER)
            .withArrayIndenter(DEFAULT_INDENTER);
    private static final JsonMapper mapper = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
            .defaultPrettyPrinter(DEFAULT_PRETTY_PRINTER)
            .build();

    /**
     * 创建 ObjectNode
     *
     * @return ObjectNode
     */
    public static ObjectNode createJsonNode() {
        return mapper.createObjectNode();
    }

    /**
     * 创建 ArrayNode
     *
     * @return ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return mapper.createArrayNode();
    }

    /**
     * 对象转json
     *
     * @param object 待转换的对象
     * @return json
     */
    public static String toJsonStr(Object object) {
        return mapper.writeValueAsString(object);
    }

    /**
     * 将普通文本转成可嵌入 JSON 字符串内容的转义文本，不包含外层双引号
     *
     * @param text 原始文本
     * @return 转义后的文本内容
     */
    public static String escapeJsonStringContent(String text) {
        if (text == null) {
            return null;
        }
        String json = mapper.writeValueAsString(text);
        return json.length() >= 2 ? json.substring(1, json.length() - 1) : json;
    }

    /**
     * 对 JSON 字符串内容做单层反转义，不要求包含外层双引号
     *
     * @param text 被转义的文本内容
     * @return 反转义后的文本
     */
    public static String unescapeJsonStringContent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != '\\' || i == text.length() - 1) {
                sb.append(ch);
                continue;
            }

            char next = text.charAt(++i);
            switch (next) {
                case '"':
                    sb.append('"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    if (i + 4 < text.length()) {
                        String hex = text.substring(i + 1, i + 5);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                            break;
                        } catch (NumberFormatException ignored) {
                            // 保留原样
                        }
                    }
                    sb.append('\\').append('u');
                    break;
                default:
                    sb.append('\\').append(next);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * json 字符串美化
     *
     * @param json 待转换的 json 字符串
     * @return json
     */
    public static String toJsonPrettyStr(String json) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json));
    }

    /**
     * json对象美化
     *
     * @param object 待转换的对象
     * @return json
     */
    public static String toJsonPrettyStr(Object object) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    /**
     * 清理 json 注释（去除注释后返回纯 JSON）
     *
     * @param json 待清理的 json
     * @return 清理后的 json
     */
    public static String cleanJsonComments(String json) {
        return mapper.readTree(json).toString();
    }


    /**
     * 判断字符串是否为有效的 JSON（支持带注释的 JSON5）
     *
     * @param json 待判断的字符串
     * @return 是否为有效的 JSON/JSON5
     */
    public static boolean isTypeJSON(String json) {
        if (CharSequenceUtil.isBlank(json)) return false;
        try {
            JsonNode rootNode = mapper.readTree(json);
            // 严格校验：只有是JSON对象或JSON数组时才返回true
            return rootNode.isObject() || rootNode.isArray();
        } catch (JacksonException e) {
            return false;
        }
    }

    /**
     * 读取 json
     *
     * @param json 待读取的 json
     * @return JsonNode
     */
    public static JsonNode readTree(String json) {
        return mapper.readTree(json);
    }

    /**
     * 深度拷贝
     *
     * @param object 待拷贝的对象
     * @param clazz  目标对象类型
     * @param <T>    目标对象类型
     * @return 拷贝后的对象
     */
    public static <T> T deepCopy(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        return mapper.readValue(mapper.writeValueAsBytes(object), clazz);
    }

    /**
     * 类型转换
     *
     * @param fromValue   来源对象
     * @param toValueType 转换的类型
     * @param <T>         泛型标记
     * @return 转换结果
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }

}
