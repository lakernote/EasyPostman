package com.laker.postman.common.component.table;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.InputMethodEvent;
import java.text.AttributedString;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class EasySmartValueCellEditorTest extends AbstractSwingUiTest {

    private static final String LONG_TEXT = "这是一段会明显超过当前单元格宽度并触发自动换行的中文输入内容";

    @Test
    public void shouldSwitchBetweenShortAndLongEditorsAfterInputSettles() throws Exception {
        AtomicReference<EasySmartValueCellEditor> editorRef = new AtomicReference<>();
        AtomicReference<JTable> tableRef = new AtomicReference<>();
        AtomicReference<JTextArea> textAreaRef = new AtomicReference<>();
        int[] originalRowHeight = new int[1];

        SwingUtilities.invokeAndWait(() -> {
            JTable table = createNarrowTable();
            EasySmartValueCellEditor editor = new EasySmartValueCellEditor();
            editor.getTableCellEditorComponent(table, "短文本", false, 0, 1);
            editor.containerPanel.setSize(80, table.getRowHeight());

            assertTrue(editor.textField.isVisible(), "短内容应使用 JTextField");
            originalRowHeight[0] = table.getRowHeight(0);
            editor.textField.setText(LONG_TEXT);

            editorRef.set(editor);
            tableRef.set(table);
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            JTable table = tableRef.get();
            JScrollPane scrollPane = findMultiLineScrollPane(editor);
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

            assertFalse(editor.textField.isVisible());
            assertTrue(scrollPane.isVisible(), "长内容应切换为 JTextArea");
            assertEquals(editor.getCellEditorValue(), LONG_TEXT);
            assertTrue(table.getRowHeight(0) > originalRowHeight[0], "长内容应立即展开行高");

            textAreaRef.set(textArea);
            textArea.setText("短文本");
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            JTable table = tableRef.get();
            assertTrue(editor.textField.isVisible(), "内容重新变短后应切回 JTextField");
            assertFalse(findMultiLineScrollPane(editor).isVisible());
            assertEquals(editor.getCellEditorValue(), "短文本");
            assertEquals(table.getRowHeight(0), originalRowHeight[0]);
            assertEquals(textAreaRef.get().getText(), "短文本");
        });
    }

    @Test
    public void shouldDeferComponentSwitchUntilInputMethodCompositionFinishes() throws Exception {
        AtomicReference<EasySmartValueCellEditor> editorRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JTable table = createNarrowTable();
            EasySmartValueCellEditor editor = new EasySmartValueCellEditor();
            editor.getTableCellEditorComponent(table, "短文本", false, 0, 1);
            editor.containerPanel.setSize(80, table.getRowHeight());

            editor.updateInputMethodComposition(inputMethodEvent(editor, "zhong", 0));
            editor.textField.setText(LONG_TEXT);
            editorRef.set(editor);
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() ->
                assertTrue(editorRef.get().textField.isVisible(),
                        "输入法仍在组合文字时不能切换焦点组件")
        );

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            editor.updateInputMethodComposition(inputMethodEvent(editor, "中", 1));
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            assertFalse(editor.textField.isVisible());
            assertTrue(findMultiLineScrollPane(editor).isVisible(),
                    "输入法完成组合后应恢复自动多行切换");
        });
    }

    private InputMethodEvent inputMethodEvent(EasySmartValueCellEditor editor,
                                              String text,
                                              int committedCharacterCount) {
        return new InputMethodEvent(
                editor.textField,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                new AttributedString(text).getIterator(),
                committedCharacterCount,
                null,
                null
        );
    }

    private JTable createNarrowTable() {
        JTable table = new JTable(1, 2);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setWidth(80);
        return table;
    }

    private JScrollPane findMultiLineScrollPane(EasySmartValueCellEditor editor) {
        for (Component component : editor.containerPanel.getComponents()) {
            if (component instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
        }
        throw new AssertionError("应当存在多行编辑器滚动面板");
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Drain the event-driven mode reevaluation.
        });
        SwingUtilities.invokeAndWait(() -> {
            // Drain nested focus/layout callbacks.
        });
    }
}
