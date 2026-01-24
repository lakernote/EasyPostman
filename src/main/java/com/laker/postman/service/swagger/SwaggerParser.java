package com.laker.postman.service.swagger;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.service.common.CollectionParseResult;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Swagger/OpenAPI 格式解析器入口
 * 支持 Swagger 2.0、OpenAPI 3.0.x 和 OpenAPI 3.1.x 格式
 */
@Slf4j
@UtilityClass
public class SwaggerParser {

    /**
     * 解析 Swagger/OpenAPI JSON 文件，返回解析结果
     *
     * @param json Swagger/OpenAPI JSON 字符串
     * @return 解析结果，如果解析失败返回 null
     */
    public static CollectionParseResult parseSwagger(String json) {
        try {
            JSONObject root = JSONUtil.parseObj(json);

            // 检测版本
            String version = detectVersion(root);
            if (version == null) {
                log.error("无法识别的Swagger/OpenAPI格式");
                return null;
            }

            log.info("检测到 {} 格式", version);

            // 根据版本分发到对应的解析器
            if (version.startsWith("Swagger 2.0")) {
                return Swagger2Parser.parse(root);
            } else if (version.startsWith("OpenAPI 3.0") || version.startsWith("OpenAPI 3.1")) {
                return OpenApi3Parser.parse(root);
            }

            return null;
        } catch (Exception e) {
            log.error("解析Swagger文件失败", e);
            return null;
        }
    }

    /**
     * 检测 Swagger/OpenAPI 版本
     */
    private static String detectVersion(JSONObject root) {
        if (root.containsKey("swagger")) {
            String ver = root.getStr("swagger");
            if (ver.startsWith("2")) {
                return "Swagger 2.0";
            }
        } else if (root.containsKey("openapi")) {
            String ver = root.getStr("openapi");
            if (ver.startsWith("3.0")) {
                return "OpenAPI 3.0.x";
            } else if (ver.startsWith("3.1")) {
                return "OpenAPI 3.1.x";
            }
        }
        return null;
    }
}
