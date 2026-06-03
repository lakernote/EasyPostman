package com.laker.postman.common.component.setting;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Validation helper for settings text fields.
 */
public final class SettingsTextFieldValidator {

    private final JTextField field;
    private final Predicate<String> validator;
    private final String errorMessage;

    private SettingsTextFieldValidator(JTextField field, Predicate<String> validator, String errorMessage) {
        this.field = Objects.requireNonNull(field, "field");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static SettingsTextFieldValidator install(JTextField field,
                                                     Predicate<String> validator,
                                                     String errorMessage) {
        SettingsTextFieldValidator support = new SettingsTextFieldValidator(field, validator, errorMessage);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                support.validateNow();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                support.validateNow();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                support.validateNow();
            }
        });
        support.validateNow();
        return support;
    }

    public boolean validateNow() {
        boolean valid = !hasValidationError();
        field.putClientProperty(FlatClientProperties.OUTLINE,
                valid ? null : FlatClientProperties.OUTLINE_ERROR);
        field.setToolTipText(valid ? null : errorMessage);
        return valid;
    }

    public boolean hasValidationError() {
        String text = field.getText().trim();
        return !text.isEmpty() && !validator.test(text);
    }
}
