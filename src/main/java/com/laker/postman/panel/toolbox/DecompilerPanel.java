package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import org.benf.cfr.reader.api.CfrDriver;
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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
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
    private static final String WAR_EXTENSION = ".war";
    private static final String JAVA_EXTENSION = ".java";

    private JTextField filePathField;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private RSyntaxTextArea codeArea;
    private JLabel statusLabel;
    private JLabel compressionInfoLabel; // 显示压缩信息的标签

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
        splitPane.setDividerLocation(350);
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
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // 文件路径显示区域
        JPanel fileInfoPanel = new JPanel(new BorderLayout(5, 0));
        filePathField = new JTextField();
        filePathField.setEditable(false);
        filePathField.setFocusable(false);
        fileInfoPanel.add(filePathField, BorderLayout.CENTER);

        // 按钮面板（浏览按钮和清空按钮）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton browseButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_BROWSE));
        browseButton.setIcon(IconUtil.createThemed("icons/file.svg", 16, 16));
        browseButton.addActionListener(e -> browseFile());

        JButton clearButton = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_CLEAR));
        clearButton.setIcon(IconUtil.createThemed("icons/clear.svg", 16, 16));
        clearButton.addActionListener(e -> clearAll());

        buttonPanel.add(browseButton);
        buttonPanel.add(clearButton);
        fileInfoPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(fileInfoPanel, BorderLayout.CENTER);

        // 拖放提示标签 - 提示拖到下方面板
        JLabel dragDropLabel = new JLabel(
                "💡 " + I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_DRAG_DROP_HINT_TO_BELOW),
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

        // 顶部面板：标题 + 压缩信息
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_TREE_TITLE));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        topPanel.add(label, BorderLayout.WEST);

        // 压缩信息标签（初始为空，加载文件后显示）
        compressionInfoLabel = new JLabel("");
        compressionInfoLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        compressionInfoLabel.setForeground(ModernColors.getTextSecondary());
        compressionInfoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(compressionInfoLabel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_NO_FILE));
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        // 单击打开文件或展开/收起目录（更灵敏）
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 只处理左键单击
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();

                        if (userObject instanceof FileNodeData fileData) {
                            if (fileData.isDirectory) {
                                // 目录：单击展开/收起
                                if (fileTree.isExpanded(path)) {
                                    fileTree.collapsePath(path);
                                } else {
                                    fileTree.expandPath(path);
                                }
                                e.consume();
                            } else {
                                // 文件：单击打开
                                handleTreeNodeClick(node);
                                e.consume();
                            }
                        }
                    }
                }
            }
        });

        // 添加回车键支持
        fileTree.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    TreePath path = fileTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        handleTreeNodeClick(node);
                        e.consume();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTree);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 为树面板添加拖拽支持
        setupDragAndDrop(scrollPane);

        // 树操作工具栏 - 使用紧凑的图标按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));

        // 展开按钮
        JButton expandAllBtn = new JButton(IconUtil.createThemed("icons/expand.svg", 16, 16));
        expandAllBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_EXPAND_ALL));
        expandAllBtn.addActionListener(e -> expandTree(fileTree, 3));
        expandAllBtn.setFocusPainted(false);

        // 收起按钮
        JButton collapseAllBtn = new JButton(IconUtil.createThemed("icons/collapse.svg", 16, 16));
        collapseAllBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COLLAPSE_ALL));
        collapseAllBtn.addActionListener(e -> collapseTree(fileTree));
        collapseAllBtn.setFocusPainted(false);

        // 分隔符
        JSeparator separator1 = new JSeparator(SwingConstants.VERTICAL);
        separator1.setPreferredSize(new Dimension(2, 20));

        // 按名称排序按钮
        JButton sortByNameBtn = new JButton(IconUtil.create("icons/text-file.svg", 16, 16));
        sortByNameBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SORT_BY_NAME));
        sortByNameBtn.addActionListener(e -> sortTreeByName());
        sortByNameBtn.setFocusPainted(false);

        // 按大小排序按钮
        JButton sortBySizeBtn = new JButton(IconUtil.createThemed("icons/detail.svg", 16, 16));
        sortBySizeBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SORT_BY_SIZE));
        sortBySizeBtn.addActionListener(e -> sortTreeBySize());
        sortBySizeBtn.setFocusPainted(false);

        buttonPanel.add(expandAllBtn);
        buttonPanel.add(collapseAllBtn);
        buttonPanel.add(separator1);
        buttonPanel.add(sortByNameBtn);
        buttonPanel.add(sortBySizeBtn);
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
        copyBtn.addActionListener(e -> copyCode());

        toolPanel.add(copyBtn);
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

        // 应用编辑器主题 - 支持亮色/暗色模式自适应
        EditorThemeUtil.loadTheme(codeArea);

        RTextScrollPane scrollPane = new RTextScrollPane(codeArea);
        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setLineNumbersEnabled(true);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 为代码面板添加拖拽支持
        setupDragAndDrop(codeArea);


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
     * 设置拖拽支持
     */
    private void setupDragAndDrop(JComponent component) {
        new DropTarget(component, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (isDragAcceptable(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    // 添加视觉反馈
                    component.setBorder(BorderFactory.createLineBorder(ModernColors.PRIMARY, 2));
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                // 移除视觉反馈
                component.setBorder(null);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                // 移除视觉反馈
                component.setBorder(null);

                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);

                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) dtde.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);

                        if (!droppedFiles.isEmpty()) {
                            File file = droppedFiles.get(0);
                            loadFile(file);
                            dtde.dropComplete(true);
                        } else {
                            dtde.dropComplete(false);
                        }
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    log.error("Failed to handle dropped file", e);
                    dtde.dropComplete(false);
                    NotificationUtil.showError(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_LOAD_ERROR) + ": " + e.getMessage()
                    );
                }
            }
        });
    }

    /**
     * 检查拖拽的数据是否可接受
     */
    private boolean isDragAcceptable(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    /**
     * 浏览并选择文件
     */
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SELECT_FILE_PROMPT));

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JAR/WAR/Class/Zip Files (*.jar, *.war, *.class, *.zip)", "jar", "war", "class", "zip");
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
        if (!fileName.endsWith(JAR_EXTENSION) && !fileName.endsWith(CLASS_EXTENSION) &&
            !fileName.endsWith(ZIP_EXTENSION) && !fileName.endsWith(WAR_EXTENSION)) {
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
                    if (fileName.endsWith(JAR_EXTENSION) || fileName.endsWith(WAR_EXTENSION)) {
                        // JAR 和 WAR 都使用 JarFile 加载（WAR 本质是特殊的 JAR）
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

        // 为根节点创建FileNodeData
        FileNodeData rootData = new FileNodeData(file.getName(), true, false);
        rootData.fullPath = file.getName();
        rootData.size = file.length(); // 设置为 JAR 文件实际大小（压缩后）
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootData);
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        long uncompressedTotal = 0; // 统计未压缩总大小
        Enumeration<JarEntry> entries = currentJarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 缓存所有非目录文件（包括class、jar/zip、文本文件以及其他未知类型文件）
            if (!entry.isDirectory()) {
                try (InputStream is = currentJarFile.getInputStream(entry)) {
                    classFileCache.put(entryName, is.readAllBytes());
                }
                uncompressedTotal += entry.getSize(); // 累加未压缩大小
            }

            // 注意：entry.getSize() 是未压缩大小，所以子文件大小之和会大于JAR文件本身
            addEntryToTree(root, packageNodes, entryName, entry.isDirectory(), entry.getSize());
        }

        // 计算所有子目录的大小（不修改根节点，因为根节点已设置为实际JAR大小）
        calculateDirectorySizesExceptRoot(root);

        // 计算压缩率
        long compressedSize = file.length();
        final long finalUncompressedTotal = uncompressedTotal;

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            expandTree(fileTree, 2);

            // 在树顶部显示压缩信息（始终可见）
            if (finalUncompressedTotal > 0) {
                double compressionRatio = (1 - (double) compressedSize / finalUncompressedTotal) * 100;
                String compressionRatioStr = String.format("%.1f%%", compressionRatio);

                compressionInfoLabel.setText(I18nUtil.getMessage(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_INFO_FORMAT,
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ACTUAL_SIZE),
                        formatFileSize(compressedSize),
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_UNCOMPRESSED_SIZE),
                        formatFileSize(finalUncompressedTotal),
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_RATIO),
                        compressionRatioStr
                ));

                compressionInfoLabel.setToolTipText(I18nUtil.getMessage(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_TOOLTIP_JAR,
                        formatFileSize(compressedSize),
                        formatFileSize(finalUncompressedTotal),
                        compressionRatioStr
                ));
            }
        });
    }

    /**
     * 加载ZIP文件
     */
    private void loadZipFile(File file) throws IOException {
        currentZipFile = new ZipFile(file);
        classFileCache.clear();

        // 为根节点创建FileNodeData
        FileNodeData rootData = new FileNodeData(file.getName(), true, false);
        rootData.fullPath = file.getName();
        rootData.size = file.length(); // 设置为 ZIP 文件实际大小（压缩后）
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootData);
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        long uncompressedTotal = 0; // 统计未压缩总大小
        Enumeration<? extends ZipEntry> entries = currentZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 缓存所有非目录文件（包括class、jar/zip、文本文件以及其他未知类型文件）
            if (!entry.isDirectory()) {
                try (InputStream is = currentZipFile.getInputStream(entry)) {
                    classFileCache.put(entryName, is.readAllBytes());
                }
                uncompressedTotal += entry.getSize(); // 累加未压缩大小
            }

            // 注意：entry.getSize() 是未压缩大小，所以子文件大小之和会大于ZIP文件本身
            addEntryToTree(root, packageNodes, entryName, entry.isDirectory(), entry.getSize());
        }

        // 计算所有子目录的大小（不修改根节点，因为根节点已设置为实际ZIP大小）
        calculateDirectorySizesExceptRoot(root);

        // 计算压缩率
        long compressedSize = file.length();
        final long finalUncompressedTotal = uncompressedTotal;

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            expandTree(fileTree, 2);

            // 在树顶部显示压缩信息（始终可见）
            if (finalUncompressedTotal > 0) {
                double compressionRatio = (1 - (double) compressedSize / finalUncompressedTotal) * 100;
                String compressionRatioStr = String.format("%.1f%%", compressionRatio);

                compressionInfoLabel.setText(I18nUtil.getMessage(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_INFO_FORMAT,
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ACTUAL_SIZE),
                        formatFileSize(compressedSize),
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_UNCOMPRESSED_SIZE),
                        formatFileSize(finalUncompressedTotal),
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_RATIO),
                        compressionRatioStr
                ));

                compressionInfoLabel.setToolTipText(I18nUtil.getMessage(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_TOOLTIP_ZIP,
                        formatFileSize(compressedSize),
                        formatFileSize(finalUncompressedTotal),
                        compressionRatioStr
                ));
            }
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
                                String entryName, boolean isDirectory, long size) {
        String[] parts = entryName.split("/");
        DefaultMutableTreeNode parentNode = root;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            // 构建当前节点的完整路径
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(part);
            String nodePath = currentPath.toString();

            boolean isLastPart = (i == parts.length - 1);
            boolean isFile = isLastPart && !isDirectory;
            boolean isClassFile = isFile && part.endsWith(CLASS_EXTENSION);
            boolean isJarFile = isFile && (part.toLowerCase().endsWith(JAR_EXTENSION) ||
                    part.toLowerCase().endsWith(ZIP_EXTENSION) ||
                    part.toLowerCase().endsWith(WAR_EXTENSION));

            // 检查节点是否已存在
            DefaultMutableTreeNode node = packageNodes.get(nodePath);
            if (node == null) {
                // 创建新节点
                FileNodeData nodeData = new FileNodeData(part, !isFile, isClassFile);
                nodeData.fullPath = nodePath;
                nodeData.isJarFile = isJarFile;
                // 只为文件设置大小，目录大小稍后计算
                if (isFile && size >= 0) {
                    nodeData.size = size;
                }
                node = new DefaultMutableTreeNode(nodeData);
                packageNodes.put(nodePath, node);
                parentNode.add(node);
            }

            // 移动到下一层
            parentNode = node;
        }
    }

    /**
     * 递归计算目录大小（所有子文件的总和）
     */
    private long calculateDirectorySizes(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof FileNodeData fileData)) {
            return 0;
        }

        // 如果是文件，直接返回其大小
        if (!fileData.isDirectory) {
            return fileData.size;
        }

        // 如果是目录，递归计算所有子节点的大小
        long totalSize = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            totalSize += calculateDirectorySizes(childNode);
        }

        // 设置目录的总大小
        fileData.size = totalSize;
        return totalSize;
    }

    /**
     * 递归计算所有子目录大小，但不修改根节点
     * （用于JAR/ZIP文件，根节点显示实际文件大小，子节点显示未压缩大小）
     */
    private void calculateDirectorySizesExceptRoot(DefaultMutableTreeNode root) {
        // 只计算根节点的直接子节点及其后代
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) root.getChildAt(i);
            calculateDirectorySizes(childNode);
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
            } else if (fileData.isJarFile) {
                // 处理嵌套的JAR/ZIP文件
                loadNestedJar(fileData.fullPath, node);
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
     * 加载嵌套的JAR/ZIP文件
     */
    private void loadNestedJar(String jarPath, DefaultMutableTreeNode parentNode) {
        // 如果节点已经有子节点，说明已经展开过，直接返回
        if (parentNode.getChildCount() > 0) {
            return;
        }

        byte[] jarBytes = classFileCache.get(jarPath);
        if (jarBytes == null) {
            NotificationUtil.showWarning("JAR file not found in cache: " + jarPath);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_LOADING) + ": " + jarPath);

                    // 创建临时文件保存嵌套的JAR
                    File tempJar = Files.createTempFile("nested-jar-", ".jar").toFile();
                    tempJar.deleteOnExit();
                    Files.write(tempJar.toPath(), jarBytes);

                    // 使用JarFile读取嵌套的JAR
                    Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();
                    try (JarFile nestedJar = new JarFile(tempJar)) {
                        Enumeration<JarEntry> entries = nestedJar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();
                            String fullEntryPath = jarPath + "!/" + entryName;

                            // 缓存所有非目录文件（包括class、jar/zip、文本文件以及其他未知类型文件）
                            if (!entry.isDirectory()) {
                                try (InputStream is = nestedJar.getInputStream(entry)) {
                                    classFileCache.put(fullEntryPath, is.readAllBytes());
                                }
                            }

                            // 添加到树中（相对于当前JAR节点）
                            addNestedEntryToTree(parentNode, packageNodes, entryName,
                                    fullEntryPath, entry.isDirectory(), entry.getSize());
                        }
                    }

                    // 计算嵌套JAR中所有目录的大小
                    calculateDirectorySizes(parentNode);

                    // 删除临时文件
                    Files.deleteIfExists(tempJar.toPath());

                } catch (Exception e) {
                    log.error("Failed to load nested JAR: {}", jarPath, e);
                    SwingUtilities.invokeLater(() ->
                            NotificationUtil.showError("Failed to load nested JAR: " + e.getMessage())
                    );
                }
                return null;
            }

            @Override
            protected void done() {
                // 刷新树显示
                treeModel.nodeStructureChanged(parentNode);
                // 展开节点
                TreePath path = new TreePath(parentNode.getPath());
                fileTree.expandPath(path);
                statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_READY));
            }
        };
        worker.execute();
    }

    /**
     * 添加嵌套JAR的条目到树中
     */
    private void addNestedEntryToTree(DefaultMutableTreeNode parentNode,
                                      Map<String, DefaultMutableTreeNode> packageNodes,
                                      String entryName, String fullPath, boolean isDirectory, long size) {
        String[] parts = entryName.split("/");
        DefaultMutableTreeNode currentParent = parentNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            // 构建当前节点的相对路径
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(part);
            String nodePath = currentPath.toString();

            boolean isLastPart = (i == parts.length - 1);
            boolean isFile = isLastPart && !isDirectory;
            boolean isClassFile = isFile && part.endsWith(CLASS_EXTENSION);
            boolean isJarFile = isFile && (part.toLowerCase().endsWith(JAR_EXTENSION) ||
                    part.toLowerCase().endsWith(ZIP_EXTENSION) ||
                    part.toLowerCase().endsWith(WAR_EXTENSION));

            // 检查节点是否已存在
            DefaultMutableTreeNode node = packageNodes.get(nodePath);
            if (node == null) {
                // 创建新节点
                FileNodeData nodeData = new FileNodeData(part, !isFile, isClassFile);
                // 使用完整路径（包含父JAR路径）
                nodeData.fullPath = fullPath.substring(0, fullPath.lastIndexOf("!/") + 2) + nodePath;
                nodeData.isJarFile = isJarFile;
                // 只为文件设置大小，目录大小稍后计算
                if (isFile && size >= 0) {
                    nodeData.size = size;
                }
                node = new DefaultMutableTreeNode(nodeData);
                packageNodes.put(nodePath, node);
                currentParent.add(node);
            }

            // 移动到下一层
            currentParent = node;
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

        // 特殊处理：module-info.class 是 Java 9+ 的模块描述符，CFR 无法反编译
        if (className.toLowerCase().endsWith("module-info.class") ||
                className.toLowerCase().contains("/module-info.class")) {
            codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            codeArea.setText("""
                    // module-info.class - Java Module Descriptor
                    // This file cannot be decompiled as it's a special module descriptor file.
                    //
                    // Module info files define:
                    // - Module name
                    // - Required modules
                    // - Exported packages
                    // - Provided services
                    //
                    // To view module information, use: jar --describe-module --file=<jar-file>
                    """);
            statusLabel.setText("Module descriptor file (not decompilable)");
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
                    // 设置 Java 语法高亮（反编译后的代码是 Java）
                    codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
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
        try {
            // 创建临时目录保存class文件
            File tempDir = Files.createTempDirectory("cfr-decompile").toFile();

            // 保存 class 文件
            File classFile = new File(tempDir, className);
            File parentDir = classFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                log.warn("Failed to create parent directory: {}", parentDir);
            }
            Files.write(classFile.toPath(), classBytes);

            // 创建输出目录
            File outputDir = new File(tempDir, "output");
            if (!outputDir.mkdirs() && !outputDir.exists()) {
                log.warn("Failed to create output directory: {}", outputDir);
            }

            // CFR配置 - 输出到文件
            Map<String, String> options = new HashMap<>();
            options.put("outputpath", outputDir.getAbsolutePath());  // 使用 outputpath 而不是 outputdir
            options.put("showversion", "false");  // 不显示版本信息
            options.put("hideutf", "false");      // 显示 UTF-8 字符
            options.put("innerclasses", "true");  // 反编译内部类
            options.put("skipbatchinnerclasses", "false");  // 不跳过批处理内部类

            log.debug("CFR decompiling: {} to {}", classFile.getAbsolutePath(), outputDir.getAbsolutePath());

            // 执行反编译
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .build();
            driver.analyse(Collections.singletonList(classFile.getAbsolutePath()));

            // 读取反编译后的 Java 文件
            String result = readDecompiledFile(outputDir, className);

            log.debug("Decompilation result length: {}", result.length());

            // 清理临时文件
            deleteDirectory(tempDir);

            return !result.isEmpty() ? result : "// Failed to decompile: No output from CFR";

        } catch (Exception e) {
            log.error("CFR decompilation failed", e);
            return "// Decompilation failed: " + e.getMessage();
        }
    }

    /**
     * 从输出目录读取反编译后的 Java 文件
     */
    private String readDecompiledFile(File outputDir, String className) {
        try {
            log.debug("Reading decompiled file for class: {} from dir: {}", className, outputDir.getAbsolutePath());

            // 处理嵌套 JAR 路径：BOOT-INF/lib/xxx.jar!/com/example/Test.class
            // 提取实际的类路径（!/ 之后的部分）
            String actualClassName = className;
            if (className.contains("!/")) {
                actualClassName = className.substring(className.lastIndexOf("!/") + 2);
                log.debug("Detected nested JAR path, extracted actual class: {}", actualClassName);
            }

            // 将 className 转换为文件路径
            // 例如: com/example/Test.class -> com/example/Test.java
            String javaFileName = actualClassName.replace(CLASS_EXTENSION, JAVA_EXTENSION);
            File javaFile = new File(outputDir, javaFileName);

            log.debug("Looking for Java file at: {}", javaFile.getAbsolutePath());

            if (javaFile.exists()) {
                log.debug("Found Java file directly at expected path");
                String content = Files.readString(javaFile.toPath(), StandardCharsets.UTF_8);
                // 清理可能的调试信息
                return cleanupDecompiledCode(content);
            }

            // 如果直接路径不存在，尝试在输出目录中查找 .java 文件
            log.debug("Java file not found at expected path, searching in output directory...");
            File[] javaFiles = findJavaFiles(outputDir);
            log.debug("Found {} Java files in output directory", javaFiles.length);

            if (javaFiles.length > 0) {
                log.debug("Using first found Java file: {}", javaFiles[0].getAbsolutePath());
                // 返回第一个找到的 Java 文件
                String content = Files.readString(javaFiles[0].toPath(), StandardCharsets.UTF_8);
                return cleanupDecompiledCode(content);
            }

            // 列出输出目录内容以便调试
            log.debug("Output directory contents:");
            listDirectoryContents(outputDir, "  ");

            return "// No Java file found in output directory\n// Expected: " + javaFileName +
                    "\n// Output dir: " + outputDir.getAbsolutePath();
        } catch (IOException e) {
            log.error("Failed to read decompiled file", e);
            return "// Failed to read decompiled file: " + e.getMessage();
        }
    }

    /**
     * 列出目录内容（用于调试）
     */
    private void listDirectoryContents(File dir, String indent) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                log.debug("{}{}  ({})", indent, file.getName(), file.isDirectory() ? "DIR" : "FILE");
                if (file.isDirectory()) {
                    listDirectoryContents(file, indent + "  ");
                }
            }
        }
    }

    /**
     * 递归查找目录中的所有 .java 文件
     */
    private File[] findJavaFiles(File dir) {
        if (!dir.isDirectory()) {
            return new File[0];
        }

        List<File> javaFiles = new ArrayList<>();
        findJavaFilesRecursive(dir, javaFiles);
        return javaFiles.toArray(new File[0]);
    }

    /**
     * 递归查找 Java 文件
     */
    private void findJavaFilesRecursive(File dir, List<File> javaFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findJavaFilesRecursive(file, javaFiles);
                } else if (file.getName().endsWith(JAVA_EXTENSION)) {
                    javaFiles.add(file);
                }
            }
        }
    }

    /**
     * 清理反编译后的代码，移除调试信息
     */
    private String cleanupDecompiledCode(String code) {
        // 按行分割
        String[] lines = code.split("\n");
        StringBuilder cleaned = new StringBuilder();
        boolean startedOutput = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳过开头的调试信息行
            if (!startedOutput && isDebugLine(trimmed)) {
                continue;
            }

            // 一旦遇到非调试信息的内容，就开始输出
            if (!startedOutput && !trimmed.isEmpty() && !isDebugLine(trimmed)) {
                startedOutput = true;
            }

            // 开始输出后，保留所有内容（包括空行）
            if (startedOutput) {
                cleaned.append(line).append("\n");
            }
        }

        return cleaned.toString();
    }

    /**
     * 判断是否为调试信息行
     */
    private boolean isDebugLine(String trimmed) {
        return trimmed.startsWith("Analysing ") ||
                trimmed.startsWith("Processing ") ||
                trimmed.startsWith("Decompiling ") ||
                (trimmed.startsWith("/*") && trimmed.contains("Decompiled with CFR"));
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

            // 优先从缓存中读取（支持嵌套 JAR 中的文件）
            byte[] cachedBytes = classFileCache.get(fileName);
            if (cachedBytes != null) {
                content = new String(cachedBytes, StandardCharsets.UTF_8);
            }
            // 从当前打开的 JAR 文件中读取
            else if (currentJarFile != null) {
                JarEntry entry = currentJarFile.getJarEntry(fileName);
                if (entry != null) {
                    try (InputStream is = currentJarFile.getInputStream(entry)) {
                        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }
            // 从当前打开的 ZIP 文件中读取
            else if (currentZipFile != null) {
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
            } else {
                codeArea.setText("// File not found: " + fileName);
                statusLabel.setText("File not found");
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
        // .java 和 .class 都使用 Java 语法高亮（.class 反编译后是 Java 代码）
        if (name.endsWith(JAVA_EXTENSION) || name.endsWith(CLASS_EXTENSION)) {
            return SyntaxConstants.SYNTAX_STYLE_JAVA;
        }
        if (name.endsWith(".xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
        if (name.endsWith(".json")) return SyntaxConstants.SYNTAX_STYLE_JSON;
        if (name.endsWith(".properties")) return SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
        if (name.endsWith(".yaml") || name.endsWith(".yml")) return SyntaxConstants.SYNTAX_STYLE_YAML;
        if (name.endsWith(".html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
        if (name.endsWith(".js")) return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
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
     * 清空所有内容
     */
    private void clearAll() {
        // 关闭当前打开的文件
        closeCurrentJar();

        // 清空文件路径
        currentFile = null;
        filePathField.setText("");

        // 清空代码区域
        codeArea.setText("");

        // 清空压缩信息
        if (compressionInfoLabel != null) {
            compressionInfoLabel.setText("");
            compressionInfoLabel.setToolTipText(null);
        }

        // 重置树
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_NO_FILE));
        treeModel.setRoot(root);

        // 更新状态
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_READY));

        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_CLEARED));
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
     * 按名称排序树节点
     */
    private void sortTreeByName() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        sortNodeByName(root);
        treeModel.reload();
        expandTree(fileTree, 2);
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SORTED_BY_NAME));
    }

    /**
     * 递归按名称排序节点的子节点
     */
    private void sortNodeByName(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0) {
            return;
        }

        // 获取所有子节点
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }

        // 按名称排序（目录优先，然后按名称字母顺序）
        children.sort((n1, n2) -> {
            Object o1 = n1.getUserObject();
            Object o2 = n2.getUserObject();

            if (o1 instanceof FileNodeData f1 && o2 instanceof FileNodeData f2) {
                // 目录排在文件前面
                if (f1.isDirectory != f2.isDirectory) {
                    return f1.isDirectory ? -1 : 1;
                }
                // 同类型按名称排序
                return f1.name.compareToIgnoreCase(f2.name);
            }
            return 0;
        });

        // 移除所有子节点并按排序后的顺序重新添加
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
            // 递归排序子节点
            sortNodeByName(child);
        }
    }

    /**
     * 按大小排序树节点
     */
    private void sortTreeBySize() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        sortNodeBySize(root);
        treeModel.reload();
        expandTree(fileTree, 2);
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_SORTED_BY_SIZE));
    }

    /**
     * 递归按大小排序节点的子节点
     */
    private void sortNodeBySize(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0) {
            return;
        }

        // 获取所有子节点
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }

        // 按大小排序（降序，大文件在前）
        children.sort((n1, n2) -> {
            Object o1 = n1.getUserObject();
            Object o2 = n2.getUserObject();

            if (o1 instanceof FileNodeData f1 && o2 instanceof FileNodeData f2) {
                // 目录排在文件前面
                if (f1.isDirectory != f2.isDirectory) {
                    return f1.isDirectory ? -1 : 1;
                }
                // 同类型按大小降序排序
                return Long.compare(f2.size, f1.size);
            }
            return 0;
        });

        // 移除所有子节点并按排序后的顺序重新添加
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
            // 递归排序子节点
            sortNodeBySize(child);
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
        boolean isJarFile;
        String fullPath;
        long size; // 文件大小（字节），目录为其所有子文件的总大小

        FileNodeData(String name, boolean isDirectory, boolean isClassFile) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.isClassFile = isClassFile;
            this.isJarFile = false;
            this.size = 0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 文件树渲染器 - 使用彩色 SVG 图标，根据文件类型显示
     */
    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final int ICON_SIZE = 16;

        // 缓存图标以提高性能
        private static final Map<String, Icon> iconCache = new HashMap<>();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof FileNodeData fileData) {
                    Icon icon = getIconForFileData(fileData);
                    if (icon != null) {
                        setIcon(icon);
                    }

                    // 显示文件/目录名称和大小
                    if (fileData.size > 0) {
                        String sizeStr = formatFileSize(fileData.size);

                        // 判断是否是根节点（JAR/ZIP/WAR文件）
                        boolean isRootNode = node.getParent() == null;
                        boolean isArchiveFile = fileData.name.toLowerCase().endsWith(".jar") ||
                                fileData.name.toLowerCase().endsWith(".zip") ||
                                fileData.name.toLowerCase().endsWith(".war");

                        if (isRootNode && isArchiveFile) {
                            // 根节点特殊标记，显示为压缩后的实际大小
                            String compressedLabel = I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_COMPRESSED);
                            setText(fileData.name + " (" + sizeStr + " " + compressedLabel + ")");

                            String tooltipLine1 = I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ROOT_TOOLTIP_LINE1);
                            String tooltipLine2 = I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER_ROOT_TOOLTIP_LINE2);
                            setToolTipText("<html>" + tooltipLine1 + "<br>" + tooltipLine2 + "</html>");
                        } else {
                            setText(fileData.name + " (" + sizeStr + ")");
                            setToolTipText(null);
                        }
                    } else {
                        setText(fileData.name);
                        setToolTipText(null);
                    }
                }
            }

            return this;
        }

        /**
         * 格式化文件大小为人类可读格式
         */
        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }

        /**
         * 根据文件数据获取对应的彩色 SVG 图标
         */
        private Icon getIconForFileData(FileNodeData fileData) {
            String iconKey = "java-file";

            if (fileData.isDirectory) {
                // 目录图标 - 根据展开状态
                iconKey = "group";
            } else if (fileData.isClassFile) {
                // Class 文件也使用 java-file 图标
                iconKey = "java-file";
            } else if (fileData.isJarFile) {
                // JAR/ZIP 文件 - 根据扩展名区分
                String fileName = fileData.name.toLowerCase();
                if (fileName.endsWith(".jar")) {
                    iconKey = "jar-file";
                }
            } else {
                // 其他文件 - 根据扩展名区分
                iconKey = getIconKeyByExtension(fileData.name);
            }

            return getOrCreateIcon(iconKey);
        }

        /**
         * 根据文件扩展名获取图标键
         */
        private String getIconKeyByExtension(String fileName) {
            String lowerName = fileName.toLowerCase();

            // .java 和 .class 都使用 java-file 图标
            if (lowerName.endsWith(JAVA_EXTENSION) || lowerName.endsWith(CLASS_EXTENSION)) {
                return "java-file";
            } else if (lowerName.endsWith(".xml")) {
                return "xml-file";
            } else if (lowerName.endsWith(".json")) {
                return "json-file";
            } else if (lowerName.endsWith(".properties")) {
                return "properties-file";
            } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
                    lowerName.endsWith(".svg")) {
                return "image-file";
            } else {
                return "text-file";
            }
        }

        /**
         * 获取或创建图标（带缓存）
         */
        private Icon getOrCreateIcon(String iconKey) {
            return iconCache.computeIfAbsent(iconKey, key -> new FlatSVGIcon("icons/" + key + ".svg", ICON_SIZE, ICON_SIZE));
        }
    }
}

