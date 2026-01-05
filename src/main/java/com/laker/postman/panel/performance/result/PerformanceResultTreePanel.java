// 类：PerformanceResultTreePanel
// 包：com.laker.postman.panel.performance.result
package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 性能测试结果表（DevTools Network 风格）
 * ✔ Space：暂停 / 恢复刷新
 * ✔ selection 不影响刷新
 * ✔ EDT 16ms 合帧刷新
 *
 * 新增：
 * ✔ 列表字段加 “结果(成功/失败)”
 * ✔ 搜索区加复选框 “只看失败”
 * ✔ 两个过滤条件同时生效（接口名称 OR 成功失败结果文本匹配；勾选则只看失败）
 */
@Slf4j
public class PerformanceResultTreePanel extends JPanel {

    /* ======================= UI ======================= */

    private JTable table;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabs;

    private JTextField searchField;
    private JCheckBox onlyFailCheckBox;

    /* ======================= 数据 ======================= */

    private final Queue<ResultNodeInfo> pendingQueue = new ConcurrentLinkedQueue<>();

    /** 是否暂停刷新（Space 控制） */
    private volatile boolean paused = false;

    /** 当前选中行（仅用于详情显示） */
    private volatile ResultNodeInfo selected;

    private static final int MAX_ROWS = 200_000;

    /** 16ms UI 帧刷新 */
    private final Timer uiFrameTimer = new Timer(16, e -> {
        if (paused) return;

        flushQueueOnEDT();     // ✅ 将队列数据搬到 all（EDT 内）
        tableModel.flushIfDirty(); // ✅ 合帧刷新 UI
    });

    public PerformanceResultTreePanel() {
        initUI();
        registerListeners();
        uiFrameTimer.start();
    }

