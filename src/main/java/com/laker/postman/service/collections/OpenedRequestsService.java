package com.laker.postman.service.collections;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenedRequestsService {

    public static final String PATHNAME = SystemUtil.getUserHomeEasyPostmanPath() + "opened_requests.json";

    private OpenedRequestsService() {
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

    public static void save() {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        List<Integer> unsavedTabs = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComp = tabbedPane.getTabComponentAt(i);
            if (tabComp instanceof ClosableTabComponent closable) {
                if (closable.dirty) {
                    unsavedTabs.add(i);
                }
            }
        }

        // 如果有未保存内容，弹出自定义对话框
        boolean saveAll = false;
        if (!unsavedTabs.isEmpty()) {
            String[] options = {
                    I18nUtil.getMessage(MessageKeys.EXIT_SAVE_ALL), // "全部保存"
                    I18nUtil.getMessage(MessageKeys.EXIT_DISCARD_ALL), // "全部不保存"
                    I18nUtil.getMessage(MessageKeys.EXIT_CANCEL) // "取消"
            };
            int result = JOptionPane.showOptionDialog(tabbedPane,
                    I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES),
                    I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES_TITLE),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (result == 2 || result == JOptionPane.CLOSED_OPTION) {
                // 用户取消，直接返回
                return;
            }
            if (result == 0) {
                // 全部保存
                for (Integer i : unsavedTabs) {
                    tabbedPane.setSelectedIndex(i);
                    editPanel.saveCurrentRequest();
                }
                saveAll = true;
            }
            // result == 1 全部不保存，直接退出
        }

        List<HttpRequestItem> openedRequestItem = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComp = tabbedPane.getTabComponentAt(i);
            Component comp = tabbedPane.getComponentAt(i);
            if (tabComp instanceof ClosableTabComponent closable && comp instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem item;
                if (closable.newRequest) {
                    item = subPanel.getCurrentRequest();
                    item.setNewRequest(true);
                } else if (closable.dirty && !saveAll) {
                    item = subPanel.getOriginalRequestItem();
                } else {
                    item = subPanel.getCurrentRequest();
                }
                openedRequestItem.add(item);
            }
        }

        // 只保存最新的多少个请求
        if (openedRequestItem.size() > 20) {
            openedRequestItem = new ArrayList<>(openedRequestItem.subList(openedRequestItem.size() - 20, openedRequestItem.size()));
        }

        // 保存所有到单独的 JSON 文件
        if (!openedRequestItem.isEmpty()) {
            try {
                File file = new File(PATHNAME);
                JSONArray arr = new JSONArray();
                for (HttpRequestItem item : openedRequestItem) {
                    arr.add(JSONUtil.parse(item));
                }
                FileUtil.writeUtf8String(arr.toStringPretty(), file);
                log.info("Saved {} new requests to {}", openedRequestItem.size(), file.getAbsolutePath());
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
