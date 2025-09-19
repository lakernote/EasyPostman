package com.laker.postman.common.component;

import com.laker.postman.model.VariableSegment;
import com.laker.postman.util.EasyPostmanVariableUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 1.支持 Postman 风格变量高亮和悬浮提示的文本输入框
 * 变量格式：{{varName}}
 * 已定义的变量显示为蓝色背景，未定义的变量显示为红色背景
 * 鼠标悬浮在变量上时显示变量值的提示
 * 2.支持win和mac下的撤回重做快捷键
 * win：Ctrl+Z 撤回，Ctrl+Y 重做
 * mac：Cmd+Z 撤回，Cmd+Shift+Z 重做
 */
@Slf4j
public class EasyPostmanTextField extends JTextField {
    // Postman 风格颜色
    private static final Color DEFINED_VAR_BG = new Color(180, 210, 255, 120); // 半透明淡蓝
    private static final Color DEFINED_VAR_BORDER = new Color(80, 150, 255); // 蓝色边框
    private static final Color UNDEFINED_VAR_BG = new Color(255, 200, 200, 120); // 半透明红
    private static final Color UNDEFINED_VAR_BORDER = new Color(255, 100, 100); // 红色
    private final UndoManager undoManager = new UndoManager();

    public EasyPostmanTextField(int columns) {
        super(columns);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");
        initUndoRedo();

    }

    public EasyPostmanTextField(String text, int columns) {
        super(text, columns);
        // 启用 ToolTip 支持，必须设置（即使内容为空）
        setToolTipText("");
        initUndoRedo();
    }


    @Override
    protected void paintComponent(Graphics g) {
        // 先绘制文本、光标、选区
        super.paintComponent(g);

        String value = getText();
        List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(value);
        if (segments.isEmpty()) return;

        try {
            Rectangle startRect = modelToView2D(0).getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int baseY = startRect.y;
            int h = fm.getHeight();
            int last = 0;

            for (VariableSegment seg : segments) {
                // 计算变量前的文本宽度
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                // 判断变量状态
                boolean isDefined = EasyPostmanVariableUtil.isVariableDefined(seg.name);
                Color bgColor = isDefined ? DEFINED_VAR_BG : UNDEFINED_VAR_BG;
                Color borderColor = isDefined ? DEFINED_VAR_BORDER : UNDEFINED_VAR_BORDER;
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                // 只绘制变量的半透明背景和边框，不绘制文本
                g.setColor(bgColor);
                g.fillRoundRect(x, baseY, varWidth, h, 8, 8);
                g.setColor(borderColor);
                g.drawRoundRect(x, baseY, varWidth, h, 8, 8);
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("paintComponent", e);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        String value = getText();
        List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(value);
        if (segments.isEmpty()) return super.getToolTipText(event);
        try {
            int mouseX = event.getX();
            Rectangle startRect = modelToView2D(0).getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int last = 0;
            for (VariableSegment seg : segments) {
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                if (mouseX >= x && mouseX <= x + varWidth) {
                    // 鼠标悬浮在变量上
                    String varName = seg.name;
                    String varValue = EasyPostmanVariableUtil.getVariableValue(varName);
                    if (varValue != null) {
                        return varName + " = " + varValue;
                    } else {
                        return "[" + varName + "] not found";
                    }
                }
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("getToolTipText", e);
        }
        return super.getToolTipText(event);
    }

    private void initUndoRedo() {
        getDocument().addUndoableEditListener(undoManager);

        // Undo
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) undoManager.undo();
                } catch (CannotUndoException ignored) {
                }
            }
        });

        // Redo
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) undoManager.redo();
                } catch (CannotRedoException ignored) {
                }
            }
        });
    }
}