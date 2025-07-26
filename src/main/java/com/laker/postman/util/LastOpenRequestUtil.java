package com.laker.postman.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LastOpenRequestUtil {
    private static final String LAST_OPEN_REQUEST_PATH = SystemUtil.getUserHomeEasyPostmanPath() + "last_open_request.json";

    public static void saveLastOpenRequestId(String requestId) {
        try {
            // 保存为JSON格式 {"requestId": "xxx"}
            String json = JSONUtil.toJsonPrettyStr(MapUtil.of("requestId", requestId));
            FileUtil.writeString(json, new File(LAST_OPEN_REQUEST_PATH), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("保存最后打开请求ID失败", e);
        }
    }

    public static String readLastOpenRequestId() {
        try {
            File file = new File(LAST_OPEN_REQUEST_PATH);
            if (!file.exists()) return null;
            String json = FileUtil.readString(file, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return null;
            return JSONUtil.parseObj(json).getStr("requestId");
        } catch (Exception e) {
            log.warn("读取最后打开请求ID失败", e);
            return null;
        }
    }
}