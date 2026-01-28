package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.MarkdownEditorPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Markdown 编辑器工具面板
 * 提供 Markdown 编辑和实时预览功能
 */
public class MarkdownToolPanel extends JPanel {

    private MarkdownEditorPanel markdownEditor;

    public MarkdownToolPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 创建 Markdown 编辑器
        markdownEditor = new MarkdownEditorPanel();

        // 设置示例内容
        String sampleContent = """
                # 欢迎使用 Markdown 编辑器
                
                这是一个功能强大的 Markdown 编辑工具，支持实时预览。
                
                ## 支持的语法
                
                ### 文本格式
                - **粗体** 使用 `**文本**`
                - _斜体_ 使用 `_文本_`
                - ~~删除线~~ 使用 `~~文本~~`
                - `行内代码` 使用反引号
                
                ### 列表
                1. 有序列表
                2. 第二项
                
                - 无序列表
                - 另一项
                
                ### 任务列表
                - [x] 已完成的任务
                - [ ] 待办事项
                
                ### 代码块
                ```java
                public class Example {
                    public static void main(String[] args) {
                        System.out.println("Hello, Markdown!");
                    }
                }
                ```
                
                ### 表格
                | 功能 | 状态 |
                | --- | --- |
                | 实时预览 | ✅ |
                | 语法高亮 | ✅ |
                | 导出 HTML | ✅ |
                
                ---
                
                开始编辑您的 Markdown 文档吧！
                """;

        markdownEditor.setText(sampleContent);

        add(markdownEditor, BorderLayout.CENTER);
    }
}
