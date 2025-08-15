package com.laker.postman.panel.history;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.service.HistoryPersistenceManager;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.JComponentUtils;

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
    public static final String EMPTY_BODY_HTML = "<html><body>Please select a record.</body></html>";
    private JList<Object> historyList;
    private JPanel historyDetailPanel;
    private JTextPane historyDetailPane;
    private DefaultListModel<Object> historyListModel;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        JPanel titlePanel = new JPanel(new BorderLayout());
        // 复合边框
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), // 外边框
                BorderFactory.createEmptyBorder(4, 8, 4, 8) // 内边框
        ));
        JLabel title = new JLabel("History");
        title.setFont(FontUtil.getDefaultFont(Font.BOLD, 13));
        JButton clearBtn = new JButton(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setMargin(new Insets(0, 4, 0, 4));
        clearBtn.setBackground(Colors.PANEL_BACKGROUND);
        clearBtn.setBorder(BorderFactory.createEmptyBorder());
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
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String dateStr) {
                    label.setText(dateStr);
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else if (value instanceof RequestHistoryItem item) {
                    String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(item.requestTime));
                    label.setText(JComponentUtils.ellipsisText(String.format("  [%s] %s", item.method, item.url), list, 0, 50));
                    label.setToolTipText("请求时间: " + timeStr);
                    label.setFont(label.getFont().deriveFont(Font.PLAIN));
                }
                if (isSelected && value instanceof RequestHistoryItem) {
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                    label.setBackground(new Color(180, 215, 255));
                }
                return label;
            }
        });
        JScrollPane listScroll = new JScrollPane(historyList);
        listScroll.setPreferredSize(new Dimension(220, 240));
        listScroll.setMinimumSize(new Dimension(220, 240));
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // 水平滚动条不需要，内容不会超出

        // 详情区
        historyDetailPanel = new JPanel(new BorderLayout());
        historyDetailPane = new JTextPane();
        historyDetailPane.setEditable(false);
        historyDetailPane.setContentType("text/html");
        historyDetailPane.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        JScrollPane detailScroll = new JScrollPane(historyDetailPane);
        detailScroll.setPreferredSize(new Dimension(340, 240));
        detailScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        historyDetailPanel.add(detailScroll, BorderLayout.CENTER);
        historyDetailPane.setText(EMPTY_BODY_HTML);
        historyDetailPanel.setVisible(true);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, historyDetailPanel);
        split.setDividerLocation(220);
        split.setDividerSize(1);
        add(split, BorderLayout.CENTER);
        setMinimumSize(new Dimension(0, 120));

        // 加载持久化的历史记录
        loadPersistedHistory();

        SwingUtilities.invokeLater(() -> historyList.repaint());
    }

    @Override
    protected void registerListeners() {
        // 监听列表选择变化
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = historyList.getSelectedIndex();
                if (idx == -1) {
                    historyDetailPane.setText(EMPTY_BODY_HTML);
                } else {
                    Object value = historyListModel.get(idx);
                    if (value instanceof RequestHistoryItem item) {
                        historyDetailPane.setText(HttpHtmlRenderer.renderHistoryDetail(item));
                        historyDetailPane.setCaretPosition(0);
                    } else {
                        historyDetailPane.setText(EMPTY_BODY_HTML);
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
        HistoryPersistenceManager.getInstance().addHistory(req, resp, requestTime);

        // 添加到UI列表
        RequestHistoryItem item = new RequestHistoryItem(req, resp, requestTime);
        if (historyListModel != null) {
            historyListModel.add(0, item);

            // 限制UI显示的历史记录数量
            int maxCount = com.laker.postman.common.setting.SettingManager.getMaxHistoryCount();
            while (historyListModel.size() > maxCount) {
                historyListModel.remove(historyListModel.size() - 1);
            }
        }
        loadPersistedHistory(); // 重新分组刷新
    }

    private void clearRequestHistory() {
        // 清空持久化的历史记录
        HistoryPersistenceManager.getInstance().clearHistory();

        // 清空UI列表
        historyListModel.clear();
        historyDetailPane.setText(EMPTY_BODY_HTML);
        historyDetailPanel.setVisible(true);
    }

    /**
     * 加载持久化的历史记录
     */
    private void loadPersistedHistory() {
        if (historyListModel == null) {
            return;
        }
        List<RequestHistoryItem> persistedHistory = HistoryPersistenceManager.getInstance().getHistory();
        historyListModel.clear();
        // 按天分组，支持 Today/Yesterday/日期
        Map<String, List<RequestHistoryItem>> dayMap = new LinkedHashMap<>();
        SimpleDateFormat showFmt = new SimpleDateFormat("yyyy-MM-dd");
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();
        long yesterdayStart = todayStart - 24 * 60 * 60 * 1000L;
        for (RequestHistoryItem item : persistedHistory) {
            long t = item.requestTime;
            String groupLabel;
            if (t >= todayStart) {
                groupLabel = "Today";
            } else if (t >= yesterdayStart) {
                groupLabel = "Yesterday";
            } else {
                groupLabel = showFmt.format(new java.util.Date(t));
            }
            dayMap.computeIfAbsent(groupLabel, k -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<RequestHistoryItem>> entry : dayMap.entrySet()) {
            historyListModel.addElement(entry.getKey()); // 日期分组标题
            for (RequestHistoryItem item : entry.getValue()) {
                historyListModel.addElement(item);
            }
        }
    }

    /**
     * 刷新历史记录显示（当设置变更时调用）
     */
    public void refreshHistory() {
        loadPersistedHistory();
    }
}