package com.laker.postman.common.component;

import lombok.experimental.UtilityClass;

import java.io.File;

@UtilityClass
class CsvFileChooserDirectoryMemory {

    static File resolveInitialDirectory(File currentCsvFile, String rememberedDirectory) {
        if (currentCsvFile != null) {
            File parent = currentCsvFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                return parent;
            }
        }
        if (rememberedDirectory == null || rememberedDirectory.isBlank()) {
            return null;
        }
        File remembered = new File(rememberedDirectory);
        return remembered.isDirectory() ? remembered : null;
    }

    static File resolveDirectoryToRemember(File selectedFile) {
        if (selectedFile == null) {
            return null;
        }
        File parent = selectedFile.getParentFile();
        return parent != null && parent.isDirectory() ? parent : null;
    }
}
