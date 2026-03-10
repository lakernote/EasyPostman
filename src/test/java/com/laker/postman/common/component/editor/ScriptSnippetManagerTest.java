package com.laker.postman.common.component.editor;

import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ScriptSnippetManagerTest {

    @Test(description = "在已有脚本中间编辑时，自动补全应基于光标前字符触发")
    public void testAutoActivateUsesCaretPositionInsideExistingScript() {
        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();
        JTextArea textArea = new JTextArea("const token = pm.response.text();");

        int caretAfterPmDot = textArea.getText().indexOf("pm.") + "pm.".length();
        textArea.setCaretPosition(caretAfterPmDot);

        assertTrue(provider.isAutoActivateOkay(textArea));
    }

    @Test(description = "光标在开头时不应触发自动补全")
    public void testAutoActivateReturnsFalseAtDocumentStart() {
        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();
        JTextArea textArea = new JTextArea("pm.response.json()");

        textArea.setCaretPosition(0);

        assertFalse(provider.isAutoActivateOkay(textArea));
    }
}
