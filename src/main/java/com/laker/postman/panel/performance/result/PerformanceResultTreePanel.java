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

    /** 是否暂停刷新（Space 控制） */
    private volatile boolean paused = false;

    /** 当前选中行（仅用于详情显示） */
    private volatile ResultNodeInfo selected;

    private static final int MAX_ROWS = 200_000;

    /** 16ms UI 帧刷新 */
    private final Timer uiFrameTimer = new Timer(16, e -> {
        if (paused) return;

        flushQueueOnEDT();     // ✅ 新增
        tableModel.flushIfDirty();
    });

    public PerformanceResultTreePanel() {
        initUI();
        registerListeners();
        uiFrameTimer.start();
    }
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
                tableModel.trimTo(MAX_ROWS); // ✅ 现在安全
            }
        }
    }

    /* ======================= UI ======================= */

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        searchField = new SearchTextField();

        tableModel = new ResultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new ResultRowRenderer());

        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        table.getColumnModel().getColumn(0).setCellRenderer(left);
        table.getColumnModel().getColumn(1).setCellRenderer(left);

        JScrollPane tableScroll = new JScrollPane(table);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(searchField, BorderLayout.NORTH);
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
        split.setDividerLocation(420);
        add(split, BorderLayout.CENTER);
    }

    /* ======================= 监听 ======================= */

    private void registerListeners() {

        // 行选择 → 仅渲染详情
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
                    tableModel.flushIfDirty(); // ⭐ 恢复时立刻补一次刷新
                }
            }
        });

        // 搜索（不影响刷新）
        searchField.addActionListener(e ->
                tableModel.filter(searchField.getText())
        );
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

        private final String[] columns = {"Name", "Cost (ms)"};

        private final List<ResultNodeInfo> all = new ArrayList<>();
        private List<ResultNodeInfo> view = all;

        private boolean dirty = false;

        @Override public int getRowCount() { return view.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            ResultNodeInfo r = view.get(row);
            return col == 0 ? r.name : r.costMs;
        }

        ResultNodeInfo getRow(int row) {
            return view.get(row);
        }

        int getTotalSize() {
            return all.size();
        }

        void append(List<ResultNodeInfo> batch) {
            all.addAll(batch);
            dirty = true;
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
            fireTableDataChanged();
        }

        void trimTo(int max) {
            int remove = all.size() - max;
            if (remove <= 0) return;

            // 从头逐个 remove（EDT 内性能完全 OK）
            for (int i = 0; i < remove; i++) {
                all.remove(0);
            }
            dirty = true;
        }

        void filter(String keyword) {
            if (keyword == null || keyword.isBlank()) {
                view = all;
            } else {
                String k = keyword.toLowerCase();
                List<ResultNodeInfo> f = new ArrayList<>();
                for (ResultNodeInfo r : all) {
                    if (r.name != null && r.name.toLowerCase().contains(k)) {
                        f.add(r);
                    }
                }
                view = f;
            }
            fireTableDataChanged();
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

            if (!isSelected) {
                setBackground(
                        r.success
                                ? new Color(235, 255, 235)
                                : new Color(255, 235, 235)
                );
            }

            setToolTipText(
                    r.hasAssertionFailed() ? "Assertion Failed" : null
            );

            return this;
        }
    }
}
