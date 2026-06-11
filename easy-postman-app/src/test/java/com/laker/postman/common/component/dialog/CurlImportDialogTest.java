package com.laker.postman.common.component.dialog;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CurlImportDialogTest {

    @Test(description = "The cURL import dialog should use an explicit stable window size")
    public void curlImportDialogShouldNotDependOnPackOnlySizing() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/common/component/dialog/CurlImportDialog.java"));

        assertTrue(source.contains("setMinimumSize(DIALOG_SIZE)"),
                "the dialog should not be allowed to collapse below its intended editor size");
        assertTrue(source.contains("setSize(DIALOG_SIZE)"),
                "the dialog should explicitly set its window size after building content");
        assertFalse(source.contains("setPreferredSize(DIALOG_SIZE);\n        pack();"),
                "pack-only sizing can collapse this editor dialog into an almost invisible sliver");
    }
}
