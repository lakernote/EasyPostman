package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.component.ResultTreeCellRenderer;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    // 批量更新控制
    private final Queue<ResultNodeInfo> pendingResults = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean batchUpdateScheduled = new AtomicBoolean(false);
    private static final int BATCH_UPDATE_DELAY_MS = 500; // 每500ms批量更新一次
    private static final int MAX_RESULT_NODES = 10000; // 最多保留10000个结果节点，防止内存溢出
    private final AtomicInteger currentResultCount = new AtomicInteger(0); // 当前结果数量

    public PerformanceResultTreePanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        // 设置面板边距
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // ========== 结果树搜索框 ==========
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new SearchTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);

        // 结果树
        resultRootNode = new DefaultMutableTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESULT_TREE));
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
        resultDetailTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REQUEST), new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESPONSE), new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TESTS), new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TIMING), new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_EVENT_INFO), new JScrollPane(new JEditorPane()));

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
                setTabHtml(0, HttpHtmlRenderer.renderRequest(req));
                // Response
                setTabHtml(1, HttpHtmlRenderer.renderResponse(resp));
                // Tests
                if (info.testResults != null && !info.testResults.isEmpty()) {
                    setTabHtml(2, HttpHtmlRenderer.renderTestResults(info.testResults));
                } else {
                    setTabHtml(2, I18nUtil.getMessage(MessageKeys.PERFORMANCE_NO_ASSERTION_RESULTS));
                }
                // Timing
                if (resp != null && resp.httpEventInfo != null) {
                    setTabHtml(3, HttpHtmlRenderer.renderTimingInfo(resp));
                    setTabHtml(4, HttpHtmlRenderer.renderEventInfo(resp));
                } else {
                    setTabHtml(3, I18nUtil.getMessage(MessageKeys.PERFORMANCE_NO_TIMING_INFO));
                    setTabHtml(4, I18nUtil.getMessage(MessageKeys.PERFORMANCE_NO_EVENT_INFO));
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

        // 将结果加入待处理队列
        pendingResults.offer(nodeInfo);

        // 调度批量更新（避免重复调度）
        scheduleBatchUpdate();
    }

    /**
     * 调度批量更新任务
     */
    private void scheduleBatchUpdate() {
        // 使用compareAndSet实现无锁的原子操作
        // 如果当前值是false，则设置为true并返回true
        // 如果当前值已经是true，则返回false（表示已经有调度任务）
        if (!batchUpdateScheduled.compareAndSet(false, true)) {
            return; // 已经有调度任务在执行，不重复调度
        }

        // 延迟执行批量更新（使用Swing Timer，自动在EDT线程执行）
        Timer timer = new Timer(BATCH_UPDATE_DELAY_MS, e -> {
            processPendingResults();
            batchUpdateScheduled.set(false);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 批量处理待添加的结果节点
     */
    private void processPendingResults() {
        if (pendingResults.isEmpty()) {
            return;
        }

        // 批量取出所有待处理的结果
        List<ResultNodeInfo> batch = new ArrayList<>();
        ResultNodeInfo info;
        while ((info = pendingResults.poll()) != null) {
            batch.add(info);
        }

        // 使用invokeLater确保在EDT线程中执行（如果不是从Swing Timer调用）
        if (SwingUtilities.isEventDispatchThread()) {
            // 已经在EDT线程，直接执行
            addBatchToTree(batch);
        } else {
            // 不在EDT线程，需要切换
            SwingUtilities.invokeLater(() -> addBatchToTree(batch));
        }
    }

    /**
     * 将批量结果添加到树中（必须在EDT线程调用）
     */
    private void addBatchToTree(List<ResultNodeInfo> batch) {
        if (batch.isEmpty()) {
            return;
        }

        int addedCount = 0;
        for (ResultNodeInfo nodeInfo : batch) {
            // 检查是否超过最大数量限制
            if (currentResultCount.get() >= MAX_RESULT_NODES && resultRootNode.getChildCount() > 0) {
                // 移除最旧的节点（FIFO）
                resultRootNode.remove(0);
                currentResultCount.decrementAndGet();
            }

            DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(nodeInfo);
            int insertIndex = resultRootNode.getChildCount();
            resultRootNode.insert(reqNode, insertIndex);
            currentResultCount.incrementAndGet();
            addedCount++;
        }

        // 使用增量更新而不是全量reload，性能更好
        if (addedCount > 0) {
            // 通知模型节点结构已改变
            int[] childIndices = new int[addedCount];
            int startIndex = resultRootNode.getChildCount() - addedCount;

            for (int i = 0; i < addedCount; i++) {
                childIndices[i] = startIndex + i;
            }

            resultTreeModel.nodesWereInserted(resultRootNode, childIndices);
        }

        // 显示警告信息（如果达到上限）
        if (currentResultCount.get() >= MAX_RESULT_NODES && !limitWarningShown) {
            limitWarningShown = true;
            log.warn("结果树已达到最大限制 {} 个节点，旧结果将被自动移除", MAX_RESULT_NODES);
        }
    }

    private boolean limitWarningShown = false;

    /**
     * 立即刷新所有待处理的结果（在测试结束时调用）
     */
    public void flushPendingResults() {
        batchUpdateScheduled.set(false); // 取消调度标记
        processPendingResults();
    }

    /**
     * 清空结果树
     */
    public void clearResults() {
        // 清空待处理队列
        pendingResults.clear();
        batchUpdateScheduled.set(false);
        currentResultCount.set(0);
        limitWarningShown = false;

        resultRootNode.removeAllChildren();
        resultTreeModel.reload();
        resultTree.clearSelection();
        clearDetailTabs();
    }

}