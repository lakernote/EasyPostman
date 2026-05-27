package com.laker.postman.panel.performance.tree;

import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class PerformanceTreeNodeTitleFormatterTest {

    @Test(description = "WebSocket 重复发送标题应展示每轮次数和间隔")
    public void shouldFormatWebSocketRepeatSendTitle() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        data.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
        data.sendCount = 3;
        data.sendIntervalMs = 1000;

        String title = PerformanceTreeNodeTitleFormatter.webSocketSendTitle(data);

        assertTrue(title.contains("每轮 3 次") || title.contains("Per loop 3x"), title);
        assertTrue(title.contains("1s"), title);
    }

    @Test(description = "WebSocket 读取节点标题应使用 Read 术语")
    public void shouldFormatWebSocketReadTitle() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        data.completionMode = WebSocketPerformanceData.CompletionMode.UNTIL_MATCH;
        data.firstMessageTimeoutMs = 10000;
        data.messageFilter = "ack";

        String title = PerformanceTreeNodeTitleFormatter.webSocketAwaitTitle(data);

        assertTrue(title.startsWith("WS Read"), title);
        assertTrue(title.contains("Until contains") || title.contains("读到包含文本"), title);
        assertTrue(title.contains("10s"), title);
        assertTrue(title.contains("contains=ack"), title);
    }

    @Test(description = "WebSocket 按消息数读取标题应展示统一读取超时")
    public void shouldFormatWebSocketMessageCountTitleWithReadTimeout() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        data.completionMode = WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;
        data.targetMessageCount = 3;
        data.firstMessageTimeoutMs = 2000;
        data.holdConnectionMs = 15000;

        String title = PerformanceTreeNodeTitleFormatter.webSocketAwaitTitle(data);

        assertTrue(title.contains("3"), title);
        assertTrue(title.contains("2s"), title);
        assertTrue(!title.contains("15s"), title);
    }

    @Test(description = "SSE 匹配消息标题应展示消息和事件过滤条件")
    public void shouldFormatSseMatchedMessageTitle() {
        SsePerformanceData data = new SsePerformanceData();
        data.completionMode = SsePerformanceData.CompletionMode.MATCHED_MESSAGE;
        data.firstMessageTimeoutMs = 10000;
        data.messageFilter = "ready";
        data.eventNameFilter = "open";

        String title = PerformanceTreeNodeTitleFormatter.sseAwaitTitle(data);

        assertTrue(title.contains("10s"), title);
        assertTrue(title.contains("contains=ready"), title);
        assertTrue(title.contains("event=open"), title);
    }

    @Test(description = "SSE 等待流关闭标题应展示模式和关闭超时")
    public void shouldFormatSseStreamClosedTitle() {
        SsePerformanceData data = new SsePerformanceData();
        data.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;
        data.holdConnectionMs = 15000;

        String title = PerformanceTreeNodeTitleFormatter.sseAwaitTitle(data);

        assertTrue(title.contains("15s"), title);
        assertTrue(title.contains("流关闭") || title.contains("Stream Closed"), title);
    }

    @Test(description = "Loop 标题应归一化循环次数")
    public void shouldFormatLoopTitleWithNormalizedIterations() {
        LoopData data = new LoopData();
        data.iterations = 0;

        String title = PerformanceTreeNodeTitleFormatter.loopTitle(data);

        assertTrue(title.contains("1x"), title);
    }
}
