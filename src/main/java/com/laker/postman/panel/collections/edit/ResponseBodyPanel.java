package com.laker.postman.panel.collections.edit;

import com.laker.postman.model.HttpResponse;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;

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
    private String currentFileName; // 新增字段，保存原始文件名
    private String contentDispositionFileName; // 新增字段，保存Content-Disposition解析出的文件名

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
        // 提取Content-Disposition文件名
        String contentDisposition = null;
        if (resp.headers != null) {
            for (Map.Entry<String, java.util.List<String>> entry : resp.headers.entrySet()) {
                if (entry.getKey() != null && "Content-Disposition".equalsIgnoreCase(entry.getKey())) {
                    contentDisposition = entry.getValue().get(0);
                    break;
                }
            }
        }
        String fileName = null;
        // 优先从Content-Disposition解析文件名
        if (contentDisposition != null) {
            fileName = parseFileNameFromContentDisposition(contentDisposition);
        }
        // 如果没有则尝试从filePath获取
        if (fileName == null && resp.filePath != null) {
            java.io.File f = new java.io.File(resp.filePath);
            fileName = f.getName();
        }
        String filePath = resp.filePath;
        String text = resp.body;
        this.currentFilePath = filePath;
        this.currentFileName = fileName;
        this.contentDispositionFileName = parseFileNameFromContentDisposition(contentDisposition);
        if (filePath != null && !filePath.isEmpty()) {
            responseBodyPane.setText("响应体为二进制内容，无法直接预览，请点击下方按钮下载。");
            downloadButton.setVisible(true);
        } else {
            // 限制最大显示内容为2MB，超出则提示
            if (text != null && text.getBytes().length > 2 * 1024 * 1024) {
                responseBodyPane.setText("响应体内容过大，无法显示。请使用其他工具查看。");
            } else {
                responseBodyPane.setText(text);
            }
            downloadButton.setVisible(false);
        }
        responseBodyPane.setCaretPosition(0);
    }

    private String parseFileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) return null;
        // 优先解析 filename*=
        String lower = contentDisposition.toLowerCase();
        int idxStar = lower.indexOf("filename*=");
        if (idxStar >= 0) {
            // 例：filename*=utf-8''f578cc7e-41da-4060-bfdf-9f5d746de6e0.tar
            String fn = contentDisposition.substring(idxStar + 9).trim();
            // 处理编码和单引号
            int firstQuote = fn.indexOf("''");
            if (firstQuote >= 0) {
                fn = fn.substring(firstQuote + 2);
            } else {
                // 兼容没有''的情况
                int semi = fn.indexOf(';');
                if (semi > 0) fn = fn.substring(0, semi);
            }
            // 去除后续分号
            int semi = fn.indexOf(';');
            if (semi > 0) fn = fn.substring(0, semi);
            return fn.trim();
        }
        // 兼容 filename="xxx" 或 filename=xxx
        int idx = lower.indexOf("filename=");
        if (idx >= 0) {
            String fn = contentDisposition.substring(idx + 9).trim();
            if (fn.startsWith("\"")) fn = fn.substring(1);
            int end = fn.indexOf('"');
            if (end >= 0) fn = fn.substring(0, end);
            else {
                int semi = fn.indexOf(';');
                if (semi > 0) fn = fn.substring(0, semi);
            }
            return fn.trim();
        }
        return null;
    }

    private void saveFile() {
        if (currentFilePath == null) return;
        // 优先使用Content-Disposition文件名，其次原始文件名
        String defaultFileName = contentDispositionFileName != null && !contentDispositionFileName.isEmpty()
                ? contentDispositionFileName
                : (currentFileName != null && !currentFileName.isEmpty() ? currentFileName : "downloaded_file");
        String fileExtension = "";
        int dotIdx = defaultFileName.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < defaultFileName.length() - 1) {
            fileExtension = defaultFileName.substring(dotIdx);
        } else {
            // 如果没有扩展名，尝试从临时文件获取
            java.io.File srcFile = new java.io.File(currentFilePath);
            String name = srcFile.getName();
            int dotIdx2 = name.lastIndexOf('.');
            if (dotIdx2 > 0 && dotIdx2 < name.length() - 1) {
                fileExtension = name.substring(dotIdx2);
            }
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存文件");
        fileChooser.setSelectedFile(new File(defaultFileName));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File destFile = fileChooser.getSelectedFile();
            // 如果没有扩展名，自动补上
            if (!fileExtension.isEmpty() && !destFile.getName().toLowerCase().endsWith(fileExtension.toLowerCase())) {
                destFile = new File(destFile.getParent(), destFile.getName() + fileExtension);
            }
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