    /**
     * 把 pendingQueue 中的数据搬到 TableModel（必须在 EDT 调用）
     */
    private void flushQueueOnEDT() {
        List<ResultNodeInfo> batch = new ArrayList<>(1024);
        ResultNodeInfo info;

        while ((info = pendingQueue.poll()) != null) {
            batch.add(info);
            if (batch.size() >= 2000) break;
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);

            if (tableModel.getTotalSize() > MAX_ROWS) {
                tableModel.trimTo(MAX_ROWS); // ✅ 仅在 EDT 中 trim，避免并发异常1
            }
        }
    }

    /* ======================= UI ======================= */

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        // ===== 搜索区：输入框 + 复选框（只看失败）=====
        searchField = new SearchTextField();
        onlyFailCheckBox = new JCheckBox("只看失败", false);

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rightBox.add(onlyFailCheckBox);
        searchPanel.add(rightBox, BorderLayout.EAST);

        // ===== 表格 =====
        tableModel = new ResultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new ResultRowRenderer());

        // ===== 单元格：全部居左（你要求的：列表字段文字居左）=====
        DefaultTableCellRenderer cellLeft = new DefaultTableCellRenderer();
        cellLeft.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellLeft);
        }

        // ===== 表头：居左（你要求的：表头也居左）=====
        DefaultTableCellRenderer headerLeft = new DefaultTableCellRenderer();
        headerLeft.setHorizontalAlignment(SwingConstants.LEFT);
        table.getTableHeader().setDefaultRenderer(headerLeft);

        JScrollPane tableScroll = new JScrollPane(table);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        // ===== 详情 Tabs =====
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
        split.setDividerLocation(520); // 结果表更宽一点
        add(split, BorderLayout.CENTER);
    }

    /* ======================= 监听 ======================= */

    private void registerListeners() {

        // 行选择 → 仅渲染详情（不会影响刷新）
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

        // 双击 → 弹出响应
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        ResultNodeInfo info = tableModel.getRow(row);
                        JOptionPane.showMessageDialog(
                                PerformanceResultTreePanel.this,
                                HttpHtmlRenderer.renderResponse(info.resp),
                                info.name,
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            }
        });

        // Space → 暂停 / 恢复
        table.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("SPACE"), "togglePause");
        table.getActionMap().put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paused = !paused;
                if (!paused) {
                    // 恢复时立刻补一次刷新
                    tableModel.flushIfDirty();
                }
            }
        });

        // 搜索（回车触发）
        searchField.addActionListener(e -> applyFilters());

        // 复选框触发过滤
        onlyFailCheckBox.addActionListener(e -> applyFilters());
    }

    /**
     * 应用过滤条件：关键字 + “只看失败”
     */
    private void applyFilters() {
        tableModel.applyFilter(searchField.getText(), onlyFailCheckBox.isSelected());
    }

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int row = table.getSelectedRow();
        if (row < 0) {
            selected = null;
            clearDetailTabs();
            return;
        }

        selected = tableModel.getRow(row);
        renderDetail(selected);
    }

    /* ======================= 对外 API ======================= */

    public void addResult(ResultNodeInfo info, boolean efficientMode) {
        if (info == null) return;
        if (efficientMode && info.success) return;

        pendingQueue.offer(info);
    }

    public void clearResults() {
        pendingQueue.clear();
        selected = null;
        table.clearSelection();
        tableModel.clear();
        clearDetailTabs();
    }

    /* ======================= 详情 ======================= */

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

    /* ======================= TableModel ======================= */

    static class ResultTableModel extends AbstractTableModel {

        // 新增：结果列
        private static final int COL_NAME = 0;
        private static final int COL_COST = 1;
        private static final int COL_RESULT = 2;

        private final String[] columns = {"Name", "Cost (ms)" , "Result"};

        private final List<ResultNodeInfo> all = new ArrayList<>();
        private List<ResultNodeInfo> view = all;

        private boolean dirty = false;

        // 当前过滤条件（用于实时追加时重算 view）
        private String keyword = "";
        private boolean onlyFail = false;

        @Override public int getRowCount() { return view.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            ResultNodeInfo r = view.get(row);
            return switch (col) {
                case COL_NAME -> r.name;
                case COL_RESULT -> r.success ? "success" : "fail";
                case COL_COST -> r.costMs;
                default -> "";
            };
        }

        ResultNodeInfo getRow(int row) {
            return view.get(row);
        }

        int getTotalSize() {
            return all.size();
        }

        void append(List<ResultNodeInfo> batch) {
            all.addAll(batch);
            // 有过滤时，需要把 view 跟着更新，否则 view 不会包含新数据
            if (hasFilter()) {
                // 增量过滤：只过滤新来的 batch（避免全量扫描）1
                appendFiltered(batch);
            }
            dirty = true;
        }

        private void appendFiltered(List<ResultNodeInfo> batch) {
            String k = (keyword == null) ? "" : keyword.trim().toLowerCase();
            boolean failOnly = onlyFail;

            for (ResultNodeInfo r : batch) {
                if (passFailFilter(r, failOnly)) continue;
                if (matchKeyword(r, k)) continue;
                view.add(r);
            }
        }

        void flushIfDirty() {
            if (!dirty) return;
            dirty = false;
            fireTableDataChanged();
        }

        void clear() {
            all.clear();
            view = all;
            dirty = false;
            keyword = "";
            onlyFail = false;
            fireTableDataChanged();
        }

        void trimTo(int max) {
            int remove = all.size() - max;
            if (remove <= 0) return;

            // 从头逐个 remove：保证安全（只在 EDT 执行）
            for (int i = 0; i < remove; i++) {
                all.remove(0);
            }

            // trim 后如果有过滤，需要重建 view（因为引用对象还在，但位置变了）
            rebuildView();
            dirty = true;
        }

        void applyFilter(String keyword, boolean onlyFail) {
            this.keyword = keyword == null ? "" : keyword;
            this.onlyFail = onlyFail;
            rebuildView();
            fireTableDataChanged();
        }

        private boolean hasFilter() {
            return (keyword != null && !keyword.trim().isEmpty()) || onlyFail;
        }

        private void rebuildView() {
            if (!hasFilter()) {
                view = all;
                return;
            }

            String k = keyword.trim().toLowerCase();
            List<ResultNodeInfo> filtered = new ArrayList<>();

            for (ResultNodeInfo r : all) {
                if (passFailFilter(r, onlyFail)) continue;
                if (matchKeyword(r, k)) continue;
                filtered.add(r);
            }
            view = filtered;
        }

        /**
         * “只看失败”过滤
         */
        private boolean passFailFilter(ResultNodeInfo r, boolean onlyFail) {
            if (!onlyFail) return false;
            return r == null || r.success;
        }

        /**
         * 关键字匹配：接口名 OR 成功/失败文本
         * - 空关键字：直接通过
         * - 关键字包含 “成功/失败/success/fail/ok/error” 时也能匹配状态
         */
        private boolean matchKeyword(ResultNodeInfo r, String k) {
            if (k == null || k.isEmpty()) return false;
            if (r == null) return true;

            String name = r.name == null ? "" : r.name.toLowerCase();
            if (name.contains(k)) return false;

            boolean wantSuccess = containsAny(k, "成功", "success", "ok", "pass", "passed");
            boolean wantFail = containsAny(k, "失败", "fail", "error", "err", "failed");

            if (wantSuccess && r.success) return false;
            return !wantFail || r.success;
        }

        private boolean containsAny(String text, String... keys) {
            if (text == null) return false;
            for (String key : keys) {
                if (key != null && !key.isEmpty() && text.contains(key)) return true;
            }
            return false;
        }
    }

    /* ======================= Renderer ======================= */

    static class ResultRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ResultTableModel model = (ResultTableModel) table.getModel();
            ResultNodeInfo r = model.getRow(row);

            // 背景色（Network 风格：成功淡绿 / 失败淡红）
            if (!isSelected) {
                setBackground(
                        r.success
                                ? new Color(235, 255, 235)
                                : new Color(255, 235, 235)
                );
            }

            // 断言失败 tooltip
            setToolTipText(r.hasAssertionFailed() ? "Assertion Failed" : null);

            // 全部居左（你要的：列表字段居左）
            setHorizontalAlignment(SwingConstants.LEFT);

            return this;
        }
    }
}
