package com.laker.postman.common.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.model.Snippet;
import com.laker.postman.model.SnippetType;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
                        case PRE_SCRIPT -> label.setIcon(new FlatSVGIcon("icons/arrow-up.svg", 16, 16));
                        case ASSERT -> label.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
                        case EXTRACT -> label.setIcon(new FlatSVGIcon("icons/arrow-down.svg", 16, 16));
                        case LOCAL_VAR -> label.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
                        case ENV_VAR -> label.setIcon(new FlatSVGIcon("icons/environments.svg", 16, 16));
                        case ENCODE -> label.setIcon(new FlatSVGIcon("icons/format.svg", 16, 16));
                        case ENCRYPT -> label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16));
                        case ARRAY -> label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                        case JSON -> label.setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                        case DATE -> label.setIcon(new FlatSVGIcon("icons/time.svg", 16, 16));
                        case LOG -> label.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
                        case REGEX -> label.setIcon(new FlatSVGIcon("icons/search.svg", 16, 16));
                        case STRING -> label.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
                        case CONTROL -> label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                        case TOKEN -> label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16));
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
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(new Color(245, 245, 245));
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
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
                if (category.equals("全部分类")) {
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
        snippetList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
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
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // 按下方向键时，转移焦点到列表并选择第一项
                    snippetList.requestFocusInWindow();
                    if (listModel.getSize() > 0 && snippetList.getSelectedIndex() == -1) {
                        snippetList.setSelectedIndex(0);
                    }
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

        closeBtn.addActionListener(e -> dispose());

        // 初始化状态
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
        }

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
                    case PRE_SCRIPT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PRE_SCRIPT);
                    case ASSERT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ASSERT);
                    case EXTRACT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXTRACT);
                    case LOCAL_VAR -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOCAL_VAR);
                    case ENV_VAR -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENV_VAR);
                    case ENCRYPT -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCRYPT);
                    case ENCODE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCODE);
                    case STRING -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_STRING);
                    case ARRAY -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ARRAY);
                    case JSON -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_JSON);
                    case DATE -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATE);
                    case REGEX -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_REGEX);
                    case LOG -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOG);
                    case CONTROL -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_CONTROL);
                    case TOKEN -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_TOKEN);
                    default -> I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_OTHER);
                }));
        String[] orderedCategories = {
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_PRE_SCRIPT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ASSERT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_EXTRACT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_LOCAL_VAR),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENV_VAR),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCRYPT),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ENCODE),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_STRING),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_ARRAY),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_JSON),
                I18nUtil.getMessage(MessageKeys.SNIPPET_DIALOG_CATEGORY_DATE),
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
