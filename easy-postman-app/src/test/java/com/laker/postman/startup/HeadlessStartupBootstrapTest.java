package com.laker.postman.startup;

import com.laker.postman.plugin.runtime.PluginRuntime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HeadlessStartupBootstrapTest {
    private String previousDataDir;

    @BeforeMethod
    public void setUp() throws IOException {
        previousDataDir = System.getProperty("easyPostman.data.dir");
        Path dataDir = Files.createTempDirectory("headless-startup-bootstrap-test");
        System.setProperty("easyPostman.data.dir", dataDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        PluginRuntime.resetForTests();
        if (previousDataDir == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", previousDataDir);
        }
    }

    @Test
    public void shouldAllowHeadlessRuntimeToInitializeAgainAfterPluginRuntimeShutdown() {
        HeadlessStartupBootstrap.initRuntime();
        assertTrue(PluginRuntime.isInitialized());

        PluginRuntime.shutdown();
        assertFalse(PluginRuntime.isInitialized());

        HeadlessStartupBootstrap.initRuntime();

        assertTrue(PluginRuntime.isInitialized());
    }
}
