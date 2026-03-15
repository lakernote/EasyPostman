package com.laker.postman.panel.toolbox.kafka.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class KafkaTopicPanel extends JPanel {

    private static final String SEPARATOR_FG = "Separator.foreground";

    public final SearchTextField topicSearchField;
    public final DefaultListModel<KafkaTopicItem> topicListModel;
    public final DefaultListModel<KafkaTopicItem> topicFilteredModel;
    public final JList<KafkaTopicItem> topicList;

    public KafkaTopicPanel(Runnable refreshAction, Consumer<String> topicSelectionAction) {
        super(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(230, 0));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 10, 6, 6)));
        JLabel titleLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_TOPIC_MANAGEMENT));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_TOPIC_REFRESH));
        refreshBtn.addActionListener(e -> refreshAction.run());
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(refreshBtn, BorderLayout.EAST);

        topicSearchField = new SearchTextField();
        topicSearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
        topicSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_SEARCH_PLACEHOLDER));
        topicSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBorder(new EmptyBorder(6, 8, 4, 8));
        searchBox.add(topicSearchField, BorderLayout.CENTER);

        topicListModel = new DefaultListModel<>();
        topicFilteredModel = new DefaultListModel<>();
        topicList = new JList<>(topicFilteredModel);
        topicList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof KafkaTopicItem item) {
                    label.setText(item.name() + " (" + item.partitionCount() + ")");
                }
                return label;
            }
        });
        topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            KafkaTopicItem selected = topicList.getSelectedValue();
            if (selected != null && !selected.name().isBlank()) {
                topicSelectionAction.accept(selected.name());
            }
        });
        topicList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = topicList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        topicList.setSelectedIndex(index);
                        KafkaTopicItem selected = topicList.getSelectedValue();
                        if (selected != null) {
                            topicSelectionAction.accept(selected.name());
                        }
                    }
                }
            }
        });
        JScrollPane topicScroll = new JScrollPane(topicList);
        topicScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel top = new JPanel(new BorderLayout());
        top.add(titleBar, BorderLayout.NORTH);
        top.add(searchBox, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(topicScroll, BorderLayout.CENTER);
    }

    private static String t(String key, Object... args) {
        return I18nUtil.getMessage(key, args);
    }
}
