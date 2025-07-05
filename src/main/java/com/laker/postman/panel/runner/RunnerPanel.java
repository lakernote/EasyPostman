package com.laker.postman.panel.runner;

import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.Postman;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.RequestCollectionsLeftPanel;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

@Slf4j
public class RunnerPanel extends BasePanel {
    private JTable table;
    private RunnerTableModel tableModel;
    private JButton runBtn;
    private JProgressBar progressBar;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(700, 400));
        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        topPanel.add(createButtonPanel(), BorderLayout.WEST);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(260, 24));
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        progressPanel.setOpaque(false);
        progressPanel.add(progressBar);
        topPanel.add(progressPanel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createButtonPanel() {
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);
        JButton loadBtn = new JButton("Load");
        loadBtn.setIcon(new FlatSVGIcon("icons/load.svg"));
        loadBtn.setPreferredSize(new Dimension(90, 28));
        loadBtn.addActionListener(e -> showLoadRequestsDialog());
        btnPanel.add(loadBtn);
        runBtn = new JButton("Run");
        runBtn.setIcon(new FlatSVGIcon("icons/run.svg"));
        runBtn.setPreferredSize(new Dimension(90, 28));
        runBtn.addActionListener(e -> runSelectedRequests());
        runBtn.setEnabled(false);
        btnPanel.add(runBtn);
        JButton clearBtn = new JButton("Clear");
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setPreferredSize(new Dimension(90, 28));
        clearBtn.addActionListener(e -> {
            tableModel.clear();
            runBtn.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("0%");
        });
        btnPanel.add(clearBtn);
        return btnPanel;
    }

    private JScrollPane createTablePanel() {
        tableModel = new RunnerTableModel();
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 7;
            }
        };
        table.setRowHeight(28);
        table.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        table.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 13));
        setTableColumnWidths();
        setTableRenderers();
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(220, 235, 252));
        table.setSelectionForeground(Color.BLACK);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return scrollPane;
    }

    private void setTableColumnWidths() {
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(50);
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            table.getColumnModel().getColumn(0).setPreferredWidth(55);
            table.getColumnModel().getColumn(3).setMinWidth(60);
            table.getColumnModel().getColumn(3).setMaxWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(70);
            table.getColumnModel().getColumn(4).setMinWidth(80);
            table.getColumnModel().getColumn(4).setMaxWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(100);
            table.getColumnModel().getColumn(5).setMinWidth(60);
            table.getColumnModel().getColumn(5).setMaxWidth(240);
            table.getColumnModel().getColumn(5).setPreferredWidth(70);
            table.getColumnModel().getColumn(7).setMinWidth(60);
            table.getColumnModel().getColumn(7).setMaxWidth(80);
            table.getColumnModel().getColumn(7).setPreferredWidth(70);
        }
    }

    private void setTableRenderers() {
        table.getColumnModel().getColumn(6).setCellRenderer(createAssertionRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(createMethodRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(createStatusRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(createDetailRenderer());
    }

    private DefaultTableCellRenderer createAssertionRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof String content && !"Pass".equals(content) && StrUtil.isNotBlank(content)) {
                    c.setForeground(new Color(220, 53, 69));
                } else if ("Pass".equals(value)) {
                    c.setForeground(new Color(40, 167, 69));
                }
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createMethodRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String color = HttpUtil.getMethodColor(value.toString());
                    c.setForeground(Color.decode(color));
                }
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    try {
                        int code = Integer.parseInt(value.toString());
                        c.setForeground(HttpUtil.getStatusColor(code));
                    } catch (Exception ignore) {
                        c.setForeground(Color.RED);
                    }
                }
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createDetailRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JButton btn = new JButton();
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setContentAreaFilled(false);
                btn.setOpaque(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setIcon(new FlatSVGIcon("icons/detail.svg"));
                return btn;
            }
        };
    }

    @Override
    protected void registerListeners() {
        // 鼠标点击详情按钮事件
        // 监听表格点击
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == 7 && row >= 0) {
                    showDetailDialog(row);
                }
            }
        });
    }

    // 弹出选择请求/分组对话框
    private void showLoadRequestsDialog() {
        RequestCollectionsLeftPanel requestCollectionsLeftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Select Request or Group", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // 用JTree展示集合树，支持多选
        JTree tree = requestCollectionsLeftPanel.createRequestSelectionTree();
        JScrollPane treeScroll = new JScrollPane(tree);
        dialog.add(treeScroll, BorderLayout.CENTER);

        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            List<HttpRequestItem> selected = requestCollectionsLeftPanel.getSelectedRequestsFromTree(tree);
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select at least one request", "Tip", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadRequests(selected);
            dialog.dispose();
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(okBtn);
        btns.add(cancelBtn);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // 加载选中的请求到表格
    public void loadRequests(List<HttpRequestItem> requests) {
        tableModel.clear();
        for (HttpRequestItem item : requests) {
            tableModel.addRow(new RunnerRowData(item, PreparedRequestBuilder.build(item)));
        }
        table.setEnabled(true);
        runBtn.setEnabled(true);
    }

    // 批量运行
    private void runSelectedRequests() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "没有可运行的请求", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        clearRunResults(rowCount);
        runBtn.setEnabled(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(rowCount);
        progressBar.setValue(0);
        progressBar.setString("0 / " + rowCount);
        new Thread(() -> executeBatchRequests(rowCount)).start();
    }

    private void clearRunResults(int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            RunnerRowData row = tableModel.getRow(i);
            row.response = null;
            row.cost = 0;
            row.status = null;
            row.assertion = null;
            row.testResults = null;
            tableModel.fireTableRowsUpdated(i, i);
        }
    }

    private void executeBatchRequests(int rowCount) {
        int finished = 0;
        for (int i = 0; i < rowCount; i++) {
            RunnerRowData row = tableModel.getRow(i);
            if (row.selected) {
                BatchResult result = executeSingleRequest(row);
                int rowIdx = i;
                SwingUtilities.invokeLater(() -> {
                    tableModel.setResponse(rowIdx, result.resp, result.cost, result.status, result.assertion);
                });
            }
            finished++;
            int finalFinished = finished;
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(finalFinished);
                progressBar.setString(finalFinished + " / " + rowCount);
            });
        }
        SwingUtilities.invokeLater(() -> {
            runBtn.setEnabled(true);
            progressBar.setString("Done");
        });
    }

    private static class BatchResult {
        HttpResponse resp;
        long cost;
        String status;
        String assertion;
    }

    private BatchResult executeSingleRequest(RunnerRowData row) {
        BatchResult result = new BatchResult();
        long start = System.currentTimeMillis();
        String status = "";
        String assertion = "not executed";
        HttpRequestItem item = row.requestItem;
        PreparedRequest req = row.preparedRequest;
        boolean preOk = true;
        Map<String, Object> bindings = HttpUtil.prepareBindings(req);
        Postman pm = (Postman) bindings.get("pm");
        String prescript = item.getPrescript();
        if (prescript != null && !prescript.isBlank()) {
            try {
                JsScriptExecutor.executeScript(
                        prescript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                SidebarTabPanel.appendConsoleLog("[PreScript Console]\n" + output);
                            }
                        }
                );
            } catch (Exception ex) {
                log.error("前置脚本执行异常: {}", ex.getMessage(), ex);
                preOk = false;
            }
        }
        HttpResponse resp = null;
        if (!preOk) {
            status = "前置脚本失败";
        } else if (HttpUtil.isSSERequest(req)) {
            status = "SSE请求，无法批量执行";
        } else if (HttpUtil.isWebSocketRequest(req)) {
            status = "WebSocket请求，无法批量执行";
        } else {
            try {
                resp = HttpSingleRequestExecutor.execute(req);
                status = String.valueOf(resp.code);
                // 后置脚本
                try {
                    String postscript = item.getPostscript();
                    if (postscript != null && !postscript.isBlank()) {
                        HttpUtil.postBindings(bindings, resp);
                        try {
                            JsScriptExecutor.executeScript(
                                    postscript,
                                    bindings,
                                    output -> {
                                        if (!output.isBlank()) {
                                            SidebarTabPanel.appendConsoleLog("[PostScript Console]\n" + output);
                                        }
                                    }
                            );
                            assertion = "Pass";
                            // 保存断言结果
                            row.testResults = new java.util.ArrayList<>();
                            if (pm.testResults != null) {
                                row.testResults.addAll(pm.testResults);
                            }
                        } catch (Exception assertionEx) {
                            assertion = assertionEx.getMessage();
                            // 保存断言结果（即使有异常也保存）
                            row.testResults = new java.util.ArrayList<>();
                            if (pm.testResults != null) {
                                row.testResults.addAll(pm.testResults);
                            }
                        }
                    } else {
                        assertion = "Pass";
                    }
                } catch (Exception ex) {
                    log.error("后置脚本执行异常: {}", ex.getMessage(), ex);
                    status = ex.getMessage();
                    assertion = ex.getMessage();
                }
            } catch (Exception ex) {
                log.error("请求执行失败", ex);
                status = ex.getMessage();
                assertion = ex.getMessage();
            }
        }
        long cost = System.currentTimeMillis() - start;
        result.resp = resp;
        result.cost = cost;
        result.status = status;
        result.assertion = assertion;
        return result;
    }

    // HTML构建方法提取到工具类
    private String buildRequestHtml(PreparedRequest req) {
        return RunnerHtmlUtil.buildRequestHtml(req);
    }

    private String buildResponseHtml(HttpResponse resp) {
        return RunnerHtmlUtil.buildResponseHtml(resp);
    }

    private String buildTestsHtml(List<? extends Object> testResults) {
        return RunnerHtmlUtil.buildTestsHtml(testResults);
    }

    // 显示详情对话框
    private void showDetailDialog(int row) {
        RunnerRowData runnerRowData = tableModel.getRow(row);
        PreparedRequest req = runnerRowData.preparedRequest;
        HttpResponse resp = runnerRowData.response;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Detail", true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        // 请求信息（HTML）
        JEditorPane reqPane = new JEditorPane();
        reqPane.setContentType("text/html");
        reqPane.setEditable(false);
        reqPane.setText(buildRequestHtml(req));
        reqPane.setCaretPosition(0); // 确保滚动到顶部
        tabbedPane.addTab("Request", new JScrollPane(reqPane));
        // 响应信息（HTML）
        JEditorPane respPane = new JEditorPane();
        respPane.setContentType("text/html");
        respPane.setEditable(false);
        respPane.setText(buildResponseHtml(resp));
        respPane.setCaretPosition(0); // 确保滚动到顶部
        tabbedPane.addTab("Response", new JScrollPane(respPane));
        // Tests 断言结果Tab
        if (runnerRowData.testResults != null && !runnerRowData.testResults.isEmpty()) {
            JEditorPane testsPane = new JEditorPane();
            testsPane.setContentType("text/html");
            testsPane.setEditable(false);
            testsPane.setText(buildTestsHtml(runnerRowData.testResults));
            testsPane.setCaretPosition(0);
            tabbedPane.addTab("Tests", new JScrollPane(testsPane));
        }
        dialog.add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

