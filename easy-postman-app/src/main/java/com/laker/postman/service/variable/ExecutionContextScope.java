package com.laker.postman.service.variable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;

/**
 * 绑定/恢复脚本执行上下文的作用域对象。
 */
public final class ExecutionContextScope implements AutoCloseable {

    private final Map<String, String> previousVariables;
    private final Map<String, String> previousIterationData;
    private final IterationInfoService.IterationInfo previousIterationInfo;
    private final DefaultMutableTreeNode previousRequestNode;
    private boolean closed;

    private ExecutionContextScope(Map<String, String> previousVariables,
                                  Map<String, String> previousIterationData,
                                  IterationInfoService.IterationInfo previousIterationInfo,
                                  DefaultMutableTreeNode previousRequestNode) {
        this.previousVariables = previousVariables;
        this.previousIterationData = previousIterationData;
        this.previousIterationInfo = previousIterationInfo;
        this.previousRequestNode = previousRequestNode;
    }

    public static ExecutionContextScope open(ExecutionVariableContext context) {
        return open(context, null);
    }

    public static ExecutionContextScope open(ExecutionVariableContext context, DefaultMutableTreeNode requestNode) {
        Map<String, String> previousVariables = VariablesService.getInstance().getCurrentContextMap();
        Map<String, String> previousIterationData = IterationDataVariableService.getInstance().getCurrentContextMap();
        IterationInfoService.IterationInfo previousIterationInfo = IterationInfoService.getInstance().getCurrentContextInfo();
        DefaultMutableTreeNode previousRequestNode = RequestContext.getCurrentRequestNode();

        VariablesService.getInstance().attachContextMap(context.getVariables());
        IterationDataVariableService.getInstance().attachContextMap(context.getIterationData());
        IterationInfoService.getInstance().attachContextInfo(new IterationInfoService.IterationInfo(
                context.getIterationIndex(),
                context.getIterationCount()
        ));
        RequestContext.setCurrentRequestNode(requestNode);

        return new ExecutionContextScope(previousVariables, previousIterationData, previousIterationInfo, previousRequestNode);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        VariablesService.getInstance().attachContextMap(previousVariables);
        IterationDataVariableService.getInstance().attachContextMap(previousIterationData);
        IterationInfoService.getInstance().attachContextInfo(previousIterationInfo);
        if (previousRequestNode != null) {
            RequestContext.setCurrentRequestNode(previousRequestNode);
        } else {
            RequestContext.clearCurrentRequestNode();
        }
        closed = true;
    }
}
