package com.laker.postman.panel.performance.model;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.timer.TimerData;

public class JMeterTreeNode {
    public String name;
    public NodeType type;
    public HttpRequestItem httpRequestItem; // 仅REQUEST节点用
    public ThreadGroupData threadGroupData; // 线程组数据
    public AssertionData assertionData;   // 断言数据
    public TimerData timerData;           // 定时器数据
    public SsePerformanceData ssePerformanceData; // SSE 压测配置，仅 SSE REQUEST 节点使用
    public boolean enabled = true;        // 是否启用，默认启用

    public JMeterTreeNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    public JMeterTreeNode(String name, NodeType type, Object data) {
        this.name = name;
        this.type = type;
        switch (type) {
            case THREAD_GROUP -> this.threadGroupData = (ThreadGroupData) data;
            case REQUEST -> this.httpRequestItem = (HttpRequestItem) data;
            case ASSERTION -> this.assertionData = (AssertionData) data;
            case TIMER -> this.timerData = (TimerData) data;
            case SSE_CONNECT, SSE_AWAIT, SSE_CLOSE, ROOT -> {
            }
        }
    }

    public Object getNodeData() {
        return switch (type) {
            case THREAD_GROUP -> threadGroupData;
            case REQUEST -> httpRequestItem;
            case ASSERTION -> assertionData;
            case TIMER -> timerData;
            case SSE_CONNECT, SSE_AWAIT, SSE_CLOSE, ROOT -> null;
            default -> null;
        };
    }

    public void setNodeData(Object data) {
        switch (type) {
            case THREAD_GROUP -> this.threadGroupData = (ThreadGroupData) data;
            case REQUEST -> this.httpRequestItem = (com.laker.postman.model.HttpRequestItem) data;
            case ASSERTION -> this.assertionData = (AssertionData) data;
            case TIMER -> this.timerData = (TimerData) data;
            case SSE_CONNECT, SSE_AWAIT, SSE_CLOSE, ROOT -> {
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
