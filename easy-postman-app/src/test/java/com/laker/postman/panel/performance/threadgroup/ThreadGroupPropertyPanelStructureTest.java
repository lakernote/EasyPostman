package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.JToggleButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.lang.reflect.Field;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

public class ThreadGroupPropertyPanelStructureTest extends AbstractSwingUiTest {
    @Test
    public void previewChartShouldBeWrappedByAVisibleSectionTitle() throws Exception {
        ThreadGroupPropertyPanel panel = new ThreadGroupPropertyPanel();
        Component previewPanel = fieldValue(panel, "previewPanel", Component.class);

        JLabel previewTitle = findLabel(
                previewPanel.getParent(),
                I18nUtil.getMessage(MessageKeys.THREADGROUP_PREVIEW_TITLE)
        );

        assertNotNull(previewTitle);
    }

    @Test
    public void modeSelectorShouldUseFixedMaxEasyComboBox() throws Exception {
        JComboBox<?> modeComboBox = fieldValue(panel(), "modeComboBox", JComboBox.class);

        assertTrue(modeComboBox instanceof EasyComboBox<?>);
        Object widthMode = fieldValue(modeComboBox, "widthMode", Object.class);
        assertTrue(widthMode == EasyComboBox.WidthMode.FIXED_MAX);
    }

    @Test
    public void modeSelectorShouldStayCloseToItsLabel() throws Exception {
        ThreadGroupPropertyPanel panel = panel();
        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);

