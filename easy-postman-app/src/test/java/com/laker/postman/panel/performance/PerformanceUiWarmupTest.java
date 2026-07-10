package com.laker.postman.panel.performance;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerformanceUiWarmupTest extends AbstractSwingUiTest {

    private Object previousFormattedTextFieldUi;

    @BeforeMethod
    public void setUp() throws Exception {
        previousFormattedTextFieldUi = UIManager.get("FormattedTextFieldUI");
        resetWarmupState();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        UIManager.put("FormattedTextFieldUI", previousFormattedTextFieldUi);
        resetWarmupState();
    }

    @Test
    public void warmUpShouldNotPropagateOptionalSpinnerInitializationFailures() throws Exception {
        UIManager.put("FormattedTextFieldUI", "missing.FormattedTextFieldUI");

        PrintStream previousErr = System.err;
        try (PrintStream capturedErr = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
            System.setErr(capturedErr);
            SwingUtilities.invokeAndWait(PerformanceUiWarmup::warmUp);
        } finally {
            System.setErr(previousErr);
        }
    }

    private static void resetWarmupState() throws Exception {
        setAtomicBoolean("scheduled", false);
        setAtomicBoolean("warmed", false);
    }

    private static void setAtomicBoolean(String fieldName, boolean value) throws Exception {
        Field field = PerformanceUiWarmup.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((AtomicBoolean) field.get(null)).set(value);
    }
}
