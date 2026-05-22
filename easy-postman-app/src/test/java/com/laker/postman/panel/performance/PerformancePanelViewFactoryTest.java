package com.laker.postman.panel.performance;

import com.laker.postman.service.PerformancePersistenceService;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class PerformancePanelViewFactoryTest {

    @Test
    public void toolbarResultControlsShouldSwitchLinkedResultTabs() throws IOException {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        AtomicInteger selectedTab = new AtomicInteger(-1);
        AtomicBoolean trendEnabled = new AtomicBoolean(true);
        AtomicBoolean reportRealtime = new AtomicBoolean(false);
        PerformancePersistenceService persistenceService = new TestablePerformancePersistenceService(
                Files.createTempDirectory("performance-toolbar-test").resolve("performance_config.json")
        );

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                null,
                true,
                true,
                false,
                persistenceService,
                () -> {
                },
                value -> {
                },
                trendEnabled::set,
                reportRealtime::set,
                selectedTab::set,
                () -> {
                },
                () -> {
                }
        );

        toolbarSection.resultTableButton().doClick();
        assertEquals(selectedTab.get(), 2);

        toolbarSection.trendCheckBox().doClick();
        assertFalse(trendEnabled.get());
        assertEquals(selectedTab.get(), 2);

        toolbarSection.trendCheckBox().doClick();
        assertTrue(trendEnabled.get());
        assertEquals(selectedTab.get(), 0);

        toolbarSection.reportButton().doClick();
        assertEquals(selectedTab.get(), 1);

        toolbarSection.reportRefreshModeBox().setSelectedIndex(1);
        assertTrue(reportRealtime.get());
        assertEquals(selectedTab.get(), 1);
    }

    private static final class TestablePerformancePersistenceService extends PerformancePersistenceService {
        private final Path configPath;

        private TestablePerformancePersistenceService(Path configPath) {
            this.configPath = configPath;
        }

        @Override
        protected Path getConfigFilePath() {
            return configPath;
        }
    }
}