        JLabel modeLabel = findLabel(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_MODE_LABEL)
        );
        Component modeComboBox = fieldValue(panel, "modeComboBox", Component.class);

        assertNotNull(modeLabel);
        int gap = horizontalGap(panel, modeLabel, modeComboBox);
        assertTrue(gap <= 16, "mode selector gap should stay compact, actual gap: " + gap);
    }

    @Test
    public void fixedExecutionModeShouldUseBorderlessInlineToggleContainer() {
        ThreadGroupPropertyPanel panel = panel();
        JToggleButton timeButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_TIME)
        );

        assertNotNull(timeButton);
        assertTrue(!(timeButton.getParent() instanceof SegmentedButtonGroupPanel),
                "execution mode should not use the heavy outer segmented container in this dense form");
    }

    @Test
    public void fixedExecutionModeShouldUseCompactButtonLabels() {
        ThreadGroupPropertyPanel panel = panel();

        JToggleButton countButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_COUNT)
        );
        JToggleButton timeButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_TIME)
        );

        assertNotNull(countButton);
        assertNotNull(timeButton);
    }

    @Test
    public void rampUpFormLabelsShouldStayCloseToTheirInputs() throws Exception {
        ThreadGroupPropertyPanel panel = panel();
        JComboBox<?> modeComboBox = fieldValue(panel, "modeComboBox", JComboBox.class);
        modeComboBox.setSelectedItem(ThreadGroupData.ThreadMode.RAMP_UP);

        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);

        JLabel startUsersLabel = findLabel(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_RAMPUP_START_USERS)
        );
        EasyJSpinner startUsersSpinner = fieldValue(panel, "rampUpStartThreadsSpinner", EasyJSpinner.class);
        JLabel endUsersLabel = findLabel(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_RAMPUP_END_USERS)
        );
        EasyJSpinner endUsersSpinner = fieldValue(panel, "rampUpEndThreadsSpinner", EasyJSpinner.class);

        assertNotNull(startUsersLabel);
        assertNotNull(endUsersLabel);
        int startGap = horizontalGap(panel, startUsersLabel, startUsersSpinner);
        int endGap = horizontalGap(panel, endUsersLabel, endUsersSpinner);
        assertTrue(startGap <= 16, "start users label/input gap should stay compact, actual gap: " + startGap);
        assertTrue(endGap <= 16, "end users label/input gap should stay compact, actual gap: " + endGap);
    }

    @Test
    public void fixedExecutionModeToggleShouldStayCloseAndUnclipped() throws Exception {
        ThreadGroupPropertyPanel panel = panel();
        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);
        Component fixedPanel = fieldValue(panel, "fixedPanel", Component.class);

        JLabel executionModeLabel = findLabel(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_MODE)
        );
        JToggleButton loopButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_COUNT)
        );
        JToggleButton timeButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_TIME)
        );

        assertNotNull(executionModeLabel);
        assertNotNull(loopButton);
        assertNotNull(timeButton);
        int toggleGap = horizontalGap(panel, executionModeLabel, loopButton);
        assertTrue(toggleGap <= 16, "execution mode toggle gap should stay compact, actual gap: " + toggleGap);
        assertTrue(loopButton.getWidth() >= loopButton.getPreferredSize().width,
                "loop count button should not be clipped");
        assertTrue(timeButton.getWidth() >= timeButton.getPreferredSize().width,
                "time mode button should not be clipped");
        assertTrue(loopButton.getInsets().left <= 10,
                "execution mode toggle button should use compact horizontal insets");
        assertTrue(timeButton.getInsets().left <= 10,
                "execution mode toggle button should use compact horizontal insets");
        JComponent group = (JComponent) timeButton.getParent();
        assertTrue(group.getHeight() >= group.getPreferredSize().height,
                "execution mode group should not be vertically clipped");
        assertTrue(fixedPanel.getPreferredSize().width <= 460,
                "fixed mode form preferred width should fit the config card, actual width: "
                        + fixedPanel.getPreferredSize().width);
    }

    @Test
    public void fixedExecutionModeShouldShareTheFirstParameterRow() {
        ThreadGroupPropertyPanel panel = panel();
        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);

        JLabel usersLabel = findLabel(panel, I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_USERS));
        JLabel executionModeLabel = findLabel(panel, I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_MODE));
        JLabel loopsLabel = findLabel(panel, I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_LOOPS));

        assertNotNull(usersLabel);
        assertNotNull(executionModeLabel);
        assertNotNull(loopsLabel);
        int usersY = verticalStart(panel, usersLabel);
        int executionModeY = verticalStart(panel, executionModeLabel);
        int loopsY = verticalStart(panel, loopsLabel);
        assertTrue(Math.abs(usersY - executionModeY) <= 4,
                "users and execution mode should stay on the same row");
        assertTrue(loopsY > usersY + 8,
                "loop count should stay on the second row");
    }

    @Test
    public void fixedExecutionModeShouldUseACompactGroupedControl() {
        ThreadGroupPropertyPanel panel = panel();
        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);

        JToggleButton timeButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_TIME)
        );

        assertNotNull(timeButton);
        JComponent group = (JComponent) timeButton.getParent();
        assertNotNull(group.getBorder());
        Insets insets = group.getBorder().getBorderInsets(group);
        assertTrue(insets.left >= 2, "execution mode group should have horizontal chrome padding");
        assertTrue(insets.top >= 2, "execution mode group should have vertical chrome padding");
        assertTrue(group.getPreferredSize().height <= 34,
                "execution mode group should stay compact, preferred height: " + group.getPreferredSize().height);
    }

    @Test
    public void fixedExecutionModeGroupShouldAvoidExcessHorizontalSlack() {
        ThreadGroupPropertyPanel panel = panel();
        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);

        JToggleButton loopButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_COUNT)
        );
        JToggleButton timeButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.THREADGROUP_FIXED_EXECUTION_TIME)
        );

        assertNotNull(loopButton);
        assertNotNull(timeButton);
        JComponent group = (JComponent) timeButton.getParent();
        Insets insets = group.getBorder().getBorderInsets(group);
        int naturalContentWidth = loopButton.getPreferredSize().width
                + timeButton.getPreferredSize().width
                + insets.left
                + insets.right
                + 1;
        int horizontalSlack = group.getPreferredSize().width - naturalContentWidth;

        assertTrue(horizontalSlack <= 4,
                "execution mode group should avoid excessive horizontal slack, actual slack: "
                        + horizontalSlack);
    }

    @Test
    public void configAndPreviewShouldKeepClearHorizontalSeparation() throws Exception {
        ThreadGroupPropertyPanel panel = panel();
        panel.setSize(new Dimension(1400, 320));
        layoutRecursively(panel);

        Component fixedPanel = fieldValue(panel, "fixedPanel", Component.class);
        Component previewPanel = fieldValue(panel, "previewPanel", Component.class);

        int gap = horizontalGap(panel, fixedPanel, previewPanel);
        assertTrue(gap >= 32,
                "config form and preview chart should keep clear horizontal separation, actual gap: " + gap);
    }

    private static ThreadGroupPropertyPanel panel() {
        return new ThreadGroupPropertyPanel();
    }

    private static int horizontalGap(Component root, Component left, Component right) {
        Point leftEnd = SwingUtilities.convertPoint(left.getParent(), left.getX() + left.getWidth(), left.getY(), root);
        Point rightStart = SwingUtilities.convertPoint(right.getParent(), right.getX(), right.getY(), root);
        return rightStart.x - leftEnd.x;
    }

    private static int verticalStart(Component root, Component component) {
        return SwingUtilities.convertPoint(component.getParent(), component.getX(), component.getY(), root).y;
    }

    private static void layoutRecursively(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    private static JLabel findLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel label = findLabel(child, text);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private static JToggleButton findToggleButton(Component component, String text) {
        if (component instanceof JToggleButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JToggleButton button = findToggleButton(child, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static <T> T fieldValue(Object instance, String fieldName, Class<T> type) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(instance));
    }

}
