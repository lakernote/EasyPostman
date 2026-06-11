package com.laker.postman.panel.collections.tree;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CollectionTreeToolbarFileChooserTest {

    @Test(description = "Collection import dialogs should use the shared file chooser utility")
    public void collectionImportsShouldUseSharedFileChooserUtility() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/collections/tree/CollectionTreeToolbar.java"));

        assertTrue(source.contains("com.laker.postman.util.FileChooserUtil"),
                "collection import dialogs should go through the shared file chooser utility");
        assertFalse(source.contains("new SystemFileChooser"),
                "file chooser creation should be centralized in FileChooserUtil");
        assertFalse(source.contains("javax.swing.filechooser.FileFilter"),
                "extension filters should be created through the shared utility");
        assertFalse(source.contains("JFileChooser"),
                "raw JFileChooser shows the old Swing file chooser instead of the native system dialog");
    }
}
