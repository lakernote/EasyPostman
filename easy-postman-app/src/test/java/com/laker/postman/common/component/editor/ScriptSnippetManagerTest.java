package com.laker.postman.common.component.editor;

import com.laker.postman.plugin.runtime.PluginRuntime;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ScriptSnippetManagerTest {

    @AfterMethod
    public void clearPluginContributors() {
        PluginRuntime.getRegistry().clear();
    }

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

    @Test(description = "插件中立补全贡献最终应适配成 RSyntax basic/shorthand completion")
    public void shouldAdaptNeutralPluginCompletionsToEditorProvider() {
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink -> {
            sink.basic("pm.neutral.basic", "Neutral basic completion");
            sink.shorthand("neutral.snippet", "pm.neutral.run();", "Neutral shorthand completion");
        });

        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        Completion basic = findCompletion(provider, "pm.neutral.basic");
        assertNotNull(basic);
        assertTrue(basic instanceof BasicCompletion);
        assertFalse(basic instanceof ShorthandCompletion);

        Completion shorthand = findCompletion(provider, "neutral.snippet");
        assertNotNull(shorthand);
        assertTrue(shorthand instanceof ShorthandCompletion);
        assertTrue("pm.neutral.run();".equals(shorthand.getReplacementText()));
    }

    private static Completion findCompletion(DefaultCompletionProvider provider, String inputText) {
        List<Completion> completions = provider.getCompletionByInputText(inputText);
        return completions.isEmpty() ? null : completions.get(0);
    }
}
