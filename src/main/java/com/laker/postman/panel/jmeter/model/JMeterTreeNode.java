package com.laker.postman.panel.jmeter.model;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.jmeter.assertion.AssertionData;
import com.laker.postman.panel.jmeter.threadgroup.ThreadGroupData;
import com.laker.postman.panel.jmeter.timer.TimerData;

public class JMeterTreeNode {
    public String name;
    public NodeType type;
    public HttpRequestItem httpRequestItem; // 仅REQUEST节点用
    public ThreadGroupData threadGroupData; // 线程组数据
    public AssertionData assertionData;   // 断言数据
    public TimerData timerData;           // 定时器数据

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
        }
    }

    public Object getNodeData() {
        return switch (type) {
            case THREAD_GROUP -> threadGroupData;
            case REQUEST -> httpRequestItem;
            case ASSERTION -> assertionData;
            case TIMER -> timerData;
            default -> null;
        };
    }

    public void setNodeData(Object data) {
        switch (type) {
            case THREAD_GROUP -> this.threadGroupData = (ThreadGroupData) data;
            case REQUEST -> this.httpRequestItem = (com.laker.postman.model.HttpRequestItem) data;
            case ASSERTION -> this.assertionData = (AssertionData) data;
            case TIMER -> this.timerData = (TimerData) data;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}