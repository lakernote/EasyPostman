package com.laker.postman.panel.performance.result;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PerformanceTrendAxisConfigurerTest {

    @Test
    public void integerAxisShouldUseIntegerTickUnitsToAvoidDuplicateRoundedLabels() {
        NumberAxis axis = new NumberAxis("虚拟用户数");

        PerformanceTrendAxisConfigurer.configureIntegerAxis(axis);

        assertNotNull(axis.getNumberFormatOverride());
        assertEquals(axis.getStandardTickUnits().getCeilingTickUnit(0.5).getSize(), 1.0);
    }

    @Test
    public void timeAxisShouldUseWallClockFormatAndOneSecondTicksForShortRuns() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 10_000L);

        assertNotNull(axis.getDateFormatOverride());
        assertTrue(axis.getDateFormatOverride().format(new Date()).matches("\\d{2}:\\d{2}:\\d{2}"));
        assertFalse(axis.isAutoTickUnitSelection());
        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.SECOND);
        assertEquals(axis.getTickUnit().getMultiple(), 1);
    }

    @Test
    public void timeAxisShouldUseCoarserTicksForLongRuns() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 120_000L);

        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.SECOND);
        assertEquals(axis.getTickUnit().getMultiple(), 15);
    }
}
