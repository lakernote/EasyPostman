package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * 文本对比工具面板
 */
@Slf4j
public class DiffPanel extends JPanel {

    private JTextArea originalArea;
    private JTextArea modifiedArea;
    private JTextPane resultPane;

    public DiffPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JButton compareBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_COMPARE));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        JButton swapBtn = new JButton("⇄ Swap");

        JPanel topPanel = ToolWindowActionToolbar.inlineLeft(compareBtn, copyBtn, swapBtn, clearBtn);

        add(topPanel, BorderLayout.NORTH);

        // 中间：三栏布局
        // 原始文本
        JPanel originalPanel = new JPanel(new BorderLayout(5, 5));
        originalPanel.setOpaque(false);
        originalPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_ORIGINAL) + ":"), BorderLayout.NORTH);
        originalArea = new JTextArea();
        originalArea.setLineWrap(true);
        originalArea.setWrapStyleWord(true);
        originalArea.setFont(FontsUtil.getMonospacedFontWithOffset(Font.PLAIN, 0));
        originalArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ToolWindowSurfaceStyle.applyTextComponentInput(originalArea);
        JScrollPane originalScrollPane = new JScrollPane(originalArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(originalScrollPane);
        originalPanel.add(originalScrollPane, BorderLayout.CENTER);

        // 修改后文本
        JPanel modifiedPanel = new JPanel(new BorderLayout(5, 5));
        modifiedPanel.setOpaque(false);
        modifiedPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_MODIFIED) + ":"), BorderLayout.NORTH);
        modifiedArea = new JTextArea();
        modifiedArea.setLineWrap(true);
        modifiedArea.setWrapStyleWord(true);
        modifiedArea.setFont(FontsUtil.getMonospacedFontWithOffset(Font.PLAIN, 0));
        modifiedArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ToolWindowSurfaceStyle.applyTextComponentInput(modifiedArea);
        JScrollPane modifiedScrollPane = new JScrollPane(modifiedArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(modifiedScrollPane);
        modifiedPanel.add(modifiedScrollPane, BorderLayout.CENTER);

        JSplitPane topSplitPane = ToolWindowChrome.createHorizontalInnerSplitPane(
                originalPanel,
                modifiedPanel,
                360
        );
        topSplitPane.setResizeWeight(0.5); // 平均分配空间

        // 整体分割面板

        // 差异结果显示
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setOpaque(false);
        resultPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF_RESULT) + ":"), BorderLayout.NORTH);
        resultPane = new JTextPane();
        resultPane.setEditable(false);
        resultPane.setFont(FontsUtil.getMonospacedFontWithOffset(Font.PLAIN, 0));
        resultPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ToolWindowSurfaceStyle.applyTextComponentCard(resultPane);
        JScrollPane resultScrollPane = new JScrollPane(resultPane);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(resultScrollPane);
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);

        JSplitPane mainSplitPane = ToolWindowChrome.createVerticalInnerSplitPane(
                topSplitPane,
                resultPanel,
                250
        );
        mainSplitPane.setResizeWeight(0.45);

        add(mainSplitPane, BorderLayout.CENTER);

        // 按钮事件
        compareBtn.addActionListener(e -> compareDiff());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            originalArea.setText("");
            modifiedArea.setText("");
            resultPane.setText("");
        });
        swapBtn.addActionListener(e -> {
            String temp = originalArea.getText();
            originalArea.setText(modifiedArea.getText());
            modifiedArea.setText(temp);
        });
    }

    private void compareDiff() {
        String original = originalArea.getText();
        String modified = modifiedArea.getText();

        if (original.isEmpty() && modified.isEmpty()) {
            resultPane.setText("⚠️ Please enter text in both areas");
            return;
        }

        try {
            // 按行分割
            String[] originalLines = original.split("\n", -1);
            String[] modifiedLines = modified.split("\n", -1);

            // 执行简单的行级差异比较
            StyledDocument doc = resultPane.getStyledDocument();
            doc.remove(0, doc.getLength());

            // 定义样式
            Style defaultStyle = resultPane.addStyle("default", null);
            StyleConstants.setForeground(defaultStyle, ModernColors.getTextPrimary());

            Style addedStyle = resultPane.addStyle("added", null);
            StyleConstants.setForeground(addedStyle, ModernColors.getSuccess());
            StyleConstants.setBackground(addedStyle, ModernColors.withAlpha(ModernColors.getSuccess(), ModernColors.isDarkTheme() ? 64 : 36));

            Style removedStyle = resultPane.addStyle("removed", null);
            StyleConstants.setForeground(removedStyle, ModernColors.getError());
            StyleConstants.setBackground(removedStyle, ModernColors.withAlpha(ModernColors.getError(), ModernColors.isDarkTheme() ? 64 : 36));

            Style headerStyle = resultPane.addStyle("header", null);
            StyleConstants.setForeground(headerStyle, ModernColors.getPrimary());
            StyleConstants.setBold(headerStyle, true);

            // 统计信息
            int additions = 0;
            int deletions = 0;
            int modifications = 0;

            // 头部信息
            doc.insertString(doc.getLength(), "📊 Diff Result\n", headerStyle);
            doc.insertString(doc.getLength(), "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", defaultStyle);

            // 使用简单的算法比较
            int maxLen = Math.max(originalLines.length, modifiedLines.length);

            for (int i = 0; i < maxLen; i++) {
                String origLine = i < originalLines.length ? originalLines[i] : null;
                String modLine = i < modifiedLines.length ? modifiedLines[i] : null;

                if (origLine == null && modLine != null) {
                    // 新增行
                    additions++;
                    doc.insertString(doc.getLength(), "+ " + modLine + "\n", addedStyle);
                } else if (origLine != null && modLine == null) {
                    // 删除行
                    deletions++;
                    doc.insertString(doc.getLength(), "- " + origLine + "\n", removedStyle);
                } else if (origLine != null && modLine != null) {
                    if (!origLine.equals(modLine)) {
                        // 修改行
                        modifications++;
                        doc.insertString(doc.getLength(), "- " + origLine + "\n", removedStyle);
                        doc.insertString(doc.getLength(), "+ " + modLine + "\n", addedStyle);
                    } else {
                        // 相同行
                        doc.insertString(doc.getLength(), "  " + origLine + "\n", defaultStyle);
                    }
                }
            }

            // 统计信息
            doc.insertString(doc.getLength(), "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", defaultStyle);
            doc.insertString(doc.getLength(), "📈 Statistics:\n", headerStyle);
            doc.insertString(doc.getLength(), "  Additions: " + additions + " line(s)\n", addedStyle);
            doc.insertString(doc.getLength(), "  Deletions: " + deletions + " line(s)\n", removedStyle);
            doc.insertString(doc.getLength(), "  Modifications: " + modifications + " line(s)\n", defaultStyle);
            doc.insertString(doc.getLength(), "  Total changes: " + (additions + deletions + modifications) + "\n", headerStyle);

            resultPane.setCaretPosition(0);

        } catch (Exception ex) {
            resultPane.setText("❌ Error: " + ex.getMessage());
            log.error("Diff comparison error", ex);
        }
    }

    private void copyToClipboard() {
        String text = resultPane.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        }
    }
}
