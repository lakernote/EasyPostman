package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.button.SwitchButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.http.RequestSettingsResolver;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 请求级 Settings 面板，参考 Postman 的列表式布局。
 */
public class RequestSettingsPanel extends JScrollPane {
    private static final int CONTROL_WIDTH = 176;
    private static final int CONTROL_MIN_WIDTH = 120;
    private static final int ROW_COLUMN_GAP = 16;
    private static final int SCROLL_UNIT_INCREMENT = 24;

    private final EasyComboBox<BooleanSettingOption> followRedirectsComboBox;
    private final SwitchButton useCookieJarSwitch;
    private final EasyComboBox<HttpVersionOption> httpVersionComboBox;
    private final JTextField requestTimeoutField;
    private final JLabel requestTimeoutHintLabel;

    public RequestSettingsPanel() {
        setBorder(BorderFactory.createEmptyBorder());
        setViewportBorder(BorderFactory.createEmptyBorder());
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        getViewport().setOpaque(false);

        JPanel content = new JPanel(new MigLayout("insets 0, fillx, novisualpadding, gap 0", "[grow,fill]", ""));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        JPanel viewportContent = new JPanel(new BorderLayout());
        viewportContent.setOpaque(false);
        viewportContent.add(content, BorderLayout.NORTH);
        setViewportView(viewportContent);

        followRedirectsComboBox = createBooleanSettingComboBox();
        useCookieJarSwitch = new SwitchButton();
        httpVersionComboBox = new EasyComboBox<>(createHttpVersionOptions(), EasyComboBox.WidthMode.FIXED_MAX);
        requestTimeoutField = new JTextField();
        requestTimeoutHintLabel = createHintLabel();

        ((AbstractDocument) requestTimeoutField.getDocument()).setDocumentFilter(new DigitsOnlyDocumentFilter());
        httpVersionComboBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        requestTimeoutField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        requestTimeoutField.setColumns(10);

        content.add(createSelectRow(
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_FOLLOW_REDIRECTS_LABEL),
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_FOLLOW_REDIRECTS_DESC),
                followRedirectsComboBox
        ), "growx, wrap");
        content.add(createSwitchRow(
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_USE_COOKIE_JAR_LABEL),
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_USE_COOKIE_JAR_DESC),
                useCookieJarSwitch
        ), "growx, wrap");
        content.add(createSelectRow(
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_HTTP_VERSION_LABEL),
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_HTTP_VERSION_DESC),
                httpVersionComboBox
        ), "growx, wrap");
        content.add(createTimeoutRow(), "growx, wrap");
        populate(null);
    }

    public void populate(HttpRequestItem item) {
        updateRequestTimeoutHint();
        followRedirectsComboBox.setSelectedItem(findBooleanOption(followRedirectsComboBox, item != null ? item.getFollowRedirects() : null));
        useCookieJarSwitch.setSelected(RequestSettingsResolver.resolveCookieJarEnabled(item));
        httpVersionComboBox.setSelectedItem(findHttpVersionOption(
                RequestSettingsResolver.normalizeStoredHttpVersion(item != null ? item.getHttpVersion() : null)
        ));
        Integer requestTimeout = item != null ? item.getRequestTimeoutMs() : null;
        requestTimeoutField.setText(requestTimeout != null ? String.valueOf(requestTimeout) : "");
    }

    public void applyTo(HttpRequestItem item) {
        item.setFollowRedirects(getSelectedBooleanValue(followRedirectsComboBox));
        item.setCookieJarEnabled(getStoredCookieJarValue());
        item.setHttpVersion(getStoredHttpVersionValue());
        item.setRequestTimeoutMs(getStoredRequestTimeoutValue());
    }

    public void rebaseline() {
        updateRequestTimeoutHint();
    }

    public String validateSettings() {
        return validateRequestTimeout(requestTimeoutField.getText());
    }

    public boolean hasCustomSettings() {
        return getSelectedBooleanValue(followRedirectsComboBox) != null
                || Boolean.FALSE.equals(getStoredCookieJarValue())
                || getStoredHttpVersionValue() != null
                || getStoredRequestTimeoutValue() != null;
    }

    public void addDirtyListener(Runnable listener) {
        if (listener == null) {
            return;
        }
        followRedirectsComboBox.addActionListener(e -> listener.run());
        useCookieJarSwitch.addActionListener(e -> listener.run());
        httpVersionComboBox.addActionListener(e -> listener.run());
        requestTimeoutField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.run();
            }
        });
    }

    private EasyComboBox<BooleanSettingOption> createBooleanSettingComboBox() {
        EasyComboBox<BooleanSettingOption> comboBox = new EasyComboBox<>(
                createBooleanSettingOptions(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        comboBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        return comboBox;
    }

    private JPanel createSwitchRow(String title, String description, SwitchButton switchButton) {
        return createSettingRow(title, description, switchButton);
    }

    private JPanel createSelectRow(String title, String description, JComponent component) {
        return createSettingRow(title, description, wrapControl(component));
    }

    private JPanel createTimeoutRow() {
        JPanel rightPanel = createHintedControlPanel(requestTimeoutField, requestTimeoutHintLabel);
        return createSettingRow(
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_TIMEOUT_LABEL),
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_TIMEOUT_DESC),
                rightPanel
        );
    }

    private JPanel createSettingRow(String title, String description, JComponent rightComponent) {
        JPanel row = new JPanel(new MigLayout(
                "insets 8 0 8 0, fillx, novisualpadding",
                "[grow,fill]" + ROW_COLUMN_GAP + "[right]",
                "[]"
        ));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getBorderLightColor()));

        JPanel textPanel = new JPanel(new MigLayout("insets 0, fillx, novisualpadding, gap 0", "[grow,fill]", "[]1[]"));
        textPanel.setOpaque(false);
        textPanel.setMinimumSize(new Dimension(0, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        titleLabel.setForeground(ModernColors.getTextPrimary());

        JTextArea descriptionLabel = createDescriptionText(description);

        textPanel.add(titleLabel, "growx, wrap");
        textPanel.add(descriptionLabel, "growx");

        row.add(textPanel, "growx, pushx, wmin 0, aligny center");
        row.add(rightComponent, "alignx right, aligny center, shrink 0");
        return row;
    }

    private JPanel wrapControl(JComponent component) {
        JPanel panel = new JPanel(new MigLayout("insets 0, fillx, novisualpadding, gap 0", "[grow,fill]", "[]"));
        panel.setOpaque(false);
        panel.add(
                component,
                "growx, pushx, wmin " + CONTROL_MIN_WIDTH + ", wmax " + CONTROL_WIDTH + ", alignx right"
        );
        return panel;
    }

    private JPanel createHintedControlPanel(JComponent component, JLabel hintLabel) {
        JPanel rightPanel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0",
                "[grow,fill]",
                "[]2[]"
        ));
        rightPanel.setOpaque(false);
        rightPanel.add(
                component,
                "growx, pushx, wmin " + CONTROL_MIN_WIDTH + ", wmax " + CONTROL_WIDTH + ", alignx right, wrap"
        );
        rightPanel.add(hintLabel, "alignx right");
        return rightPanel;
    }

    private JLabel createHintLabel() {
        JLabel label = new JLabel();
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        label.setForeground(ModernColors.getTextSecondary());
        return label;
    }

    private JTextArea createDescriptionText(String description) {
        JTextArea area = new ShrinkableWrapTextArea(description);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        area.setForeground(ModernColors.getTextSecondary());
        return area;
    }

    private void updateRequestTimeoutHint() {
        requestTimeoutHintLabel.setText(I18nUtil.getMessage(
                MessageKeys.REQUEST_SETTINGS_TIMEOUT_HINT,
                SettingManager.getRequestTimeout()
        ));
    }

    private Boolean getStoredCookieJarValue() {
        return RequestSettingsResolver.normalizeStoredCookieJarEnabled(
                useCookieJarSwitch.isSelected() ? Boolean.TRUE : Boolean.FALSE
        );
    }

    private String getStoredHttpVersionValue() {
        return RequestSettingsResolver.normalizeStoredHttpVersion(getSelectedHttpVersion());
    }

    private Integer getStoredRequestTimeoutValue() {
        return parseRequestTimeoutOrNull();
    }

    private BooleanSettingOption[] createBooleanSettingOptions() {
        return new BooleanSettingOption[]{
                new BooleanSettingOption(
                        null,
                        I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_BOOLEAN_DEFAULT)
                ),
                new BooleanSettingOption(
                        Boolean.TRUE,
                        I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_BOOLEAN_ENABLED)
                ),
                new BooleanSettingOption(
                        Boolean.FALSE,
                        I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_BOOLEAN_DISABLED)
                )
        };
    }

    private HttpVersionOption[] createHttpVersionOptions() {
        List<HttpVersionOption> options = new ArrayList<>();
        options.add(new HttpVersionOption(
                HttpRequestItem.HTTP_VERSION_AUTO,
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_HTTP_VERSION_AUTO)
        ));
        options.add(new HttpVersionOption(
                HttpRequestItem.HTTP_VERSION_HTTP_1_1,
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_HTTP_VERSION_HTTP_1_1)
        ));
        options.add(new HttpVersionOption(
                HttpRequestItem.HTTP_VERSION_HTTP_2,
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_HTTP_VERSION_HTTP_2)
        ));
        return options.toArray(new HttpVersionOption[0]);
    }

    private BooleanSettingOption findBooleanOption(EasyComboBox<BooleanSettingOption> comboBox, Boolean value) {
        ComboBoxModel<BooleanSettingOption> model = comboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            BooleanSettingOption option = model.getElementAt(i);
            if ((value == null && option.value == null) || (value != null && value.equals(option.value))) {
                return option;
            }
        }
        return model.getElementAt(0);
    }

    private HttpVersionOption findHttpVersionOption(String value) {
        String normalizedValue = (value == null || value.trim().isEmpty())
                ? HttpRequestItem.HTTP_VERSION_AUTO
                : value;
        ComboBoxModel<HttpVersionOption> model = httpVersionComboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            HttpVersionOption option = model.getElementAt(i);
            if (option.value.equals(normalizedValue)) {
                return option;
            }
        }
        return model.getElementAt(0);
    }

    private Boolean getSelectedBooleanValue(EasyComboBox<BooleanSettingOption> comboBox) {
        BooleanSettingOption option = (BooleanSettingOption) comboBox.getSelectedItem();
        return option != null ? option.value : null;
    }

    private String getSelectedHttpVersion() {
        HttpVersionOption option = (HttpVersionOption) httpVersionComboBox.getSelectedItem();
        return option != null ? option.value : HttpRequestItem.HTTP_VERSION_AUTO;
    }

    private Integer parseRequestTimeoutOrNull() {
        String value = requestTimeoutField.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0 || parsed > Integer.MAX_VALUE) {
                return null;
            }
            return (int) parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String validateRequestTimeout(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed < 0 || parsed > Integer.MAX_VALUE) {
                return I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_TIMEOUT_VALIDATION);
            }
            return null;
        } catch (NumberFormatException ex) {
            return I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_TIMEOUT_VALIDATION);
        }
    }

    private static final class BooleanSettingOption {
        private final Boolean value;
        private final String label;

        private BooleanSettingOption(Boolean value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class HttpVersionOption {
        private final String value;
        private final String label;

        private HttpVersionOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ShrinkableWrapTextArea extends JTextArea {
        private ShrinkableWrapTextArea(String text) {
            super(text);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension minimumSize = super.getMinimumSize();
            return new Dimension(0, minimumSize.height);
        }
    }

    private static final class DigitsOnlyDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (isDigits(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (isDigits(text)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean isDigits(String text) {
            if (text == null || text.isEmpty()) {
                return true;
            }
            for (int i = 0; i < text.length(); i++) {
                if (!Character.isDigit(text.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
