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

    private final SwitchButton followRedirectsSwitch;
    private final SwitchButton useCookieJarSwitch;
    private final EasyComboBox<HttpVersionOption> httpVersionComboBox;
    private final SwitchButton sslVerificationSwitch;
    private final JTextField requestTimeoutField;
    private final JLabel requestTimeoutHintLabel;
    private Boolean initialFollowRedirects;
    private Boolean initialCookieJarEnabled;
    private Boolean initialSslVerificationEnabled;
    private String initialHttpVersion;
    private boolean initialEffectiveFollowRedirects;
    private boolean initialEffectiveCookieJarEnabled;
    private boolean initialEffectiveSslVerificationEnabled;
    private String initialEffectiveHttpVersion;

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

        followRedirectsSwitch = new SwitchButton();
        useCookieJarSwitch = new SwitchButton();
        httpVersionComboBox = new EasyComboBox<>(createHttpVersionOptions(), EasyComboBox.WidthMode.FIXED_MAX);
        sslVerificationSwitch = new SwitchButton();
        requestTimeoutField = new JTextField();
        requestTimeoutHintLabel = createHintLabel();

        ((AbstractDocument) requestTimeoutField.getDocument()).setDocumentFilter(new DigitsOnlyDocumentFilter());
        httpVersionComboBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        requestTimeoutField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        requestTimeoutField.setColumns(10);

        content.add(createSwitchRow(
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_FOLLOW_REDIRECTS_LABEL),
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_FOLLOW_REDIRECTS_DESC),
                followRedirectsSwitch
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
        content.add(createSwitchRow(
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_SSL_VERIFICATION_LABEL),
                I18nUtil.getMessage(MessageKeys.REQUEST_SETTINGS_SSL_VERIFICATION_DESC),
                sslVerificationSwitch
        ), "growx, wrap");
        content.add(createTimeoutRow(), "growx, wrap");
        captureInitialState(null);
        resetToDefaults(null);
    }

    public void populate(HttpRequestItem item) {
        captureInitialState(item);
        resetToDefaults(item);
    }

    public void applyTo(HttpRequestItem item) {
        item.setFollowRedirects(resolveFollowRedirectsValueForSave());
        item.setCookieJarEnabled(resolveCookieJarValueForSave());
        item.setSslVerificationEnabled(resolveSslVerificationValueForSave());
        item.setHttpVersion(resolveHttpVersionValueForSave());
        item.setRequestTimeoutMs(resolveRequestTimeoutValueForSave());
    }

    public void rebaseline(HttpRequestItem item) {
        captureInitialState(item);
    }

    public String validateSettings() {
        return validateRequestTimeout(requestTimeoutField.getText());
    }

    public boolean hasCustomSettings() {
        return resolveFollowRedirectsValueForSave() != null
                || Boolean.FALSE.equals(resolveCookieJarValueForSave())
                || resolveSslVerificationValueForSave() != null
                || resolveHttpVersionValueForSave() != null
                || resolveRequestTimeoutValueForSave() != null;
    }

    public void addDirtyListener(Runnable listener) {
        if (listener == null) {
            return;
        }
        followRedirectsSwitch.addActionListener(e -> listener.run());
        useCookieJarSwitch.addActionListener(e -> listener.run());
        httpVersionComboBox.addActionListener(e -> listener.run());
        sslVerificationSwitch.addActionListener(e -> listener.run());
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

    private JPanel createSwitchRow(String title, String description, SwitchButton switchButton) {
        return createSettingRow(title, description, switchButton);
    }

    private JPanel createSelectRow(String title, String description, JComponent component) {
        return createSettingRow(title, description, component);
    }

    private JPanel createTimeoutRow() {
        requestTimeoutHintLabel.setText(I18nUtil.getMessage(
                MessageKeys.REQUEST_SETTINGS_TIMEOUT_HINT,
                SettingManager.getRequestTimeout()
        ));

        JPanel rightPanel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0",
                "[grow,fill]",
                "[]2[]"
        ));
        rightPanel.setOpaque(false);
        rightPanel.add(
                requestTimeoutField,
                "growx, pushx, wmin " + CONTROL_MIN_WIDTH + ", wmax " + CONTROL_WIDTH + ", alignx right, wrap"
        );
        rightPanel.add(requestTimeoutHintLabel, "alignx right");

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

    private void resetToDefaults(HttpRequestItem item) {
        boolean followRedirects = resolveEffectiveFollowRedirects(item);
        boolean cookieJarEnabled = resolveEffectiveCookieJarEnabled(item);
        boolean sslVerificationEnabled = resolveEffectiveSslVerificationEnabled(item);
        String httpVersion = resolveEffectiveHttpVersion(item);
        Integer requestTimeout = item != null ? item.getRequestTimeoutMs() : null;

        followRedirectsSwitch.setSelected(followRedirects);
        useCookieJarSwitch.setSelected(cookieJarEnabled);
        sslVerificationSwitch.setSelected(sslVerificationEnabled);
        httpVersionComboBox.setSelectedItem(findHttpVersionOption(httpVersion));
        requestTimeoutField.setText(requestTimeout != null ? String.valueOf(requestTimeout) : "");
    }

    private void captureInitialState(HttpRequestItem item) {
        initialFollowRedirects = item != null ? item.getFollowRedirects() : null;
        initialCookieJarEnabled = item != null ? item.getCookieJarEnabled() : null;
        initialSslVerificationEnabled = item != null ? item.getSslVerificationEnabled() : null;
        initialHttpVersion = item != null ? item.getHttpVersion() : null;
        initialEffectiveFollowRedirects = resolveEffectiveFollowRedirects(item);
        initialEffectiveCookieJarEnabled = resolveEffectiveCookieJarEnabled(item);
        initialEffectiveSslVerificationEnabled = resolveEffectiveSslVerificationEnabled(item);
        initialEffectiveHttpVersion = resolveEffectiveHttpVersion(item);
    }

    private boolean resolveEffectiveFollowRedirects(HttpRequestItem item) {
        return RequestSettingsResolver.resolveFollowRedirects(item);
    }

    private boolean resolveEffectiveCookieJarEnabled(HttpRequestItem item) {
        return RequestSettingsResolver.resolveCookieJarEnabled(item);
    }

    private boolean resolveEffectiveSslVerificationEnabled(HttpRequestItem item) {
        return RequestSettingsResolver.resolveSslVerificationEnabled(item);
    }

    private String resolveEffectiveHttpVersion(HttpRequestItem item) {
        return RequestSettingsResolver.resolveHttpVersion(item);
    }

    private Boolean resolveFollowRedirectsValueForSave() {
        boolean selected = followRedirectsSwitch.isSelected();
        if (selected == initialEffectiveFollowRedirects) {
            return initialFollowRedirects;
        }
        return selected;
    }

    private Boolean resolveCookieJarValueForSave() {
        boolean selected = useCookieJarSwitch.isSelected();
        if (selected == initialEffectiveCookieJarEnabled) {
            return Boolean.FALSE.equals(initialCookieJarEnabled) ? Boolean.FALSE : null;
        }
        return selected ? null : Boolean.FALSE;
    }

    private Boolean resolveSslVerificationValueForSave() {
        boolean selected = sslVerificationSwitch.isSelected();
        if (selected == initialEffectiveSslVerificationEnabled) {
            return initialSslVerificationEnabled;
        }
        return selected;
    }

    private String resolveHttpVersionValueForSave() {
        String selectedHttpVersion = getSelectedHttpVersion();
        if (selectedHttpVersion.equals(initialEffectiveHttpVersion)) {
            if (HttpRequestItem.HTTP_VERSION_HTTP_1_1.equals(initialHttpVersion)
                    || HttpRequestItem.HTTP_VERSION_HTTP_2.equals(initialHttpVersion)) {
                return initialHttpVersion;
            }
            return null;
        }
        return HttpRequestItem.HTTP_VERSION_AUTO.equals(selectedHttpVersion) ? null : selectedHttpVersion;
    }

    private Integer resolveRequestTimeoutValueForSave() {
        return parseRequestTimeoutOrNull();
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

    private HttpVersionOption findHttpVersionOption(String value) {
        ComboBoxModel<HttpVersionOption> model = httpVersionComboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            HttpVersionOption option = model.getElementAt(i);
            if (option.value.equals(value)) {
                return option;
            }
        }
        return model.getElementAt(0);
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
