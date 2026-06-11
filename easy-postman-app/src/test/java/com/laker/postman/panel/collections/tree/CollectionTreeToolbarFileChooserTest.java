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

    @Test(description = "Detected clipboard cURL text should prefill the import dialog instead of bypassing it")
    public void defaultCurlImportShouldStillOpenEditableDialog() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/collections/tree/CollectionTreeToolbar.java"));
        String importMethod = source.substring(source.indexOf("private void importCurlToCollection"));
        importMethod = importMethod.substring(0, importMethod.indexOf("private boolean containsCurlCommands"));

        assertTrue(importMethod.contains("CurlImportDialog.show(mainFrame"),
                "cURL imports should always open the editable dialog");
        assertFalse(importMethod.contains("if (defaultCurl != null && !defaultCurl.trim().isEmpty())"),
                "default cURL text should prefill the dialog, not bypass it");
    }
}
