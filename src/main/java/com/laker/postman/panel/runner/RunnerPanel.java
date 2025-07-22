package com.laker.postman.panel.runner;

import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.StartButton;
import com.laker.postman.common.component.StopButton;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.RequestCollectionsLeftPanel;
import com.laker.postman.panel.history.HistoryHtmlBuilder;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.TimeDisplayUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public class RunnerPanel extends BasePanel {
    private JTable table;
    private RunnerTableModel tableModel;
    private StartButton runBtn;
    private StopButton stopBtn;    // 停止按钮
    private JLabel timeLabel;     // 执行时间标签
    private JLabel progressLabel; // 进度标签
    private long startTime;       // 记录开始时间
    private Timer executionTimer; // 执行时间计时器
    private volatile boolean isStopped = false; // 停止标志

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
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 12));
        topPanel.add(createButtonPanel(), BorderLayout.WEST);

        // 创建右侧信息面板，包含执行时间和进度显示
        JPanel rightPanel = new JPanel();
        // 使用更紧凑的布局
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rightPanel.setOpaque(false);

        // 创建执行时间显示面板
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));
        timePanel.setOpaque(false);
        JLabel timeIcon = new JLabel(new FlatSVGIcon("icons/time.svg", 20, 20));
        timeLabel = new JLabel("0 ms");
        timeLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
        timePanel.add(timeIcon);
        timePanel.add(Box.createHorizontalStrut(3));
        timePanel.add(timeLabel);

        // 创建任务进度显示面板
        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.X_AXIS));
        taskPanel.setOpaque(false);
        JLabel taskIcon = new JLabel(new FlatSVGIcon("icons/functional.svg", 20, 20));

        // 创建进度文本标签
        progressLabel = new JLabel("0/0");
        progressLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));

        taskPanel.add(taskIcon);
        taskPanel.add(Box.createHorizontalStrut(3));
        taskPanel.add(progressLabel);

        // 添加到右侧面板，并设置间距
        rightPanel.add(timePanel);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(taskPanel);

        topPanel.add(rightPanel, BorderLayout.EAST);

        // 固定顶部面板高度，避免挤压表格区域
        topPanel.setPreferredSize(new Dimension(700, 40));
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
        runBtn = new StartButton();
        runBtn.addActionListener(e -> {
            runSelectedRequests();
            stopBtn.setEnabled(true); // 启动时可用
        });
        btnPanel.add(runBtn);
        stopBtn = new StopButton();
        stopBtn.addActionListener(e -> {
            isStopped = true;
            if (executionTimer != null && executionTimer.isRunning()) {
                executionTimer.stop();
            }
            stopBtn.setEnabled(false); // 停止后不可用
            runBtn.setEnabled(true);  // 可再次运行
        });
        btnPanel.add(stopBtn);
        JButton clearBtn = new JButton("Clear");
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setPreferredSize(new Dimension(90, 28));
        clearBtn.addActionListener(e -> {
            tableModel.clear();
            runBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            resetProgress();
        });
        btnPanel.add(clearBtn);
        return btnPanel;
    }

    // 重置进度和时间显示
    private void resetProgress() {
        // 如果计时器在运行，停止它
        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }

        // 重置标签文本
        timeLabel.setText("0 ms");
        progressLabel.setText("0/0");
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
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
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
        RequestCollectionsLeftPanel.showMultiSelectRequestDialog(
                selected -> {
                    if (selected == null || selected.isEmpty()) return;
                    loadRequests(selected);
                }
        );
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
        isStopped = false; // 开始运行时重置停止标志
        int rowCount = tableModel.getRowCount();
        int selectedCount = (int) IntStream.range(0, rowCount).mapToObj(i -> tableModel.getRow(i)).filter(row -> row != null && row.selected).count();
        if (selectedCount == 0) {
            JOptionPane.showMessageDialog(this, "没有可运行的请求", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        clearRunResults(rowCount);
        runBtn.setEnabled(false);
        // 使用progressLabel显示进度文本，而不是progressBar的string属性
        progressLabel.setText("0/" + selectedCount);

        startTime = System.currentTimeMillis(); // 记录开始时间
        executionTimer = new Timer(100, e -> updateExecutionTime());
        executionTimer.start(); // 启动计时器
        new Thread(() -> executeBatchRequests(rowCount, selectedCount)).start();
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

    private void executeBatchRequests(int rowCount, int selectedCount) {
        int finished = 0;
        for (int i = 0; i < rowCount; i++) {
            if (isStopped) break; // 检查停止标志
            RunnerRowData row = tableModel.getRow(i);
            if (row == null || row.requestItem == null || row.preparedRequest == null) {
                log.warn("Row {} is invalid, skipping execution", i);
                continue; // 跳过无效行
            }
            if (row.selected) {
                BatchResult result = executeSingleRequest(row);
                int rowIdx = i;
                SwingUtilities.invokeLater(() -> tableModel.setResponse(rowIdx, result.resp, result.cost, result.status, result.assertion));
                finished++;
                int finalFinished = finished;
                SwingUtilities.invokeLater(() -> {
                    // 更新进度标签显示
                    progressLabel.setText(finalFinished + "/" + selectedCount);
                });
            }
        }
        SwingUtilities.invokeLater(() -> {
            runBtn.setEnabled(true);
            // 停止计时器
            if (executionTimer != null && executionTimer.isRunning()) {
                executionTimer.stop();
            }
        });
    }

    private static class BatchResult {
        HttpResponse resp;
        long cost;
        String status;
        String assertion;
    }

    private BatchResult executeSingleRequest(RunnerRowData row) {
        if (isStopped) return new BatchResult(); // 检查停止标志，直接返回空结果
        BatchResult result = new BatchResult();
        long start = System.currentTimeMillis();
        HttpRequestItem item = row.requestItem;
        PreparedRequest req = row.preparedRequest;
        Map<String, Object> bindings = HttpUtil.prepareBindings(req);
        Postman pm = (Postman) bindings.get("pm");
        boolean preOk = runPreScript(item, bindings);
        HttpResponse resp = null;
        String status;
        String assertion = "not executed";
        if (!preOk) {
            status = "前置脚本失败";
        } else if (HttpUtil.isSSERequest(req)) {
            status = "SSE请求，无法批量执行";
        } else if (HttpUtil.isWebSocketRequest(req)) {
            status = "WebSocket请求，无法批量执行";
        } else {
            try {
                req.logEvent = true; // 确保日志事件开启
                resp = HttpSingleRequestExecutor.execute(req);
                status = String.valueOf(resp.code);
                assertion = runPostScriptAndAssert(item, bindings, resp, row, pm);
            } catch (Exception ex) {
                log.error("请求执行失败", ex);
                status = ex.getMessage();
                assertion = ex.getMessage();
            }
        }
        long cost = System.currentTimeMillis() - start;
        result.resp = resp;
        result.cost = resp == null ? cost : resp.costMs;
        result.status = status;
        result.assertion = assertion;
        return result;
    }

    private boolean runPreScript(HttpRequestItem item, Map<String, Object> bindings) {
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
                return true;
            } catch (Exception ex) {
                log.error("前置脚本执行异常: {}", ex.getMessage(), ex);
                return false;
            }
        }
        return true;
    }

    private String runPostScriptAndAssert(HttpRequestItem item, Map<String, Object> bindings, HttpResponse resp, RunnerRowData row, Postman pm) {
        String assertion = "Pass";
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
                row.testResults = new java.util.ArrayList<>();
                if (pm.testResults != null) {
                    row.testResults.addAll(pm.testResults);
                }
            } catch (Exception assertionEx) {
                assertion = assertionEx.getMessage();
                row.testResults = new java.util.ArrayList<>();
                if (pm.testResults != null) {
                    row.testResults.addAll(pm.testResults);
                }
            }
        }
        return assertion;
    }

    private String buildRequestHtml(PreparedRequest req) {
        return RunnerHtmlUtil.buildRequestHtml(req);
    }

    private String buildResponseHtml(HttpResponse resp) {
        return RunnerHtmlUtil.buildResponseHtml(resp);
    }

    private String buildTestsHtml(List<TestResult> testResults) {
        return RunnerHtmlUtil.buildTestsHtml(testResults);
    }

    // 显示详情对话框
    private void showDetailDialog(int row) {
        RunnerRowData runnerRowData = tableModel.getRow(row);
        PreparedRequest req = runnerRowData.preparedRequest;
        HttpResponse resp = runnerRowData.response;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Detail", true);
        dialog.setSize(800, 600);
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
        // Timing & Event Info Tab
        if (resp != null && resp.httpEventInfo != null) {
            JEditorPane timingPane = new JEditorPane();
            timingPane.setContentType("text/html");
            timingPane.setEditable(false);
            timingPane.setText(buildTimingHtml(req, resp));
            timingPane.setCaretPosition(0);
            tabbedPane.addTab("Timing", new JScrollPane(timingPane));

            JEditorPane eventInfoPane = new JEditorPane();
            eventInfoPane.setContentType("text/html");
            eventInfoPane.setEditable(false);
            eventInfoPane.setText(buildEventInfoHtml(req, resp));
            eventInfoPane.setCaretPosition(0);
            tabbedPane.addTab("Event Info", new JScrollPane(eventInfoPane));
        }
        dialog.add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private String buildTimingHtml(PreparedRequest request, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(request, resp);
        return HistoryHtmlBuilder.formatHistoryDetailPrettyHtml_Timing(item);
    }

    private String buildEventInfoHtml(PreparedRequest request, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(request, resp);
        item.response = resp;
        return HistoryHtmlBuilder.formatHistoryDetailPrettyHtml_EventInfo(item);
    }

    // 更新执行时间显示
    private void updateExecutionTime() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        timeLabel.setText(TimeDisplayUtil.formatElapsedTime(elapsedTime));
    }
}

