package com.laker.postman.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class UnSavedNewRequestService {

    public static final String PATHNAME = SystemUtil.getUserHomeEasyPostmanPath() + "unsaved_new_requests.json";

    private UnSavedNewRequestService() {
        throw new IllegalStateException("Utility class");
    }

    public static List<HttpRequestItem> getAll() {
        File file = new File(PATHNAME);
        if (!file.exists()) {
            return List.of();
        }
        try {
            String json = FileUtil.readString(file, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return List.of();
            }
            JSONArray arr = JSONUtil.parseArray(json);
            return arr.toList(HttpRequestItem.class);
        } catch (Exception ex) {
            log.error("Failed to read unsaved new requests", ex);
            return List.of();
        }
    }

    public static void save(List<HttpRequestItem> newRequestItems) {
        // 保存所有 newRequestItems 到单独的 JSON 文件
        if (!newRequestItems.isEmpty()) {
            try {
                File file = new File(PATHNAME);
                JSONArray arr = new JSONArray();
                for (HttpRequestItem item : newRequestItems) {
                    arr.add(JSONUtil.parse(item));
                }
                FileUtil.writeUtf8String(arr.toStringPretty(), file);
                log.info("Saved {} new requests to {}", newRequestItems.size(), file.getAbsolutePath());
            } catch (Exception ex) {
                log.error("Failed to save new requests on exit", ex);
            }
        }
    }

    public static void clear() {
        File file = new File(PATHNAME);
        if (file.exists()) {
            try {
                FileUtil.del(file);
                log.info("Cleared unsaved new requests file at {}", file.getAbsolutePath());
            } catch (Exception ex) {
                log.error("Failed to delete unsaved new requests file", ex);
            }
        }
    }
}
