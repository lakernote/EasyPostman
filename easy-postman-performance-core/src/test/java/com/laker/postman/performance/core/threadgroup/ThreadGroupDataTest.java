package com.laker.postman.performance.core.threadgroup;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ThreadGroupDataTest {

    @Test
    public void normalizeShouldPreserveLargeThreadCounts() {
        ThreadGroupData data = new ThreadGroupData();
        data.numThreads = 123_456;
        data.duration = 987_654;
        data.loops = 1_234_567;
        data.rampUpStartThreads = 11_111;
        data.rampUpEndThreads = 22_222;
        data.rampUpTime = 33_333;
        data.rampUpDuration = 44_444;
        data.spikeMinThreads = 55_555;
        data.spikeMaxThreads = 66_666;
        data.spikeRampUpTime = 77_777;
        data.spikeHoldTime = 88_888;
        data.spikeRampDownTime = 99_999;
        data.spikeDuration = 111_111;
        data.stairsStartThreads = 222_222;
        data.stairsEndThreads = 333_333;
        data.stairsStep = 444;
        data.stairsHoldTime = 555_555;
        data.stairsDuration = 666_666;

        data.normalize();

        assertEquals(data.numThreads, 123_456);
        assertEquals(data.duration, 987_654);
        assertEquals(data.loops, 1_234_567);
        assertEquals(data.rampUpStartThreads, 11_111);
        assertEquals(data.rampUpEndThreads, 22_222);
        assertEquals(data.rampUpTime, 33_333);
        assertEquals(data.rampUpDuration, 44_444);
        assertEquals(data.spikeMinThreads, 55_555);
        assertEquals(data.spikeMaxThreads, 66_666);
        assertEquals(data.spikeRampUpTime, 77_777);
        assertEquals(data.spikeHoldTime, 88_888);
        assertEquals(data.spikeRampDownTime, 99_999);
        assertEquals(data.spikeDuration, 111_111);
        assertEquals(data.stairsStartThreads, 222_222);
        assertEquals(data.stairsEndThreads, 333_333);
        assertEquals(data.stairsStep, 444);
        assertEquals(data.stairsHoldTime, 555_555);
        assertEquals(data.stairsDuration, 666_666);
    }

    @Test
    public void normalizeShouldClampThreadCountsToMinimumOnly() {
        ThreadGroupData data = new ThreadGroupData();
        data.numThreads = 0;
        data.rampUpStartThreads = 0;
        data.rampUpEndThreads = -10;
        data.spikeMinThreads = -20;
        data.spikeMaxThreads = 0;
        data.stairsStartThreads = -30;
        data.stairsEndThreads = 0;

        data.normalize();

        assertEquals(data.numThreads, ThreadGroupData.MIN_THREADS);
        assertEquals(data.rampUpStartThreads, ThreadGroupData.MIN_THREADS);
        assertEquals(data.rampUpEndThreads, ThreadGroupData.MIN_THREADS);
        assertEquals(data.spikeMinThreads, ThreadGroupData.MIN_THREADS);
        assertEquals(data.spikeMaxThreads, ThreadGroupData.MIN_THREADS);
        assertEquals(data.stairsStartThreads, ThreadGroupData.MIN_THREADS);
        assertEquals(data.stairsEndThreads, ThreadGroupData.MIN_THREADS);
    }

    @Test
    public void threadModeToStringShouldStayHeadlessSafeAndStorageStable() {
        assertEquals(ThreadGroupData.ThreadMode.FIXED.toString(), "FIXED");
        assertEquals(ThreadGroupData.ThreadMode.RAMP_UP.toString(), "RAMP_UP");
    }
}
