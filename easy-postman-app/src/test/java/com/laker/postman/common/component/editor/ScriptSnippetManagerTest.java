package com.laker.postman.common.component.editor;

import com.laker.postman.plugin.api.ScriptCompletionItem;
import com.laker.postman.plugin.api.ScriptCompletionKind;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.snippet.SnippetType;
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
import static org.testng.Assert.assertNull;
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

    @Test(description = "BASIC 中立补全应使用 inputText 作为 provider 输入")
    public void shouldUseInputTextForDirectBasicCompletionItems() {
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink ->
                sink.add(new ScriptCompletionItem(
                        ScriptCompletionKind.BASIC,
                        "pm.direct.basic",
                        "pm.direct.replacement",
                        "Direct basic completion"
                )));

        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        assertNotNull(findCompletion(provider, "pm.direct.basic"));
        assertNull(findCompletion(provider, "pm.direct.replacement"));
    }

    @Test(description = "单个插件补全贡献异常不应阻断后续插件补全")
    public void shouldContinueAfterPluginCompletionContributorThrows() {
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink -> {
            throw new IllegalStateException("broken contributor");
        });
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink ->
                sink.basic("pm.after.failure", "Contributor after failure"));

        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        assertNotNull(findCompletion(provider, "pm.after.failure"));
    }

    @Test(description = "内置代码片段应优先使用 Postman 官方 pm.environment，而不是历史 pm.env 简写")
    public void builtInSnippetsShouldUseCanonicalEnvironmentApi() {
        for (SnippetType snippetType : SnippetType.values()) {
            assertFalse(snippetType.code.contains("pm.env."),
                    snippetType.name() + " should not advertise pm.env in built-in snippets");
        }
    }

    @Test(description = "补全不应提示当前运行时无法可靠支持的 Chai 关键字方法")
    public void shouldNotAdvertiseUnsupportedChaiKeywordMethods() {
        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        assertNull(findCompletion(provider, "expect.instanceof"));
        assertNull(findCompletion(provider, "expect.throw"));
        assertNull(findCompletion(provider, "expect.throw.error"));
    }

    private static Completion findCompletion(DefaultCompletionProvider provider, String inputText) {
        List<Completion> completions = provider.getCompletionByInputText(inputText);
        return completions == null || completions.isEmpty() ? null : completions.get(0);
    }
}
