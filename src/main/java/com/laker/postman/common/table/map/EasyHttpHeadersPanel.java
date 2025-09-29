package com.laker.postman.common.table.map;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.panel.collections.right.request.sub.EasyHttpHeadersTablePanel;
import com.laker.postman.util.SystemUtil;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 高仿Postman的Headers面板
 * 1. 二列表格，第一列为Key，第二列为Value
 * 2. 默认请求头 User-Agent: EasyPostman/版本号 Accept: *, Accept-Encoding: gzip, deflate, br, Connection: keep-alive
 * 3. 左上角有Headers标签和eye图标按钮和(4)标签，点击可切换显示/隐藏 默认请求头
 * 4. 中间是表格
 */
public class EasyHttpHeadersPanel extends JPanel {
    private final EasyHttpHeadersTablePanel tablePanel;
    // Default headers
    private static final String USER_AGENT = "User-Agent";
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CONNECTION = "Connection";
    private static final String USER_AGENT_VALUE = "EasyPostman/" + SystemUtil.getCurrentVersion();
    private static final String ACCEPT_VALUE = "*/*";
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
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        JLabel label = new JLabel("Headers");
        JButton eyeButton = new JButton(eyeOpenIcon); // Use SVG icon
        eyeButton.setFocusable(false);
        eyeButton.setBorderPainted(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 鼠标手型
        eyeButton.addActionListener(e -> {
            toggleDefaultHeadersVisibility();
            eyeButton.setIcon(showDefaultHeaders ? eyeCloseIcon : eyeOpenIcon);
        });
        int hiddenCount = DEFAULT_HEADERS.length;
        String countText = "(" + hiddenCount + ")";
        String countHtml = "<span style='color:#009900;font-weight:bold;'>" + countText + "</span>";
        countLabel = new JLabel("<html>" + countHtml + "</html>");
        countLabel.setVisible(true); // 初始就显示
        countLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 鼠标手型
        countLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleDefaultHeadersVisibility();
                eyeButton.setIcon(showDefaultHeaders ? eyeCloseIcon : eyeOpenIcon);
            }
        });
        headerPanel.add(label);
        headerPanel.add(eyeButton);
        headerPanel.add(countLabel); // 始终显示
        tablePanel = new EasyHttpHeadersTablePanel();
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
        JScrollPane scrollPane = new JScrollPane(tablePanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addDefaultHeaders() {
        for (Object[] header : DEFAULT_HEADERS) {
            tablePanel.addRow(header[0], header[1]);
        }
    }

    private void toggleDefaultHeadersVisibility() {
        showDefaultHeaders = !showDefaultHeaders;
        // 只在隐藏默认 header 时显示数量
        if (!showDefaultHeaders) {
            int hiddenCount = DEFAULT_HEADERS.length;
            String countText = "(" + hiddenCount + ")";
            String countHtml = "<span style='color:#009900;font-weight:bold;'>" + countText + "</span>";
            countLabel.setText("<html>" + countHtml + "</html>");
            countLabel.setVisible(true);
        } else {
            countLabel.setText(""); // 显示默认 header 时不显示数量
            countLabel.setVisible(false);
        }
        rowSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object keyObj = entry.getValue(0);
                return showDefaultHeaders || !isDefaultHeader(keyObj);
            }
        });
    }

    private boolean isDefaultHeader(Object keyObj) {
        if (keyObj == null) return false;
        String key = keyObj.toString();
        for (Object[] header : DEFAULT_HEADERS) {
            if (header[0].equals(key)) return true;
        }
        return false;
    }

    public void addTableModelListener(TableModelListener l) {
        tablePanel.addTableModelListener(l);
    }

    public void addRow(Object... values) {
        tablePanel.addRow(values);
    }

    public void scrollRectToVisible() {
        tablePanel.scrollRectToVisible();
    }

    public Map<String, String> getMap() {
        Map<String, String> map = new LinkedHashMap<>();
        java.util.List<Map<String, Object>> rows = tablePanel.getRows();
        for (Map<String, Object> row : rows) {
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

    public void setHeadersMap(Map<String, String> map) {
        tablePanel.clear();
        if (map == null) return;
        // 优化：先按默认头顺序补全和排序 map
        String[] defaultKeys = {USER_AGENT, ACCEPT, ACCEPT_ENCODING, CONNECTION};
        LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();
        for (String key : defaultKeys) {
            String value = map.containsKey(key) ? map.get(key) : getDefaultValue(key);
            sortedMap.put(key, value);
        }
        // 追加其他 header，跳过默认头
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            boolean isDefault = false;
            for (String defKey : defaultKeys) {
                if (defKey.equalsIgnoreCase(key)) {
                    isDefault = true;
                    break;
                }
            }
            if (isDefault) continue;
            sortedMap.put(key, entry.getValue());
        }
        // 同步 sortedMap 到 map
        map.clear();
        map.putAll(sortedMap);
        // 构造 rows
        java.util.List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Key", entry.getKey());
            row.put("Value", entry.getValue());
            rows.add(row);
        }
        tablePanel.setRows(rows);
    }

    private String getDefaultValue(String key) {
        if (USER_AGENT.equals(key)) return USER_AGENT_VALUE;
        if (ACCEPT.equals(key)) return ACCEPT_VALUE;
        if (ACCEPT_ENCODING.equals(key)) return ACCEPT_ENCODING_VALUE;
        if (CONNECTION.equals(key)) return CONNECTION_VALUE;
        return "";
    }

    public void setOrUpdateHeader(String key, String value) {
        boolean found = false;
        int rowCount = tablePanel.getTable().getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Object keyObj = tablePanel.getTable().getValueAt(i, 0);
            if (keyObj != null && keyObj.toString().equalsIgnoreCase(key)) {
                tablePanel.getTable().setValueAt(value, i, 1);
                found = true;
                break;
            }
        }
        if (!found) {
            tablePanel.addRow(key, value);
        }
    }

    public void removeHeader(String key) {
        JTable table = tablePanel.getTable();
        int rowCount = table.getRowCount();
        java.util.List<Integer> modelIndexesToRemove = new java.util.ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            Object keyObj = table.getValueAt(i, 0);
            if (keyObj != null && keyObj.toString().equalsIgnoreCase(key)) {
                int modelIndex = table.convertRowIndexToModel(i);
                modelIndexesToRemove.add(modelIndex);
            }
        }
        // 倒序删除，避免索引错乱
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        modelIndexesToRemove.sort(java.util.Collections.reverseOrder());
        for (int modelIndex : modelIndexesToRemove) {
            model.removeRow(modelIndex);
        }
    }
}
