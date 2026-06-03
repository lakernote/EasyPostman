package com.laker.postman.service.setting;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class SettingManagerTypedFacadeTest {

    @Test
    public void settingManagerShouldNotReadIndividualKeysFromRawProperties() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/service/setting/SettingManager.java"
        ));

        assertFalse(source.contains("props.getProperty("),
                "SettingManager should delegate typed key reads to PreferencesStore/SettingKey");
    }
}
