package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.timer.TimerData;

import javax.swing.tree.DefaultMutableTreeNode;

public final class PerformanceTimerElement implements PerformancePlanElement {
    private final String name;
    private final TimerData timerData;

    public PerformanceTimerElement(String name, TimerData timerData) {
        this.name = name;
        this.timerData = PerformancePlanNodeCopies.copyTimerData(timerData);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.TIMER;
    }

    public TimerData getTimerData() {
        return PerformancePlanNodeCopies.copyTimerData(timerData);
    }

    @Override
    public DefaultMutableTreeNode toTreeNode() {
        JMeterTreeNode node = new JMeterTreeNode(name, NodeType.TIMER);
        node.timerData = PerformancePlanNodeCopies.copyTimerData(timerData);
        return new DefaultMutableTreeNode(node);
    }
}
