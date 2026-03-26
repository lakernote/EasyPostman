package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import java.awt.*;

/**
 * 延迟编辑区占位面板。
 * 用一组骨架块模拟“顶部操作区 + 中间编辑卡片”的布局，
 * 适合在真实编辑器或主内容还没准备好时先占位，避免出现大面积空白。
 */
public class RequestEditorPlaceholderPanel extends AbstractPlaceholderPanel {

    public RequestEditorPlaceholderPanel() {
        super(new BorderLayout(0, 16));
        configureRoot(ModernColors.getBackgroundColor(), new Insets(18, 18, 18, 18));

        // 顶部两块短骨架，模拟工具栏或标签区。
        JPanel topRow = createTransparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.add(createBlock(120, 30, ModernColors.primaryWithAlpha(20)));
        topRow.add(createBlock(140, 30, ModernColors.primaryWithAlpha(12)));
        add(topRow, BorderLayout.NORTH);

        // 中间主卡片，模拟延迟加载中的编辑区主体。
        JPanel content = createCardPanel(
                new BorderLayout(0, 14),
                ModernColors.getCardBackgroundColor(),
                ModernColors.getDividerBorderColor(),
                new Insets(18, 18, 18, 18)
        );
        content.add(createBlock(420, 36, ModernColors.primaryWithAlpha(14)), BorderLayout.NORTH);

        // 左右两列长短不一的骨架，模拟表单/编辑器正文。
        JPanel body = createTransparentPanel(new GridLayout(1, 2, 16, 0));
        body.add(createColumn(new int[][]{{160, 16}, {220, 16}, {180, 16}, {240, 16}}, ModernColors.primaryWithAlpha(10), 12));
        body.add(createColumn(new int[][]{{260, 16}, {200, 16}, {280, 16}, {220, 16}}, ModernColors.primaryWithAlpha(10), 12));
        content.add(body, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }
}
