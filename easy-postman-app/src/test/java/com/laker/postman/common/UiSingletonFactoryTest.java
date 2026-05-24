package com.laker.postman.common;

import com.laker.postman.service.FunctionalPersistenceService;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

public class UiSingletonFactoryTest extends AbstractSwingUiTest {

    @Test
    public void shouldRejectNonUiSingletonClasses() {
        assertThrows(IllegalArgumentException.class,
                () -> UiSingletonFactory.getInstance(FunctionalPersistenceService.class));
    }

    @Test
    public void shouldInitializeUiSingletonPanelOnlyOnce() {
        TestPanel.initCount = 0;
        TestPanel.listenerCount = 0;

        TestPanel first = UiSingletonFactory.getInstance(TestPanel.class);
        TestPanel second = UiSingletonFactory.getInstance(TestPanel.class);

        assertSame(first, second);
        assertEquals(TestPanel.initCount, 1);
        assertEquals(TestPanel.listenerCount, 1);
    }

    @Test
    public void shouldResetMenuBarCreationPermissionAfterFactoryCreatesMenuBar() {
        UiSingletonFactory.getInstance(TestMenuBar.class);

        assertThrows(IllegalStateException.class, TestMenuBar::new);
    }

    public static class TestPanel extends UiSingletonPanel {
        static int initCount;
        static int listenerCount;

        @Override
        protected void initUI() {
            initCount++;
        }

        @Override
        protected void registerListeners() {
            listenerCount++;
        }
    }

    public static class TestMenuBar extends UiSingletonMenuBar {
        @Override
        protected void initUI() {
            // no-op
        }

        @Override
        protected void registerListeners() {
            // no-op
        }
    }
}
