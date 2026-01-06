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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 性能测试结果表（DevTools Network 风格）
 * - Space 键暂停/恢复刷新
 * - 16ms 帧刷新机制
 * - 支持关键字搜索和"只看失败"过滤
 */
@Slf4j
public class PerformanceResultTreePanel extends JPanel {


    private JTable table;
    private ResultTableModel tableModel;
    private JTabbedPane detailTabs;

    private JTextField searchField;
    private JCheckBox onlyFailCheckBox;


    private final Queue<ResultNodeInfo> pendingQueue = new ConcurrentLinkedQueue<>();

    /**
     * 是否暂停刷新（Space 控制）
     */
    private final AtomicBoolean paused = new AtomicBoolean(false);

    /**
     * 当前选中行（仅用于详情显示）
     */
    private volatile ResultNodeInfo selected;

    private static final int MAX_ROWS = 200_000;

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
            if (batch.size() >= 2000) break;
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
        onlyFailCheckBox = new JCheckBox("只看失败接口", false);

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rightBox.add(onlyFailCheckBox);
        searchPanel.add(rightBox, BorderLayout.EAST);

        tableModel = new ResultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new ResultRowRenderer());

        DefaultTableCellRenderer cellLeft = new DefaultTableCellRenderer();
        cellLeft.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellLeft);
        }

        DefaultTableCellRenderer headerLeft = new DefaultTableCellRenderer();
        headerLeft.setHorizontalAlignment(SwingConstants.LEFT);
        table.getTableHeader().setDefaultRenderer(headerLeft);

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
        split.setDividerLocation(520);
        add(split, BorderLayout.CENTER);
    }

    private void registerListeners() {

        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

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

        table.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("SPACE"), "togglePause");
        table.getActionMap().put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean newValue = !paused.getAndSet(!paused.get());
                if (newValue) {
                    tableModel.flushIfDirty();
                }
            }
        });

        searchField.addActionListener(e -> applyFilters());

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

    static class ResultTableModel extends AbstractTableModel {

        private static final int COL_NAME = 0;
        private static final int COL_COST = 1;
        private static final int COL_RESULT = 2;

        private final String[] columns = {"Name", "Cost (ms)", "Result"};

        private final List<ResultNodeInfo> all = new ArrayList<>();
        private List<ResultNodeInfo> view = all;

        private boolean dirty = false;

        private String keyword = "";
        private boolean onlyFail = false;

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
            if (hasFilter()) {
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

            all.subList(0, remove).clear();

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

        private boolean passFailFilter(ResultNodeInfo r, boolean onlyFail) {
            if (!onlyFail) return false;
            return r == null || r.success;
        }

        /**
         * 关键字匹配：接口名或状态文本
         *
         * @return true=跳过此记录，false=保留此记录
         */
        private boolean matchKeyword(ResultNodeInfo r, String k) {
            if (k == null || k.isEmpty()) return false;
            if (r == null) return true;

            String name = r.name == null ? "" : r.name.toLowerCase();
            if (name.contains(k)) return false;

            boolean wantSuccess = containsAny(k, "成功", "success", "ok", "pass", "passed");
            boolean wantFail = containsAny(k, "失败", "fail", "error", "err", "failed");

            if (!wantSuccess && !wantFail) {
                return true;
            }

            if (wantSuccess && r.success) return false;
            if (wantFail && !r.success) return false;

            return true;
        }

        private boolean containsAny(String text, String... keys) {
            if (text == null) return false;
            for (String key : keys) {
                if (key != null && !key.isEmpty() && text.contains(key)) return true;
            }
            return false;
        }
    }


    static class ResultRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ResultTableModel model = (ResultTableModel) table.getModel();
            ResultNodeInfo r = model.getRow(row);

            if (!isSelected) {
                setBackground(
                        r.success
                                ? new Color(235, 255, 235)
                                : new Color(255, 235, 235)
                );
            }

            setToolTipText(r.hasAssertionFailed() ? "Assertion Failed" : null);

            setHorizontalAlignment(SwingConstants.LEFT);

            return this;
        }
    }
}
