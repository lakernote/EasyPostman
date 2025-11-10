package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.dialog.SnippetDialog;
import com.laker.postman.model.Snippet;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class ScriptPanel extends JPanel {
    private final RSyntaxTextArea prescriptArea;
    private final RSyntaxTextArea postscriptArea;
    private final JTabbedPane tabbedPane;
    private final JButton snippetBtn;

    public ScriptPanel() {
        setLayout(new BorderLayout());

        // 初始化并配置 PreScript 编辑器
        prescriptArea = new RSyntaxTextArea(6, 40);
        configureEditor(prescriptArea);

        // 初始化并配置 PostScript 编辑器
        postscriptArea = new RSyntaxTextArea(6, 40);
        configureEditor(postscriptArea);

        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SCRIPT_TAB_PRESCRIPT), new RTextScrollPane(prescriptArea));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SCRIPT_TAB_POSTSCRIPT), new RTextScrollPane(postscriptArea));

        // 创建帮助区域
        JTextArea helpArea = new JTextArea();
        helpArea.setEditable(false);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        helpArea.setMargin(new Insets(10, 10, 10, 10));
        helpArea.setText(I18nUtil.getMessage(MessageKeys.SCRIPT_HELP_TEXT));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SCRIPT_TAB_HELP), new JScrollPane(helpArea));

        add(tabbedPane, BorderLayout.CENTER);

        // 右下角添加 Snippets 按钮
        snippetBtn = new JButton(I18nUtil.getMessage(MessageKeys.SCRIPT_BUTTON_SNIPPETS));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btnPanel.add(snippetBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // 标签页切换监听器 - 只在脚本标签页显示 Snippets 按钮
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            snippetBtn.setVisible(selectedIndex == 0 || selectedIndex == 1);
        });

        // Snippets 按钮点击事件
        snippetBtn.addActionListener(e -> openSnippetDialog());
    }

    /**
     * 配置编辑器的通用设置
     */
    private void configureEditor(RSyntaxTextArea area) {
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);  // 启用抗锯齿，文字更清晰
        area.setAutoIndentEnabled(true);  // 启用自动缩进
        area.setBracketMatchingEnabled(true);  // 启用括号匹配
        area.setPaintTabLines(true);  // 显示缩进参考线
        area.setMarkOccurrences(true);  // 高亮显示相同的标识符
        area.setTabSize(4);  // 设置 Tab 为 4 个空格
        loadEditorTheme(area);
        addAutoCompletion(area);
    }


    /**
     * 打开代码片段对话框
     */
    private void openSnippetDialog() {
        int tab = tabbedPane.getSelectedIndex();
        if (tab != 0 && tab != 1) {
            return;  // 不在脚本标签页，不执行
        }

        SnippetDialog dialog = new SnippetDialog();
        dialog.setVisible(true);
        Snippet selected = dialog.getSelectedSnippet();

        if (selected != null) {
            RSyntaxTextArea targetArea = (tab == 0) ? prescriptArea : postscriptArea;

            // 智能插入：如果有选中文本，替换它；否则在光标位置插入
            String selectedText = targetArea.getSelectedText();
            String codeToInsert = selected.code;

            // 如果光标不在行首且前面有内容，添加换行
            int caretPosition = targetArea.getCaretPosition();
            try {
                int lineStart = targetArea.getLineStartOffsetOfCurrentLine();
                if (caretPosition > lineStart && selectedText == null) {
                    String lineText = targetArea.getText(lineStart, caretPosition - lineStart);
                    if (!lineText.trim().isEmpty()) {
                        codeToInsert = "\n" + codeToInsert;
                    }
                }
            } catch (Exception ex) {
                log.error("Error calculating insert position", ex);
            }

            targetArea.replaceSelection(codeToInsert);
            targetArea.requestFocusInWindow();
        }
    }

    public void setPrescript(String text) {
        prescriptArea.setText(text);
    }

    public void setPostscript(String text) {
        postscriptArea.setText(text);
    }

    public String getPrescript() {
        return prescriptArea.getText();
    }

    public String getPostscript() {
        return postscriptArea.getText();
    }

    /**
     * 添加脏监听，内容变更时回调
     */
    public void addDirtyListeners(Runnable dirtyCallback) {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }

            public void removeUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }

            public void changedUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }
        };
        prescriptArea.getDocument().addDocumentListener(listener);
        postscriptArea.getDocument().addDocumentListener(listener);
    }


    private void loadEditorTheme(RSyntaxTextArea area) {
        // 尝试加载主题文件
        // 这里使用 IntelliJ IDEA 风格的主题
        // dark.xml
        // vs.xml
        try (InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/vs.xml")) {
            if (in != null) {
                Theme theme = Theme.load(in);
                theme.apply(area);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 为 RSyntaxTextArea 添加自动补全、悬浮提示和代码片段
     */
    private void addAutoCompletion(RSyntaxTextArea area) {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        // 常用 JS/pm/postman 相关补全
        provider.addCompletion(new BasicCompletion(provider, "pm", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM)));
        provider.addCompletion(new BasicCompletion(provider, "postman", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_POSTMAN)));
        provider.addCompletion(new BasicCompletion(provider, "request", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_REQUEST)));
        provider.addCompletion(new BasicCompletion(provider, "response", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE)));
        provider.addCompletion(new BasicCompletion(provider, "env", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENV)));
        provider.addCompletion(new BasicCompletion(provider, "responseBody", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE_BODY)));
        provider.addCompletion(new BasicCompletion(provider, "responseHeaders", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "status", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "statusCode", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_STATUS_CODE)));
        provider.addCompletion(new BasicCompletion(provider, "setEnvironmentVariable", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SET_ENV)));
        provider.addCompletion(new BasicCompletion(provider, "getEnvironmentVariable", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_GET_ENV)));
        provider.addCompletion(new BasicCompletion(provider, "if", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_IF)));
        provider.addCompletion(new BasicCompletion(provider, "else", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ELSE)));
        provider.addCompletion(new BasicCompletion(provider, "for", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_FOR)));
        provider.addCompletion(new BasicCompletion(provider, "while", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_WHILE)));
        provider.addCompletion(new BasicCompletion(provider, "function", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_FUNCTION)));
        provider.addCompletion(new BasicCompletion(provider, "return", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RETURN)));
        // 代码片段 JsPolyfillInjector
        provider.addCompletion(new ShorthandCompletion(provider, "pm.environment.set", "pm.environment.set('key', 'value');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_SET_ENV)));
        provider.addCompletion(new ShorthandCompletion(provider, "pm.environment.get", "pm.environment.get('key');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_GET_ENV)));
        provider.addCompletion(new ShorthandCompletion(provider, "btoa", "btoa('String');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_BTOA)));
        provider.addCompletion(new ShorthandCompletion(provider, "atob", "atob('Base64');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ATOB)));
        provider.addCompletion(new ShorthandCompletion(provider, "encodeURIComponent", "encodeURIComponent('String');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ENCODE_URI)));
        provider.addCompletion(new ShorthandCompletion(provider, "decodeURIComponent", "decodeURIComponent('String');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_DECODE_URI)));
        provider.addCompletion(new ShorthandCompletion(provider, "console.log", "console.log('内容');", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_CONSOLE_LOG)));
        provider.addCompletion(new ShorthandCompletion(provider, "JSON.parse(responseBody)", "JSON.parse(responseBody);", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_PARSE)));
        provider.addCompletion(new ShorthandCompletion(provider, "JSON.stringify", "JSON.stringify(obj);", I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_STRINGIFY)));
        AutoCompletion ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true); // 启用自动补全
        ac.setAutoActivationEnabled(true); // 启用自动激活
        ac.setAutoActivationDelay(200); // 200ms后触发自动补全
        ac.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)); // 使用Tab键触发补全
        ac.setShowDescWindow(true); // 显示悬浮说明
        ac.install(area);
    }
}