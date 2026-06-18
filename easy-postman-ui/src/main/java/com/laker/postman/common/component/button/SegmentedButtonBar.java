package com.laker.postman.common.component.button;

import javax.swing.ButtonGroup;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Single-selection segmented control for type and mode switchers.
 */
public class SegmentedButtonBar<T> extends SegmentedButtonGroupPanel {
    public enum Size {
        DEFAULT(14, 26, 2, 8, 0, 0, new Insets(2, 2, 2, 2), 9, false),
        COMPACT(8, 24, 2, 8, 0, 0, new Insets(2, 2, 2, 2), 8, true);

        private final int buttonHorizontalPadding;
        private final int buttonMinHeight;
        private final int buttonBackgroundInset;
        private final int buttonArc;
        private final int hgap;
        private final int vgap;
        private final Insets groupInsets;
        private final int groupArc;
        private final boolean equalSegmentWidth;

        Size(int buttonHorizontalPadding,
             int buttonMinHeight,
             int buttonBackgroundInset,
             int buttonArc,
             int hgap,
             int vgap,
             Insets groupInsets,
             int groupArc,
             boolean equalSegmentWidth) {
            this.buttonHorizontalPadding = buttonHorizontalPadding;
            this.buttonMinHeight = buttonMinHeight;
            this.buttonBackgroundInset = buttonBackgroundInset;
            this.buttonArc = buttonArc;
            this.hgap = hgap;
            this.vgap = vgap;
            this.groupInsets = groupInsets;
            this.groupArc = groupArc;
            this.equalSegmentWidth = equalSegmentWidth;
        }
    }

    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final Map<T, SegmentedToggleButton> buttons = new LinkedHashMap<>();
    private final Size size;
    private boolean equalSegmentWidth = true;
    private Consumer<T> selectionListener;
    private T selectedValue;

    public SegmentedButtonBar() {
        this(FlowLayout.LEFT, Size.DEFAULT);
    }

    public SegmentedButtonBar(int alignment) {
        this(alignment, Size.DEFAULT);
    }

    public SegmentedButtonBar(int alignment, Size size) {
        super(
                alignment,
                Objects.requireNonNull(size, "size").hgap,
                size.vgap,
                size.groupInsets,
                size.groupArc
        );
        this.size = size;
        this.equalSegmentWidth = size.equalSegmentWidth;
    }

    public SegmentedToggleButton addOption(T value, String text, boolean selected) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(text, "text");
        if (buttons.containsKey(value)) {
            throw new IllegalArgumentException("Duplicate segmented option: " + value);
        }
        SegmentedToggleButton button = new SegmentedToggleButton(
                text,
                selected,
                size.buttonHorizontalPadding,
                size.buttonMinHeight,
                size.buttonBackgroundInset,
                size.buttonArc
        );
        buttons.put(value, button);
        buttonGroup.add(button);
        add(button);
        button.addActionListener(e -> {
            if (button.isSelected()) {
                selectedValue = value;
                if (selectionListener != null) {
                    selectionListener.accept(value);
                }
            }
        });
        if (selected || selectedValue == null) {
            setSelectedValue(value);
        }
        syncSegmentWidths();
        return button;
    }

    public SegmentedToggleButton getButton(T value) {
        return buttons.get(value);
    }

    public T getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(T value) {
        Objects.requireNonNull(value, "value");
        SegmentedToggleButton button = buttons.get(value);
        if (button == null) {
            return;
        }
        button.setSelected(true);
        selectedValue = value;
    }

    public void setSelectionListener(Consumer<T> selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setEqualSegmentWidth(boolean equalSegmentWidth) {
        this.equalSegmentWidth = equalSegmentWidth;
        syncSegmentWidths();
    }

    private void syncSegmentWidths() {
        for (SegmentedToggleButton button : buttons.values()) {
            button.setPreferredSize(null);
            button.setMinimumSize(null);
            button.setMaximumSize(null);
        }
        if (!equalSegmentWidth || buttons.isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        int width = 0;
        int height = 0;
        for (SegmentedToggleButton button : buttons.values()) {
            Dimension preferred = button.getPreferredSize();
            width = Math.max(width, preferred.width);
            height = Math.max(height, preferred.height);
        }
        Dimension segmentSize = new Dimension(width, height);
        for (SegmentedToggleButton button : buttons.values()) {
            button.setPreferredSize(segmentSize);
            button.setMinimumSize(segmentSize);
        }
        revalidate();
        repaint();
    }
}
