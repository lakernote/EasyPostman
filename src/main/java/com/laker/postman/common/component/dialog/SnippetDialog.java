package com.laker.postman.common.component.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.Snippet;
import com.laker.postman.model.SnippetType;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码片段弹窗，基于 ListModel
 */
public class SnippetDialog extends JDialog {
    private final JList<Snippet> snippetList;
    private final DefaultListModel<Snippet> listModel;
    private final JTextField searchField;
    private final JTextArea previewArea;
    private final JLabel descriptionLabel;
    private final JComboBox<String> categoryCombo;
    private final Map<String, List<Snippet>> snippetCategories = new LinkedHashMap<>();
    @Getter
    private Snippet selectedSnippet;
    List<Snippet> snippets;

    private static List<Snippet> getI18nSnippets() {
        return Arrays.stream(SnippetType.values())
                .map(Snippet::new)
                .toList();
    }


    public SnippetDialog() {
        super(SingletonFactory.getInstance(MainFrame.class), I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_TITLE), true);
        Frame owner = SingletonFactory.getInstance(MainFrame.class);
        setLayout(new BorderLayout(10, 10));

        // 初始化片段数据
        snippets = getI18nSnippets();
        // 初始化分类
        initCategories();

        // 创建北部面板：搜索框和分类选择器
        JPanel northPanel = new JPanel(new BorderLayout(5, 0));
        northPanel.setBorder(new EmptyBorder(10, 10, 5, 10));

        // 搜索框带图标和提示
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new SearchTextField();

        // 添加搜索图标
        searchPanel.add(searchField, BorderLayout.CENTER);

        // 下拉分类选择器
        String[] categories = snippetCategories.keySet().toArray(new String[0]);
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setPreferredSize(new Dimension(150, 30));

        northPanel.add(searchPanel, BorderLayout.CENTER);
        northPanel.add(categoryCombo, BorderLayout.EAST);

        // 创建中部面板：片段列表
        listModel = new DefaultListModel<>();
        loadSnippets(snippets);

        snippetList = new JList<>(listModel);
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setVisibleRowCount(8);

