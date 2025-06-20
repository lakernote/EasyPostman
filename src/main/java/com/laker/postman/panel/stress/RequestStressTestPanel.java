package com.laker.postman.panel.stress;

import com.laker.postman.common.AbstractBasePanel;
import com.laker.postman.common.SingletonPanelFactory;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.StressResult;
import com.laker.postman.panel.collections.RequestCollectionsSubPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.service.StressTestService;
import com.laker.postman.util.FontUtil;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 轻量级Jmeter
 */
@Slf4j
public class RequestStressTestPanel extends AbstractBasePanel {

    private JSpinner concurrencySpinner;
    private JSpinner requestCountSpinner;
    private JButton stressTestButton;
    private JButton cancelButton; // 取消按钮
    private JTextPane stressResultArea;
    private ChartPanel chartPanel;   // 图表面板
    private JFreeChart lineChart;    // 折线图对象
    private JComboBox<RequestItem> requestComboBox; // 请求选择下拉框
    private JProgressBar progressBar; // 进度条

    private HttpRequestItem currentRequestItem; // 当前选中的请求项
    private StressTestTask currentTask; // 当前压测任务

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        JPanel stressSettingPanel = new JPanel(new GridBagLayout());
        stressSettingPanel.setBorder(BorderFactory.createEmptyBorder(10, 18, 6, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 10, 4, 10);

        // 并发数
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.15;
        stressSettingPanel.add(new JLabel("并发数:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        concurrencySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        ((JSpinner.DefaultEditor) concurrencySpinner.getEditor()).getTextField().setColumns(4);
        stressSettingPanel.add(concurrencySpinner, gbc);

        // 请求次数
        gbc.gridx = 2;
        gbc.weightx = 0.15;
        stressSettingPanel.add(new JLabel("请求次数:"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.35;
        requestCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        ((JSpinner.DefaultEditor) requestCountSpinner.getEditor()).getTextField().setColumns(6);
        stressSettingPanel.add(requestCountSpinner, gbc);

        // 请求选择下拉框
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.15;
        stressSettingPanel.add(new JLabel("压测请求:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.85;
        requestComboBox = new JComboBox<>();
        requestComboBox.setRenderer(new RequestItemRenderer());
        requestComboBox.setPreferredSize(new Dimension(260, 28));
        requestComboBox.addActionListener(e -> {
            RequestItem selected = (RequestItem) requestComboBox.getSelectedItem();
            if (selected != null) {
                currentRequestItem = selected.getRequestItem();
            }
        });
        stressSettingPanel.add(requestComboBox, gbc);
        gbc.gridwidth = 1;

        // 按钮面板
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel leftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JPanel rightBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        leftBtnPanel.setOpaque(false);
        rightBtnPanel.setOpaque(false);
        JButton refreshButton = new JButton("刷新请求");
        refreshButton.setIcon(IconFontSwing.buildIcon(FontAwesome.REFRESH, 14, Color.BLUE));
        refreshButton.addActionListener(e -> loadRequestsFromCollection());
        leftBtnPanel.add(refreshButton);
        stressTestButton = new JButton("Start");
        stressTestButton.setIcon(IconFontSwing.buildIcon(FontAwesome.BOLT, 14, Color.ORANGE));
        stressTestButton.addActionListener(this::stressTest);
        leftBtnPanel.add(stressTestButton);
        // 取消按钮
        cancelButton = new JButton("Cancel");
        cancelButton.setIcon(IconFontSwing.buildIcon(FontAwesome.TIMES, 14, Color.RED));
        cancelButton.setEnabled(false);
        cancelButton.setVisible(false);
        cancelButton.addActionListener(e -> {
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(true);
            }
        });
        leftBtnPanel.add(cancelButton);
        JButton resetStressSettingsButton = new JButton("Reset");
        resetStressSettingsButton.setIcon(IconFontSwing.buildIcon(FontAwesome.ERASER, 14, Color.GRAY));
        resetStressSettingsButton.addActionListener(e -> {
            concurrencySpinner.setValue(1);
            requestCountSpinner.setValue(1);
        });
        rightBtnPanel.add(resetStressSettingsButton);
        buttonPanel.add(leftBtnPanel, BorderLayout.WEST);
        buttonPanel.add(rightBtnPanel, BorderLayout.EAST);
        stressSettingPanel.add(buttonPanel, gbc);
        gbc.gridwidth = 1;

        // 进度条
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        stressSettingPanel.add(progressBar, gbc);
        gbc.gridwidth = 1;

        add(stressSettingPanel, BorderLayout.NORTH);

        // ===== 图表与结果分割区 =====
        JPanel chartPanelWrapper = new JPanel(new BorderLayout());
        chartPanelWrapper.setBorder(BorderFactory.createTitledBorder("响应时间趋势"));
        createLineChart();
        chartPanelWrapper.add(chartPanel, BorderLayout.CENTER);

        // 结果区美化
        stressResultArea = new JTextPane();
        stressResultArea.setEditable(false);
        stressResultArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Result"),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JScrollPane stressScroll = new JScrollPane(stressResultArea);
        stressScroll.setBorder(BorderFactory.createEmptyBorder());

        // 分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanelWrapper, stressScroll);
        splitPane.setResizeWeight(0.4); // 图表占40%
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(2);
        splitPane.setDividerLocation(300); // 初始位置
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        add(splitPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::loadRequestsFromCollection);
    }

    @Override
    protected void registerListeners() {

    }

    /**
     * 从请求集合中加载请求到下拉框
     */
    public void loadRequestsFromCollection() {
        requestComboBox.removeAllItems();

        // 遍历 window 中所有的请求集合面板，找到请求并添加到下拉框
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            RequestCollectionsSubPanel collectionPanel = findRequestCollectionPanel(window);
            if (collectionPanel != null) {
                addRequestsFromCollectionPanel(collectionPanel);
            } else {
                addRequestsFromCollectionPanel(SingletonPanelFactory.getInstance(RequestCollectionsSubPanel.class));
            }
        }

        if (requestComboBox.getItemCount() > 0) {
            requestComboBox.setSelectedIndex(0);
            currentRequestItem = ((RequestItem) Objects.requireNonNull(requestComboBox.getSelectedItem())).getRequestItem();
        }
    }

    private RequestCollectionsSubPanel findRequestCollectionPanel(Container container) {
        if (container instanceof RequestCollectionsSubPanel) {
            return (RequestCollectionsSubPanel) container;
        }

        for (Component comp : container.getComponents()) {
            if (comp instanceof RequestCollectionsSubPanel) {
                return (RequestCollectionsSubPanel) comp;
            } else if (comp instanceof Container) {
                RequestCollectionsSubPanel panel = findRequestCollectionPanel((Container) comp);
                if (panel != null) {
                    return panel;
                }
            }
        }

        return null;
    }

    private void addRequestsFromCollectionPanel(RequestCollectionsSubPanel collectionPanel) {
        // 获取集合面板中的树
        JTree tree = null;
        for (Component comp : collectionPanel.getComponents()) {
            if (comp instanceof JScrollPane) {
                Component view = ((JScrollPane) comp).getViewport().getView();
                if (view instanceof JTree) {
                    tree = (JTree) view;
                    break;
                }
            }
        }

        if (tree != null) {
            TreeModel model = tree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            addRequestsFromNode(root, "");
        }
    }

    private void addRequestsFromNode(DefaultMutableTreeNode node, String path) {
        if (node == null) return;

        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj) {
            if ("group".equals(obj[0])) {
                String groupName = (String) obj[1];
                String newPath = path.isEmpty() ? groupName : path + " > " + groupName;

                for (int i = 0; i < node.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                    addRequestsFromNode(child, newPath);
                }
            } else if ("request".equals(obj[0]) && obj[1] instanceof HttpRequestItem item) {
                String displayName = path + " > " + item.getName();
                requestComboBox.addItem(new RequestItem(displayName, item));
            }
        }

        // 递归处理子节点（适用于根节点）
        if (node.getUserObject() instanceof String) {
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                addRequestsFromNode(child, path);
            }
        }
    }

    private void createLineChart() {
        // 初始化空图表
        lineChart = ChartFactory.createLineChart(
                "请求响应时间", // 图表标题
                "请求", // X轴标签
                "响应时间 (ms)", // Y轴标签
                null, // 初始数据集为空
                PlotOrientation.VERTICAL, // 图表方向
                true, // 是否显示图例
                false, // 是否生成提示工具
                false // 是否生成URL链接
        );
        // 获取 plot 设置线条样式
        CategoryPlot plot = lineChart.getCategoryPlot();
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();

        // 设置第一条线的样式 粗细线条
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        // 设置第一条线的颜色
        renderer.setSeriesPaint(0, Color.BLUE);
        // 显示第一条线的数据标签
        renderer.setSeriesItemLabelsVisible(0, Boolean.TRUE);
        // 设置第一条的数值标签 设置标签生成器（显示具体数值）
        renderer.setSeriesItemLabelGenerator(0, new StandardCategoryItemLabelGenerator());
        // 设置第一条线的数据标签位置
        renderer.setSeriesPositiveItemLabelPosition(0, new ItemLabelPosition(
                ItemLabelAnchor.OUTSIDE10, // 标签在点上方稍远
                TextAnchor.BOTTOM_CENTER   // 居中对齐
        ));

        // 显示图例
        renderer.setDefaultSeriesVisibleInLegend(true);

        // 设置字体，防止中文乱码
        Font font = FontUtil.getDefaultFont(Font.PLAIN, 12);
        lineChart.getTitle().setFont(font.deriveFont(13f)); // 设置标题字体
        lineChart.getLegend().setItemFont(font); // 设置图例字体
        plot.getDomainAxis().setTickLabelFont(font); // 设置X轴标签字体
        plot.getDomainAxis().setLabelFont(font); // 设置X轴标签字体
        plot.getRangeAxis().setTickLabelFont(font);  // 设置Y轴标签字体
        plot.getRangeAxis().setLabelFont(font);  //

        chartPanel = new ChartPanel(lineChart);
        chartPanel.setMouseWheelEnabled(true); // 支持鼠标滚轮缩放
        chartPanel.setBackground(Colors.PANEL_BACKGROUND);
        chartPanel.setDisplayToolTips(true); // 显示工具提示
        chartPanel.setPreferredSize(new Dimension(300, 300));
    }

    private void upperChartYBound() {
        // 动态调整Y轴最大值，避免遮挡
        CategoryDataset dataset = lineChart.getCategoryPlot().getDataset();
        double max = 0;
        for (int i = 0; i < dataset.getColumnCount(); i++) {
            Number value = dataset.getValue(0, i);
            if (value != null && value.doubleValue() > max) {
                max = value.doubleValue();
            }
        }
        if (max > 0) {
            CategoryPlot plot = lineChart.getCategoryPlot();
            plot.getRangeAxis().setUpperBound(max * 1.15); // 最大值上浮15%
        }
    }

    private void setStyledResult(String resultText) {
        // 关键数据高亮：数字、百分比、ms等
        StyledDocument doc = stressResultArea.getStyledDocument();
        doc.removeUndoableEditListener(e -> {
        }); // 防止多余监听
        doc.setLogicalStyle(0, null);
        doc.setParagraphAttributes(0, doc.getLength(), new SimpleAttributeSet(), true);
        try {
            doc.remove(0, doc.getLength());
            String[] lines = resultText.split("\\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    int idx = line.indexOf(":");
                    String key = line.substring(0, idx + 1);
                    String value = line.substring(idx + 1);
                    // key样式
                    SimpleAttributeSet keyAttr = new SimpleAttributeSet();
                    StyleConstants.setBold(keyAttr, true);
                    doc.insertString(doc.getLength(), key, keyAttr);
                    // value样式
                    SimpleAttributeSet valueAttr = new SimpleAttributeSet();
                    if (line.contains("错误") && !line.contains("率")) {
                        StyleConstants.setForeground(valueAttr, Color.RED);
                        StyleConstants.setBold(valueAttr, true);
                    } else if (line.contains("错误率")) {
                        StyleConstants.setForeground(valueAttr, new Color(255, 140, 0));
                        StyleConstants.setBold(valueAttr, true);
                    } else if (line.contains("平均") || line.contains("最小") || line.contains("最大")) {
                        StyleConstants.setForeground(valueAttr, new Color(0, 102, 204));
                        StyleConstants.setBold(valueAttr, true);
                    } else {
                        StyleConstants.setForeground(valueAttr, new Color(0, 128, 0));
                    }
                    doc.insertString(doc.getLength(), value + "\n", valueAttr);
                } else {
                    doc.insertString(doc.getLength(), line + "\n", null);
                }
            }
        } catch (BadLocationException e) {
            // fallback
            stressResultArea.setText(resultText);
        }
    }

    /**
     * 执行压力测试
     */
    private void stressTest(ActionEvent e) {
        stressTestButton.setEnabled(false); // 禁用按钮防止重复点击
        cancelButton.setEnabled(true);
        cancelButton.setVisible(true);
        int concurrency = (Integer) concurrencySpinner.getValue();
        int requestCount = (Integer) requestCountSpinner.getValue();

        if (concurrency <= 0 || requestCount <= 0) {
            JOptionPane.showMessageDialog(this, "请输入有效的并发数和请求次数", "错误", JOptionPane.ERROR_MESSAGE);
            stressTestButton.setEnabled(true);
            cancelButton.setEnabled(false);
            cancelButton.setVisible(false);
            return;
        }

        // 检查是否有选中的请求
        if (currentRequestItem == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个请求", "错误", JOptionPane.ERROR_MESSAGE);
            stressTestButton.setEnabled(true);
            cancelButton.setEnabled(false);
            cancelButton.setVisible(false);
            return;
        }

        currentTask = new StressTestTask(concurrency, requestCount);
        currentTask.execute();
    }

    /**
     * 压力测试任务类（SwingWorker 子类）
     */
    private class StressTestTask extends SwingWorker<Void, Integer> {
        private final int concurrency;
        private final int requestCount;
        private final StringBuilder stressResult = new StringBuilder();

        public StressTestTask(int concurrency, int requestCount) {
            this.concurrency = concurrency;
            this.requestCount = requestCount;
        }

        @Override
        protected Void doInBackground() {
            try {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(0);
                    progressBar.setVisible(true);
                });
                HttpRequestItem item = currentRequestItem;
                StressResult result = StressTestService.stressTest(item, concurrency, requestCount, (completed) -> {
                    if (isCancelled()) return;
                    int percent = (int) ((completed * 100.0) / requestCount);
                    publish(percent);
                }, (reqItem, resp) -> {
                    // 每个请求完成时写入历史
                    SwingUtilities.invokeLater(() -> {
                        try {
                            String method = reqItem.getMethod();
                            String url = reqItem.getUrl();
                            String requestBody = reqItem.getBody();
                            String requestHeaders = reqItem.getHeaders() != null ? reqItem.getHeaders().toString() : "";
                            String responseStatus = resp != null ? String.valueOf(resp.code) : "ERROR";
                            String responseHeaders = resp != null && resp.headers != null ? resp.headers.toString() : "";
                            String responseBody = resp != null ? resp.body : "";
                            String connectionInfo = resp != null ? resp.connectionInfo : null;
                            HistoryPanel historyPanel =
                                    SingletonPanelFactory.getInstance(HistoryPanel.class);
                            historyPanel.addRequestHistory(method, url, requestBody, requestHeaders, responseStatus, responseHeaders, responseBody, "", resp != null ? resp.threadName : null, connectionInfo);
                        } catch (Exception ex) {
                            log.error("保存单次压测请求到历史失败", ex);
                        }
                    });
                });
                if (isCancelled()) {
                    stressResult.append("压测已取消\n");
                    return null;
                }
                List<Long> times = result.times;
                int errorCount = result.errorCount;
                Map<Integer, Long> timeData = result.timeData;
                long total = times.stream().mapToLong(Long::longValue).sum();
                long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
                long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
                double avg = times.isEmpty() ? 0 : (double) total / times.size();
                double errorRate = (double) errorCount / requestCount * 100;
                // TP90/TP99
                double tp90 = 0, tp99 = 0;
                if (!times.isEmpty()) {
                    List<Long> sorted = new ArrayList<>(times);
                    sorted.sort(Long::compareTo);
                    int idx90 = Math.max(0, (int) (sorted.size() * 0.9) - 1);
                    int idx99 = Math.max(0, (int) (sorted.size() * 0.99) - 1);
                    tp90 = sorted.get(idx90);
                    tp99 = sorted.get(idx99);
                }
                // QPS
                double qps = 0;
                if (!times.isEmpty()) {
                    long duration = result.totalDuration > 0 ? result.totalDuration : (max * times.size());
                    qps = duration > 0 ? (requestCount * 1000.0 / duration) : 0;
                }
                stressResult.append("压测结果:\n")
                        .append("请求: ").append(((RequestItem) Objects.requireNonNull(requestComboBox.getSelectedItem())).getDisplayName()).append("\n")
                        .append("并发数: ").append(concurrency).append("\n")
                        .append("请求次数: ").append(requestCount).append("\n")
                        .append("错误次数: ").append(errorCount).append("\n")
                        .append("错误率: ").append(String.format("%.0f", errorRate)).append("%\n")
                        .append(String.format("平均响应时间: %.0f ms\n", avg))
                        .append("最小响应时间: ").append(min).append(" ms\n")
                        .append("最大响应时间: ").append(max).append(" ms\n")
                        .append(String.format("TP90: %.0f ms\n", tp90))
                        .append(String.format("TP99: %.0f ms\n", tp99))
                        .append(String.format("QPS: %.2f\n", qps));

                // 更新图表
                // 统计和展示时排序
                List<Map.Entry<Integer, Long>> sortedTimeData = new ArrayList<>(timeData.entrySet());
                sortedTimeData.sort(Map.Entry.comparingByKey());

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (Map.Entry<Integer, Long> entry : sortedTimeData) {
                    dataset.addValue(entry.getValue(), "响应时间", String.valueOf(entry.getKey()));
                }

                // 更新图表数据集
                SwingUtilities.invokeLater(() -> {
                    lineChart.getCategoryPlot().setDataset(dataset);
                    upperChartYBound(); // 调整Y轴上限
                });

            } catch (Exception ex) {
                stressResult.append("压测出错: ").append(ex.getMessage());
                log.error("压测失败", ex);
            } finally {
                stressTestButton.setEnabled(true);
                cancelButton.setEnabled(false);
                cancelButton.setVisible(false);
                SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
            }

            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            if (!chunks.isEmpty()) {
                int value = chunks.get(chunks.size() - 1);
                progressBar.setValue(value);
            }
        }

        @Override
        protected void done() {
            setStyledResult(stressResult.toString());
            // 自动滚动到底部
            stressResultArea.setCaretPosition(stressResultArea.getDocument().getLength());
        }
    }

    /**
     * 请求项封装类，用于在下拉框中显示
     */
    static class RequestItem {
        private final String displayName;
        private final HttpRequestItem requestItem;

        public RequestItem(String displayName, HttpRequestItem requestItem) {
            this.displayName = displayName;
            this.requestItem = requestItem;
        }

        public String getDisplayName() {
            return displayName;
        }

        public HttpRequestItem getRequestItem() {
            return requestItem;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * 请求项渲染器，用于在下拉框中显示带图标的请求项
     */
    static class RequestItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof RequestItem) {
                label.setIcon(IconFontSwing.buildIcon(FontAwesome.CODEPEN, 16, new Color(0, 180, 136)));
            }
            return label;
        }
    }
}

