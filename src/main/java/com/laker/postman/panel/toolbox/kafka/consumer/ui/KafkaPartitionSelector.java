package com.laker.postman.panel.toolbox.kafka.consumer.ui;

import com.laker.postman.common.component.button.SecondaryButton;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class KafkaPartitionSelector extends JPanel {

    private final SecondaryButton triggerButton;
    private final JPopupMenu popupMenu;
    private final JPanel optionsPanel;
    private final JCheckBox allPartitionsCheckBox;
    private final LinkedHashMap<Integer, JCheckBox> partitionCheckBoxes = new LinkedHashMap<>();

    public KafkaPartitionSelector() {
        super(new BorderLayout());
        setOpaque(false);

        popupMenu = new JPopupMenu();
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        allPartitionsCheckBox = new JCheckBox(t(MessageKeys.TOOLBOX_KAFKA_ALL_PARTITIONS), true);
        allPartitionsCheckBox.addActionListener(e -> {
            if (allPartitionsCheckBox.isSelected()) {
                setAllPartitionChecksSelected(false);
            }
            updateTriggerText();
        });

        triggerButton = new SecondaryButton(t(MessageKeys.TOOLBOX_KAFKA_ALL_PARTITIONS));
        triggerButton.setHorizontalAlignment(SwingConstants.LEFT);
        triggerButton.addActionListener(e -> popupMenu.show(triggerButton, 0, triggerButton.getHeight()));

        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(220, 180));
        popupMenu.setLayout(new BorderLayout());
        popupMenu.add(scrollPane, BorderLayout.CENTER);

        add(triggerButton, BorderLayout.CENTER);
        rebuildOptions(Collections.emptyList());
        updateTriggerText();
    }

    public void setAvailablePartitions(Collection<Integer> partitions) {
        Set<Integer> previousSelection = getSelectedPartitions();
        List<Integer> normalized = new ArrayList<>(new LinkedHashSet<>(partitions));
        Collections.sort(normalized);
        rebuildOptions(normalized);

        if (normalized.isEmpty() || previousSelection.isEmpty()) {
            allPartitionsCheckBox.setSelected(true);
            setAllPartitionChecksSelected(false);
        } else {
            allPartitionsCheckBox.setSelected(false);
            for (var entry : partitionCheckBoxes.entrySet()) {
                entry.getValue().setSelected(previousSelection.contains(entry.getKey()));
            }
            if (getSelectedPartitions().isEmpty()) {
                allPartitionsCheckBox.setSelected(true);
            }
        }
        updateTriggerText();
    }

    public Set<Integer> getSelectedPartitions() {
        if (allPartitionsCheckBox.isSelected()) {
            return Collections.emptySet();
        }
        Set<Integer> selected = new LinkedHashSet<>();
        for (var entry : partitionCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private void rebuildOptions(List<Integer> partitions) {
        optionsPanel.removeAll();
        partitionCheckBoxes.clear();

        optionsPanel.add(allPartitionsCheckBox);
        optionsPanel.add(Box.createVerticalStrut(4));
        for (Integer partition : partitions) {
            JCheckBox checkBox = new JCheckBox(String.valueOf(partition));
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    allPartitionsCheckBox.setSelected(false);
                } else if (nonePartitionSelected()) {
                    allPartitionsCheckBox.setSelected(true);
                }
                updateTriggerText();
            });
            partitionCheckBoxes.put(partition, checkBox);
            optionsPanel.add(checkBox);
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private boolean nonePartitionSelected() {
        for (JCheckBox checkBox : partitionCheckBoxes.values()) {
            if (checkBox.isSelected()) {
                return false;
            }
        }
        return true;
    }

    private void setAllPartitionChecksSelected(boolean selected) {
        for (JCheckBox checkBox : partitionCheckBoxes.values()) {
            checkBox.setSelected(selected);
        }
    }

    private void updateTriggerText() {
        Set<Integer> selectedPartitions = getSelectedPartitions();
        if (selectedPartitions.isEmpty()) {
            triggerButton.setText(t(MessageKeys.TOOLBOX_KAFKA_ALL_PARTITIONS));
            return;
        }
        String text = selectedPartitions.stream()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse(t(MessageKeys.TOOLBOX_KAFKA_ALL_PARTITIONS));
        triggerButton.setText(text);
    }

    private static String t(String key, Object... args) {
        return I18nUtil.getMessage(key, args);
    }
}
