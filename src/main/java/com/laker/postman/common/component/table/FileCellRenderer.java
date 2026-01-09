package com.laker.postman.common.component.table;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * 文件类型单元格渲染器
 * 增强版：支持文件状态显示、路径截断、文件类型图标
 */
public class FileCellRenderer implements TableCellRenderer {

    // 文件路径显示的最大长度，超过则截断
    private static final int MAX_PATH_DISPLAY_LENGTH = 35;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String val = value == null ? "" : value.toString();
        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;
        boolean isEmpty = val.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(val);
        boolean isHovered = row == hoverRow;

        if (isEmpty) {
            // 未选择文件时显示按钮样式
            JButton button = createFileButton(TableUIConstants.SELECT_FILE_TEXT, true);
            button.setBackground(TableUIConstants.getCellBackground(isSelected, isHovered, true, table));
            return button;
        } else {
            // 已选择文件时显示文件信息
            JPanel panel = new JPanel(new BorderLayout(5, 0));
            panel.setOpaque(true);
            panel.setBackground(TableUIConstants.getCellBackground(isSelected, isHovered, false, table));

            // 创建并添加文件图标
            JLabel iconLabel = new JLabel();
            iconLabel.setIcon(getFileTypeIcon(val));
            panel.add(iconLabel, BorderLayout.WEST);

            // 创建并添加文件路径文本
            JLabel pathLabel = new JLabel(formatFilePath(val));
            pathLabel.setForeground(TableUIConstants.getFileSelectedTextColor());
            pathLabel.setToolTipText(val); // 完整路径作为工具提示
            panel.add(pathLabel, BorderLayout.CENTER);

            // 添加文件状态指示器
            JLabel statusLabel = new JLabel();
            statusLabel.setIcon(getFileStatusIcon(val));
            panel.add(statusLabel, BorderLayout.EAST);

            // 设置边框和内边距
            panel.setBorder(TableUIConstants.createLabelBorder());
            return panel;
        }
    }

    /**
     * 创建文件选择按钮
     */
    private JButton createFileButton(String text, boolean isEmpty) {
        JButton button = new JButton();
        button.setText(text);
        FontAwesome icon = isEmpty ? FontAwesome.FILE : FontAwesome.FILE_O;
        Color iconColor = isEmpty ? TableUIConstants.getFileEmptyTextColor() : TableUIConstants.getFileSelectedTextColor();
        button.setIcon(IconFontSwing.buildIcon(icon, TableUIConstants.ICON_SIZE, iconColor));
        button.setForeground(isEmpty ? TableUIConstants.getFileEmptyTextColor() : TableUIConstants.getFileSelectedTextColor());
        button.setFocusPainted(false);
        button.setBorder(TableUIConstants.createButtonBorder());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
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

    /**
     * 根据文件类型获取对应图标
     */
    private Icon getFileTypeIcon(String path) {
        if (path == null || path.isEmpty()) {
            return IconFontSwing.buildIcon(FontAwesome.FILE, TableUIConstants.ICON_SIZE, TableUIConstants.getFileEmptyTextColor());
        }

        // 根据文件扩展名设置不同图标
        String lowerPath = path.toLowerCase();
        FontAwesome icon;

        if (lowerPath.endsWith(".json")) {
            icon = FontAwesome.FILE_CODE_O;
        } else if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".log")) {
            icon = FontAwesome.FILE_TEXT_O;
        } else if (lowerPath.endsWith(".zip") || lowerPath.endsWith(".jar") || lowerPath.endsWith(".rar")) {
            icon = FontAwesome.FILE_ARCHIVE_O;
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".png") || lowerPath.endsWith(".gif")) {
            icon = FontAwesome.FILE_IMAGE_O;
        } else if (lowerPath.endsWith(".pdf")) {
            icon = FontAwesome.FILE_PDF_O;
        } else if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")) {
            icon = FontAwesome.FILE_WORD_O;
        } else if (lowerPath.endsWith(".xls") || lowerPath.endsWith(".xlsx")) {
            icon = FontAwesome.FILE_EXCEL_O;
        } else {
            icon = FontAwesome.FILE_O;
        }

        return IconFontSwing.buildIcon(icon, TableUIConstants.ICON_SIZE, TableUIConstants.getFileSelectedTextColor());
    }

    /**
     * 获取文件状态图标（存在/不存在/可读等）
     */
    private Icon getFileStatusIcon(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        File file = new File(path);

        if (!file.exists()) {
            return IconFontSwing.buildIcon(FontAwesome.EXCLAMATION_TRIANGLE, TableUIConstants.ICON_SIZE, Color.RED);
        } else if (!file.canRead()) {
            return IconFontSwing.buildIcon(FontAwesome.LOCK, TableUIConstants.ICON_SIZE, Color.ORANGE);
        } else {
            return IconFontSwing.buildIcon(FontAwesome.CHECK_CIRCLE, TableUIConstants.ICON_SIZE, new Color(76, 175, 80));
        }
    }
}