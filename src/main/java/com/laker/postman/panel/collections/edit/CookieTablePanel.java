package com.laker.postman.panel.collections.edit;

import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.HttpRequestExecutor;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * CookieTablePanel: 展示和编辑当前请求host下的cookie
 */
public class CookieTablePanel extends JPanel {
    private final EasyNameValueTablePanel cookieTablePanel;
    @Setter
    private JTextField urlField = null;

    private final Runnable cookieListener = this::loadCookies;

    public CookieTablePanel(JTextField urlField) {
        this();
        setUrlField(urlField);
        HttpRequestExecutor.registerCookieChangeListener(cookieListener);
    }

    public CookieTablePanel() {
        setLayout(new BorderLayout());
        cookieTablePanel = new EasyNameValueTablePanel();
        cookieTablePanel.setEditable(false);
        add(cookieTablePanel, BorderLayout.CENTER);
        HttpRequestExecutor.registerCookieChangeListener(cookieListener);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        HttpRequestExecutor.unregisterCookieChangeListener(cookieListener);
    }

    private void loadCookies() {
        String url = urlField != null ? urlField.getText() : null;
        String host = null;
        try {
            if (url != null && !url.isEmpty()) {
                url = EnvironmentService.replaceVariables(url);
                host = new URL(url).getHost();
            }
        } catch (Exception ignore) {
        }
        if (host == null) host = "";
        cookieTablePanel.clear();
        Map<String, Map<String, String>> all = HttpRequestExecutor.getAllCookies();
        Map<String, String> cookies = all.getOrDefault(host, new HashMap<>());
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            cookieTablePanel.addRow(entry.getKey(), entry.getValue());
        }
    }
}