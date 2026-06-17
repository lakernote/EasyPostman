package com.laker.postman.common.component.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * 文件类型单元格渲染器
 * 增强版：支持文件状态提示和路径截断
 */
public class FileCellRenderer implements TableCellRenderer {

    // 文件路径显示的最大长度，超过则截断
    private static final int MAX_PATH_DISPLAY_LENGTH = 35;
    private final JLabel label = new JLabel();

    public FileCellRenderer() {
        label.setOpaque(true);
        label.setBorder(TableUIConstants.createLabelBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String val = value == null ? "" : value.toString();
        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;
        boolean isEmpty = val.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(val);
        boolean isHovered = row == hoverRow;

        label.setBackground(TableUIConstants.getCellBackground(isSelected, isHovered, isEmpty, table, row));
        label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

        if (isEmpty) {
            label.setText(TableUIConstants.SELECT_FILE_TEXT);
            if (!isSelected) {
                label.setForeground(TableUIConstants.getFileEmptyTextColor());
            }
            label.setToolTipText(null);
            return label;
        }

        label.setText(formatFilePath(val));
        label.setToolTipText(fileTooltip(val));
        return label;
    }

    /**
     * 格式化文件路径，过长时截断中间部分
     */
    private String formatFilePath(String path) {
        if (path == null || path.length() <= MAX_PATH_DISPLAY_LENGTH) {
            return path;
        }

        File file = new File(path);
        String fileName = file.getName();
        String parentPath = file.getParent();
        if (parentPath == null || parentPath.isBlank()) {
            return path;
        }

        // 如果文件名已经很长，只显示文件名的一部分
        if (fileName.length() > MAX_PATH_DISPLAY_LENGTH) {
            return fileName.substring(0, 10) + "..." +
                    fileName.substring(fileName.length() - 20);
        }

        // 否则保留完整文件名，但截断父路径
        int remainingLength = MAX_PATH_DISPLAY_LENGTH - fileName.length() - 5; // 5为"..."和分隔符的长度
        if (remainingLength < 10) remainingLength = 10;

        return parentPath.substring(0, Math.min(remainingLength, parentPath.length()))
                + "..." + File.separator + fileName;
    }

    private String fileTooltip(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            return path + " (file not found)";
        }
        if (!file.canRead()) {
            return path + " (file is not readable)";
        }
        return path;
    }
}
