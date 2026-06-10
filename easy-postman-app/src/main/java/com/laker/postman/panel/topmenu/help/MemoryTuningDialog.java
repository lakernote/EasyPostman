package com.laker.postman.panel.topmenu.help;

import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Memory tuning help dialog for load-test users.
 */
public class MemoryTuningDialog extends JDialog {

    private static final int CONTENT_WIDTH = 760;
    private static final int SECTION_HORIZONTAL_PADDING = 28;
    private static final int LABEL_WIDTH = 132;
    private static final int LABEL_GAP = 18;
    private static final int BODY_TEXT_WIDTH = CONTENT_WIDTH - SECTION_HORIZONTAL_PADDING;
    private static final int DESCRIPTION_TEXT_WIDTH = BODY_TEXT_WIDTH - LABEL_WIDTH - LABEL_GAP;

    private final Font sectionFont = FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0);
    private final Font labelFont = FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1);
    private final Font bodyFont = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2);

    public MemoryTuningDialog(Frame parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_TITLE), true);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createScrollContent(), BorderLayout.CENTER);
        add(createButtonBar(), BorderLayout.SOUTH);

        setSize(820, 620);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(parent);
    }

    private JScrollPane createScrollContent() {
        JPanel content = new ScrollableContentPanel();
        ToolWindowSurfaceStyle.applyDialogSurface(content);
        content.setBorder(new EmptyBorder(14, 16, 14, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);

        int row = 0;
        gbc.gridy = row++;
        content.add(createWhenSection(), gbc);

        gbc.gridy = row++;
        content.add(createRecommendationSection(), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 0, 0);
        content.add(createXmxSection(), gbc);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(CONTENT_WIDTH + 60, 540));
        return scrollPane;
    }

    private JPanel createWhenSection() {
        SectionPanel section = createSection(MessageKeys.MEMORY_TUNING_WHEN_TITLE);
        addBodyText(section, I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_WHEN_BODY), 1);
        return section;
    }

    private JPanel createRecommendationSection() {
        SectionPanel section = createSection(MessageKeys.MEMORY_TUNING_RECOMMENDED_TITLE);
        int row = 1;
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_COMPACT_TITLE),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_COMPACT_DESC));
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_PREVIEW_TITLE),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_PREVIEW_DESC));
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_ROW_LIMIT_TITLE),
                I18nUtil.getMessage(
                        MessageKeys.MEMORY_TUNING_RECOMMEND_ROW_LIMIT_DESC,
                        SettingManager.DEFAULT_PERFORMANCE_RESULT_ROW_LIMIT
                ));
        addInfoRow(section, row,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_FAILURE_TITLE),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_RECOMMEND_FAILURE_DESC));
        return section;
    }

    private JPanel createXmxSection() {
        SectionPanel section = createSection(MessageKeys.MEMORY_TUNING_XMX_TITLE);
        int row = 1;
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_MAC),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_MAC_DESC));
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_WINDOWS_INSTALLER),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_WINDOWS_INSTALLER_DESC));
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_WINDOWS_PORTABLE),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_WINDOWS_PORTABLE_DESC));
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_LINUX),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_LINUX_DESC));
        addInfoRow(section, row++,
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_JAR),
                I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_PLATFORM_JAR_DESC));
        addBodyText(section, I18nUtil.getMessage(MessageKeys.MEMORY_TUNING_XMX_NOTE), row);
        return section;
    }

    private SectionPanel createSection(String titleKey) {
        SectionPanel section = new SectionPanel();
        section.setLayout(new GridBagLayout());
        section.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel(I18nUtil.getMessage(titleKey));
        title.setFont(sectionFont);
        title.setForeground(ModernColors.getTextPrimary());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);
        section.add(title, gbc);
        return section;
    }

    private void addBodyText(JPanel section, String text, int row) {
        JTextArea textArea = createTextArea(text, BODY_TEXT_WIDTH);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        section.add(textArea, gbc);
    }

    private void addInfoRow(JPanel section, int row, String label, String description) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(labelFont);
        labelComponent.setForeground(ModernColors.getTextPrimary());
        labelComponent.setVerticalAlignment(SwingConstants.TOP);

        JTextArea descArea = createTextArea(description, DESCRIPTION_TEXT_WIDTH);

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        labelGbc.insets = new Insets(0, 0, 8, LABEL_GAP);
        labelGbc.fill = GridBagConstraints.HORIZONTAL;
        labelGbc.weightx = 0;
        labelComponent.setPreferredSize(new Dimension(LABEL_WIDTH, labelComponent.getPreferredSize().height));
        section.add(labelComponent, labelGbc);

        GridBagConstraints descGbc = new GridBagConstraints();
        descGbc.gridx = 1;
        descGbc.gridy = row;
        descGbc.weightx = 1;
        descGbc.fill = GridBagConstraints.HORIZONTAL;
        descGbc.insets = new Insets(0, 0, 8, 0);
        section.add(descArea, descGbc);
    }

    private JTextArea createTextArea(String text, int preferredWidth) {
        JTextArea textArea = new WrappingTextArea(text, preferredWidth);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(bodyFont);
        textArea.setForeground(ModernColors.getTextSecondary());
        textArea.setBorder(BorderFactory.createEmptyBorder());
        ToolWindowSurfaceStyle.applyTextComponentDialogSurface(textArea);
        textArea.setForeground(ModernColors.getTextSecondary());
        return textArea;
    }

    private JPanel createButtonBar() {
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonBar);

        JButton okButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        okButton.addActionListener(e -> dispose());
        buttonBar.add(okButton);

        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        return buttonBar;
    }

    public static void showDialog(Frame parent) {
        MemoryTuningDialog dialog = new MemoryTuningDialog(parent);
        dialog.setVisible(true);
    }

    private static final class ScrollableContentPanel extends JPanel implements Scrollable {
        private ScrollableContentPanel() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class WrappingTextArea extends JTextArea {
        private final int preferredWidth;

        private WrappingTextArea(String text, int preferredWidth) {
            super(text);
            this.preferredWidth = Math.max(120, preferredWidth);
        }

        @Override
        public Dimension getPreferredSize() {
            setSize(preferredWidth, Short.MAX_VALUE);
            Dimension preferredSize = super.getPreferredSize();
            return new Dimension(preferredWidth, preferredSize.height);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension preferredSize = getPreferredSize();
            return new Dimension(Math.min(160, preferredWidth), preferredSize.height);
        }
    }

    private static final class SectionPanel extends JPanel {
        private static final int ARC = 8;

        private SectionPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ModernColors.getCardBackgroundColor());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            g2.setColor(ModernColors.getBorderLightColor());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
