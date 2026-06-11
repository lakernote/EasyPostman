package com.laker.postman.util;

import com.formdev.flatlaf.util.SystemFileChooser;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class FileChooserUtilTest {

    @Test
    public void openFileChooserShouldInstallSharedDefaults() {
        SystemFileChooser chooser = FileChooserUtil.createOpenFileChooser(
                "test.open",
                "Open Test File"
        );

        assertEquals(chooser.getDialogType(), SystemFileChooser.OPEN_DIALOG);
        assertEquals(chooser.getFileSelectionMode(), SystemFileChooser.FILES_ONLY);
        assertEquals(chooser.getDialogTitle(), "Open Test File");
        assertEquals(chooser.getStateStoreID(), "test.open");
    }

    @Test
    public void extensionFilterShouldUseFlatLafSystemFilter() {
        SystemFileChooser.FileNameExtensionFilter filter =
                FileChooserUtil.extensionFilter("JSON Files (*.json)", "json");

        assertEquals(filter.getDescription(), "JSON Files (*.json)");
        assertEquals(filter.getExtensions(), new String[]{"json"});
    }

    @Test
    public void fileChooserStateIdShouldBeRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> FileChooserUtil.createOpenFileChooser(" ", "Open Test File"));
    }

    @Test
    public void patternFilterShouldUseFlatLafSystemFilter() {
        SystemFileChooser.PatternFilter filter =
                FileChooserUtil.patternFilter("Archives (*.tar.gz)", "*.tar.gz");

        assertEquals(filter.getDescription(), "Archives (*.tar.gz)");
        assertEquals(filter.getPatterns(), new String[]{"*.tar.gz"});
    }
}
