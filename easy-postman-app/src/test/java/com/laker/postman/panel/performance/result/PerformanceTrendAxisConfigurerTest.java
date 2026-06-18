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
    public void timeAxisShouldUseReadableWallClockTicksForShortRuns() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 10_500L);

        assertEquals(axis.getDateFormatOverride(), null);
        assertTrue(axis.getTickUnit().dateToString(new Date()).matches("\\d{2}:\\d{2}:\\d{2}"));
        assertFalse(axis.isAutoTickUnitSelection());
        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.SECOND);
        assertEquals(axis.getTickUnit().getMultiple(), 2);
    }

    @Test
    public void timeAxisShouldUseTwoSecondTicksWhenOneSecondLabelsWouldCrowd() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 14_000L);

        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.SECOND);
        assertEquals(axis.getTickUnit().getMultiple(), 2);
    }

    @Test
    public void timeAxisShouldUseTenSecondTicksForOneMinuteRuns() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 60_000L);

        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.SECOND);
        assertEquals(axis.getTickUnit().getMultiple(), 10);
    }

    @Test
    public void timeAxisShouldUseCoarserTicksForLongRuns() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 120_000L);

        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.SECOND);
        assertEquals(axis.getTickUnit().getMultiple(), 15);
    }

    @Test
    public void timeAxisShouldUseThirtyMinuteTicksForTwelveHourRuns() {
        DateAxis axis = new DateAxis("时间");

        PerformanceTrendAxisConfigurer.configureTimeAxis(axis, 12 * 60 * 60_000L);

        assertEquals(axis.getTickUnit().getUnitType(), DateTickUnitType.HOUR);
        assertEquals(axis.getTickUnit().getMultiple(), 2);
        assertTrue(axis.getTickUnit().dateToString(new Date()).matches("\\d{2}:\\d{2}"));
    }

    @Test
    public void timeAxisShouldReserveSmallRightPaddingWithoutCreatingLargeBlankTail() {
        assertEquals(PerformanceTrendAxisConfigurer.domainRightPaddingMs(50_000L), 1_250L);
        assertEquals(PerformanceTrendAxisConfigurer.domainRightPaddingMs(12 * 60 * 60_000L), 1_080_000L);
    }
}
