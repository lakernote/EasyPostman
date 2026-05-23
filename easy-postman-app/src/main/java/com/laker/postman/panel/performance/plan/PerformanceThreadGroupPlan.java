package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceThreadGroupPlan {
    private final String name;
    private final ThreadGroupData threadGroupData;
    private final List<PerformancePlanElement> elements;

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      List<PerformancePlanElement> elements) {
        this.name = name;
        this.threadGroupData = PerformancePlanNodeCopies.copyThreadGroupData(threadGroupData);
        if (this.threadGroupData != null) {
            this.threadGroupData.normalize();
        }
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    public String getName() {
        return name;
    }

    public ThreadGroupData getThreadGroupData() {
        return PerformancePlanNodeCopies.copyThreadGroupData(threadGroupData);
    }

    public List<PerformancePlanElement> getElements() {
        return elements;
    }

    public DefaultMutableTreeNode toTreeNode() {
        JMeterTreeNode node = new JMeterTreeNode(name, NodeType.THREAD_GROUP);
        node.threadGroupData = PerformancePlanNodeCopies.copyThreadGroupData(threadGroupData);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        for (PerformancePlanElement element : elements) {
            treeNode.add(element.toTreeNode());
        }
        return treeNode;
    }
}
