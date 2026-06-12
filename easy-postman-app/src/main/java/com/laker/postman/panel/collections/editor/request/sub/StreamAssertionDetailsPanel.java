package com.laker.postman.panel.collections.editor.request.sub;

import cn.hutool.core.collection.CollUtil;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.CloseButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

final class StreamAssertionDetailsPanel extends JPanel {
    private static final int COLUMN_RESULT = 0;
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_MESSAGE = 2;

    private final JTable resultTable;
    private final StreamMessageTableModel<TestResult> resultModel;
    private final JLabel titleLabel;
    private final JLabel metaLabel;
    private String renderedText = "";
    private Runnable visibilityChangeListener = () -> {
    };

    StreamAssertionDetailsPanel() {
        super(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applySectionCard(this);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getBorderLightColor()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        setPreferredSize(new Dimension(0, 220));
        setVisible(false);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        metaLabel = new JLabel();
        metaLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        metaLabel.setForeground(ModernColors.getTextSecondary());
        titlePanel.add(titleLabel);
        titlePanel.add(metaLabel);
        CloseButton closeButton = new CloseButton();
        closeButton.addActionListener(e -> hideDetails());
        header.add(titlePanel, BorderLayout.WEST);
        header.add(closeButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_RESULT),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_NAME),
                I18nUtil.getMessage(MessageKeys.STREAM_ASSERTION_COLUMN_MESSAGE)
        };
        resultModel = new StreamMessageTableModel<>(columnNames, this::resultValueAt);
        resultTable = new JTable(resultModel);
        resultTable.setRowHeight(26);
        resultTable.setFillsViewportHeight(true);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.setGridColor(ModernColors.getTableGridColor());
        resultTable.setShowGrid(true);
        resultTable.setBorder(BorderFactory.createEmptyBorder());
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                component.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
                return component;
            }
        });
        resultTable.getColumnModel().getColumn(COLUMN_RESULT).setMinWidth(90);
        resultTable.getColumnModel().getColumn(COLUMN_RESULT).setPreferredWidth(110);
        resultTable.getColumnModel().getColumn(COLUMN_RESULT).setMaxWidth(140);
        resultTable.getColumnModel().getColumn(COLUMN_RESULT).setCellRenderer(new ResultCellRenderer());
        resultTable.getColumnModel().getColumn(COLUMN_NAME).setMinWidth(140);
        resultTable.getColumnModel().getColumn(COLUMN_NAME).setPreferredWidth(260);
        resultTable.getColumnModel().getColumn(COLUMN_MESSAGE).setMinWidth(180);
        resultTable.getColumnModel().getColumn(COLUMN_MESSAGE).setPreferredWidth(420);
        JTableHeader tableHeader = resultTable.getTableHeader();
        tableHeader.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));

        JScrollPane scrollPane = new JScrollPane(resultTable);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, resultTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    void showResults(List<TestResult> testResults) {
        showResults(testResults, StreamAssertionSummary.from(testResults), "", "");
    }

    void showResults(List<TestResult> testResults, StreamAssertionSummary summary, String typeDisplay, String time) {
        if (CollUtil.isEmpty(testResults)) {
            hideDetails();
            return;
        }
        if (summary != null) {
            titleLabel.setText(I18nUtil.getMessage(
                    MessageKeys.STREAM_ASSERTION_DETAILS_TITLE,
                    summary.passedCount(),
                    summary.totalCount()
            ));
        } else {
            titleLabel.setText(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION));
        }
        metaLabel.setText(I18nUtil.getMessage(MessageKeys.STREAM_ASSERTION_DETAILS_META, typeDisplay, time));
        renderedText = renderPlainText(testResults);
        resultModel.setRows(testResults);
        setVisible(true);
        visibilityChangeListener.run();
        revalidate();
        repaint();
    }

    void hideDetails() {
        renderedText = "";
        titleLabel.setText(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION));
        metaLabel.setText("");
        resultModel.clear();
        setVisible(false);
        visibilityChangeListener.run();
        revalidate();
        repaint();
    }

    String getRenderedHtml() {
        return renderedText;
    }

    int getResultRowCount() {
        return resultModel.getRowCount();
    }

    String getResultNameAt(int row) {
        return resultModel.getRow(row).name;
    }

    void setVisibilityChangeListener(Runnable visibilityChangeListener) {
        this.visibilityChangeListener = visibilityChangeListener == null ? () -> {
        } : visibilityChangeListener;
    }

    String getTitleText() {
        return titleLabel.getText();
    }

    String getMetaText() {
        return metaLabel.getText();
    }

    private Object resultValueAt(TestResult testResult, int column) {
        if (testResult == null) {
            return "";
        }
        return switch (column) {
            case COLUMN_RESULT -> testResult.passed;
            case COLUMN_NAME -> nullToEmpty(testResult.name);
            case COLUMN_MESSAGE -> nullToEmpty(testResult.message);
            default -> "";
        };
    }

    private static String renderPlainText(List<TestResult> testResults) {
        StringBuilder builder = new StringBuilder();
        for (TestResult testResult : testResults) {
            if (testResult == null) {
                continue;
            }
            builder.append(testResult.passed ? "PASS" : "FAIL")
                    .append(" ")
                    .append(nullToEmpty(testResult.name));
            if (testResult.message != null && !testResult.message.isBlank()) {
                builder.append(" - ").append(testResult.message);
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class ResultCellRenderer extends DefaultTableCellRenderer {
        private final Icon passedIcon = IconUtil.create("icons/pass.svg", 16, 16);
        private final Icon failedIcon = IconUtil.create("icons/fail.svg", 16, 16);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            boolean passed = Boolean.TRUE.equals(value);
            setText(I18nUtil.getMessage(passed ? MessageKeys.SUCCESS : MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL));
            setIcon(passed ? passedIcon : failedIcon);
            setIconTextGap(6);
            setHorizontalAlignment(LEFT);
            if (!isSelected) {
                setForeground(passed ? ModernColors.getSuccess() : ModernColors.getError());
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }
}
