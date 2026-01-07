package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.SearchTextField;
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
 * 性能测试结果表（DevTools Network 风格）
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

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        searchField = new SearchTextField();
        searchField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_SEARCH_PLACEHOLDER));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);

        tableModel = new ResultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);

        // 显示表格线
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));

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
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        detailTabs = new JTabbedPane();
        for (String key : new String[]{
                MessageKeys.PERFORMANCE_TAB_REQUEST,
                MessageKeys.PERFORMANCE_TAB_RESPONSE,
                MessageKeys.PERFORMANCE_TAB_TESTS,
                MessageKeys.PERFORMANCE_TAB_TIMING,
                MessageKeys.PERFORMANCE_TAB_EVENT_INFO
        }) {
            JEditorPane pane = new JEditorPane("text/html", "");
            pane.setEditable(false);
            detailTabs.addTab(I18nUtil.getMessage(key), new JScrollPane(pane));
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, detailTabs);
        split.setDividerLocation(0.3);
        add(split, BorderLayout.CENTER);
    }

    // 配置排序比较器
    private void configureSorterComparators() {
        // 列 0: Name - 字符串排序
        rowSorter.setComparator(0, Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER));

        // 列 1: Cost (ms) - 数值排序
        rowSorter.setComparator(1, Comparator.comparingLong(Long.class::cast));

        // 列 2: Result - 按成功/失败排序
        rowSorter.setComparator(2, Comparator.comparing(String::toString));
    }

    // 配置列宽度
    private void configureColumnWidths() {
        table.getColumnModel().getColumn(0).setPreferredWidth(300); // 接口名称
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // 耗时
        table.getColumnModel().getColumn(1).setMaxWidth(120);
        table.getColumnModel().getColumn(1).setMinWidth(80);

        table.getColumnModel().getColumn(2).setPreferredWidth(60);  // 结果（只有 Emoji）
        table.getColumnModel().getColumn(2).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setMinWidth(60);
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

    public void addResult(ResultNodeInfo info, boolean efficientMode) {
        if (info == null) return;
        if (efficientMode && info.success) return;

        pendingQueue.offer(info);
    }

    public void clearResults() {
        pendingQueue.clear();
        table.clearSelection();
        tableModel.clear();
        clearDetailTabs();
    }

    private void renderDetail(ResultNodeInfo info) {
        setTabHtml(0, HttpHtmlRenderer.renderRequest(info.req));
        setTabHtml(1, HttpHtmlRenderer.renderResponse(info.resp));
        setTabHtml(2,
                info.testResults == null || info.testResults.isEmpty()
                        ? I18nUtil.getMessage(MessageKeys.PERFORMANCE_NO_ASSERTION_RESULTS)
                        : HttpHtmlRenderer.renderTestResults(info.testResults)
        );
        setTabHtml(3, HttpHtmlRenderer.renderTimingInfo(info.resp));
        setTabHtml(4, HttpHtmlRenderer.renderEventInfo(info.resp));
    }

    private void setTabHtml(int idx, String html) {
        JScrollPane scroll = (JScrollPane) detailTabs.getComponentAt(idx);
        JEditorPane pane = (JEditorPane) scroll.getViewport().getView();
        pane.setText(html);
        pane.setCaretPosition(0);
    }

    private void clearDetailTabs() {
        for (int i = 0; i < detailTabs.getTabCount(); i++) {
            setTabHtml(i, "");
        }
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
        private static final int COL_COST = 1;
        private static final int COL_RESULT = 2;

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
            return 3;
        }

        @Override
        public String getColumnName(int col) {
            return switch (col) {
                case COL_NAME -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_NAME);
                case COL_COST -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_COST);
                case COL_RESULT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_COLUMN_RESULT);
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            ResultNodeInfo r = dataList.get(row);
            return switch (col) {
                case COL_NAME -> r.name;
                case COL_COST -> r.costMs;
                case COL_RESULT -> formatResult(r);
                default -> "";
            };
        }

        // 格式化结果列：✅ 成功 或 ❌ 失败
        private String formatResult(ResultNodeInfo r) {
            return r.isActuallySuccessful() ? "✅" : "❌";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_NAME -> String.class;
                case COL_COST -> Long.class;
                case COL_RESULT -> String.class;
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

    // 行渲染器 - 设置不同列的对齐方式
    static class ResultRowRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // 获取列索引
            int modelColumn = table.convertColumnIndexToModel(column);

            // 设置对齐方式
            switch (modelColumn) {
                case 0: // 接口名称 - 左对齐
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case 1: // 耗时 - 右对齐
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                case 2: // 结果 - 居中
                    setHorizontalAlignment(SwingConstants.CENTER);
                    break;
                default: // 其他列 - 左对齐
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
            }

            return this;
        }
    }
}
