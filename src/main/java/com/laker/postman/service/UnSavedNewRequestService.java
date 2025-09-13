package com.laker.postman.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class UnSavedNewRequestService {

    private UnSavedNewRequestService() {
        throw new IllegalStateException("Utility class");
    }

    public static void save(List<HttpRequestItem> newRequestItems) {
        // 保存所有 newRequestItems 到单独的 JSON 文件
        if (!newRequestItems.isEmpty()) {
            try {
                File file = new File(SystemUtil.getUserHomeEasyPostmanPath() + "unsaved_new_requests.json");
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
}
