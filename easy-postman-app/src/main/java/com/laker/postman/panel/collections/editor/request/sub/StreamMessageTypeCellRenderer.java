package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.panel.collections.editor.request.StreamMessageUiMetadata;
import com.laker.postman.stream.MessageType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

final class StreamMessageTypeCellRenderer extends DefaultTableCellRenderer {
    private final Map<MessageType, Icon> iconCache = new EnumMap<>(MessageType.class);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof MessageType messageType) {
            String text = StreamMessageUiMetadata.display(messageType);
            setText("");
            setToolTipText(text);
            setIcon(iconCache.computeIfAbsent(messageType, StreamMessageUiMetadata::icon));
        } else {
            setText("");
            setToolTipText(null);
            setIcon(null);
        }
        setHorizontalAlignment(CENTER);
        setIconTextGap(0);
        return this;
    }
}
