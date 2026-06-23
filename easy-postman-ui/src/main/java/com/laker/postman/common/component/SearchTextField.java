package com.laker.postman.common.component;

import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 通用搜索输入框组件，带搜索图标、占位符、清除按钮、大小写敏感和整词匹配选项。
 * 使用 FlatLaf 官方的图标样式。
 */
public class SearchTextField extends FlatTextField {
    static final int DEFAULT_WIDTH = 220;
    static final int DEFAULT_HEIGHT = 30;
    static final int MAX_WIDTH = 300;
    static final int MIN_WIDTH = 50;
    static final int OPTION_BUTTON_SIZE = 22;

    private static final String USER_ACTIVATED_FOCUS_INSTALLED =
            SearchTextField.class.getName() + ".userActivatedFocusInstalled";
    private static final String IN_TEXT_FIELD_STYLE_CLASS = "inTextField";

    private final UndoManager undoManager = new UndoManager();
    /**
     * -- GETTER --
     * 获取大小写敏感状态
     */
    @Getter
    private boolean caseSensitive = false;
    /**
     * -- GETTER --
     * 获取整词匹配状态
     */
    @Getter
    private boolean wholeWord = false;

    public SearchTextField() {
        super();
        setLeadingIcon(IconUtil.createThemed("icons/search.svg", 16, 16));
        setPlaceholderText(CommonI18n.get(CommonMessageKeys.BUTTON_SEARCH));
        setShowClearButton(true);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMaximumSize(new Dimension(MAX_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, DEFAULT_HEIGHT));

        // 创建选项按钮工具栏
        initOptionsToolbar();

        // 撤销/重做功能
        Document doc = getDocument();
        doc.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        // 撤销快捷键 (Cmd+Z / Ctrl+Z)
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo");
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    try {
                        undoManager.undo();
                    } catch (CannotUndoException ex) {
                        // ignore
                    }
                }
            }
        });
        // 重做快捷键 (Ctrl+Y / Cmd+Shift+Z)
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo");
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    try {
                        undoManager.redo();
                    } catch (CannotRedoException ex) {
                        // ignore
                    }
                }
            }
        });
    }

    /**
     * 初始化选项按钮工具栏（大小写敏感、整词匹配）
     * 使用 JToolBar 和 SVG 图标，与 FlatLaf 官方 Demo 一致
     */
    private void initOptionsToolbar() {
        JToggleButton caseSensitiveButton = createOptionButton(
                "icons/matchCase.svg",
                "icons/matchCaseHovered.svg",
                "icons/matchCaseSelected.svg",
                UiI18n.get(UiMessageKeys.SEARCH_MATCH_CASE)
        );
        caseSensitiveButton.addActionListener(e -> {
            caseSensitive = caseSensitiveButton.isSelected();
            firePropertyChange("caseSensitive", !caseSensitive, caseSensitive);
        });

        JToggleButton wholeWordButton = createOptionButton(
                "icons/words.svg",
                "icons/wordsHovered.svg",
                "icons/wordsSelected.svg",
                UiI18n.get(UiMessageKeys.SEARCH_MATCH_WHOLE_WORD)
        );
        wholeWordButton.addActionListener(e -> {
            wholeWord = wholeWordButton.isSelected();
            firePropertyChange("wholeWord", !wholeWord, wholeWord);
        });

        // 使用 JToolBar 作为容器，这是 FlatLaf 官方推荐的方式
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 2, 0, 4));
        toolbar.putClientProperty(FlatClientProperties.STYLE_CLASS, IN_TEXT_FIELD_STYLE_CLASS);
        toolbar.add(caseSensitiveButton);
        toolbar.add(wholeWordButton);

        // 设置为 trailing component
        putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolbar);
    }

    private JToggleButton createOptionButton(String iconPath, String rolloverIconPath, String selectedIconPath,
                                             String tooltipText) {
        JToggleButton button = new JToggleButton(new FlatSVGIcon(iconPath, 16, 16));
        button.setRolloverIcon(new FlatSVGIcon(rolloverIconPath, 16, 16));
        button.setSelectedIcon(new FlatSVGIcon(selectedIconPath, 16, 16));
        button.setToolTipText(tooltipText);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(OPTION_BUTTON_SIZE, OPTION_BUTTON_SIZE));
        button.setMinimumSize(new Dimension(OPTION_BUTTON_SIZE, OPTION_BUTTON_SIZE));
        button.setMaximumSize(new Dimension(OPTION_BUTTON_SIZE, OPTION_BUTTON_SIZE));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.putClientProperty(FlatClientProperties.STYLE_CLASS, IN_TEXT_FIELD_STYLE_CLASS);
        return button;
    }

    /**
     * 设置搜索无结果状态：无结果时输入框变红，有结果时恢复正常（同 IDEA 行为）。
     *
     * @param noResult true=无结果（红色边框），false=有结果或清空（恢复正常）
     */
    public void setNoResult(boolean noResult) {
        putClientProperty(FlatClientProperties.OUTLINE, noResult ? FlatClientProperties.OUTLINE_ERROR : null);
    }

    /**
     * Prevents the search field from taking initial window focus until the user explicitly clicks it.
     */
    public void installUserActivatedFocus() {
        if (Boolean.TRUE.equals(getClientProperty(USER_ACTIVATED_FOCUS_INSTALLED))) {
            return;
        }
        putClientProperty(USER_ACTIVATED_FOCUS_INSTALLED, true);
        setFocusable(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setFocusable(true);
                SwingUtilities.invokeLater(SearchTextField.this::requestFocusInWindow);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    setFocusable(false);
                }
            }
        });
    }

}
