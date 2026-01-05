package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 性能测试结果表（替代 ResultTree）
 *
 * 设计目标：
 * 1. 百万请求不卡
 * 2. EDT 16ms 合帧刷新
 * 3. selection 锁定
 * 4. 行虚拟化（JTable 原生支持）
 */
@Slf4j
public class PerformanceResultTreePanel extends JPanel {

    /* ======================= UI ======================= */

    private JTable table;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabs;
    private JTextField searchField;

    /* ======================= 数据 ======================= */

    private final Queue<ResultNodeInfo> pendingQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean flushScheduled = false;

    /** 当前锁定查看的行 */
    private volatile ResultNodeInfo lockedSelection;

    /** 最大保留行数（防止 OOM） */
    private static final int MAX_ROWS = 200_000;

    public PerformanceResultTreePanel() {
        initUI();
        registerListeners();
    }

    /* ======================= UI ======================= */

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 搜索
        searchField = new SearchTextField();

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);

        // 表格
        tableModel = new ResultTableModel();
        table = new JTable(tableModel);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // 居中渲染
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);

        JScrollPane tableScroll = new JScrollPane(table);

        JPanel left = new JPanel(new BorderLayout());
        left.add(searchPanel, BorderLayout.NORTH);
        left.add(tableScroll, BorderLayout.CENTER);

        // 详情 Tabs
        detailTabs = new JTabbedPane();
        for (String key : new String[]{
                MessageKeys.PERFORMANCE_TAB_REQUEST,
                MessageKeys.PERFORMANCE_TAB_RESPONSE,
                MessageKeys.PERFORMANCE_TAB_TESTS,
                MessageKeys.PERFORMANCE_TAB_TIMING,
                MessageKeys.PERFORMANCE_TAB_EVENT_INFO
        }) {
            JEditorPane pane = new JEditorPane();
            pane.setContentType("text/html");
            pane.setEditable(false);
            detailTabs.addTab(I18nUtil.getMessage(key), new JScrollPane(pane));
        }

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                left,
                detailTabs
        );
        split.setDividerLocation(420);

        add(split, BorderLayout.CENTER);
    }

    /* ======================= 监听 ======================= */

    private void registerListeners() {

        // 行选择 → 锁定详情
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;

            int row = table.getSelectedRow();
            if (row < 0) {
                lockedSelection = null;
                clearDetailTabs();
                return;
            }

            ResultNodeInfo info = tableModel.getRow(row);
            lockedSelection = info;
            renderDetail(info);
        });

        // 搜索
        searchField.addActionListener(e ->
                tableModel.filter(searchField.getText())
        );
    }

    /* ======================= 对外 API ======================= */

    public void addResult(ResultNodeInfo info, boolean efficientMode) {
        if (info == null) return;
        if (efficientMode && info.success) return;

        pendingQueue.offer(info);
        scheduleFlush();
    }

    public void flushNow() {
        flushScheduled = false;
        flushToTable();
    }

    public void clearResults() {
        pendingQueue.clear();
        lockedSelection = null;
        tableModel.clear();
        clearDetailTabs();
    }

    /* ======================= EDT 合帧刷新 ======================= */

    private void scheduleFlush() {
        if (flushScheduled) return;
        flushScheduled = true;
        SwingUtilities.invokeLater(this::flushToTable);
    }

    private void flushToTable() {
        flushScheduled = false;

        List<ResultNodeInfo> batch = new ArrayList<>(512);
        ResultNodeInfo info;

        while ((info = pendingQueue.poll()) != null) {
            batch.add(info);
            if (batch.size() >= 1000) break;
        }

        if (!batch.isEmpty()) {
            tableModel.append(batch);

            // 行数限制
            if (tableModel.getRowCount() > MAX_ROWS) {
                tableModel.trimTo(MAX_ROWS);
            }
        }

        if (!pendingQueue.isEmpty()) {
            scheduleFlush();
        }
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
        setTabHtml(3,
                info.resp != null && info.resp.httpEventInfo != null
                        ? HttpHtmlRenderer.renderTimingInfo(info.resp)
                        : I18nUtil.getMessage(MessageKeys.PERFORMANCE_NO_TIMING_INFO)
        );
        setTabHtml(4,
                info.resp != null && info.resp.httpEventInfo != null
                        ? HttpHtmlRenderer.renderEventInfo(info.resp)
                        : I18nUtil.getMessage(MessageKeys.PERFORMANCE_NO_EVENT_INFO)
        );
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

        private final String[] columns = {
                "Name",  "Cost(ms)"

        };


        private final List<ResultNodeInfo> all = new ArrayList<>();
        private List<ResultNodeInfo> view = all;

        @Override
        public int getRowCount() {
            return view.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        public ResultNodeInfo getRow(int row) {
            return view.get(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            ResultNodeInfo r = view.get(row);

            return switch (col) {
                case 0 -> r.name; // 接口名称
                case 1 -> r.resp != null ? r.resp.costMs : "";
                default -> "";
            };
        }


        void append(List<ResultNodeInfo> batch) {
            int start = all.size();
            all.addAll(batch);
            fireTableRowsInserted(start, all.size() - 1);
        }

        void clear() {
            all.clear();
            view = all;
            fireTableDataChanged();
        }

        void trimTo(int max) {
            int remove = all.size() - max;
            if (remove <= 0) return;
            all.subList(0, remove).clear();
            fireTableDataChanged();
        }

        void filter(String keyword) {
            if (keyword == null || keyword.isBlank()) {
                view = all;
            } else {
                String k = keyword.toLowerCase();
                List<ResultNodeInfo> filtered = new ArrayList<>();
                for (ResultNodeInfo r : all) {
                    if (r.toString().toLowerCase().contains(k)) {
                        filtered.add(r);
                    }
                }
                view = filtered;
            }
            fireTableDataChanged();
        }
    }
}
