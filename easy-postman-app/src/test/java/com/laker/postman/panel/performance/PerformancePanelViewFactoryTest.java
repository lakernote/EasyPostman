package com.laker.postman.panel.performance;

import com.laker.postman.service.PerformancePersistenceService;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformancePanelViewFactoryTest {

    @Test
    public void resultControlsShouldSwitchLinkedResultTabs() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        AtomicBoolean trendEnabled = new AtomicBoolean(true);
        AtomicBoolean reportRealtime = new AtomicBoolean(false);
        AtomicInteger reportRefreshCount = new AtomicInteger();

        PerformancePanelViewFactory.ResultSection resultSection = viewFactory.createResultSection(
                true,
                false,
                trendEnabled::set,
                reportRealtime::set,
                reportRefreshCount::incrementAndGet,
                () -> {
                }
        );

        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TABLE);

        resultSection.trendButton().doClick();
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TREND);

        resultSection.trendCheckBox().doClick();
        assertFalse(trendEnabled.get());
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TABLE);

        resultSection.trendButton().doClick();
        resultSection.trendCheckBox().doClick();
        assertTrue(trendEnabled.get());
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TREND);

        resultSection.reportButton().doClick();
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_REPORT);
        assertEquals(reportRefreshCount.get(), 1);

        resultSection.reportRefreshModeBox().setSelectedIndex(1);
        assertTrue(reportRealtime.get());
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_REPORT);
        assertEquals(reportRefreshCount.get(), 2);
    }

    @Test
    public void topToolbarShouldOnlyContainExecutionControls() throws IOException {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        PerformancePersistenceService persistenceService = new TestablePerformancePersistenceService(
                Files.createTempDirectory("performance-toolbar-test").resolve("performance_config.json")
        );

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                null,
                true,
                persistenceService,
                () -> {
                },
                value -> {
                },
                () -> {
                },
                () -> {
                }
        );

        assertTrue(toolbarSection.efficientCheckBox().isSelected());
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
