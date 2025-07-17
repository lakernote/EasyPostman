package com.laker.postman.panel.jmeter;

/**
 * 线程组数据模型，支持多种线程模式
 */
public class ThreadGroupData {
    // 线程组类型
    public enum ThreadMode {
        FIXED("固定线程数"),           // 固定线程数
        RAMP_UP("递增线程数"),         // 递增线程数
        SPIKE("尖刺线程"),            // 尖刺模式
        PEAK("峰值线程"),             // 峰值模式
        STAIRS("阶梯线程");           // 阶梯模式

        private final String displayName;

        ThreadMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
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
    public int spikeRampUpTime = 10;                  // 尖刺上升时间(秒)
    public int spikeHoldTime = 5;                     // 尖刺保持时间(秒)
    public int spikeRampDownTime = 10;                // 尖刺下降时间(秒)
    public int spikeDuration = 60;                   // 尖刺模式总测试持续时间(秒)

    // 峰值模式属性
    public int peakMinThreads = 1;                    // 峰值最小线程数
    public int peakMaxThreads = 20;                   // 峰值最大线程数
    public int peakIterations = 3;                    // 峰值迭代次数
    public int peakHoldTime = 10;                     // 峰值保持时间(秒)
    public int peakDuration = 90;                    // 峰值模式总测试持续时间(秒)

    // 阶梯模式属性
    public int stairsStartThreads = 1;                // 阶梯起始线程数
    public int stairsEndThreads = 20;                 // 阶梯最终线程数
    public int stairsStep = 5;                        // 阶梯步长
    public int stairsHoldTime = 15;                   // 每阶段保持时间(秒)
    public int stairsDuration = 60;                  // 阶梯模式总测试持续时间(秒)
}