package com.laker.postman.panel.runner;

import cn.hutool.core.util.StrUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.RequestCollectionsSubPanel;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class RunnerPanel extends JPanel {
    private final JTable table;
    private DefaultTableModel tableModel;
    private final JButton runBtn;
    private JProgressBar progressBar;
    // 用于缓存每行的HttpRequestItem
    private final java.util.List<HttpRequestItem> loadedRequestItems = new ArrayList<>();
    // 用于缓存每行的PreparedRequest
    private final java.util.List<PreparedRequest> loadedPreparedRequests = new ArrayList<>();
    // 保存每行的响应信息
    private final java.util.List<HttpResponse> loadedResponses = new ArrayList<>();

    public RunnerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(700, 400));

        // 按钮面板放到顶部
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JButton loadBtn = new JButton("加载请求");
        loadBtn.setPreferredSize(new Dimension(110, 32));
        loadBtn.addActionListener(e -> showLoadRequestsDialog());
        btnPanel.add(loadBtn);
        runBtn = new JButton("批量运行");
        runBtn.setPreferredSize(new Dimension(110, 32));
        runBtn.addActionListener(e -> runSelectedRequests());
        runBtn.setEnabled(false);
        btnPanel.add(runBtn);
        JButton clearBtn = new JButton("清空");
        clearBtn.setPreferredSize(new Dimension(110, 32));
        clearBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            loadedRequestItems.clear();
            loadedPreparedRequests.clear();
            loadedResponses.clear();
            runBtn.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("0%");
        });
        btnPanel.add(clearBtn);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(400, 24));
        btnPanel.add(progressBar);

        add(btnPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"选择", "请求名称", "URL", "方法", "耗时(ms)", "状态", "断言", "详情"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 7; // 详情列可点击
            }
        };
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 7;
            }
        };
        table.setRowHeight(28);
        table.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        table.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 13));
        // 优化选择列宽度
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(50);
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            table.getColumnModel().getColumn(0).setPreferredWidth(55);
            // 优化方法列宽度
            table.getColumnModel().getColumn(3).setMinWidth(60);
            table.getColumnModel().getColumn(3).setMaxWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(70);
            // 优化耗时列宽度
            table.getColumnModel().getColumn(4).setMinWidth(80);
            table.getColumnModel().getColumn(4).setMaxWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(100);
            // 优化状态列宽度
            table.getColumnModel().getColumn(5).setMinWidth(60);
            table.getColumnModel().getColumn(5).setMaxWidth(240);
            table.getColumnModel().getColumn(5).setPreferredWidth(70);
            // 优化详情列宽度
            table.getColumnModel().getColumn(7).setMinWidth(60);
            table.getColumnModel().getColumn(7).setMaxWidth(80);
            table.getColumnModel().getColumn(7).setPreferredWidth(70);
        }
        // 设置断言列渲染器，异常时标红
        DefaultTableCellRenderer assertionRenderer = new DefaultTableCellRenderer() {
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
        table.getColumnModel().getColumn(6).setCellRenderer(assertionRenderer);

        // 详情列渲染为按钮
        DefaultTableCellRenderer detailRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JButton btn = new JButton("查看");
                btn.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
                return btn;
            }
        };
        table.getColumnModel().getColumn(7).setCellRenderer(detailRenderer);

        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(220, 235, 252));
        table.setSelectionForeground(Color.BLACK);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        // 启用行拖拽
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // 鼠标点击详情按钮事件
        // 监听表格点击
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
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
        RequestCollectionsSubPanel requestCollectionsSubPanel = SingletonFactory.getInstance(RequestCollectionsSubPanel.class);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "选择请求或分组", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // 用JTree展示集合树，支持多选
        JTree tree = requestCollectionsSubPanel.createRequestSelectionTree();
        JScrollPane treeScroll = new JScrollPane(tree);
        dialog.add(treeScroll, BorderLayout.CENTER);

        JButton okBtn = new JButton("确定");
        okBtn.addActionListener(e -> {
            List<HttpRequestItem> selected = requestCollectionsSubPanel.getSelectedRequestsFromTree(tree);
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请选择至少一个请求", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadRequests(selected);
            dialog.dispose();
        });
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(okBtn);
        btns.add(cancelBtn);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // 加载选中的请求到表格
    public void loadRequests(List<HttpRequestItem> requests) {
        tableModel.setRowCount(0);
        loadedRequestItems.clear();
        loadedPreparedRequests.clear();
        loadedResponses.clear();
        for (HttpRequestItem item : requests) {
            tableModel.addRow(new Object[]{true, item.getName(), item.getUrl(), item.getMethod(), "", "", ""});
            loadedRequestItems.add(item);
            loadedPreparedRequests.add(PreparedRequestBuilder.build(item));
            loadedResponses.add(null); // 初始化响应为null
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
        runBtn.setEnabled(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(rowCount);
        progressBar.setValue(0);
        progressBar.setString("0 / " + rowCount);
        new Thread(() -> {
            int finished = 0;
            for (int i = 0; i < rowCount; i++) {
                Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
                if (selected != null && selected) {
                    long start = System.currentTimeMillis();
                    String status = "未执行";
                    String assertion = "";
                    HttpRequestItem item = loadedRequestItems.get(i);
                    PreparedRequest req = loadedPreparedRequests.get(i);
                    boolean preOk = true;
                    Map<String, Object> bindings = HttpUtil.prepareBindings(req);
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
                    } else {
                        if (HttpUtil.isSSERequest(req)) {
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
                                        } catch (Exception assertionEx) {
                                            assertion = assertionEx.getMessage();
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
                    }
                    long cost = System.currentTimeMillis() - start;
                    int rowIdx = i;
                    String finalStatus = status;
                    String finalAssertion = assertion;
                    HttpResponse finalResp = resp;
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setValueAt(finalStatus, rowIdx, 5);
                        tableModel.setValueAt(finalAssertion, rowIdx, 6);
                        tableModel.setValueAt(cost, rowIdx, 4);
                        loadedResponses.set(rowIdx, finalResp); // 保存响应
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
        }).start();
    }

    // 显示详情对话框
    private void showDetailDialog(int row) {
        PreparedRequest req = loadedPreparedRequests.get(row);
        HttpResponse resp = loadedResponses.size() > row ? loadedResponses.get(row) : null;
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
        tabbedPane.addTab("Request", new JScrollPane(reqPane));
        // 响应信息（HTML）
        JEditorPane respPane = new JEditorPane();
        respPane.setContentType("text/html");
        respPane.setEditable(false);
        respPane.setText(buildResponseHtml(resp));
        tabbedPane.addTab("Response", new JScrollPane(respPane));
        dialog.add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private String buildRequestHtml(PreparedRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:10px;'>");
        sb.append("<b>URL:</b> ").append(escapeHtml(req.url)).append("<br/>");
        sb.append("<b>方法:</b> ").append(escapeHtml(req.method)).append("<br/>");
        if (req.headers != null && !req.headers.isEmpty()) {
            sb.append("<b>请求头:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        if (req.body != null && !req.body.isEmpty()) {
            sb.append("<b>请求体:</b><br/><pre style='background:#f8f8f8;border:1px solid #eee;padding:8px;'>")
                    .append(escapeHtml(req.body)).append("</pre>");
        }
        if (req.formData != null && !req.formData.isEmpty()) {
            sb.append("<b>Form Data:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.formData.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        if (req.formFiles != null && !req.formFiles.isEmpty()) {
            sb.append("<b>Form Files:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.formFiles.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        if (req.urlencoded != null && !req.urlencoded.isEmpty()) {
            sb.append("<b>x-www-form-urlencoded:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.urlencoded.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildResponseHtml(HttpResponse resp) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:10px;'>");
        if (resp == null) {
            sb.append("<span style='color:gray;'>无响应信息</span>");
        } else {
            sb.append("<b>状态码:</b> ").append(resp.code).append("<br/>");
            if (resp.headers != null && !resp.headers.isEmpty()) {
                sb.append("<b>响应头:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
                for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                    sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>");
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        for (int i = 0; i < values.size(); i++) {
                            sb.append(escapeHtml(values.get(i)));
                            if (i < values.size() - 1) sb.append(", ");
                        }
                    }
                    sb.append("</td></tr>");
                }
                sb.append("</table>");
            }
            sb.append("<b>响应体:</b><br/><pre style='background:#f8f8f8;border:1px solid #eee;padding:8px;'>")
                    .append(resp.body != null ? escapeHtml(resp.body) : "<无响应体>")
                    .append("</pre>");
        }
        sb.append("</body></html>");
        return sb.toString();
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

