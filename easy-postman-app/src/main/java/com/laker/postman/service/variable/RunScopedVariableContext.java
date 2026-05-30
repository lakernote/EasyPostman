package com.laker.postman.service.variable;

import com.laker.postman.model.Environment;

public final class RunScopedVariableContext implements AutoCloseable {
    private static final InheritableThreadLocal<Environment> SCOPED_ENVIRONMENT = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Environment> SCOPED_GLOBALS = new InheritableThreadLocal<>();

    private final Environment previousEnvironment;
    private final Environment previousGlobals;
    private boolean closed;

    private RunScopedVariableContext(Environment previousEnvironment, Environment previousGlobals) {
        this.previousEnvironment = previousEnvironment;
        this.previousGlobals = previousGlobals;
    }

    public static RunScopedVariableContext open(Environment environment, Environment globals) {
        Environment previousEnvironment = SCOPED_ENVIRONMENT.get();
        Environment previousGlobals = SCOPED_GLOBALS.get();
        setOrClear(SCOPED_ENVIRONMENT, environment);
        setOrClear(SCOPED_GLOBALS, globals);
        return new RunScopedVariableContext(previousEnvironment, previousGlobals);
    }

    public static Environment currentEnvironment() {
        return SCOPED_ENVIRONMENT.get();
    }

    public static Environment currentGlobals() {
        return SCOPED_GLOBALS.get();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        setOrClear(SCOPED_ENVIRONMENT, previousEnvironment);
        setOrClear(SCOPED_GLOBALS, previousGlobals);
        closed = true;
    }

    private static void setOrClear(ThreadLocal<Environment> holder, Environment value) {
        if (value == null) {
            holder.remove();
            return;
        }
        holder.set(value);
    }
}
