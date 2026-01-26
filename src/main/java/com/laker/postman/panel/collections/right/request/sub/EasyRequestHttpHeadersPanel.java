package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.SystemUtil;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * 高仿Postman的Headers面板
 * 1. 二列表格，第一列为Key，第二列为Value
 * 2. 默认请求头 User-Agent: EasyPostman/版本号 Accept: *, Accept-Encoding: gzip, deflate, br, Connection: keep-alive
 * 3. 左上角有Headers标签和eye图标按钮和(4)标签，点击可切换显示/隐藏 默认请求头
 * 4. 中间是表格
 */
public class EasyRequestHttpHeadersPanel extends JPanel {
    private EasyHttpHeadersTablePanel tablePanel;

    // Default headers constants
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

    private static final Set<String> DEFAULT_HEADER_KEYS = new HashSet<>();

    static {
        for (Object[] header : DEFAULT_HEADERS) {
            DEFAULT_HEADER_KEYS.add((String) header[0]);
        }
    }

    // UI components
    private final FlatSVGIcon eyeOpenIcon;
    private final FlatSVGIcon eyeCloseIcon;
    private JButton eyeButton;
    private JLabel countLabel;

    // Table filtering
    private TableRowSorter<DefaultTableModel> rowSorter;
    private DefaultHeaderRowFilter defaultHeaderFilter;
    private boolean showDefaultHeaders = false;

