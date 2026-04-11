package com.laker.postman.service.variable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;

/**
 * 绑定/恢复脚本执行上下文的作用域对象。
 */
public final class ExecutionContextScope implements AutoCloseable {

    private final Map<String, String> previousVariables;
    private final Map<String, String> previousIterationData;
    private final DefaultMutableTreeNode previousRequestNode;
    private boolean closed;

    private ExecutionContextScope(Map<String, String> previousVariables,
                                  Map<String, String> previousIterationData,
                                  DefaultMutableTreeNode previousRequestNode) {
        this.previousVariables = previousVariables;
        this.previousIterationData = previousIterationData;
        this.previousRequestNode = previousRequestNode;
    }

    public static ExecutionContextScope open(ExecutionVariableContext context) {
        return open(context, null);
    }

    public static ExecutionContextScope open(ExecutionVariableContext context, DefaultMutableTreeNode requestNode) {
        Map<String, String> previousVariables = VariablesService.getInstance().getCurrentContextMap();
        Map<String, String> previousIterationData = IterationDataVariableService.getInstance().getCurrentContextMap();
        DefaultMutableTreeNode previousRequestNode = RequestContext.getCurrentRequestNode();

        VariablesService.getInstance().attachContextMap(context.getVariables());
        IterationDataVariableService.getInstance().attachContextMap(context.getIterationData());
        RequestContext.setCurrentRequestNode(requestNode);

        return new ExecutionContextScope(previousVariables, previousIterationData, previousRequestNode);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        VariablesService.getInstance().attachContextMap(previousVariables);
        IterationDataVariableService.getInstance().attachContextMap(previousIterationData);
        if (previousRequestNode != null) {
            RequestContext.setCurrentRequestNode(previousRequestNode);
        } else {
            RequestContext.clearCurrentRequestNode();
        }
        closed = true;
    }
}
