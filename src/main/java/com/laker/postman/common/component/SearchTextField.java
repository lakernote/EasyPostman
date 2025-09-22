package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 通用搜索输入框组件，带搜索图标、占位符、清除按钮和固定宽度。
 */
public class SearchTextField extends FlatTextField {
    private final UndoManager undoManager = new UndoManager();

    public SearchTextField() {
        super();
        setLeadingIcon(new FlatSVGIcon("icons/search.svg", 16, 16));
        setPlaceholderText(I18nUtil.getMessage(MessageKeys.BUTTON_SEARCH));
        setShowClearButton(true);
        setPreferredSize(new Dimension(180, 28));
        setMaximumSize(new Dimension(180, 28));
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
}