    public EasyRequestHttpHeadersPanel() {
        // 初始化图标（添加颜色过滤器以适配主题）
        eyeOpenIcon = IconUtil.createThemed("icons/eye-open.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);
        eyeCloseIcon = IconUtil.createThemed("icons/eye-close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);

        initializeComponents();
        setupLayout();
        initializeTableWithDefaults();
        setupFiltering();
    }


    /**
     * 创建眼睛按钮（用于切换默认请求头的显示/隐藏）
     *
     * @return 配置好的眼睛按钮
     */
    private JButton createEyeButton() {
        JButton button = new JButton(eyeOpenIcon);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> toggleDefaultHeadersVisibility());
        return button;
    }

    /**
     * 创建计数标签（显示隐藏的默认请求头数量）
     *
     * @return 配置好的计数标签
     */
    private JLabel createCountLabel() {
        JLabel label = new JLabel();
        // 设置初始文本（隐藏状态显示数量）
        int count = DEFAULT_HEADERS.length;
        String countText = "(" + count + ")";
        String countHtml = "<span style='color:#009900;font-weight:bold;'>" + countText + "</span>";
        label.setText("<html>" + countHtml + "</html>");

        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleDefaultHeadersVisibility();
            }
        });
        return label;
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // Create header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        JLabel label = new JLabel("Headers");

        // Create eye button and count label using helper methods
        eyeButton = createEyeButton();
        countLabel = createCountLabel();

        headerPanel.add(label);
        headerPanel.add(eyeButton);
        headerPanel.add(countLabel);

        // Create table panel and set parent reference
        tablePanel = new EasyHttpHeadersTablePanel();
        tablePanel.setParentPanel(this);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void setupLayout() {
        // tablePanel 已经内部包含了 JScrollPane，直接添加即可
        add(tablePanel, BorderLayout.CENTER);
    }

    private LinkedHashMap<String, String> initializeTableWithDefaults() {
        LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();
        // Add default headers to the table
        for (Object[] header : DEFAULT_HEADERS) {
            tablePanel.addRow(header[0], header[1]);
            sortedMap.put((String) header[0], (String) header[1]);
        }
        return sortedMap;
    }

    private void setupFiltering() {
        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Initialize row sorter and filter
        rowSorter = new TableRowSorter<>(model);
        defaultHeaderFilter = new DefaultHeaderRowFilter();

        table.setRowSorter(rowSorter);

        // Apply initial filter (hide default headers by default)
        applyCurrentFilter();
    }

    private void updateCountLabel() {
        if (!showDefaultHeaders) {
            int hiddenCount = DEFAULT_HEADERS.length;
            String countText = "(" + hiddenCount + ")";
            String countHtml = "<span style='color:#009900;font-weight:bold;'>" + countText + "</span>";
            countLabel.setText("<html>" + countHtml + "</html>");
            countLabel.setVisible(true);
        } else {
            countLabel.setText("");
            countLabel.setVisible(false);
        }
    }

    private void toggleDefaultHeadersVisibility() {
        showDefaultHeaders = !showDefaultHeaders;

        // Update UI components
        eyeButton.setIcon(showDefaultHeaders ? eyeCloseIcon : eyeOpenIcon);
        updateCountLabel();

        // Apply filter
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        if (rowSorter != null && defaultHeaderFilter != null) {
            defaultHeaderFilter.setShowDefaultHeaders(showDefaultHeaders);
            rowSorter.setRowFilter(defaultHeaderFilter);
        }
    }

    /**
     * Custom row filter for managing default headers visibility
     */
    private static class DefaultHeaderRowFilter extends RowFilter<DefaultTableModel, Integer> {
        private boolean showDefaultHeaders = false;

        public void setShowDefaultHeaders(boolean showDefaultHeaders) {
            this.showDefaultHeaders = showDefaultHeaders;
        }

        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
            try {
                Object keyObj = entry.getValue(1);
                if (keyObj == null) {
                    return true; // Show empty rows
                }

                String key = keyObj.toString().trim();
                if (key.isEmpty()) {
                    return true; // Show empty key rows
                }

                boolean isDefaultHeader = DEFAULT_HEADER_KEYS.contains(key);
                return showDefaultHeaders || !isDefaultHeader;
            } catch (Exception e) {
                // In case of any errors, show the row
                return true;
            }
        }
    }

    // Public API methods

    public void addTableModelListener(TableModelListener l) {
        if (tablePanel != null) {
            tablePanel.addTableModelListener(l);
        }
    }

    /**
     * Get all headers as a list (including enabled state) for persistence
     */
    public List<HttpHeader> getHeadersList() {
        List<HttpHeader> headersList = new ArrayList<>();

        if (tablePanel == null) {
            return headersList;
        }

        // Get all rows from the model (not view) to include both visible and hidden headers
        List<Map<String, Object>> allRows = tablePanel.getRows();

        for (Map<String, Object> row : allRows) {
            Object enabledObj = row.get("Enabled");
            Object keyObj = row.get("Key");
            Object valueObj = row.get("Value");

            boolean enabled = enabledObj == null || (Boolean) enabledObj;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();

            // Only add non-empty headers
            if (!key.isEmpty()) {
                headersList.add(new HttpHeader(enabled, key, value));
            }
        }

        return headersList;
    }

    /**
     * Set headers from list (including enabled state) for loading from persistence
     */
    public void setHeadersList(List<HttpHeader> headersList) {
        tablePanel.clear();

        if (headersList == null || headersList.isEmpty()) {
            // Re-add default headers
            initializeTableWithDefaults();
            return;
        }

        // Build sorted list with default headers first
        List<HttpHeader> sortedList = buildSortedHeadersList(headersList);

        // Set rows in table
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HttpHeader header : sortedList) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Enabled", header.isEnabled());
            row.put("Key", header.getKey());
            row.put("Value", header.getValue());
            rows.add(row);
        }

        tablePanel.setRows(rows);

        // Reapply filter after setting new data
        applyCurrentFilter();
    }

    /**
     * Build sorted headers list with default headers first
     */
    private List<HttpHeader> buildSortedHeadersList(List<HttpHeader> inputList) {
        List<HttpHeader> sortedList = new ArrayList<>();
        Map<String, HttpHeader> inputMap = new LinkedHashMap<>();

        // Convert list to map for easy lookup
        for (HttpHeader header : inputList) {
            inputMap.put(header.getKey(), header);
        }

        // Add default headers first (in order)
        for (Object[] defaultHeader : DEFAULT_HEADERS) {
            String key = (String) defaultHeader[0];
            HttpHeader header = findHeaderIgnoreCase(inputMap, key);

            if (header != null) {
                sortedList.add(header);
            } else {
                // Add default header with default value
                String defaultValue = (String) defaultHeader[1];
                sortedList.add(new HttpHeader(true, key, defaultValue));
            }
        }

        // Add non-default headers
        for (HttpHeader header : inputList) {
            if (!isDefaultHeader(header.getKey())) {
                sortedList.add(header);
            }
        }

        return sortedList;
    }

    /**
     * Find header ignoring case
     */
    private HttpHeader findHeaderIgnoreCase(Map<String, HttpHeader> map, String targetKey) {
        // First try exact match
        if (map.containsKey(targetKey)) {
            return map.get(targetKey);
        }

        // Then try case-insensitive match
        for (Map.Entry<String, HttpHeader> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(targetKey)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Check if header is a default header (case-insensitive)
     */
    private boolean isDefaultHeader(String key) {
        // First try exact match for performance
        if (DEFAULT_HEADER_KEYS.contains(key)) {
            return true;
        }

        // Then try case-insensitive match
        for (String defaultKey : DEFAULT_HEADER_KEYS) {
            if (defaultKey.equalsIgnoreCase(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Show default headers if they are currently hidden
     */
    public void ensureDefaultHeadersVisible() {
        if (!showDefaultHeaders) {
            toggleDefaultHeadersVisibility();
        }
    }

    /**
     * 聚焦到指定的 header 行并开始编辑 Value 列
     * 用于从自定义 header 行跳转到默认 header 行
     *
     * @param key header 的 key 名称（大小写不敏感）
     */
    public void focusOnHeader(String key) {
        if (tablePanel == null || key == null || key.trim().isEmpty()) {
            return;
        }

        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // 在模型中查找匹配的行
        int modelRow = findHeaderRowInModel(model, key);
        if (modelRow < 0) {
            return;
        }

        // 转换为视图行索引（考虑过滤器）
        int viewRow = table.convertRowIndexToView(modelRow);
        if (viewRow < 0) {
            return;
        }

        // 选中并滚动到该行
        table.setRowSelectionInterval(viewRow, viewRow);
        table.scrollRectToVisible(table.getCellRect(viewRow, 2, true));

        // 延迟启动编辑，确保UI已更新
        startEditingValueCell(table, viewRow);
    }

    /**
     * 在表格模型中查找指定 key 的行
     */
    private int findHeaderRowInModel(DefaultTableModel model, String key) {
        for (int row = 0; row < model.getRowCount(); row++) {
            Object keyObj = model.getValueAt(row, 1); // Column 1 is Key
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                return row;
            }
        }
        return -1;
    }

    /**
     * 启动 Value 单元格的编辑
     */
    private void startEditingValueCell(JTable table, int viewRow) {
        SwingUtilities.invokeLater(() -> {
            table.editCellAt(viewRow, 2); // Column 2 is Value
            Component editor = table.getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
            }
        });
    }

    /**
     * Remove a header by key name
     */
    public void removeHeader(String key) {
        if (tablePanel == null || key == null) {
            return;
        }

        key = key.trim();
        if (key.isEmpty()) {
            return;
        }

        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Find and remove rows with matching key (case-insensitive)
        // Note: Column index 1 is now the Key column (0 is Enabled checkbox)
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            Object keyObj = model.getValueAt(i, 1);
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                model.removeRow(i);
            }
        }

        // Reapply filter after removing data
        applyCurrentFilter();
    }

    /**
     * Set or update a header value. If the header exists, update its value; otherwise, add a new header
     */
    public void setOrUpdateHeader(String key, String value) {
        if (tablePanel == null || key == null) {
            return;
        }

        key = key.trim();
        value = value == null ? "" : value.trim();

        if (key.isEmpty()) {
            return;
        }

        JTable table = tablePanel.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // First, try to find existing header (case-insensitive)
        // Note: Column index 1 is now the Key column (0 is Enabled checkbox)
        boolean found = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object keyObj = model.getValueAt(i, 1);
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                // Update existing header value
                model.setValueAt(value, i, 2); // Column 2 is Value
                model.setValueAt(true, i, 0); // Ensure it's enabled
                found = true;
                break;
            }
        }

        // If not found, add new header
        if (!found) {
            // Check if this is a default header that should be added in the correct position
            if (DEFAULT_HEADER_KEYS.contains(key)) {
                addDefaultHeaderInOrder(key, value, model);
            } else {
                // Add as regular header using direct model manipulation to avoid triggering auto-append
                addNonDefaultHeader(key, value, model);
            }
        }

        // Reapply filter after updating data
        applyCurrentFilter();
    }

    /**
     * Add a default header in the correct position among other default headers
     */
    private void addDefaultHeaderInOrder(String key, String value, DefaultTableModel model) {
        // Find the correct position to insert the default header
        int insertPosition = -1;

        // Define the order of default headers
        String[] defaultOrder = {USER_AGENT, ACCEPT, ACCEPT_ENCODING, CONNECTION};
        int keyIndex = -1;
        for (int i = 0; i < defaultOrder.length; i++) {
            if (defaultOrder[i].equals(key)) {
                keyIndex = i;
                break;
            }
        }

        if (keyIndex >= 0) {
            // Find where to insert this header
            for (int row = 0; row < model.getRowCount(); row++) {
                Object rowKey = model.getValueAt(row, 1); // Column 1 is Key
                if (rowKey != null) {
                    String rowKeyStr = rowKey.toString().trim();

                    // Find the index of this row's key in default order
                    int rowKeyIndex = -1;
                    for (int i = 0; i < defaultOrder.length; i++) {
                        if (defaultOrder[i].equals(rowKeyStr)) {
                            rowKeyIndex = i;
                            break;
                        }
                    }

                    if (rowKeyIndex > keyIndex || rowKeyIndex == -1) {
                        // Insert before this row
                        insertPosition = row;
                        break;
                    }
                }
            }
        }

        // Insert at the determined position
        if (insertPosition >= 0) {
            model.insertRow(insertPosition, new Object[]{true, key, value, ""});
        } else {
            // Add at the end
            model.addRow(new Object[]{true, key, value, ""});
        }
    }

    /**
     * Add a non-default header intelligently to avoid creating extra blank rows
     */
    private void addNonDefaultHeader(String key, String value, DefaultTableModel model) {
        // Check if the last row is empty
        int rowCount = model.getRowCount();
        if (rowCount > 0) {
            int lastRow = rowCount - 1;
            Object lastKey = model.getValueAt(lastRow, 1); // Column 1 is Key
            Object lastValue = model.getValueAt(lastRow, 2); // Column 2 is Value

            boolean lastRowIsEmpty = (lastKey == null || lastKey.toString().trim().isEmpty()) &&
                    (lastValue == null || lastValue.toString().trim().isEmpty());

            if (lastRowIsEmpty) {
                // Replace the empty row with the new header
                model.setValueAt(true, lastRow, 0); // Enabled
                model.setValueAt(key, lastRow, 1); // Key
                model.setValueAt(value, lastRow, 2); // Value
                return;
            }
        }

        // If no empty row at the end, add new row directly
        model.addRow(new Object[]{true, key, value, ""});
    }
}
