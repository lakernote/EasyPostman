package com.laker.postman.panel.jmeter;

import com.laker.postman.model.HttpRequestItem;

public class JMeterTreeNode {
    String name;
    NodeType type;
    HttpRequestItem httpRequestItem; // 仅REQUEST节点用
    ThreadGroupData threadGroupData; // 线程组数据
    AssertionData assertionData;   // 断言数据
    TimerData timerData;           // 定时器数据

    JMeterTreeNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    JMeterTreeNode(String name, NodeType type, Object data) {
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