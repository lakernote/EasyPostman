package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 性能测试结果表
 * - 200ms 增量刷新机制
 * - 支持排序和深度搜索过滤
 */
@Slf4j
public class PerformanceResultTablePanel extends JPanel {

    private JTable table;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabs;
    private TableRowSorter<ResultTableModel> rowSorter;

    private SearchTextField searchField;

    private final Queue<ResultNodeInfo> pendingQueue = new ConcurrentLinkedQueue<>();

    private static final int BATCH_SIZE = 2000;

    // 搜索防抖定时器（300ms）
    private Timer searchDebounceTimer;

    // UI 帧刷新定时器（200ms）
    private final Timer uiFrameTimer = new Timer(200, e -> {

        flushQueueOnEDT();
        tableModel.flushIfDirty();
    });

    public PerformanceResultTablePanel() {
        initUI();
        registerListeners();
        uiFrameTimer.start();
    }

    // 批量刷新队列数据到 TableModel
    private void flushQueueOnEDT() {
        List<ResultNodeInfo> batch = new ArrayList<>(1024);
        ResultNodeInfo info;

        while ((info = pendingQueue.poll()) != null) {
            batch.add(info);
            if (batch.size() >= BATCH_SIZE) break;
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);
        }
    }

    /**
     * 强制刷新所有待处理的结果（测试停止/完成时调用）
     * 确保 pendingQueue 中的所有数据都显示到表格中
     */
    public void flushPendingResults() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::flushPendingResults);
            return;
        }

        List<ResultNodeInfo> batch = new ArrayList<>();
        ResultNodeInfo info;

        // 一次性刷新所有待处理的结果
        while ((info = pendingQueue.poll()) != null) {
            batch.add(info);
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);
            log.debug("强制刷新了 {} 条待处理的结果到结果树", batch.size());
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        // 设置边距
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        searchField = new SearchTextField();
        searchField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_SEARCH_PLACEHOLDER));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);

        tableModel = new ResultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.setFocusable(false);
        // 设置自定义渲染器
        table.setDefaultRenderer(Object.class, new ResultRowRenderer());

        // 配置 TableRowSorter
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        configureSorterComparators();

        // 配置列宽
        configureColumnWidths();

        JScrollPane tableScroll = new JScrollPane(table);

        JPanel leftPanel = new JPanel(new BorderLayout());
        // 设置左侧面板边距
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        detailTabs = new JTabbedPane();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, detailTabs);
        split.setDividerLocation(400);
        add(split, BorderLayout.CENTER);
    }

    // 配置排序比较器
    private void configureSorterComparators() {
        // 列 0: Name - 字符串排序
        rowSorter.setComparator(0, Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER));

        // 列 1: Status - 数值排序（状态码）
        rowSorter.setComparator(1, Comparator.comparing((Object o) -> {
            String s = o.toString();
            if ("-".equals(s)) return 0;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }));

        // 列 2: Cost (ms) - 数值排序
        rowSorter.setComparator(2, Comparator.comparingInt(Integer.class::cast));

        // 列 3: Assertion - 按成功/失败排序
        rowSorter.setComparator(3, Comparator.comparing(String::toString));
    }

    // 配置列宽度
    private void configureColumnWidths() {
        // Name 列 - 接口名称，允许较宽显示
        table.getColumnModel().getColumn(0).setMinWidth(150);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);

        // Status 列 - 显示 "Status"（6个字符）+ 状态码（3位数）
        table.getColumnModel().getColumn(1).setMinWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setMaxWidth(100);

        // Cost (ms) 列 - 显示 "Cost (ms)"（9个字符）+ 时间值
        table.getColumnModel().getColumn(2).setMinWidth(95);
        table.getColumnModel().getColumn(2).setPreferredWidth(105);
        table.getColumnModel().getColumn(2).setMaxWidth(130);

        // Assertion 列 - 显示 "Assertion"（9个字符）+ emoji
        table.getColumnModel().getColumn(3).setMinWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setMaxWidth(120);
    }

    private void registerListeners() {
        // 选择监听器 - 显示详情
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);


        // 搜索防抖
        searchDebounceTimer = new Timer(300, e -> doFilter());
        searchDebounceTimer.setRepeats(false);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            private void scheduleFilter() {
                searchDebounceTimer.restart();
            }
        });

        // Enter 键立即过滤
        searchField.addActionListener(e -> {
            searchDebounceTimer.stop();
            doFilter();
        });
    }


    /**
     * 执行过滤
     */
    private void doFilter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(new ResultRowFilter(text.trim().toLowerCase()));
        }
    }

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int row = table.getSelectedRow();
        if (row < 0) {
            clearDetailTabs();
            return;
        }

        // 转换视图索引到模型索引
        int modelRow = table.convertRowIndexToModel(row);
        ResultNodeInfo info = tableModel.getRow(modelRow);
        renderDetail(info);
    }

    public void addResult(ResultNodeInfo info, boolean efficientMode, int slowRequestThresholdMs) {
        if (info == null) return;

        // 高效模式：只记录失败的请求或慢请求
        if (efficientMode) {
            // 如果请求失败，记录
            if (!info.isActuallySuccessful()) {
                pendingQueue.offer(info);
                return;
            }

            // 如果设置了慢请求阈值（>0），且耗时超过阈值，记录
            if (slowRequestThresholdMs > 0 && info.costMs >= slowRequestThresholdMs) {
                pendingQueue.offer(info);
                return;
            }

            // 其他成功且不慢的请求，不记录
            return;
        }

        // 非高效模式：记录所有请求
        pendingQueue.offer(info);
    }

    public void clearResults() {
        pendingQueue.clear();
        table.clearSelection();
        tableModel.clear();
        clearDetailTabs();
    }

    private void renderDetail(ResultNodeInfo info) {
        // 清除所有标签页
        detailTabs.removeAll();

        int responseTabIndex = -1;

        // 1. Request 标签页（始终显示）
        if (info.req != null) {
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REQUEST),
                    HttpHtmlRenderer.renderRequest(info.req)
            );
        }

        // 2. Response 标签页（始终显示，整合响应数据和错误信息）
        String responseHtml = HttpHtmlRenderer.renderResponseWithError(info);
        addTabWithContent(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESPONSE),
                responseHtml
        );
        responseTabIndex = detailTabs.getTabCount() - 1;

        // 3. Tests 标签页（仅在有断言结果时显示）
        if (info.testResults != null && !info.testResults.isEmpty()) {
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TESTS),
                    HttpHtmlRenderer.renderTestResults(info.testResults)
            );
        }

        // 4. Timing 标签页（仅在有事件信息时显示）
        // 5. Event Info 标签页（仅在有事件信息时显示）
        if (info.resp != null && info.resp.httpEventInfo != null) {
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TIMING),
                    HttpHtmlRenderer.renderTimingInfo(info.resp)
            );
            addTabWithContent(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_EVENT_INFO),
                    HttpHtmlRenderer.renderEventInfo(info.resp)
            );
        }

        // 如果有错误信息，自动切换到 Response 标签页
        if (info.errorMsg != null && !info.errorMsg.isEmpty() && responseTabIndex >= 0) {
            detailTabs.setSelectedIndex(responseTabIndex);
        }
    }

    /**
     * 添加标签页并设置内容
     */
    private void addTabWithContent(String title, String html) {
        JEditorPane pane = new JEditorPane("text/html", "");
        pane.setEditable(false);
        pane.setText(html);
        pane.setCaretPosition(0);
        detailTabs.addTab(title, new JScrollPane(pane));
    }

    private void clearDetailTabs() {
        detailTabs.removeAll();
    }

    // 资源清理
    public void dispose() {
        uiFrameTimer.stop();
        if (searchDebounceTimer != null) {
            searchDebounceTimer.stop();
        }
    }

    // 自定义 RowFilter - 支持深度搜索
    static class ResultRowFilter extends RowFilter<ResultTableModel, Integer> {
        private final String keyword;

        public ResultRowFilter(String keyword) {
            this.keyword = keyword;
        }

        @Override
        public boolean include(Entry<? extends ResultTableModel, ? extends Integer> entry) {
            ResultTableModel model = entry.getModel();
            int row = entry.getIdentifier();
            ResultNodeInfo info = model.getRow(row);

            if (info == null) return false;

            // 1. 检查接口名称
            if (info.name != null && info.name.toLowerCase().contains(keyword)) {
                return true;
            }

            // 2. 检查请求内容（URL、Headers、Body）
            if (matchesRequest(info)) {
                return true;
            }

            // 3. 检查响应内容（Headers、Body）
            return matchesResponse(info);
        }

        // 检查请求内容是否匹配关键字
        private boolean matchesRequest(ResultNodeInfo info) {
            if (info.req == null) return false;

            // 检查 URL
            if (info.req.url != null && info.req.url.toLowerCase().contains(keyword)) {
                return true;
            }

            // 检查请求 Headers
            if (info.req.headersList != null) {
                for (HttpHeader header : info.req.headersList) {
                    if (!header.isEnabled()) continue;
                    String headerStr = (header.getKey() + ": " + header.getValue()).toLowerCase();
                    if (headerStr.contains(keyword)) {
                        return true;
                    }
                }
            }

            // 检查请求 Body
            return info.req.body != null && info.req.body.toLowerCase().contains(keyword);
        }

        // 检查响应内容是否匹配关键字
        private boolean matchesResponse(ResultNodeInfo info) {
            if (info.resp == null) return false;

            // 检查响应 Headers
            if (info.resp.headers != null) {
                for (var entry : info.resp.headers.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    String headerStr = (key + ": " + String.join(", ", values)).toLowerCase();
                    if (headerStr.contains(keyword)) {
                        return true;
                    }
                }
            }

            // 检查响应 Body
            return info.resp.body != null && info.resp.body.toLowerCase().contains(keyword);
        }

    }

    // TableModel - 增量刷新优化
    static class ResultTableModel extends AbstractTableModel {

        private static final int COL_NAME = 0;
        private static final int COL_STATUS = 1;
        private static final int COL_COST = 2;
        private static final int COL_ASSERTION = 3;

        private final List<ResultNodeInfo> dataList = new ArrayList<>(1024);
        private boolean dirty = false;

        // 追踪新增行的起始位置，用于增量刷新
        private int firstNewRow = -1;

        @Override
        public int getRowCount() {
            return dataList.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_NAME -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_NAME);
                case COL_STATUS -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_STATUS);
                case COL_COST -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_COST);
                case COL_ASSERTION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_ASSERTION);
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            ResultNodeInfo r = dataList.get(row);
            return switch (col) {
                case COL_NAME -> r.name;
                case COL_STATUS -> r.responseCode > 0 ? String.valueOf(r.responseCode) : "-";
                case COL_COST -> r.costMs;
                case COL_ASSERTION -> formatAssertion(r);
                default -> "";
            };
        }

        // 格式化断言列：✅ 通过、❌ 失败、💨 无测试
        private String formatAssertion(ResultNodeInfo r) {
            // 1. 如果有断言结果
            if (r.testResults != null && !r.testResults.isEmpty()) {
                return r.hasAssertionFailed() ? "❌" : "✅";
            }

            // 2. 无断言测试
            return "💨";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_NAME -> String.class;
                case COL_STATUS -> String.class;
                case COL_COST -> Integer.class;
                case COL_ASSERTION -> String.class;
                default -> Object.class;
            };
        }

        ResultNodeInfo getRow(int row) {
            if (row < 0 || row >= dataList.size()) {
                return null;
            }
            return dataList.get(row);
        }


        void append(List<ResultNodeInfo> batch) {
            if (batch.isEmpty()) return;

            // 记录新增行的起始位置
            if (firstNewRow == -1) {
                firstNewRow = dataList.size();
            }

            dataList.addAll(batch);
            dirty = true;
        }

        void flushIfDirty() {
            if (!dirty) return;
            dirty = false;

            // 增量刷新：仅通知新增的行
            if (firstNewRow != -1 && firstNewRow < dataList.size()) {
                int lastRow = dataList.size() - 1;
                fireTableRowsInserted(firstNewRow, lastRow);
                firstNewRow = -1; // 重置
            }
        }

        void clear() {
            dataList.clear();
            dirty = false;
            firstNewRow = -1; // 重置
            fireTableDataChanged();
        }
    }

    // 行渲染器 - 设置不同列的对齐方式和颜色
    static class ResultRowRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // 获取列索引
            int modelColumn = table.convertColumnIndexToModel(column);

            // 重置前景色为默认值（避免颜色污染其他列）
            if (!isSelected) {
                setForeground(table.getForeground());
            }

            // 设置对齐方式和颜色
            switch (modelColumn) {
                case 0: // 接口名称 - 左对齐
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case 1: // 状态码 - 居中对齐，带颜色
                    setHorizontalAlignment(SwingConstants.CENTER);
                    if (!isSelected && value != null && !"-".equals(value)) {
                        applyStatusColors(this, value.toString());
                    }
                    break;
                case 2: // 耗时 - 右对齐
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                case 3: // 断言 - 居中
                    setHorizontalAlignment(SwingConstants.CENTER);
                    break;
                default: // 其他列 - 左对齐
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
            }

            return this;
        }

        /**
         * 根据状态码应用颜色 - 参考 FunctionalRunnerTableModel
         */
        private void applyStatusColors(Component c, String status) {
            Color foreground = ModernColors.getTextPrimary();

            try {
                int code = Integer.parseInt(status);
                if (code >= 200 && code < 300) {
                    // 成功：使用绿色
                    foreground = ModernColors.SUCCESS_DARK;
                } else if (code >= 400 && code < 500) {
                    // 客户端错误：使用警告色
                    foreground = ModernColors.WARNING_DARKER;
                } else if (code >= 500) {
                    // 服务器错误：使用错误色
                    foreground = ModernColors.ERROR_DARKER;
                }
            } catch (NumberFormatException e) {
                // 非数字状态（如错误消息）
                foreground = ModernColors.ERROR_DARK;
            }

            // 只设置文字颜色
            c.setForeground(foreground);
        }
    }
}
