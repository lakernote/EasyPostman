package com.laker.postman.common.component;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class EasyJSpinnerTest extends AbstractSwingUiTest {

    @Test
    public void getCommittedIntValueShouldCommitEditedTextBeforeRead() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = EasyJSpinner.intSpinner(1, 1, null, 1);
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            textField.setText("123456");

            assertEquals(spinner.getValue(), 1);

            int committedValue = spinner.getCommittedIntValue();

            assertEquals(committedValue, 123456);
            assertEquals(spinner.getValue(), 123456);
        });
    }

    @Test
    public void commitCurrentEditShouldReportInvalidEditedText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyJSpinner spinner = EasyJSpinner.intSpinner(1, 1, 10, 1);
            JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            textField.setText("20");

            assertTrue(!spinner.commitCurrentEdit());
            assertEquals(spinner.getValue(), 1);
        });
    }
}
