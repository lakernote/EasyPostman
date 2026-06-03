package com.laker.postman.common.component.setting;

import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

/**
 * Warning bar for settings pages.
 */
@Getter
public class SettingsWarningBar extends JPanel {

    private final JLabel messageLabel;
    private final JButton discardButton;
    private final JButton saveButton;

    public SettingsWarningBar(String message,
                              String discardText,
                              String saveText,
                              Runnable onDiscard,
                              Runnable onSave) {
        setLayout(new MigLayout(
                "fillx, insets 12 20 12 20, gap 8, novisualpadding",
                "[][grow,fill][pref!][pref!]",
                "[]"
        ));
        setBackground(ModernColors.getWarningBackgroundColor());
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getWarningBorderColor()));

        JLabel iconLabel = new JLabel("!");
        iconLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        iconLabel.setForeground(ModernColors.getWarning());

        messageLabel = new JLabel(message == null ? "" : message);
        messageLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        messageLabel.setForeground(ModernColors.getTextPrimary());
        messageLabel.setBorder(new EmptyBorder(0, 0, 0, 0));

        discardButton = ModernButtonFactory.createButton(discardText == null ? "" : discardText, false);
        discardButton.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        discardButton.addActionListener(e -> {
            if (onDiscard != null) {
                onDiscard.run();
            }
        });

        saveButton = ModernButtonFactory.createButton(saveText == null ? "" : saveText, false);
        saveButton.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        saveButton.addActionListener(e -> {
            if (onSave != null) {
                onSave.run();
            }
        });

        add(iconLabel, "shrink 0");
        add(messageLabel, "growx, wmin 0");
        add(discardButton, "shrink 0");
        add(saveButton, "shrink 0");
    }
}
