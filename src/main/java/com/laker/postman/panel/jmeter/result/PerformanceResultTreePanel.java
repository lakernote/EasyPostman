package com.laker.postman.panel.jmeter.result;

import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.model.TestResult;
import com.laker.postman.panel.history.HistoryHtmlBuilder;
import com.laker.postman.panel.jmeter.component.ResultTreeCellRenderer;
import com.laker.postman.panel.jmeter.model.ResultNodeInfo;
import com.laker.postman.panel.runner.RunnerHtmlUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

/**
 * 结果树面板
 * 包含搜索框、结果树和详情区域
 */
@Slf4j
public class PerformanceResultTreePanel extends JPanel {

    private JTree resultTree;

    private DefaultTreeModel resultTreeModel;

    private DefaultMutableTreeNode resultRootNode;

    private JTabbedPane resultDetailTabbedPane;

    private JTextField searchField;

    public PerformanceResultTreePanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ========== 结果树搜索框 ==========
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new SearchTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);

        // 结果树
        resultRootNode = new DefaultMutableTreeNode("Result Tree");
        resultTreeModel = new DefaultTreeModel(resultRootNode);
        resultTree = new JTree(resultTreeModel);
        resultTree.setRootVisible(true);
        resultTree.setShowsRootHandles(true);
        resultTree.setCellRenderer(new ResultTreeCellRenderer());
        JScrollPane resultTreeScroll = new JScrollPane(resultTree);

        // 左侧面板 - 包含搜索框和结果树
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(resultTreeScroll, BorderLayout.CENTER);

        // 详情tab
        resultDetailTabbedPane = new JTabbedPane();
        resultDetailTabbedPane.addTab("Request", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Response", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Tests", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Timing", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Event Info", new JScrollPane(new JEditorPane()));

        // 主分割面板 - 左侧结果树，右侧详情
        JSplitPane resultSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, resultDetailTabbedPane);
        resultSplit.setDividerLocation(260);

        // 将分割面板添加到当前面板
        add(resultSplit, BorderLayout.CENTER);
    }

    private void registerListeners() {
        // 结果树节点点击，展示请求响应详情
        resultTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) resultTree.getLastSelectedPathComponent();
            if (node == null) {
                clearDetailTabs();
                return;
            }
            Object userObj = node.getUserObject();
            if (userObj instanceof ResultNodeInfo info) {
                // 构建HTML内容
                PreparedRequest req = info.req;
                HttpResponse resp = info.resp;
                // Request
                setTabHtml(0, buildRequestHtml(req));
                // Response
                setTabHtml(1, buildResponseHtml(resp));
                // Tests
                if (info.testResults != null && !info.testResults.isEmpty()) {
                    setTabHtml(2, buildTestsHtml(info.testResults));
                } else {
                    setTabHtml(2, "<html><body><i>No assertion results</i></body></html>");
                }
                // Timing
                if (resp != null && resp.httpEventInfo != null) {
                    setTabHtml(3, buildTimingHtml(req, resp));
                    setTabHtml(4, buildEventInfoHtml(req, resp));
                } else {
                    setTabHtml(3, "<html><body><i>No TimingInfo</i></body></html>");
                    setTabHtml(4, "<html><body><i>No EventInfo</i></body></html>");
                }
            } else {
                clearDetailTabs();
            }
        });

        searchField.addActionListener(e -> searchResultTree(searchField.getText()));
    }

    private void clearDetailTabs() {
        for (int i = 0; i < resultDetailTabbedPane.getTabCount(); i++) {
            JScrollPane scroll = (JScrollPane) resultDetailTabbedPane.getComponentAt(i);
            JEditorPane pane = (JEditorPane) scroll.getViewport().getView();
            pane.setText("");
        }
    }

    // 设置tab页内容
    private void setTabHtml(int tabIdx, String html) {
        JScrollPane scroll = (JScrollPane) resultDetailTabbedPane.getComponentAt(tabIdx);
        JEditorPane pane = (JEditorPane) scroll.getViewport().getView();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText(html);
        pane.setCaretPosition(0);
    }

    // 复用RunnerPanel的HTML构建方法
    private String buildRequestHtml(PreparedRequest req) {
        return RunnerHtmlUtil.buildRequestHtml(req);
    }

    private String buildResponseHtml(HttpResponse resp) {
        return RunnerHtmlUtil.buildResponseHtml(resp);
    }

    private String buildTestsHtml(List<TestResult> testResults) {
        return RunnerHtmlUtil.buildTestsHtml(testResults);
    }

    private String buildTimingHtml(PreparedRequest req, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(req, resp);
        return HistoryHtmlBuilder.formatHistoryDetailPrettyHtml_Timing(item);
    }

    private String buildEventInfoHtml(PreparedRequest req, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(req, resp);
        item.response = resp;
        return HistoryHtmlBuilder.formatHistoryDetailPrettyHtml_EventInfo(item);
    }

    // ========== 结果树搜索方法 ==========
    public void searchResultTree(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 关键字为空，显示全部
            resultTreeModel.setRoot(resultRootNode);
            resultTree.updateUI();
            return;
        }
        // 过滤结果树，仅显示匹配的节点及其父节点
        DefaultMutableTreeNode filteredRoot = filterResultTree(resultRootNode, keyword);
        resultTreeModel.setRoot(filteredRoot);
        resultTree.updateUI();
        // 展开所有节点
        for (int i = 0; i < resultTree.getRowCount(); i++) {
            resultTree.expandRow(i);
        }
    }

    private DefaultMutableTreeNode filterResultTree(DefaultMutableTreeNode node, String keyword) {
        boolean matched = node.toString().toLowerCase().contains(keyword.toLowerCase());
        DefaultMutableTreeNode filteredNode = new DefaultMutableTreeNode(node.getUserObject());
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode filteredChild = filterResultTree(child, keyword);
            if (filteredChild != null) {
                filteredNode.add(filteredChild);
                matched = true;
            }
        }
        return matched ? filteredNode : null;
    }

    /**
     * 添加结果节点
     *
     * @param nodeInfo      结果节点信息
     * @param efficientMode 是否为高效模式（只记录失败结果）
     */
    public void addResult(ResultNodeInfo nodeInfo, boolean efficientMode) {
        if (nodeInfo == null) return;
        if (efficientMode && nodeInfo.success) return; // 高效模式下只记录失败结果

        DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(nodeInfo);
        SwingUtilities.invokeLater(() -> {
            resultRootNode.add(reqNode);
            resultTreeModel.reload(resultRootNode);
        });
    }

    /**
     * 清空结果树
     */
    public void clearResults() {
        resultRootNode.removeAllChildren();
        resultTreeModel.reload();
        resultTree.clearSelection();
        clearDetailTabs();
    }

}