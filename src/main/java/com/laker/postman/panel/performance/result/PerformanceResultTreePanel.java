package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.SearchTextField;
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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 性能测试结果表（DevTools Network 风格）
 * - Space 键暂停/恢复刷新
 * - 16ms 帧刷新机制
 * - 支持表头点击排序和搜索过滤
 */
@Slf4j
public class PerformanceResultTreePanel extends JPanel {

    private JTable table;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabs;
    private TableRowSorter<ResultTableModel> rowSorter;

    private JTextField searchField;
    private JLabel pauseStatusLabel;

    private final Queue<ResultNodeInfo> pendingQueue = new ConcurrentLinkedQueue<>();

    /**
     * 是否暂停刷新（Space 控制）
     */
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private static final int MAX_ROWS = 200_000;
    private static final int BATCH_SIZE = 2000;

    /**
     * 搜索防抖定时器 - 延迟 300ms 执行
     */
    private Timer searchDebounceTimer;

    /**
     * 16ms UI 帧刷新
     */
    private final Timer uiFrameTimer = new Timer(16, e -> {
        if (paused.get()) return;

        flushQueueOnEDT();
        tableModel.flushIfDirty();
    });

    public PerformanceResultTreePanel() {
        initUI();
        registerListeners();
        uiFrameTimer.start();
    }

    /**
     * 将队列中的数据批量添加到 TableModel
     */
    private void flushQueueOnEDT() {
        List<ResultNodeInfo> batch = new ArrayList<>(1024);
        ResultNodeInfo info;

        while ((info = pendingQueue.poll()) != null) {
            batch.add(info);
            if (batch.size() >= BATCH_SIZE) break;
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);

            if (tableModel.getTotalSize() > MAX_ROWS) {
                tableModel.trimTo(MAX_ROWS);
            }
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        searchField = new SearchTextField();
        searchField.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_SEARCH_PLACEHOLDER));

        pauseStatusLabel = new JLabel();
        pauseStatusLabel.setForeground(new Color(255, 140, 0)); // Orange color
        pauseStatusLabel.setVisible(false);

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(pauseStatusLabel, BorderLayout.EAST);

        tableModel = new ResultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);

        // ✅ 显示表格线
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230)); // 浅灰色网格线

        // 设置自定义渲染器
        table.setDefaultRenderer(Object.class, new ResultRowRenderer());

        // 创建并配置 TableRowSorter
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        configureSorterComparators();

        // ✅ 优化列宽
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

    /**
     * 配置排序比较器
     */
    private void configureSorterComparators() {
        // 列 0: Name - 字符串排序
        rowSorter.setComparator(0, Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER));

        // 列 1: Cost (ms) - 数值排序
        rowSorter.setComparator(1, Comparator.comparingLong(Long.class::cast));

        // 列 2: Result - 按成功/失败排序
        rowSorter.setComparator(2, Comparator.comparing(String::toString));
    }

    /**
     * 配置列宽度
     * - 接口名称：自动填充剩余空间
     * - 耗时：固定 100px
     * - 结果：固定 60px（只显示 Emoji）
     */
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

        // 空格键暂停/恢复
        table.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("SPACE"), "togglePause");
        table.getActionMap().put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isPaused = !paused.getAndSet(!paused.get());
                updatePauseStatus(isPaused);
                if (!isPaused) {
                    tableModel.flushIfDirty();
                }
            }
        });

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
     * 更新暂停状态显示
     */
    private void updatePauseStatus(boolean isPaused) {
        pauseStatusLabel.setVisible(isPaused);
        if (isPaused) {
            pauseStatusLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_PAUSED));
        }
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

    /**
     * 资源清理
     */
    public void dispose() {
        uiFrameTimer.stop();
        if (searchDebounceTimer != null) {
            searchDebounceTimer.stop();
        }
    }

    /**
     * 自定义 RowFilter - 支持名称和状态过滤
     */
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

            // 2. 检查状态关键字
            return matchesStatusKeyword(info);
        }

        private boolean matchesStatusKeyword(ResultNodeInfo info) {
            // 成功关键字
            if (containsAny(keyword, "成功", "success", "ok", "pass")) {
                return info.isActuallySuccessful();
            }
            // 失败关键字
            return containsAny(keyword, "失败", "fail", "error", "err") && !info.isActuallySuccessful();
        }

        private boolean containsAny(String text, String... keys) {
            for (String key : keys) {
                if (text.contains(key)) return true;
            }
            return false;
        }
    }

    /**
     * 简化的 TableModel
     */
    static class ResultTableModel extends AbstractTableModel {

        private static final int COL_NAME = 0;
        private static final int COL_COST = 1;
        private static final int COL_RESULT = 2;

        private final List<ResultNodeInfo> dataList = new ArrayList<>();
        private boolean dirty = false;

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

        /**
         * 格式化结果列：只显示 Emoji
         * ✅ 成功 或 ❌ 失败
         */
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

        int getTotalSize() {
            return dataList.size();
        }

        void append(List<ResultNodeInfo> batch) {
            dataList.addAll(batch);
            dirty = true;
        }

        void flushIfDirty() {
            if (!dirty) return;
            dirty = false;
            fireTableDataChanged();
        }

        void clear() {
            dataList.clear();
            dirty = false;
            fireTableDataChanged();
        }

        void trimTo(int max) {
            int remove = dataList.size() - max;
            if (remove <= 0) return;

            dataList.subList(0, remove).clear();
            dirty = true;
        }
    }

    /**
     * 优化的行渲染器 - 根据列设置不同的对齐方式
     */
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

            // 从 TableModel 获取数据，设置工具提示
            if (table.getModel() instanceof ResultTableModel model) {
                int modelRow = table.convertRowIndexToModel(row);
                ResultNodeInfo info = model.getRow(modelRow);

                if (info != null) {
                    // 设置工具提示
                    String tooltip = buildTooltip(info, modelColumn);
                    setToolTipText(tooltip);
                }
            }

            return this;
        }

        /**
         * 构建工具提示
         */
        private String buildTooltip(ResultNodeInfo info, int column) {
            return switch (column) {
                case 0 -> info.name; // 接口名称
                case 1 -> info.costMs + " ms"; // 耗时
                case 2 -> { // 结果列
                    if (info.hasAssertionFailed()) {
                        yield I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_ASSERTION_FAILED);
                    } else if (info.isActuallySuccessful()) {
                        yield I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_SUCCESS);
                    } else {
                        yield I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TREE_FAIL);
                    }
                }
                default -> null;
            };
        }
    }
}

