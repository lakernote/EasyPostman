package com.laker.postman.panel.performance.model;

// 节点类型定义
public enum NodeType {
    ROOT,
    THREAD_GROUP,
    CSV_DATA_SET,
    LOOP,
    REQUEST,
    ASSERTION,
    EXTRACTOR,
    TIMER,
    SSE_CONNECT,
    SSE_AWAIT,
    WS_CONNECT,
    WS_SEND,
    WS_AWAIT,
    WS_CLOSE
}
