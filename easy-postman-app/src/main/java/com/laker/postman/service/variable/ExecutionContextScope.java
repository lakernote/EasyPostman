package com.laker.postman.service.variable;

import java.util.Map;

/**
 * 绑定/恢复脚本执行上下文的作用域对象。
 */
public final class ExecutionContextScope implements AutoCloseable {

    private final Map<String, String> previousVariables;
    private final Map<String, String> previousIterationData;
    private final IterationInfoService.IterationInfo previousIterationInfo;
    private final RequestExecutionScope previousRequestScope;
    private boolean closed;

    private ExecutionContextScope(Map<String, String> previousVariables,
                                  Map<String, String> previousIterationData,
                                  IterationInfoService.IterationInfo previousIterationInfo,
                                  RequestExecutionScope previousRequestScope) {
        this.previousVariables = previousVariables;
        this.previousIterationData = previousIterationData;
        this.previousIterationInfo = previousIterationInfo;
        this.previousRequestScope = previousRequestScope;
    }

    public static ExecutionContextScope open(ExecutionVariableContext context) {
        return open(context, RequestExecutionScope.empty());
    }

    public static ExecutionContextScope open(ExecutionVariableContext context, RequestExecutionScope requestExecutionScope) {
        return openInternal(context, requestExecutionScope == null ? RequestExecutionScope.empty() : requestExecutionScope);
    }

    private static ExecutionContextScope openInternal(ExecutionVariableContext context,
                                                      RequestExecutionScope requestExecutionScope) {
        Map<String, String> previousVariables = VariablesService.getInstance().getCurrentContextMap();
        Map<String, String> previousIterationData = IterationDataVariableService.getInstance().getCurrentContextMap();
        IterationInfoService.IterationInfo previousIterationInfo = IterationInfoService.getInstance().getCurrentContextInfo();
        RequestExecutionScope previousRequestScope = RequestExecutionContext.getCurrentScope();

        VariablesService.getInstance().attachContextMap(context.getVariables());
        IterationDataVariableService.getInstance().attachContextMap(context.getIterationData());
        IterationInfoService.getInstance().attachContextInfo(new IterationInfoService.IterationInfo(
                context.getIterationIndex(),
                context.getIterationCount()
        ));
        RequestExecutionContext.setCurrentScope(requestExecutionScope);

        return new ExecutionContextScope(
                previousVariables,
                previousIterationData,
                previousIterationInfo,
                previousRequestScope
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
        if (previousRequestScope != null) {
            RequestExecutionContext.setCurrentScope(previousRequestScope);
        } else {
            RequestExecutionContext.clearCurrentScope();
        }
        closed = true;
    }
}
