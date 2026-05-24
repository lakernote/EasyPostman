package com.laker.postman.common;

import com.laker.postman.common.exception.GetInstanceException;
import com.laker.postman.service.FunctionalPersistenceService;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

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

    @Test
    public void shouldCreateFirstUiSingletonInstanceOnEdtWhenRequestedFromBackgroundThread() throws Exception {
        EdtTrackingPanel.createdOnEdt = false;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<EdtTrackingPanel> future = executorService.submit(
                    () -> UiSingletonFactory.getInstance(EdtTrackingPanel.class));

            EdtTrackingPanel panel = future.get();

            assertSame(panel, UiSingletonFactory.getInstance(EdtTrackingPanel.class));
            assertTrue(EdtTrackingPanel.createdOnEdt);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void shouldFailFastOnCircularUiSingletonCreationWithoutReturningPlaceholder() {
        GetInstanceException exception = expectThrows(GetInstanceException.class,
                () -> UiSingletonFactory.getInstance(CircularPanelA.class));

        assertTrue(hasCauseMessageContaining(exception, "Circular UI singleton creation"));
        assertFalse(hasCauseOfType(exception, ClassCastException.class));
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

    public static class EdtTrackingPanel extends UiSingletonPanel {
        static boolean createdOnEdt;

        @Override
        protected void initUI() {
            createdOnEdt = SwingUtilities.isEventDispatchThread();
        }

        @Override
        protected void registerListeners() {
            // no-op
        }
    }

    public static class CircularPanelA extends UiSingletonPanel {
        @Override
        protected void initUI() {
            UiSingletonFactory.getInstance(CircularPanelB.class);
        }

        @Override
        protected void registerListeners() {
            // no-op
        }
    }

    public static class CircularPanelB extends UiSingletonPanel {
        @Override
        protected void initUI() {
            UiSingletonFactory.getInstance(CircularPanelA.class);
        }

        @Override
        protected void registerListeners() {
            // no-op
        }
    }

    private boolean hasCauseMessageContaining(Throwable throwable, String expectedMessage) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expectedMessage)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasCauseOfType(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
