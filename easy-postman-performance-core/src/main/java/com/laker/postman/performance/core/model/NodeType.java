package com.laker.postman.performance.core.model;

// 节点类型定义
public enum NodeType {
    ROOT,
    THREAD_GROUP,
    CSV_DATA_SET,
    SIMPLE,
    LOOP,
    CONDITION,
    WHILE,
    ONCE_ONLY,
    REQUEST,
    ASSERTION,
    EXTRACTOR,
    TIMER,
    SSE_CONNECT,
    SSE_READ,
    WS_CONNECT,
    WS_SEND,
    WS_READ,
    WS_CLOSE
}