        // 自定义渲染器，让列表项更美观
        snippetList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Snippet snippet) {
                    label.setText(snippet.title);
                    label.setBorder(new EmptyBorder(5, 10, 5, 10));
                    // 根据类型设置图标
                    switch (snippet.type.type) {
                        // 基础分类
                        case PRE_SCRIPT -> label.setIcon(new FlatSVGIcon("icons/arrow-up.svg", 16, 16));
                        case ASSERT -> label.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
                        case EXTRACT -> label.setIcon(new FlatSVGIcon("icons/arrow-down.svg", 16, 16));
                        case LOCAL_VAR -> label.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
                        case ENV_VAR -> label.setIcon(new FlatSVGIcon("icons/environments.svg", 16, 16));

                        // 编码和加密
                        case ENCODE -> label.setIcon(new FlatSVGIcon("icons/format.svg", 16, 16));
                        case ENCRYPT -> label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16));

                        // 数据类型
                        case ARRAY -> label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                        case JSON -> label.setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                        case STRING -> label.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
                        case DATE -> label.setIcon(new FlatSVGIcon("icons/time.svg", 16, 16));
                        case REGEX -> label.setIcon(new FlatSVGIcon("icons/search.svg", 16, 16));

                        // 其他基础
                        case LOG -> label.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
                        case CONTROL -> label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                        case TOKEN -> label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16));
                        case COOKIES -> label.setIcon(new FlatSVGIcon("icons/cookie.svg", 16, 16));

                        // 内置库分类
                        case CRYPTOJS -> label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16));
                        case LODASH -> label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                        case MOMENT -> label.setIcon(new FlatSVGIcon("icons/time.svg", 16, 16));

                        // 高级场景分类
                        case AUTHENTICATION -> label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16));
                        case PERFORMANCE -> label.setIcon(new FlatSVGIcon("icons/performance.svg", 16, 16));
                        case VALIDATION -> label.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
                        case DATA_PROCESSING -> label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                        case REQUEST_MODIFICATION -> label.setIcon(new FlatSVGIcon("icons/edit.svg", 16, 16));
                        case EXAMPLES -> label.setIcon(new FlatSVGIcon("icons/collections.svg", 16, 16));

                        // 其他
                        case OTHER -> label.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
                        default -> label.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
                    }
                }
                return label;
            }
        });

        JScrollPane listScrollPane = new JScrollPane(snippetList);
        listScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 15, 0));

        // 创建南部面板：预览区域和按钮
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        // 预览区域
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_PREVIEW_TITLE)));

        previewArea = new JTextArea(8, 40);
        previewArea.setEditable(false);
        previewArea.setLineWrap(false);  // 代码不应该自动换行
        previewArea.setBackground(new Color(245, 245, 245));
        previewArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1)); // 比标准字体大1号
        previewArea.setTabSize(4);  // 设置 Tab 缩进为 4 个空格
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
        previewScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);

        // 描述标签
        descriptionLabel = new JLabel(" ");
        descriptionLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        previewPanel.add(descriptionLabel, BorderLayout.SOUTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton insertBtn = new JButton(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_INSERT));
        insertBtn.setPreferredSize(new Dimension(100, 30));

        JButton closeBtn = new JButton(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CLOSE));
        closeBtn.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(insertBtn);
        buttonPanel.add(closeBtn);

        southPanel.add(previewPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 将分割面板添加到主面板
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                listScrollPane,
                southPanel
        );
        splitPane.setResizeWeight(0.3); // 设置左右比例
        splitPane.setDividerLocation(230); // 设置初始分割位置

        // 添加到对话框
        add(northPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // 绑定事件监听器

        // 搜索框事件
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterSnippets();
            }

            public void removeUpdate(DocumentEvent e) {
                filterSnippets();
            }

            public void changedUpdate(DocumentEvent e) {
                filterSnippets();
            }
        });

        // 分类选择器事件
        categoryCombo.addActionListener(e -> {
            String category = (String) categoryCombo.getSelectedItem();
            if (category != null) {
                if (category.equals(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ALL))) {
                    loadSnippets(snippets);
                } else {
                    loadSnippets(snippetCategories.get(category));
                }

                // 如果有搜索关键词，则还需要过滤
                if (!searchField.getText().trim().isEmpty()) {
                    filterSnippets();
                } else {
                    // 如果列表不为空，自动选择第一个并显示预览
                    if (listModel.getSize() > 0) {
                        snippetList.setSelectedIndex(0);
                        selectedSnippet = snippetList.getSelectedValue();
                        if (selectedSnippet != null) {
                            previewArea.setText(selectedSnippet.code);
                            previewArea.setCaretPosition(0);
                            descriptionLabel.setText(selectedSnippet.desc);
                        }
                    } else {
                        // 如果列表为空，清空预览区域
                        previewArea.setText("");
                        descriptionLabel.setText("");
                    }
                }
            }
        });

        // 列表选择事件
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedSnippet = snippetList.getSelectedValue();
                if (selectedSnippet != null) {
                    previewArea.setText(selectedSnippet.code);
                    previewArea.setCaretPosition(0);
                    descriptionLabel.setText(selectedSnippet.desc);
                }
            }
        });

        // 列表双击事件
        snippetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // 双击事件
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                }
            }
        });

        // 键盘事件
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
                    // 按下方向键时，转移焦点到列表并选择第一项
                    snippetList.requestFocusInWindow();
                    if (listModel.getSize() > 0 && snippetList.getSelectedIndex() == -1) {
                        snippetList.setSelectedIndex(0);
                    }
                    e.consume();  // 消费事件，防止其他组件处理
                }
            }
        });

        snippetList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // 按下回车键时，如果有选中项，则选择并关闭对话框
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // ESC 键取消并关闭对话框
                    selectedSnippet = null;
                    dispose();
                }
            }
        });

        // 按钮事件
        insertBtn.addActionListener(e -> {
            selectedSnippet = snippetList.getSelectedValue();
            if (selectedSnippet != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_SELECT_SNIPPET_FIRST), I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_TIP), JOptionPane.INFORMATION_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> {
            selectedSnippet = null;
            dispose();
        });

        // 初始化状态
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
            // 显示第一项的预览
            selectedSnippet = snippetList.getSelectedValue();
            if (selectedSnippet != null) {
                previewArea.setText(selectedSnippet.code);
                previewArea.setCaretPosition(0);
                descriptionLabel.setText(selectedSnippet.desc);
            }
        }

        // 添加窗口关闭监听器，处理点击 X 按钮的情况
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                selectedSnippet = null;
            }

            @Override
            public void windowOpened(WindowEvent e) {
                // 对话框打开时，搜索框自动获得焦点
                searchField.requestFocusInWindow();
            }
        });

        // 设置对话框属性
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(600, 400));
    }

    // 初始化代码片段分类
    private void initCategories() {
        snippetCategories.put(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ALL), this.snippets);
        Map<String, List<Snippet>> categorized = this.snippets.stream()
                .collect(Collectors.groupingBy(snippet -> switch (snippet.type.type) {
                    // 基础分类
                    case PRE_SCRIPT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PRE_SCRIPT);
                    case ASSERT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ASSERT);
                    case EXTRACT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXTRACT);
                    case LOCAL_VAR -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOCAL_VAR);
                    case ENV_VAR -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENV_VAR);

                    // 高级场景分类
                    case AUTHENTICATION -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_AUTHENTICATION);
                    case PERFORMANCE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PERFORMANCE);
                    case VALIDATION -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_VALIDATION);
                    case DATA_PROCESSING -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATA_PROCESSING);
                    case REQUEST_MODIFICATION ->
                            I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REQUEST_MODIFICATION);

                    // Cookie 操作
                    case COOKIES -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_COOKIES);

                    // 编码和加密
                    case ENCRYPT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCRYPT);
                    case ENCODE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCODE);

                    // 数据类型
                    case STRING -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_STRING);
                    case ARRAY -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ARRAY);
                    case JSON -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_JSON);
                    case DATE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATE);
                    case REGEX -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REGEX);

                    // 内置库分类
                    case CRYPTOJS -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CRYPTOJS);
                    case LODASH -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LODASH);
                    case MOMENT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_MOMENT);

                    // 完整示例
                    case EXAMPLES -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXAMPLES);

                    // 其他
                    case LOG -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOG);
                    case CONTROL -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CONTROL);
                    case TOKEN -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_TOKEN);
                    case OTHER -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER);
                    default -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER);
                }));

        // 按照使用频率和重要性排序的分类列表
        String[] orderedCategories = {
                // 常用基础
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PRE_SCRIPT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ASSERT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXTRACT),

                // 高级场景（新增，放在前面）
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_AUTHENTICATION),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PERFORMANCE),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_VALIDATION),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATA_PROCESSING),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REQUEST_MODIFICATION),

                // 变量和Cookie
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOCAL_VAR),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENV_VAR),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_COOKIES),

                // 数据类型
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_STRING),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ARRAY),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_JSON),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATE),

                // 内置库
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CRYPTOJS),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LODASH),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_MOMENT),

                // 编码加密
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCRYPT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCODE),

                // 完整示例
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXAMPLES),

                // 其他辅助
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REGEX),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CONTROL),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOG),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_TOKEN),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER)
        };

        for (String category : orderedCategories) {
            List<Snippet> categorySnippets = categorized.get(category);
            if (categorySnippets != null && !categorySnippets.isEmpty()) {
                snippetCategories.put(category, categorySnippets);
            }
        }
    }

    // 加载片段到列表
    private void loadSnippets(List<Snippet> snippetsToLoad) {
        listModel.clear();
        for (Snippet s : snippetsToLoad) {
            listModel.addElement(s);
        }
    }

    // 根据搜索词过滤片段
    private void filterSnippets() {
        String query = searchField.getText().trim().toLowerCase();
        String currentCategory = (String) categoryCombo.getSelectedItem();
        List<Snippet> searchSource;
        if (currentCategory != null && !currentCategory.equals(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ALL))) {
            searchSource = snippetCategories.get(currentCategory);
        } else {
            searchSource = this.snippets;
        }

        // 搜索框为空时，显示当前分类的所有片段
        if (query.isEmpty()) {
            loadSnippets(searchSource);
            return;
        }

        // 在当前选中分类中搜索
        DefaultListModel<Snippet> filteredModel = new DefaultListModel<>();
        for (Snippet s : searchSource) {
            if (s.title.toLowerCase().contains(query) ||
                    (s.desc != null && s.desc.toLowerCase().contains(query)) ||
                    (s.code != null && s.code.toLowerCase().contains(query))) {
                filteredModel.addElement(s);
            }
        }

        // 更新列表模型
        listModel.clear();
        for (int i = 0; i < filteredModel.getSize(); i++) {
            listModel.addElement(filteredModel.getElementAt(i));
        }

        // 如果有结果，选择第一个
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
            // 显示预览
            selectedSnippet = snippetList.getSelectedValue();
            previewArea.setText(selectedSnippet.code);
            previewArea.setCaretPosition(0);
            descriptionLabel.setText(selectedSnippet.desc);
        } else {
            // 没有结果时清空预览
            previewArea.setText("");
            descriptionLabel.setText(I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_NOT_FOUND));
        }
    }
}
