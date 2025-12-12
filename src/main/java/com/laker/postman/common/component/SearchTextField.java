package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 通用搜索输入框组件，带搜索图标、占位符、清除按钮、大小写敏感和整词匹配选项。
 * 使用 FlatLaf 官方的图标样式。
 */
public class SearchTextField extends FlatTextField {
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
        setLeadingIcon(new FlatSVGIcon("icons/search.svg", 16, 16));
        setPlaceholderText(I18nUtil.getMessage(MessageKeys.BUTTON_SEARCH));
        setShowClearButton(true);
        setPreferredSize(new Dimension(220, 28));
        setMaximumSize(new Dimension(220, 28));

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
        JToggleButton wholeWordButton;
        JToggleButton caseSensitiveButton;
        // 大小写敏感按钮 (Cc 图标)
        caseSensitiveButton = new JToggleButton(new FlatSVGIcon("icons/matchCase.svg"));
        caseSensitiveButton.setRolloverIcon(new FlatSVGIcon("icons/matchCaseHovered.svg"));
        caseSensitiveButton.setSelectedIcon(new FlatSVGIcon("icons/matchCaseSelected.svg"));
        caseSensitiveButton.setToolTipText("Match Case (大小写敏感)");
        caseSensitiveButton.addActionListener(e -> {
            caseSensitive = caseSensitiveButton.isSelected();
            firePropertyChange("caseSensitive", !caseSensitive, caseSensitive);
        });

        // 整词匹配按钮 (W 图标)
        wholeWordButton = new JToggleButton(new FlatSVGIcon("icons/words.svg"));
        wholeWordButton.setRolloverIcon(new FlatSVGIcon("icons/wordsHovered.svg"));
        wholeWordButton.setSelectedIcon(new FlatSVGIcon("icons/wordsSelected.svg"));
        wholeWordButton.setToolTipText("Match Whole Word (整词匹配)");
        wholeWordButton.addActionListener(e -> {
            wholeWord = wholeWordButton.isSelected();
            firePropertyChange("wholeWord", !wholeWord, wholeWord);
        });

        // 使用 JToolBar 作为容器，这是 FlatLaf 官方推荐的方式
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(null);
        toolbar.add(caseSensitiveButton);
        toolbar.add(wholeWordButton);

        // 设置为 trailing component
        putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolbar);
    }

}