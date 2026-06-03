package com.laker.postman.common.component;

import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class EasyJSpinnerTest {

    @Test
    public void shouldDisplayIntegerValuesWithoutGroupingSeparators() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = EasyJSpinner.intSpinner(2000, 1, null, 1);
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

            assertEquals(textField.getText(), "2000");

            spinner.setValue(6120);

            assertEquals(textField.getText(), "6120");
        });
    }

    @Test
    public void shouldDisplayDirectNumberModelValuesWithoutGroupingSeparators() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = new EasyJSpinner(new SpinnerNumberModel(2000, 0, 1_000_000, 1));
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

            assertEquals(textField.getText(), "2000");
        });
    }

    @Test
    public void shouldCommitEditorTextIntoSpinnerModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = EasyJSpinner.intSpinner(5, 0, 10, 1);
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

            textField.setText("8");

            assertTrue(spinner.commitCurrentEdit());
            assertEquals(spinner.getCommittedIntValue(), 8);
        });
    }

    @Test
    public void shouldCommitLargeEditorTextWhenMaximumIsUnbounded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = EasyJSpinner.intSpinner(1, 1, null, 1);
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            textField.setText("123456");

            assertEquals(spinner.getValue(), 1);

            int committedValue = spinner.getCommittedIntValue();

            assertEquals(committedValue, 123456);
            assertEquals(spinner.getValue(), 123456);
            assertEquals(textField.getText(), "123456");
        });
    }

    @Test
    public void shouldReportInvalidEditorTextWhenValueExceedsBoundedMaximum() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = EasyJSpinner.intSpinner(1, 1, 10, 1);
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            textField.setText("20");

            assertTrue(!spinner.commitCurrentEdit());
            assertEquals(spinner.getValue(), 1);
        });
    }
}
