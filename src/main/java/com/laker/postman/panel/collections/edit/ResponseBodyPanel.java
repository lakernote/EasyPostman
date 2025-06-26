package com.laker.postman.panel.collections.edit;

import com.laker.postman.model.HttpResponse;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 响应体面板，展示响应体内容和格式化按钮
 */
public class ResponseBodyPanel extends JPanel {
    @Getter
    private final JTextPane responseBodyPane;
    @Getter
    private final JButton formatButton;
    private final JButton downloadButton;
    private String currentFilePath;
    private String fileName = "downloaded_file"; // 默认下载文件名

    public ResponseBodyPanel() {
        setLayout(new BorderLayout());
        responseBodyPane = new JTextPane();
        responseBodyPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(responseBodyPane);
        add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        formatButton = new JButton("Format");
        buttonPanel.add(formatButton);
        downloadButton = new JButton("下载文件");
        downloadButton.setVisible(false);
        buttonPanel.add(downloadButton);
        add(buttonPanel, BorderLayout.SOUTH);
        downloadButton.addActionListener(e -> saveFile());
    }

    public void setBodyText(HttpResponse resp) {
        if (resp == null) {
            return;
        }
        this.currentFilePath = resp.filePath;
        this.fileName = resp.fileName;
        String filePath = resp.filePath;
        String text = resp.body;
        responseBodyPane.setText(text);
        if (filePath != null && !filePath.isEmpty()) {
            downloadButton.setVisible(true);
        } else {
            downloadButton.setVisible(false);
        }
        responseBodyPane.setCaretPosition(0);
    }

    private void saveFile() {
        if (currentFilePath == null) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存文件");
        fileChooser.setSelectedFile(new File(fileName));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File destFile = fileChooser.getSelectedFile();
            try (java.io.InputStream in = new java.io.FileInputStream(currentFilePath);
                 java.io.OutputStream out = new java.io.FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                JOptionPane.showMessageDialog(this, "文件已保存到: " + destFile.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "保存文件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}