package com.laker.postman.common.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * 文件选择单元格编辑器
 * 增强版：支持文件预览、文件类型过滤、最近路径记忆
 */
public class FileCellEditor extends DefaultCellEditor {

    private final JPanel editorPanel;
    private final JButton browseButton;
    private final JTextField pathField;
    private String filePath;
    private final Component parentComponent;

    // 记住最近打开的目录
    private static File lastDirectory = null;

    // 可选的文件类型过滤器
    private FileNameExtensionFilter fileFilter = null;

    public FileCellEditor(Component parentComponent) {
        super(new JCheckBox());
        this.parentComponent = parentComponent;

        // 创建面板，使用BorderLayout
        editorPanel = new JPanel(new BorderLayout(5, 0));
        editorPanel.setBackground(Color.WHITE);

        // 创建文本字段显示路径
        pathField = new JTextField();
        pathField.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // 创建浏览按钮
        browseButton = new JButton();
        browseButton.setIcon(new FlatSVGIcon("icons/file.svg", TableUIConstants.ICON_SIZE, TableUIConstants.ICON_SIZE));
        browseButton.setText("浏览");
        browseButton.setMargin(new Insets(2, 8, 2, 8));
        browseButton.setFocusPainted(false);
        browseButton.setBackground(TableUIConstants.ZEBRA_LIGHT);
        browseButton.setForeground(TableUIConstants.FILE_BUTTON_TEXT_COLOR);
        browseButton.setBorder(TableUIConstants.createButtonBorder());
        browseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 为浏览按钮添加悬停效果
        browseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                browseButton.setBackground(TableUIConstants.HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                browseButton.setBackground(TableUIConstants.ZEBRA_LIGHT);
            }
        });

        // 设置按钮点击事件
        browseButton.addActionListener(e -> openFileChooser());

        // 也可以通过双击文本框选择文件
        pathField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openFileChooser();
                }
            }
        });

        // 将组件添加到面板
        editorPanel.add(pathField, BorderLayout.CENTER);
        editorPanel.add(browseButton, BorderLayout.EAST);
    }

    /**
     * 设置文件类型过滤器
     *
     * @param description 过滤器描述
     * @param extensions  文件扩展名，如 "json", "txt"
     */
    public void setFileFilter(String description, String... extensions) {
        if (extensions != null && extensions.length > 0) {
            this.fileFilter = new FileNameExtensionFilter(description, extensions);
        } else {
            this.fileFilter = null;
        }
    }

    /**
     * 打开文件选择对话框
     */
    private void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(TableUIConstants.SELECT_FILE_TEXT);

        // 应用文件过滤器
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }

        // 使用最近的目录
        if (lastDirectory != null && lastDirectory.exists()) {
            fileChooser.setCurrentDirectory(lastDirectory);
        }

        // 如果已有文件路径，预选该文件
        if (filePath != null && !filePath.isEmpty() && !TableUIConstants.SELECT_FILE_TEXT.equals(filePath)) {
            File currentFile = new File(filePath);
            if (currentFile.exists()) {
                fileChooser.setSelectedFile(currentFile);
            }
        }

        // 显示文件选择器
        int result = fileChooser.showOpenDialog(parentComponent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePath = selectedFile.getAbsolutePath();
            pathField.setText(filePath);
            lastDirectory = selectedFile.getParentFile();

            // 自动提交编辑
            SwingUtilities.invokeLater(this::stopCellEditing);
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        filePath = value == null ? "" : value.toString();

        if (filePath.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(filePath)) {
            pathField.setText("");
            pathField.setToolTipText("双击此处选择文件");
        } else {
            pathField.setText(filePath);
            pathField.setToolTipText(filePath);

            // 设置文本框颜色
            File file = new File(filePath);
            if (!file.exists()) {
                pathField.setForeground(Color.RED);
            } else if (!file.canRead()) {
                pathField.setForeground(Color.ORANGE);
            } else {
                pathField.setForeground(TableUIConstants.FILE_SELECTED_TEXT_COLOR);
            }
        }

        // 设置面板背景色
        Color bgColor = row % 2 == 0 ? TableUIConstants.ZEBRA_LIGHT : TableUIConstants.ZEBRA_DARK;
        if (isSelected) {
            bgColor = table.getSelectionBackground();
        }
        editorPanel.setBackground(bgColor);
        pathField.setBackground(bgColor);

        return editorPanel;
    }

    @Override
    public Object getCellEditorValue() {
        String value = pathField.getText();
        return value.isEmpty() ? "" : value;
    }

    /**
     * 获取选中的文件对象
     */
    public File getSelectedFile() {
        if (filePath == null || filePath.isEmpty() || TableUIConstants.SELECT_FILE_TEXT.equals(filePath)) {
            return null;
        }
        return new File(filePath);
    }
}