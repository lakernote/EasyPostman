package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Java反编译器面板
 * 使用CFR反编译器，支持查看JAR、ZIP、Class文件
 *
 * @author laker
 */
@Slf4j
public class DecompilerPanel extends JPanel {

    private static final String CLASS_EXTENSION = ".class";
    private static final String JAR_EXTENSION = ".jar";
    private static final String ZIP_EXTENSION = ".zip";

    private JTextField filePathField;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private RSyntaxTextArea codeArea;
    private JLabel statusLabel;

    private File currentFile;
    private transient JarFile currentJarFile;
    private transient ZipFile currentZipFile;
    private final Map<String, byte[]> classFileCache = new HashMap<>();

    public DecompilerPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部文件选择面板
        add(createFileSelectionPanel(), BorderLayout.NORTH);

        // 中间主要内容区域（分割面板：文件树 | 代码显示）
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createTreePanel());
        splitPane.setRightComponent(createCodePanel());
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.3);
        add(splitPane, BorderLayout.CENTER);

        // 底部状态栏
        add(createStatusPanel(), BorderLayout.SOUTH);
    }

    /**
     * 创建文件选择面板 - 优化布局和视觉效果
     */
    private JPanel createFileSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ModernColors.getBorderLightColor(), 1, true),
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SELECT_JAR)
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // 文件路径显示区域
        JPanel fileInfoPanel = new JPanel(new BorderLayout(5, 0));
        filePathField = new JTextField();
        filePathField.setEditable(false);
        filePathField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        filePathField.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_FILE_PATH_TOOLTIP));
        fileInfoPanel.add(filePathField, BorderLayout.CENTER);

        // 浏览按钮
        JButton browseButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_BROWSE));
        browseButton.setIcon(new FlatSVGIcon("icons/file.svg", 16, 16));
        browseButton.addActionListener(e -> browseFile());
        fileInfoPanel.add(browseButton, BorderLayout.EAST);

        panel.add(fileInfoPanel, BorderLayout.CENTER);

        // 拖放提示标签 - 更醒目的提示
        JLabel dragDropLabel = new JLabel(
                "💡 " + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_DRAG_DROP_HINT),
                SwingConstants.CENTER
        );
        dragDropLabel.setFont(dragDropLabel.getFont().deriveFont(Font.ITALIC));
        dragDropLabel.setForeground(ModernColors.getTextSecondary());
        panel.add(dragDropLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建文件树面板
     */
    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_TREE_TITLE));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_NO_FILE));
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        // 双击树节点时反编译并显示
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        handleTreeNodeClick(node);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTree);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 树操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton expandAllBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPAND_ALL));
        expandAllBtn.addActionListener(e -> expandTree(fileTree, 3));
        JButton collapseAllBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COLLAPSE_ALL));
        collapseAllBtn.addActionListener(e -> collapseTree(fileTree));
        buttonPanel.add(expandAllBtn);
        buttonPanel.add(collapseAllBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建代码显示面板 - 优化工具栏和布局
     */
    private JPanel createCodePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 顶部工具栏 - 标题和操作按钮整合
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_OUTPUT));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        headerPanel.add(label, BorderLayout.WEST);

        // 工具按钮
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COPY_CODE));
        copyBtn.setIcon(new FlatSVGIcon("icons/copy.svg", 14, 14));
        copyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COPY_CODE_TOOLTIP));
        copyBtn.addActionListener(e -> copyCode());

        JButton exportBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT));
        exportBtn.setIcon(new FlatSVGIcon("icons/export.svg", 14, 14));
        exportBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT_TOOLTIP));
        exportBtn.addActionListener(e -> exportCode());

        toolPanel.add(copyBtn);
        toolPanel.add(exportBtn);
        headerPanel.add(toolPanel, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // 创建代码编辑器
        codeArea = new RSyntaxTextArea();
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setEditable(false);
        codeArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        codeArea.setMargin(new Insets(10, 10, 10, 10));

        RTextScrollPane scrollPane = new RTextScrollPane(codeArea);
        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setLineNumbersEnabled(true);
        panel.add(scrollPane, BorderLayout.CENTER);


        return panel;
    }

    /**
     * 创建状态栏 - 优化样式和分隔
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getBorderLightColor()),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_READY));
        statusLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    /**
     * 浏览并选择文件
     */
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SELECT_FILE_PROMPT));

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JAR/Class/Zip Files (*.jar, *.class, *.zip)", "jar", "class", "zip");
        fileChooser.setFileFilter(filter);

        if (currentFile != null && currentFile.getParentFile() != null) {
            fileChooser.setCurrentDirectory(currentFile.getParentFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadFile(selectedFile);
        }
    }

    /**
     * 加载文件
     */
    private void loadFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(JAR_EXTENSION) && !fileName.endsWith(CLASS_EXTENSION) && !fileName.endsWith(ZIP_EXTENSION)) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_UNSUPPORTED_FILE),
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ERROR),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 关闭之前打开的文件
        closeCurrentJar();

        currentFile = file;
        filePathField.setText(file.getAbsolutePath());

        // 在后台线程中加载文件
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_LOADING));
                try {
                    if (fileName.endsWith(JAR_EXTENSION)) {
                        loadJarFile(file);
                    } else if (fileName.endsWith(ZIP_EXTENSION)) {
                        loadZipFile(file);
                    } else if (fileName.endsWith(CLASS_EXTENSION)) {
                        loadClassFile(file);
                    }
                } catch (Exception e) {
                    log.error("Failed to load file: {}", file.getAbsolutePath(), e);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(DecompilerPanel.this,
                                I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_LOAD_ERROR) + ": " + e.getMessage(),
                                I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ERROR),
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_FILE_INFO) + ": " +
                        file.getName() + " (" + formatFileSize(file.length()) + ")");
            }
        };
        worker.execute();
    }

    /**
     * 加载JAR文件
     */
    private void loadJarFile(File file) throws IOException {
        currentJarFile = new JarFile(file);
        classFileCache.clear();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName());
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        Enumeration<JarEntry> entries = currentJarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 缓存class文件
            if (entryName.endsWith(CLASS_EXTENSION)) {
                try (InputStream is = currentJarFile.getInputStream(entry)) {
                    classFileCache.put(entryName, is.readAllBytes());
                }
            }

            addEntryToTree(root, packageNodes, entryName, entry.isDirectory());
        }

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            expandTree(fileTree, 2);
        });
    }

    /**
     * 加载ZIP文件
     */
    private void loadZipFile(File file) throws IOException {
        currentZipFile = new ZipFile(file);
        classFileCache.clear();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName());
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        Enumeration<? extends ZipEntry> entries = currentZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 缓存class文件
            if (entryName.endsWith(CLASS_EXTENSION)) {
                try (InputStream is = currentZipFile.getInputStream(entry)) {
                    classFileCache.put(entryName, is.readAllBytes());
                }
            }

            addEntryToTree(root, packageNodes, entryName, entry.isDirectory());
        }

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            expandTree(fileTree, 2);
        });
    }

    /**
     * 加载单个Class文件
     */
    private void loadClassFile(File file) throws IOException {
        classFileCache.clear();
        byte[] classBytes = Files.readAllBytes(file.toPath());
        classFileCache.put(file.getName(), classBytes);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName());
        root.setUserObject(new FileNodeData(file.getName(), false, true));

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            fileTree.expandRow(0);
        });
    }

    /**
     * 添加条目到树中
     */
    private void addEntryToTree(DefaultMutableTreeNode root, Map<String, DefaultMutableTreeNode> packageNodes,
                                String entryName, boolean isDirectory) {
        String[] parts = entryName.split("/");
        DefaultMutableTreeNode parentNode = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            boolean isLastPart = (i == parts.length - 1);
            boolean isFile = isLastPart && !isDirectory;
            boolean isClassFile = isFile && part.endsWith(CLASS_EXTENSION);

            String nodePath = entryName.substring(0, entryName.lastIndexOf(part) + part.length());

            DefaultMutableTreeNode node = packageNodes.get(nodePath);
            if (node == null) {
                FileNodeData nodeData = new FileNodeData(part, !isFile, isClassFile);
                nodeData.fullPath = entryName;
                node = new DefaultMutableTreeNode(nodeData);
                packageNodes.put(nodePath, node);
                parentNode.add(node);
            }

            parentNode = node;
        }
    }

    /**
     * 处理树节点点击
     */
    private void handleTreeNodeClick(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        if (userObject instanceof FileNodeData fileData) {
            if (fileData.isClassFile) {
                decompileAndShow(fileData.fullPath != null ? fileData.fullPath : fileData.name);
            } else if (!fileData.isDirectory) {
                // 尝试显示文本文件
                showTextFile(fileData.fullPath != null ? fileData.fullPath : fileData.name);
            }
        } else if (userObject instanceof String fileName && fileName.endsWith(CLASS_EXTENSION)) {
            // 单个class文件的情况
            decompileAndShow(fileName);
        }
    }

    /**
     * 反编译并显示代码
     */
    private void decompileAndShow(String className) {
        byte[] classBytes = classFileCache.get(className);
        if (classBytes == null) {
            codeArea.setText("// " + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ERROR) +
                    ": Class file not found");
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_DECOMPILING) + ": " + className);
                try {
                    return decompileClass(classBytes, className);
                } catch (Exception e) {
                    log.error("Failed to decompile class: {}", className, e);
                    return "// " + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ERROR) +
                            ": " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String code = get();
                    codeArea.setText(code);
                    codeArea.setCaretPosition(0);

                    // 显示类信息
                    String classInfo = extractClassInfo(classBytes);
                    statusLabel.setText(classInfo);
                } catch (Exception e) {
                    log.error("Error displaying decompiled code", e);
                    codeArea.setText("// " + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ERROR));
                }
            }
        };
        worker.execute();
    }

    /**
     * 使用CFR反编译Class字节码
     */
    private String decompileClass(byte[] classBytes, String className) {
        StringBuilder result = new StringBuilder();

        try {
            // 创建临时目录保存class文件
            File tempDir = Files.createTempDirectory("cfr-decompile").toFile();
            File classFile = new File(tempDir, className);
            File parentDir = classFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                log.warn("Failed to create parent directory: {}", parentDir);
            }
            Files.write(classFile.toPath(), classBytes);

            // CFR配置
            Map<String, String> options = new HashMap<>();
            options.put("outputdir", tempDir.getAbsolutePath());

            // 自定义输出收集器
            OutputSinkFactory mySink = new OutputSinkFactory() {
                @Override
                public List<OutputSinkFactory.SinkClass> getSupportedSinks(OutputSinkFactory.SinkType sinkType,
                                                                           Collection<OutputSinkFactory.SinkClass> collection) {
                    return Arrays.asList(OutputSinkFactory.SinkClass.STRING,
                            OutputSinkFactory.SinkClass.DECOMPILED,
                            OutputSinkFactory.SinkClass.DECOMPILED_MULTIVER);
                }

                @Override
                public <T> OutputSinkFactory.Sink<T> getSink(OutputSinkFactory.SinkType sinkType,
                                                             OutputSinkFactory.SinkClass sinkClass) {
                    return sinkable -> result.append(sinkable.toString());
                }
            };

            // 执行反编译
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .withOutputSink(mySink)
                    .build();
            driver.analyse(Collections.singletonList(classFile.getAbsolutePath()));

            // 清理临时文件
            deleteDirectory(tempDir);

            return !result.isEmpty() ? result.toString() :
                    "// Failed to decompile: No output from CFR";

        } catch (Exception e) {
            log.error("CFR decompilation failed", e);
            return "// Decompilation failed: " + e.getMessage();
        }
    }

    /**
     * 提取类信息（版本号等）
     */
    private String extractClassInfo(byte[] classBytes) {
        if (classBytes.length < 8) {
            return "";
        }

        try {
            // Class文件格式: magic(4) + minor(2) + major(2)
            int major = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
            String javaVersion = getJavaVersion(major);

            return I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_CLASS_VERSION) + ": " + major +
                    " (" + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_JAVA_VERSION) + ": " + javaVersion + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 显示文本文件内容
     */
    private void showTextFile(String fileName) {
        try {
            String content = null;

            if (currentJarFile != null) {
                JarEntry entry = currentJarFile.getJarEntry(fileName);
                if (entry != null) {
                    try (InputStream is = currentJarFile.getInputStream(entry)) {
                        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } else if (currentZipFile != null) {
                ZipEntry entry = currentZipFile.getEntry(fileName);
                if (entry != null) {
                    try (InputStream is = currentZipFile.getInputStream(entry)) {
                        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }

            if (content != null) {
                codeArea.setSyntaxEditingStyle(getSyntaxStyle(fileName));
                codeArea.setText(content);
                codeArea.setCaretPosition(0);
                statusLabel.setText(fileName);
            }
        } catch (Exception e) {
            log.error("Failed to read file: {}", fileName, e);
            codeArea.setText("// Failed to read file: " + e.getMessage());
        }
    }

    /**
     * 获取语法高亮样式
     */
    private String getSyntaxStyle(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
        if (name.endsWith(".json")) return SyntaxConstants.SYNTAX_STYLE_JSON;
        if (name.endsWith(".properties")) return SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
        if (name.endsWith(".yaml") || name.endsWith(".yml")) return SyntaxConstants.SYNTAX_STYLE_YAML;
        if (name.endsWith(".html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
        if (name.endsWith(".js")) return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
        if (name.endsWith(".java")) return SyntaxConstants.SYNTAX_STYLE_JAVA;
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    /**
     * 复制代码到剪贴板
     */
    private void copyCode() {
        String code = codeArea.getText();
        if (code != null && !code.isEmpty()) {
            StringSelection selection = new StringSelection(code);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_CODE_COPIED));
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_CODE_COPIED));
        }
    }

    /**
     * 导出代码到文件
     */
    private void exportCode() {
        String code = codeArea.getText();
        if (code == null || code.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_NO_CODE_TO_EXPORT));
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT_CODE));
        fileChooser.setSelectedFile(new File("DecompiledCode.java"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                Files.writeString(file.toPath(), code, StandardCharsets.UTF_8);
                statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT_SUCCESS) + ": " + file.getAbsolutePath());
                NotificationUtil.showSuccess(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT_SUCCESS) + ": " + file.getName()
                );
            } catch (IOException e) {
                log.error("Failed to export code", e);
                statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT_FAILED));
                NotificationUtil.showError(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPORT_FAILED) + ": " + e.getMessage()
                );
            }
        }
    }

    /**
     * 关闭当前打开的JAR/ZIP文件
     */
    private void closeCurrentJar() {
        try {
            if (currentJarFile != null) {
                currentJarFile.close();
                currentJarFile = null;
            }
            if (currentZipFile != null) {
                currentZipFile.close();
                currentZipFile = null;
            }
            classFileCache.clear();
        } catch (IOException e) {
            log.error("Failed to close jar/zip file", e);
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " " + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_BYTES);
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }

    /**
     * 获取Java版本号
     */
    private String getJavaVersion(int majorVersion) {
        return switch (majorVersion) {
            case 45 -> "1.1";
            case 46 -> "1.2";
            case 47 -> "1.3";
            case 48 -> "1.4";
            case 49 -> "5";
            case 50 -> "6";
            case 51 -> "7";
            case 52 -> "8";
            case 53 -> "9";
            case 54 -> "10";
            case 55 -> "11";
            case 56 -> "12";
            case 57 -> "13";
            case 58 -> "14";
            case 59 -> "15";
            case 60 -> "16";
            case 61 -> "17";
            case 62 -> "18";
            case 63 -> "19";
            case 64 -> "20";
            case 65 -> "21";
            default -> majorVersion + " (unknown)";
        };
    }

    /**
     * 展开树到指定层级
     */
    private void expandTree(JTree tree, int level) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        expandNode(tree, root, 0, level);
    }

    private void expandNode(JTree tree, DefaultMutableTreeNode node, int currentLevel, int targetLevel) {
        if (currentLevel < targetLevel) {
            tree.expandPath(new TreePath(node.getPath()));
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                expandNode(tree, child, currentLevel + 1, targetLevel);
            }
        }
    }

    /**
     * 收起整个树
     */
    private void collapseTree(JTree tree) {
        for (int i = tree.getRowCount() - 1; i >= 1; i--) {
            tree.collapseRow(i);
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        try {
            Files.deleteIfExists(dir.toPath());
        } catch (IOException e) {
            log.warn("Failed to delete: {}", dir.getAbsolutePath(), e);
        }
    }

    /**
     * 文件节点数据
     */
    private static class FileNodeData {
        String name;
        boolean isDirectory;
        boolean isClassFile;
        String fullPath;

        FileNodeData(String name, boolean isDirectory, boolean isClassFile) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.isClassFile = isClassFile;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 文件树渲染器
     */
    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof FileNodeData fileData) {
                    if (fileData.isClassFile) {
                        setIcon(UIManager.getIcon("FileView.fileIcon"));
                    } else if (fileData.isDirectory) {
                        setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    }
                }
            }

            return this;
        }
    }
}

