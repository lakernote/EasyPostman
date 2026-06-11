package com.laker.postman.util;

import com.formdev.flatlaf.util.SystemFileChooser;
import lombok.experimental.UtilityClass;

import java.awt.Component;
import java.io.File;
import java.util.Optional;

/**
 * Shared entry point for file chooser dialogs.
 * <p>
 * Uses FlatLaf's system file chooser so supported platforms get native OS dialogs,
 * with automatic JFileChooser fallback handled by FlatLaf.
 */
@UtilityClass
public class FileChooserUtil {
    private static final String STATE_STORE_PREFIX = "fileChooser.";
    private static final Object STATE_STORE_LOCK = new Object();
    private static volatile boolean stateStoreInstalled;

    public static SystemFileChooser createOpenFileChooser(String stateStoreId, String dialogTitle) {
        return createFileChooser(SystemFileChooser.OPEN_DIALOG, SystemFileChooser.FILES_ONLY, stateStoreId, dialogTitle);
    }

    public static SystemFileChooser createSaveFileChooser(String stateStoreId, String dialogTitle) {
        return createFileChooser(SystemFileChooser.SAVE_DIALOG, SystemFileChooser.FILES_ONLY, stateStoreId, dialogTitle);
    }

    public static SystemFileChooser createDirectoryChooser(String stateStoreId, String dialogTitle) {
        return createFileChooser(SystemFileChooser.OPEN_DIALOG, SystemFileChooser.DIRECTORIES_ONLY, stateStoreId, dialogTitle);
    }

    public static Optional<File> showOpenFile(Component parent, String stateStoreId, String dialogTitle) {
        SystemFileChooser chooser = createOpenFileChooser(stateStoreId, dialogTitle);
        return selectedFile(chooser.showOpenDialog(parent), chooser);
    }

    public static Optional<File> showSaveFile(Component parent, String stateStoreId, String dialogTitle) {
        SystemFileChooser chooser = createSaveFileChooser(stateStoreId, dialogTitle);
        return selectedFile(chooser.showSaveDialog(parent), chooser);
    }

    public static Optional<File> showOpenDirectory(Component parent, String stateStoreId, String dialogTitle) {
        SystemFileChooser chooser = createDirectoryChooser(stateStoreId, dialogTitle);
        return selectedFile(chooser.showOpenDialog(parent), chooser);
    }

    public static SystemFileChooser.FileNameExtensionFilter extensionFilter(String description, String... extensions) {
        return new SystemFileChooser.FileNameExtensionFilter(description, extensions);
    }

    public static SystemFileChooser.PatternFilter patternFilter(String description, String... patterns) {
        return new SystemFileChooser.PatternFilter(description, patterns);
    }

    public static Optional<File> selectedFile(int dialogResult, SystemFileChooser chooser) {
        if (dialogResult != SystemFileChooser.APPROVE_OPTION || chooser == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(chooser.getSelectedFile());
    }

    private static SystemFileChooser createFileChooser(int dialogType,
                                                       int fileSelectionMode,
                                                       String stateStoreId,
                                                       String dialogTitle) {
        if (stateStoreId == null || stateStoreId.isBlank()) {
            throw new IllegalArgumentException("stateStoreId must not be blank");
        }
        installStateStore();
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogType(dialogType);
        chooser.setFileSelectionMode(fileSelectionMode);
        chooser.setStateStoreID(stateStoreId);
        if (dialogTitle != null && !dialogTitle.isBlank()) {
            chooser.setDialogTitle(dialogTitle);
        }
        return chooser;
    }

    private static void installStateStore() {
        if (stateStoreInstalled) {
            return;
        }
        synchronized (STATE_STORE_LOCK) {
            if (stateStoreInstalled) {
                return;
            }
            SystemFileChooser.setStateStore(new SystemFileChooser.StateStore() {
                @Override
                public String get(String key, String defaultValue) {
                    String value = UserPreferencesStore.getString(preferenceKey(key));
                    return value == null ? defaultValue : value;
                }

                @Override
                public void put(String key, String value) {
                    String preferenceKey = preferenceKey(key);
                    if (value == null) {
                        UserPreferencesStore.remove(preferenceKey);
                    } else {
                        UserPreferencesStore.put(preferenceKey, value);
                    }
                }
            });
            stateStoreInstalled = true;
        }
    }

    private static String preferenceKey(String key) {
        return STATE_STORE_PREFIX + (key == null ? "" : key);
    }
}
