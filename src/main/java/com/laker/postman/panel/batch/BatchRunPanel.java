package com.laker.postman.panel.batch;

import com.laker.postman.common.SingletonPanelFactory;
import com.laker.postman.common.table.ErrorMessageCellRenderer;
import com.laker.postman.common.table.generic.GenericTablePanel;
import com.laker.postman.common.table.StatusCodeCellRenderer;
import com.laker.postman.common.tree.RequestTreeCellRenderer;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.RequestCollectionsSubPanel;
import com.laker.postman.service.HttpService;
import com.laker.postman.util.HttpRequestExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量执行请求面板：选择分组，批量顺序执行分组下所有请求，并输出每个请求的状态码、耗时、响应大小等
 */
@Slf4j
public class BatchRunPanel extends JPanel {
    private final JTree groupTree;
    private final DefaultTreeModel groupTreeModel;
    private final ResultTableModel resultTableModel;
    private final GenericTablePanel<ResultRow> resultTablePanel;
    private final JButton runButton;
    private final JProgressBar progressBar;

    public BatchRunPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));

        // 左侧分组树
        RequestCollectionsSubPanel collectionPanel = SingletonPanelFactory.getInstance(RequestCollectionsSubPanel.class);
        groupTreeModel = collectionPanel.getGroupTreeModel();
        groupTree = new JTree(groupTreeModel);
        groupTree.setRootVisible(false);
        groupTree.setShowsRootHandles(true);
        groupTree.setCellRenderer(new RequestTreeCellRenderer());
        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(220, 200));

        // 右侧结果表格
        resultTableModel = new ResultTableModel();
        resultTablePanel = new GenericTablePanel<>(resultTableModel);
        JTable resultTable = resultTablePanel.getTable();
        // 第二列状态码使用自定义渲染器
        resultTable.getColumnModel().getColumn(1).setCellRenderer(new StatusCodeCellRenderer());
        // 第五列错误信息列渲染器
        resultTable.getColumnModel().getColumn(4).setCellRenderer(new ErrorMessageCellRenderer());
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setPreferredSize(new Dimension(500, 400));

        // 按钮
        runButton = new JButton("Batch Run");
        runButton.addActionListener(e -> startBatchRun());

        // 布局
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("选择分组："), BorderLayout.NORTH);
        leftPanel.add(treeScroll, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false); // 默认隐藏
        leftPanel.add(progressBar, BorderLayout.NORTH); // 放在左侧面板顶部
        leftPanel.add(runButton, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(tableScroll, BorderLayout.CENTER);
    }

    // 启动批量执行
    private void startBatchRun() {
        TreePath selectedPath = groupTree.getSelectionPath();
        if (selectedPath == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个分组节点", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        Object userObj = node.getUserObject();
        if (!(userObj instanceof Object[] arr) || !"group".equals(arr[0])) {
            JOptionPane.showMessageDialog(this, "请选择有效的分组节点", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 获取该分组下所有请求节点
        List<HttpRequestItem> requests = new ArrayList<>();
        collectRequests(node, requests);
        if (requests.isEmpty()) {
            JOptionPane.showMessageDialog(this, "该分组下没有请求", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 清空结果表
        resultTableModel.clear();
        // 禁用按钮
        runButton.setEnabled(false);
        // 显示进度条
        progressBar.setVisible(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(requests.size());
        progressBar.setValue(0);

        // 执行请求
        new SwingWorker<Void, ResultRow>() {
            @Override
            protected Void doInBackground() {
                for (HttpRequestItem item : requests) {
                    long start = System.currentTimeMillis();
                    int code = 0;
                    String error = null;
                    String sizeStr = "-";
                    long cost = 0;
                    try {
                        HttpRequestExecutor.PreparedRequest req = HttpRequestExecutor.buildPreparedRequest(item);
                        HttpService.HttpResponse resp = HttpRequestExecutor.execute(req);
                        code = resp.code;
                        cost = System.currentTimeMillis() - start;
                        if (resp.body != null) {
                            int bytes = resp.body.getBytes().length;
                            if (bytes < 1024) sizeStr = bytes + " B";
                            else if (bytes < 1024 * 1024) sizeStr = String.format("%.1f KB", bytes / 1024.0);
                            else sizeStr = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
                        }
                    } catch (Exception ex) {
                        error = ex.getMessage();
                        cost = System.currentTimeMillis() - start;
                    }
                    publish(new ResultRow(item.getName(), code, cost, sizeStr, error));
                }
                return null;
            }

            @Override
            protected void process(List<ResultRow> chunks) {
                for (ResultRow row : chunks) {
                    resultTableModel.addRow(row);
                    progressBar.setValue(resultTableModel.getRowCount());
                }
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                progressBar.setVisible(false);
                JOptionPane.showMessageDialog(BatchRunPanel.this, "执行完成", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    // 递归收集请求节点
    private void collectRequests(DefaultMutableTreeNode node, List<HttpRequestItem> list) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof Object[] arr) {
                if ("request".equals(arr[0])) {
                    list.add((HttpRequestItem) arr[1]);
                } else if ("group".equals(arr[0])) {
                    collectRequests(child, list);
                }
            }
        }
    }
}