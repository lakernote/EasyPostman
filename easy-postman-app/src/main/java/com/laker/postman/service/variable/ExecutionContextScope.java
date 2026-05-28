package com.laker.postman.service.variable;

import com.laker.postman.service.collections.GroupInheritanceHelper;

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
    private final RequestExecutionScope previousRequestExecutionScope;
    private boolean closed;

    private ExecutionContextScope(Map<String, String> previousVariables,
                                  Map<String, String> previousIterationData,
                                  IterationInfoService.IterationInfo previousIterationInfo,
                                  DefaultMutableTreeNode previousRequestNode,
                                  RequestExecutionScope previousRequestExecutionScope) {
        this.previousVariables = previousVariables;
        this.previousIterationData = previousIterationData;
        this.previousIterationInfo = previousIterationInfo;
        this.previousRequestNode = previousRequestNode;
        this.previousRequestExecutionScope = previousRequestExecutionScope;
    }

    public static ExecutionContextScope open(ExecutionVariableContext context) {
        return open(context, RequestExecutionScope.empty());
    }

    public static ExecutionContextScope open(ExecutionVariableContext context, DefaultMutableTreeNode requestNode) {
        RequestExecutionScope requestExecutionScope = requestNode == null
                ? RequestExecutionScope.empty()
                : RequestExecutionScope.fromVariables(GroupInheritanceHelper.getMergedGroupVariables(requestNode));
        return openInternal(context, requestExecutionScope, requestNode);
    }

    public static ExecutionContextScope open(ExecutionVariableContext context, RequestExecutionScope requestExecutionScope) {
        return openInternal(
                context,
                requestExecutionScope == null ? RequestExecutionScope.empty() : requestExecutionScope,
                null
        );
    }

    private static ExecutionContextScope openInternal(ExecutionVariableContext context,
                                                      RequestExecutionScope requestExecutionScope,
                                                      DefaultMutableTreeNode requestNode) {
        Map<String, String> previousVariables = VariablesService.getInstance().getCurrentContextMap();
        Map<String, String> previousIterationData = IterationDataVariableService.getInstance().getCurrentContextMap();
        IterationInfoService.IterationInfo previousIterationInfo = IterationInfoService.getInstance().getCurrentContextInfo();
        DefaultMutableTreeNode previousRequestNode = RequestContext.getCurrentRequestNode();
        RequestExecutionScope previousRequestExecutionScope = RequestContext.getCurrentExecutionScope();

        VariablesService.getInstance().attachContextMap(context.getVariables());
        IterationDataVariableService.getInstance().attachContextMap(context.getIterationData());
        IterationInfoService.getInstance().attachContextInfo(new IterationInfoService.IterationInfo(
                context.getIterationIndex(),
                context.getIterationCount()
        ));
        if (requestNode != null) {
            RequestContext.setCurrentRequestNode(requestNode);
        } else {
            RequestContext.setCurrentExecutionScope(requestExecutionScope);
        }

        return new ExecutionContextScope(
                previousVariables,
                previousIterationData,
                previousIterationInfo,
                previousRequestNode,
                previousRequestExecutionScope
        );
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
        } else if (previousRequestExecutionScope != null) {
            RequestContext.setCurrentExecutionScope(previousRequestExecutionScope);
        } else {
            RequestContext.clearCurrentRequestNode();
        }
        closed = true;
    }
}
