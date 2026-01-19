package com.laker.postman.panel.history;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.service.HistoryPersistenceService;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * 历史记录面板
 */
public class HistoryPanel extends SingletonBasePanel {
    private JList<Object> historyList;
    private JPanel historyDetailPanel;
    private JPanel titlePanel; // 标题面板，用于主题切换时更新边框
    private JTextPane requestPane;
    private JTextPane responsePane;
    private JTextPane timingPane;
    private JTextPane eventPane;
    private DefaultListModel<Object> historyListModel;

    // 缓存当前选中的项，避免重复渲染
    private RequestHistoryItem currentSelectedItem = null;
    // 标记是否正在更新，避免递归
    private volatile boolean isUpdating = false;
    // 缓存日期格式化器
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    // 缓存今天和昨天的时间戳
    private long todayStartCache = 0;
    private long yesterdayStartCache = 0;

    @Override
    protected void initUI() {
        JTabbedPane historyDetailTabPane;
        setLayout(new BorderLayout());
        titlePanel = new JPanel(new BorderLayout());
        // 复合边框
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()), // 外边框
                BorderFactory.createEmptyBorder(4, 8, 4, 8) // 内边框
        ));
        JLabel title = new JLabel(I18nUtil.getMessage(MessageKeys.MENU_HISTORY));
        title.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1));
        JButton clearBtn = new JButton(IconUtil.createThemed("icons/clear.svg", 24, 24));
        clearBtn.setMargin(new Insets(0, 4, 0, 4));
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> clearRequestHistory());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(btnPanel, BorderLayout.EAST);
        add(titlePanel, BorderLayout.PAGE_START);

        // 历史列表
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);// 优化：设置固定行高，提升渲染性能
        historyList.setFixedCellHeight(24);
        // 使用缓存的 Renderer
        historyList.setCellRenderer(new OptimizedHistoryListCellRenderer());
        JScrollPane listScroll = new JScrollPane(historyList);
        listScroll.setPreferredSize(new Dimension(220, 240));
        listScroll.setMinimumSize(new Dimension(220, 240));
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // 水平滚动条不需要，内容不会超出

        // 详情区 - 改为 Tab 形式
        historyDetailPanel = new JPanel(new BorderLayout());

        // 创建 Tab 面板
        historyDetailTabPane = new JTabbedPane();
        historyDetailTabPane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // 创建各个标签页
        requestPane = createDetailPane();
        responsePane = createDetailPane();
        timingPane = createDetailPane();
        eventPane = createDetailPane();

        // 添加标签页
        historyDetailTabPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST), new JScrollPane(requestPane));
        historyDetailTabPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_RESPONSE), new JScrollPane(responsePane));
        historyDetailTabPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_TIMING), new JScrollPane(timingPane));
        historyDetailTabPane.addTab(I18nUtil.getMessage(MessageKeys.TAB_EVENTS), new JScrollPane(eventPane));

        // 设置滚动策略
        for (int i = 0; i < historyDetailTabPane.getTabCount(); i++) {
            JScrollPane scrollPane = (JScrollPane) historyDetailTabPane.getComponentAt(i);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        historyDetailPanel.add(historyDetailTabPane, BorderLayout.CENTER);

        // 初始化显示空内容
        clearDetailPanes();
        historyDetailPanel.setVisible(true);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, historyDetailPanel);
        split.setDividerLocation(220);
        split.setDividerSize(3);
        add(split, BorderLayout.CENTER);
        setMinimumSize(new Dimension(0, 120));

        // 异步加载持久化的历史记录，避免阻塞UI
        SwingUtilities.invokeLater(this::loadPersistedHistory);
    }

    /**
     * 优化的列表单元格渲染器 - 极简版，最大化性能
     */
    private static class OptimizedHistoryListCellRenderer extends DefaultListCellRenderer {
        private final Font boldFont;
        private final Font plainFont;
        private final Color selectedBackground = new Color(180, 215, 255);

        public OptimizedHistoryListCellRenderer() {
            Font baseFont = FontsUtil.getDefaultFont(Font.PLAIN);
            boldFont = baseFont.deriveFont(Font.BOLD);
            plainFont = baseFont.deriveFont(Font.PLAIN);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof String dateStr) {
                label.setText(dateStr);
                label.setFont(boldFont);
            } else if (value instanceof RequestHistoryItem item) {
                // 直接显示，不做任何截断处理
                label.setText(String.format(" [%s] %s", item.method, item.url));
                label.setFont(isSelected ? boldFont : plainFont);

                if (isSelected) {
                    label.setBackground(selectedBackground);
                }
            }
            return label;
        }
    }

    /**
     * 创建详情面板
     */
    private JTextPane createDetailPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        return pane;
    }

    /**
     * 清空所有详情面板
     */
    private void clearDetailPanes() {
        String EMPTY_BODY_HTML = I18nUtil.getMessage(MessageKeys.HISTORY_EMPTY_BODY);
        requestPane.setText(EMPTY_BODY_HTML);
        responsePane.setText(EMPTY_BODY_HTML);
        timingPane.setText(EMPTY_BODY_HTML);
        eventPane.setText(EMPTY_BODY_HTML);
        currentSelectedItem = null;
    }

    /**
     * 更新详情面板内容（优化：延迟渲染，避免卡顿）
     */
    private void updateDetailPanes(RequestHistoryItem item) {
        if (item == null) {
            clearDetailPanes();
            return;
        }

        // 如果已经是当前选中的项，不重复渲染
        if (item == currentSelectedItem) {
            return;
        }

        currentSelectedItem = item;

        // 优化：在后台线程渲染HTML，避免阻塞UI
        SwingWorker<Map<String, String>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, String> doInBackground() {
                Map<String, String> htmlMap = new HashMap<>();
                try {
                    htmlMap.put("request", HttpHtmlRenderer.renderRequest(item.request));
                    htmlMap.put("response", HttpHtmlRenderer.renderResponse(item.response));
                    htmlMap.put("timing", HttpHtmlRenderer.renderTimingInfo(item.response));
                    htmlMap.put("event", HttpHtmlRenderer.renderEventInfo(item.response));
                } catch (Exception e) {
                    String errorHtml = "<html><body style='font-family:monospace;font-size:9px;'>" +
                            "<div style='color:#d32f2f;'>渲染详情时出错: " + e.getMessage() + "</div>" +
                            "</body></html>";
                    htmlMap.put("error", errorHtml);
                }
                return htmlMap;
            }

            @Override
            protected void done() {
                try {
                    Map<String, String> htmlMap = get();
                    if (htmlMap.containsKey("error")) {
                        String errorHtml = htmlMap.get("error");
                        requestPane.setText(errorHtml);
                        responsePane.setText(errorHtml);
                        timingPane.setText(errorHtml);
                        eventPane.setText(errorHtml);
                    } else {
                        requestPane.setText(htmlMap.get("request"));
                        requestPane.setCaretPosition(0);

                        responsePane.setText(htmlMap.get("response"));
                        responsePane.setCaretPosition(0);

                        timingPane.setText(htmlMap.get("timing"));
                        timingPane.setCaretPosition(0);

                        eventPane.setText(htmlMap.get("event"));
                        eventPane.setCaretPosition(0);
                    }
                } catch (Exception e) {
                    // Ignore if cancelled or interrupted
                }
            }
        };
        worker.execute();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换时重新设置边框，确保分隔线颜色更新
        if (titlePanel != null) {
            titlePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }
    }

    @Override
    protected void registerListeners() {
        // 监听列表选择变化
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isUpdating) {
                int idx = historyList.getSelectedIndex();
                if (idx == -1) {
                    clearDetailPanes();
                } else {
                    Object value = historyListModel.get(idx);
                    if (value instanceof RequestHistoryItem item) {
                        updateDetailPanes(item);
                    } else {
                        clearDetailPanes();
                    }
                }
            }
        });

        // 双击选中列表项
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx != -1) {
                        historyList.setSelectedIndex(idx);
                    }
                }
            }
        });
    }

    public void addRequestHistory(PreparedRequest req, HttpResponse resp) {
        long requestTime = System.currentTimeMillis();
        // 添加到持久化管理器
        BeanFactory.getBean(HistoryPersistenceService.class).addHistory(req, resp, requestTime);

        // 优化：增量更新UI，而不是完全重新加载
        RequestHistoryItem newItem = new RequestHistoryItem(req, resp, requestTime);
        if (historyListModel != null) {
            isUpdating = true;
            try {
                // 检查是否需要添加新的日期分组
                String groupLabel = getDateGroupLabel(requestTime);
                int insertIndex = 0;

                // 如果列表为空或第一个元素不是今天的分组，需要添加分组标题
                if (historyListModel.isEmpty() || !historyListModel.get(0).equals(groupLabel)) {
                    historyListModel.add(0, groupLabel);
                    insertIndex = 1;
                } else {
                    insertIndex = 1; // 插入到分组标题后
                }

                historyListModel.add(insertIndex, newItem);

                // 限制UI显示的历史记录数量
                int maxCount = SettingManager.getMaxHistoryCount();
                int itemCount = 0;
                for (int i = 0; i < historyListModel.size(); i++) {
                    if (historyListModel.get(i) instanceof RequestHistoryItem) {
                        itemCount++;
                    }
                }

                // 如果超过最大数量，移除最旧的记录
                while (itemCount > maxCount) {
                    for (int i = historyListModel.size() - 1; i >= 0; i--) {
                        if (historyListModel.get(i) instanceof RequestHistoryItem) {
                            historyListModel.remove(i);
                            itemCount--;
                            // 检查是否需要移除空的日期分组
                            if (i > 0 && i == historyListModel.size() &&
                                    historyListModel.get(i - 1) instanceof String) {
                                historyListModel.remove(i - 1);
                            }
                            break;
                        }
                    }
                }
            } finally {
                isUpdating = false;
            }
        }
    }

    /**
     * 获取日期分组标签（带缓存优化）
     */
    private String getDateGroupLabel(long timestamp) {
        updateDateCache();

        if (timestamp >= todayStartCache) {
            return I18nUtil.getMessage(MessageKeys.HISTORY_TODAY);
        } else if (timestamp >= yesterdayStartCache) {
            return I18nUtil.getMessage(MessageKeys.HISTORY_YESTERDAY);
        } else {
            return dateFormatter.format(new Date(timestamp));
        }
    }

    /**
     * 更新日期缓存（每小时更新一次）
     */
    private void updateDateCache() {
        long now = System.currentTimeMillis();
        // 如果缓存的今天时间戳是0或者已经过了今天，重新计算
        if (todayStartCache == 0 || now >= todayStartCache + 24 * 60 * 60 * 1000L) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            todayStartCache = today.getTimeInMillis();
            yesterdayStartCache = todayStartCache - 24 * 60 * 60 * 1000L;
        }
    }

    private void clearRequestHistory() {
        // 清空持久化的历史记录
        BeanFactory.getBean(HistoryPersistenceService.class).clearHistory();

        // 清空UI列表
        isUpdating = true;
        try {
            historyListModel.clear();
            clearDetailPanes();
        } finally {
            isUpdating = false;
        }
        historyDetailPanel.setVisible(true);
    }

    /**
     * 加载持久化的历史记录（优化：异步加载）
     */
    private void loadPersistedHistory() {
        if (historyListModel == null) {
            return;
        }

        // 在后台线程加载和分组数据
        SwingWorker<List<Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object> doInBackground() {
                List<RequestHistoryItem> persistedHistory = BeanFactory.getBean(HistoryPersistenceService.class).getHistory();
                List<Object> result = new ArrayList<>();

                // 按天分组
                Map<String, List<RequestHistoryItem>> dayMap = new LinkedHashMap<>();
                updateDateCache();

                for (RequestHistoryItem item : persistedHistory) {
                    String groupLabel = getDateGroupLabel(item.requestTime);
                    dayMap.computeIfAbsent(groupLabel, k -> new ArrayList<>()).add(item);
                }

                // 构建显示列表
                for (Map.Entry<String, List<RequestHistoryItem>> entry : dayMap.entrySet()) {
                    result.add(entry.getKey()); // 日期分组标题
                    result.addAll(entry.getValue());
                }

                return result;
            }

            @Override
            protected void done() {
                try {
                    List<Object> items = get();
                    isUpdating = true;
                    try {
                        historyListModel.clear();
                        for (Object item : items) {
                            historyListModel.addElement(item);
                        }
                    } finally {
                        isUpdating = false;
                    }
                } catch (Exception e) {
                    // Ignore if cancelled or interrupted
                }
            }
        };
        worker.execute();
    }

    /**
     * 刷新历史记录显示（当设置变更时调用）
     */
    public void refreshHistory() {
        loadPersistedHistory();
    }
}
