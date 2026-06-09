package com.laker.postman.plugin.kafka.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.plugin.kafka.MessageKeys;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowSidebarHeader;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.RefreshButton;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;

public class KafkaTopicPanel extends JPanel {

    public final SearchTextField topicSearchField;
    public final DefaultListModel<KafkaTopicItem> topicListModel;
    public final DefaultListModel<KafkaTopicItem> topicFilteredModel;
    public final JList<KafkaTopicItem> topicList;

    public KafkaTopicPanel(Runnable refreshAction, Consumer<String> topicSelectionAction) {
        super(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyCard(this);
        setPreferredSize(new Dimension(230, 0));

        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_TOPIC_REFRESH));
        refreshBtn.addActionListener(e -> refreshAction.run());
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                t(MessageKeys.TOOLBOX_KAFKA_TOPIC_MANAGEMENT), refreshBtn);

        topicSearchField = new SearchTextField();
        topicSearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
        topicSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_SEARCH_PLACEHOLDER));
        topicSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

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
        ToolWindowSurfaceStyle.applyListScrollPaneCard(topicScroll, topicList);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleBar, BorderLayout.NORTH);
        top.add(new ToolWindowSidebarToolbar(null, topicSearchField), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(topicScroll, BorderLayout.CENTER);
    }
}
