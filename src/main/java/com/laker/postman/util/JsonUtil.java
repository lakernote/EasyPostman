package com.laker.postman.util;

import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonFactory;
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
     */
    private static final DefaultIndenter DEFAULT_INDENTER = new DefaultIndenter("    ", "\n");
    private static final DefaultPrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter(
            Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))
            .withObjectIndenter(DEFAULT_INDENTER)
            .withArrayIndenter(DEFAULT_INDENTER);
    private static final JsonMapper mapper = new JsonMapper.Builder(new JsonFactory())
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
     * json 字符串美化
     *
     * @param json 待转换的 json 字符串
     * @return json
     */
    public static String toJsonPrettyStr(String json) {
        return mapper.readTree(json).toPrettyString();
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
        return mapper.rebuild()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                .build()
                .readTree(json)
                .toString();
    }

    /**
     * 格式化 JSON5（支持注释）
     * 注意：由于 Jackson 在解析后会丢失注释信息，此方法实际上会去除注释后格式化
     * 如果需要保留注释，建议在代码编辑器中使用语法高亮和缩进功能
     *
     * @param json5 待格式化的 JSON5 字符串（可以包含注释）
     * @return 格式化后的 JSON 字符串（注释会被去除）
     */
    public static String formatJson5(String json5) {
        if (json5 == null || json5.trim().isEmpty()) {
            return json5;
        }

        try {
            // 使用支持注释的 mapper 来解析
            JsonMapper commentSupportedMapper = JsonMapper.builder()
                    .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                    .build();

            // 解析并格式化（注释会丢失）
            JsonNode node = commentSupportedMapper.readTree(json5);
            return commentSupportedMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(node);
        } catch (Exception e) {
            // 如果解析失败，尝试用默认 mapper
            try {
                return toJsonPrettyStr(json5);
            } catch (Exception ex) {
                // 如果还是失败，返回原始字符串
                return json5;
            }
        }
    }

    /**
     * 判断字符串是否为有效的 JSON 或 JSON5（支持注释）
     *
     * @param json 待判断的字符串
     * @return 是否为有效的 JSON/JSON5
     */
    public static boolean isTypeJSON5(String json) {
        try {
            // 使用支持注释的 mapper 来解析
            JsonMapper commentSupportedMapper = JsonMapper.builder()
                    .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                    .build();

            JsonNode rootNode = commentSupportedMapper.readTree(json);
            // 严格校验：只有是JSON对象或JSON数组时才返回true
            return rootNode.isObject() || rootNode.isArray();
        } catch (JacksonException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 json
     *
     * @param json 待判断的字符串
     * @return 是否为 json
     */
    public static boolean isTypeJSON(String json) {
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
