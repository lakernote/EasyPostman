package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import java.awt.*;

public class DeferredEditorPlaceholderPanel extends AbstractPlaceholderPanel {

    public DeferredEditorPlaceholderPanel() {
        super(new BorderLayout(0, 16));
        configureRoot(ModernColors.getBackgroundColor(), new Insets(18, 18, 18, 18));

        JPanel topRow = createTransparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.add(createBlock(120, 30, ModernColors.primaryWithAlpha(20)));
        topRow.add(createBlock(140, 30, ModernColors.primaryWithAlpha(12)));
        add(topRow, BorderLayout.NORTH);

        JPanel content = createCardPanel(
                new BorderLayout(0, 14),
                ModernColors.getCardBackgroundColor(),
                ModernColors.getDividerBorderColor(),
                new Insets(18, 18, 18, 18)
        );
        content.add(createBlock(420, 36, ModernColors.primaryWithAlpha(14)), BorderLayout.NORTH);

        JPanel body = createTransparentPanel(new GridLayout(1, 2, 16, 0));
        body.add(createColumn(new int[][]{{160, 16}, {220, 16}, {180, 16}, {240, 16}}, ModernColors.primaryWithAlpha(10), 12));
        body.add(createColumn(new int[][]{{260, 16}, {200, 16}, {280, 16}, {220, 16}}, ModernColors.primaryWithAlpha(10), 12));
        content.add(body, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }
}
