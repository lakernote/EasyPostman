package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

/**
 * 线程组数据模型，支持多种线程模式
 */
public class ThreadGroupData {
    private static final int MIN_THREADS = 1;
    private static final int MAX_THREADS = 1000;
    private static final int MIN_SECONDS = 1;
    private static final int MAX_DURATION_SECONDS = 86400;
    private static final int MAX_PHASE_SECONDS = 3600;
    private static final int MIN_LOOPS = 1;
    private static final int MAX_LOOPS = 100000;
    private static final int MIN_STEP = 1;
    private static final int MAX_STEP = 100;

    // 线程组类型
    public enum ThreadMode {
        FIXED(MessageKeys.THREADGROUP_MODE_FIXED),           // 固定线程数
        RAMP_UP(MessageKeys.THREADGROUP_MODE_RAMP_UP),       // 递增线程数
        SPIKE(MessageKeys.THREADGROUP_MODE_SPIKE),           // 尖刺模式
        STAIRS(MessageKeys.THREADGROUP_MODE_STAIRS);         // 阶梯模式

        private final String messageKey;

        ThreadMode(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getDisplayName() {
            return I18nUtil.getMessage(messageKey);
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    // 公共属性
    public ThreadMode threadMode = ThreadMode.FIXED;  // 默认固定线程数
    public int numThreads = 20;                        // 固定模式-默认用户数
    public int duration = 60;                         // 所有模式-默认持续时间(秒)
    public int loops = 1;                             // 固定模式-默认循环次数
    public boolean useTime = true;                   // 是否使用时间而不是循环次数

    // 递增模式属性
    public int rampUpStartThreads = 1;                // 递增起始线程数
    public int rampUpEndThreads = 20;                 // 递增最终线程数
    public int rampUpTime = 30;                       // 递增时间(秒)
    public int rampUpDuration = 60;                  // 递增模式总测试持续时间(秒)

    // 尖刺模式属性
    public int spikeMinThreads = 1;                   // 尖刺最小线程数
    public int spikeMaxThreads = 20;                  // 尖刺最大线程数
    public int spikeRampUpTime = 20;                  // 尖刺上升时间(秒)
    public int spikeHoldTime = 15;                     // 尖刺保持时间(秒)
    public int spikeRampDownTime = 20;                // 尖刺下降时间(秒)
    public int spikeDuration = 60;                   // 尖刺模式总测试持续时间(秒)


    // 阶梯模式属性
    public int stairsStartThreads = 5;                // 阶梯起始线程数
    public int stairsEndThreads = 20;                 // 阶梯最终线程数
    public int stairsStep = 5;                        // 阶梯步长
    public int stairsHoldTime = 15;                   // 每阶段保持时间(秒)
    public int stairsDuration = 60;                  // 阶梯模式总测试持续时间(秒)

    public void normalize() {
        if (threadMode == null) {
            threadMode = ThreadMode.FIXED;
        }

        numThreads = clamp(numThreads, MIN_THREADS, MAX_THREADS);
        duration = clamp(duration, MIN_SECONDS, MAX_DURATION_SECONDS);
        loops = clamp(loops, MIN_LOOPS, MAX_LOOPS);

        rampUpStartThreads = clamp(rampUpStartThreads, MIN_THREADS, MAX_THREADS);
        rampUpEndThreads = clamp(rampUpEndThreads, MIN_THREADS, MAX_THREADS);
        if (rampUpStartThreads > rampUpEndThreads) {
            int previousStart = rampUpStartThreads;
            rampUpStartThreads = rampUpEndThreads;
            rampUpEndThreads = previousStart;
        }
        rampUpTime = clamp(rampUpTime, MIN_SECONDS, MAX_PHASE_SECONDS);
        rampUpDuration = clamp(rampUpDuration, MIN_SECONDS, MAX_DURATION_SECONDS);

        spikeMinThreads = clamp(spikeMinThreads, MIN_THREADS, MAX_THREADS);
        spikeMaxThreads = clamp(spikeMaxThreads, MIN_THREADS, MAX_THREADS);
        if (spikeMinThreads > spikeMaxThreads) {
            int previousMin = spikeMinThreads;
            spikeMinThreads = spikeMaxThreads;
            spikeMaxThreads = previousMin;
        }
        spikeRampUpTime = clamp(spikeRampUpTime, MIN_SECONDS, MAX_PHASE_SECONDS);
        spikeHoldTime = clamp(spikeHoldTime, 0, MAX_PHASE_SECONDS);
        spikeRampDownTime = clamp(spikeRampDownTime, MIN_SECONDS, MAX_PHASE_SECONDS);
        spikeDuration = clamp(spikeDuration, MIN_SECONDS, MAX_DURATION_SECONDS);

        stairsStartThreads = clamp(stairsStartThreads, MIN_THREADS, MAX_THREADS);
        stairsEndThreads = clamp(stairsEndThreads, MIN_THREADS, MAX_THREADS);
        if (stairsStartThreads > stairsEndThreads) {
            int previousStart = stairsStartThreads;
            stairsStartThreads = stairsEndThreads;
            stairsEndThreads = previousStart;
        }
        stairsStep = clamp(stairsStep, MIN_STEP, MAX_STEP);
        stairsHoldTime = clamp(stairsHoldTime, MIN_SECONDS, MAX_PHASE_SECONDS);
        stairsDuration = clamp(stairsDuration, MIN_SECONDS, MAX_DURATION_SECONDS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
