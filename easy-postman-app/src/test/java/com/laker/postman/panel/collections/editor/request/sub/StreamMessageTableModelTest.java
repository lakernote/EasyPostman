package com.laker.postman.panel.collections.editor.request.sub;

import org.testng.annotations.Test;

import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class StreamMessageTableModelTest {

    @Test
    public void appendRowsShouldFireSingleInsertedEventForBatch() {
        StreamMessageTableModel<String> model = new StreamMessageTableModel<>(
                new String[]{"Content"},
                (row, column) -> row
        );
        List<TableModelEvent> events = new ArrayList<>();
        model.addTableModelListener(events::add);

        model.appendRows(List.of("a", "b", "c"));

        assertEquals(model.getRowCount(), 3);
        assertEquals(model.getValueAt(2, 0), "c");
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getType(), TableModelEvent.INSERT);
        assertEquals(events.get(0).getFirstRow(), 0);
        assertEquals(events.get(0).getLastRow(), 2);
    }

    @Test
    public void removeFirstRowsShouldFireSingleDeletedEventForBatch() {
        StreamMessageTableModel<String> model = new StreamMessageTableModel<>(
                new String[]{"Content"},
                (row, column) -> row
        );
        model.appendRows(List.of("a", "b", "c"));
        List<TableModelEvent> events = new ArrayList<>();
        model.addTableModelListener(events::add);

        model.removeFirstRows(2);

        assertEquals(model.getRowCount(), 1);
        assertEquals(model.getRow(0), "c");
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getType(), TableModelEvent.DELETE);
        assertEquals(events.get(0).getFirstRow(), 0);
        assertEquals(events.get(0).getLastRow(), 1);
    }
}
