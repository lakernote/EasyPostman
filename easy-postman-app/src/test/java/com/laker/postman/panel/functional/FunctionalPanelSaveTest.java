package com.laker.postman.panel.functional;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.functional.table.FunctionalRunnerTableModel;
import com.laker.postman.panel.functional.table.RunnerRowData;
import com.laker.postman.service.FunctionalPersistenceService;
import org.testng.annotations.Test;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class FunctionalPanelSaveTest {

    @Test(description = "显式保存提交表格编辑后应取消编辑事件重新触发的防抖保存")
    public void shouldCancelAutosaveScheduledByCellEditingCommit() throws Exception {
        FunctionalPanel panel = newPanelWithoutInit();
        FunctionalRunnerTableModel tableModel = new FunctionalRunnerTableModel();
        JTable table = new JTable(tableModel);
        RecordingFunctionalPersistenceService persistenceService = new RecordingFunctionalPersistenceService();

        tableModel.addRow(new RunnerRowData(requestItem(), new PreparedRequest()));
        installSelectionAutosaveListener(panel, tableModel);
        setField(panel, "tableModel", tableModel);
        setField(panel, "table", table);
        setField(panel, "persistenceService", persistenceService);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(table.editCellAt(0, 0));
            if (table.getEditorComponent() instanceof JCheckBox checkBox) {
                checkBox.setSelected(false);
            }
            panel.save();
        });

        assertEquals(persistenceService.syncSaveCount.get(), 1);
        assertFalse(
                persistenceService.asyncSaveLatch.await(800, TimeUnit.MILLISECONDS),
                "The autosave scheduled by stopCellEditing() must be canceled by the explicit save"
        );
    }

    private static FunctionalPanel newPanelWithoutInit() throws Exception {
        SingletonBasePanel.setCreatingAllowed(true);
        try {
            return new FunctionalPanel();
        } finally {
            SingletonBasePanel.setCreatingAllowed(false);
        }
    }

    private static HttpRequestItem requestItem() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("req-save-order");
        item.setName("Save order");
        item.setMethod("GET");
        item.setUrl("https://example.com");
        return item;
    }

    private static void installSelectionAutosaveListener(FunctionalPanel panel,
                                                         FunctionalRunnerTableModel tableModel) throws Exception {
        Method scheduleSave = FunctionalPanel.class.getDeclaredMethod("scheduleSave");
        scheduleSave.setAccessible(true);
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                try {
                    scheduleSave.invoke(panel);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
            }
        });
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingFunctionalPersistenceService extends FunctionalPersistenceService {
        private final AtomicInteger syncSaveCount = new AtomicInteger();
        private final CountDownLatch asyncSaveLatch = new CountDownLatch(1);

        @Override
        public void save(List<RunnerRowData> rows, CsvDataPanel.CsvState csvState) {
            syncSaveCount.incrementAndGet();
        }

        @Override
        public void saveAsync(List<RunnerRowData> rows, CsvDataPanel.CsvState csvState) {
            asyncSaveLatch.countDown();
        }
    }
}
