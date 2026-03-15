package com.laker.postman.panel.toolbox.kafka.shared.ui;

import com.laker.postman.common.component.PlaceholderTextArea;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class KafkaPropertiesEditorPanel extends JPanel {

    private final PlaceholderTextArea textArea;

    public KafkaPropertiesEditorPanel(String title, String hint, String placeholder, Color separatorColor, Color hintColor) {
        super(new BorderLayout(0, 0));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, separatorColor));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(6, 10, 4, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 10f));
        hintLabel.setForeground(hintColor);

        JPanel labels = new JPanel(new BorderLayout(0, 2));
        labels.setOpaque(false);
        labels.add(titleLabel, BorderLayout.NORTH);
        labels.add(hintLabel, BorderLayout.CENTER);
        header.add(labels, BorderLayout.CENTER);

        textArea = new PlaceholderTextArea(4, 0);
        textArea.setLineWrap(false);
        textArea.setBackground(ModernColors.getInputBackgroundColor());
        textArea.setPlaceholder(placeholder);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public String getValue() {
        return textArea.getText();
    }
}
