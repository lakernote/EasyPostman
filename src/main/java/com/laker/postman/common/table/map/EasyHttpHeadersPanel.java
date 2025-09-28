package com.laker.postman.common.table.map;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.table.EasyPostmanTextFieldCellRenderer;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.SystemUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Map;

/**
 * 高仿Postman的Headers面板
 * 1. 二列表格，第一列为Key，第二列为Value
 * 2. 默认请求头 User-Agent: EasyPostman/版本号 Accept: *, Accept-Encoding: gzip, deflate, br, Connection: keep-alive
 * 3. 左上角有Headers标签和eye图标按钮和(4)标签，点击可切换显示/隐藏 默认请求头
 * 4. 中间是表格
 */
public class EasyHttpHeadersPanel extends JPanel {
    private final EasyTablePanel tablePanel;
    // Default headers
    private static final String USER_AGENT = "User-Agent";
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CONNECTION = "Connection";
    private static final String USER_AGENT_VALUE = "EasyPostman/" + SystemUtil.getCurrentVersion();
    private static final String ACCEPT_VALUE = "*";
    private static final String ACCEPT_ENCODING_VALUE = "gzip, deflate, br";
    private static final String CONNECTION_VALUE = "keep-alive";
    private static final Object[][] DEFAULT_HEADERS = {
            {USER_AGENT, USER_AGENT_VALUE},
            {ACCEPT, ACCEPT_VALUE},
            {ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE},
            {CONNECTION, CONNECTION_VALUE}
    };
    private boolean showDefaultHeaders = false;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private final ImageIcon eyeOpenIcon = new FlatSVGIcon("icons/eye-open.svg", 16, 16);
    private final ImageIcon eyeCloseIcon = new FlatSVGIcon("icons/eye-close.svg", 16, 16);
    private JLabel countLabel; // Store as field for toggling

    public EasyHttpHeadersPanel() {
        setLayout(new BorderLayout());
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Headers");
        JButton eyeButton = new JButton(eyeOpenIcon); // Use SVG icon
        eyeButton.setFocusable(false);
        eyeButton.setBorderPainted(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.addActionListener(e -> {
            toggleDefaultHeadersVisibility();
            eyeButton.setIcon(showDefaultHeaders ? eyeCloseIcon : eyeOpenIcon);
        });
        countLabel = new JLabel("(4)");
        countLabel.setFont(label.getFont());
        countLabel.setForeground(new Color(0, 128, 255)); // 显示时高亮
        headerPanel.add(label);
        headerPanel.add(eyeButton);
        headerPanel.add(countLabel); // 始终显示
        tablePanel = new EasyTablePanel(new String[]{"Key", "Value"}, 24, true, true);
        tablePanel.setColumnEditor(0, new EasyPostmanTextFieldCellEditor());
        tablePanel.setColumnEditor(1, new EasyPostmanTextFieldCellEditor());
        tablePanel.setColumnRenderer(0, new EasyPostmanTextFieldCellRenderer());
        tablePanel.setColumnRenderer(1, new EasyPostmanTextFieldCellRenderer());
        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        addDefaultHeaders();
        rowSorter = new TableRowSorter<>(model);
        table.setRowSorter(rowSorter);
        // 默认隐藏默认请求头
        rowSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object keyObj = entry.getValue(0);
                return !isDefaultHeader(keyObj);
            }
        });
        add(headerPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(tablePanel.getTable());
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addDefaultHeaders() {
        for (Object[] header : DEFAULT_HEADERS) {
            tablePanel.addRow(header[0], header[1]);
        }
    }

    private void toggleDefaultHeadersVisibility() {
        countLabel.setVisible(showDefaultHeaders);
        if (showDefaultHeaders) {
            // 隐藏默认请求头
            rowSorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    Object keyObj = entry.getValue(0);
                    return !isDefaultHeader(keyObj);
                }
            });
        } else {
            // 显示所有请求头
            rowSorter.setRowFilter(null);
        }
        showDefaultHeaders = !showDefaultHeaders;
    }

    private boolean isDefaultHeader(Object keyObj) {
        if (keyObj == null) return false;
        String key = keyObj.toString();
        for (Object[] header : DEFAULT_HEADERS) {
            if (header[0].equals(key)) return true;
        }
        return false;
    }

    public void addTableModelListener(javax.swing.event.TableModelListener l) {
        tablePanel.addTableModelListener(l);
    }

    public void addRow(Object... values) {
        tablePanel.addRow(values);
    }

    public void scrollRectToVisible() {
        tablePanel.scrollRectToVisible();
    }

    public java.util.Map<String, String> getMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        java.util.List<java.util.Map<String, Object>> rows = tablePanel.getRows();
        for (java.util.Map<String, Object> row : rows) {
            Object keyObj = row.get("Key");
            Object valueObj = row.get("Value");
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    public void setHeadersMap(java.util.Map<String, String> map) {
        // 自动补充 User-Agent、Accept、Accept-Encoding、Connection
        boolean hasUserAgent = map != null && map.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(USER_AGENT));
        if (map != null && !hasUserAgent) {
            map.put(USER_AGENT, USER_AGENT_VALUE);
            tablePanel.addRow(USER_AGENT, USER_AGENT_VALUE);
        }
        boolean hasAccept = map != null && map.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(ACCEPT));
        if (map != null && !hasAccept) {
            map.put(ACCEPT, ACCEPT_VALUE);
            tablePanel.addRow(ACCEPT, ACCEPT_VALUE);
        }
        boolean hasAcceptEncoding = map != null && map.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(ACCEPT_ENCODING));
        if (map != null && !hasAcceptEncoding) {
            map.put(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
            tablePanel.addRow(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
        }
        boolean hasConnection = map != null && map.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(CONNECTION));
        if (map != null && !hasConnection) {
            map.put(CONNECTION, CONNECTION_VALUE);
            tablePanel.addRow(CONNECTION, CONNECTION_VALUE);
        }
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        if (map != null) {
            for (java.util.Map.Entry<String, String> entry : map.entrySet()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("Key", entry.getKey());
                row.put("Value", entry.getValue());
                rows.add(row);
            }
        }
        tablePanel.setRows(rows);
    }

    /**
     * 如果 headers 中不包含指定 Content-Type，则补充，并同步更新 headersPanel
     */
    public void ensureContentTypeHeader(Map<String, String> headers, String contentType) {
        if (!HttpUtil.containsContentType(headers, contentType)) {
            headers.put("Content-Type", contentType);
            tablePanel.addRow("Content-Type", contentType);
        }
    }
}