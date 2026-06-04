package com.laker.postman.common.component.connection;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;
import lombok.experimental.UtilityClass;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Function;

@UtilityClass
public class ConnectionToolbarUi {
    public static final int PROFILE_LABEL_WIDTH = 42;
    public static final int PROFILE_FIELD_WIDTH = 166;
    public static final int CONNECTION_LABEL_WIDTH = 52;
    public static final int WIDE_CONNECTION_LABEL_WIDTH = 86;
    public static final int TOOLBAR_BUTTON_SIZE = 24;
    public static final int FORM_CONTROL_HEIGHT = 28;
    public static final int CONNECTION_BUTTON_HEIGHT = 28;
    public static final int VERTICAL_SEPARATOR_HEIGHT = 22;
    public static final int SINGLE_ROW_HEIGHT = 38;
    public static final int DOUBLE_ROW_HEIGHT = 70;

    private static final String SEPARATOR_FOREGROUND = "Separator.foreground";
    private static final int TOOLBAR_ICON_SIZE = 14;

    public static String profileActionColumns() {
        return "[" + PROFILE_LABEL_WIDTH + "!,right]4["
                + PROFILE_FIELD_WIDTH + "!,fill]2["
                + TOOLBAR_BUTTON_SIZE + "!]0["
                + TOOLBAR_BUTTON_SIZE + "!]0["
                + TOOLBAR_BUTTON_SIZE + "!]0["
                + TOOLBAR_BUTTON_SIZE + "!]4[1!]4";
    }

    public static String connectionFieldColumns(int fieldWidth) {
        return "[" + CONNECTION_LABEL_WIDTH + "!,right]4[" + fieldWidth + "!,fill]";
    }

    public static String wideConnectionFieldColumns(int fieldWidth) {
        return connectionFieldColumns(WIDE_CONNECTION_LABEL_WIDTH, fieldWidth);
    }

    public static String connectionFieldColumns(int labelWidth, int fieldWidth) {
        return "[" + labelWidth + "!,right]4[" + fieldWidth + "!,fill]";
    }

    public static void lockConnectionPanelHeight(JComponent component, boolean hasSecondRow) {
        int height = hasSecondRow ? DOUBLE_ROW_HEIGHT : SINGLE_ROW_HEIGHT;
        Dimension preferredSize = component.getPreferredSize();
        int width = Math.max(preferredSize == null ? 0 : preferredSize.width, 1);
        component.setPreferredSize(new Dimension(width, height));
        component.setMinimumSize(new Dimension(1, height));
    }

    public static <T extends JComponent> T compactControl(T component) {
        Dimension preferredSize = component.getPreferredSize();
        int width = Math.max(preferredSize == null ? 0 : preferredSize.width, 1);
        component.setPreferredSize(new Dimension(width, FORM_CONTROL_HEIGHT));
        component.setMinimumSize(new Dimension(1, FORM_CONTROL_HEIGHT));
        return component;
    }

    public static JButton iconButton(String tooltip, String iconPath, ActionListener actionListener) {
        JButton button = new JButton(IconUtil.createThemed(iconPath, TOOLBAR_ICON_SIZE, TOOLBAR_ICON_SIZE));
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setPreferredSize(new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE));
        button.setMinimumSize(new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE));
        button.setMaximumSize(new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE));
        button.setMargin(new Insets(0, 0, 0, 0));
        if (actionListener != null) {
            button.addActionListener(actionListener);
        }
        return button;
    }

    public static JButton iconButton(String tooltip, String iconPath) {
        return iconButton(tooltip, iconPath, null);
    }

    public static void compactButton(JButton button, int width) {
        button.setPreferredSize(new Dimension(width, CONNECTION_BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(width, CONNECTION_BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(width, CONNECTION_BUTTON_HEIGHT));
        button.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    public static JComponent verticalSeparator() {
        JPanel separator = new JPanel();
        separator.setOpaque(true);
        separator.setBackground(UIManager.getColor(SEPARATOR_FOREGROUND));
        separator.setPreferredSize(new Dimension(1, VERTICAL_SEPARATOR_HEIGHT));
        separator.setMinimumSize(new Dimension(1, VERTICAL_SEPARATOR_HEIGHT));
        separator.setMaximumSize(new Dimension(1, VERTICAL_SEPARATOR_HEIGHT));
        return separator;
    }

    public static <T> ListCellRenderer<? super T> displayRenderer(Function<T, String> displayNameProvider) {
        DefaultListCellRenderer delegate = new DefaultListCellRenderer();
        return (JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) -> {
            JLabel label = (JLabel) delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                label.setText(displayNameProvider.apply(value));
            }
            return label;
        };
    }

    public static <T> JComboBox<T> comboBox(T[] values, Function<T, String> displayNameProvider) {
        JComboBox<T> comboBox = new JComboBox<>(values);
        compactControl(comboBox);
        comboBox.setRenderer(displayRenderer(displayNameProvider));
        return comboBox;
    }

    public static void registerSaveShortcut(JComponent component, Runnable saveAction) {
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutMask()), "connectionToolbarSave");
        component.getActionMap().put("connectionToolbarSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAction.run();
            }
        });
    }

    private static int menuShortcutMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (HeadlessException e) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }
}
