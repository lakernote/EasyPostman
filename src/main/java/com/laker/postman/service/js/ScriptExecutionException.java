package com.laker.postman.service.js;

import lombok.Getter;

/**
 * 脚本执行异常
 */
@Getter
public class ScriptExecutionException extends RuntimeException {
    private final ScriptExecutionContext.ScriptType scriptType;

    public ScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.scriptType = null;
    }

    public ScriptExecutionException(String message, Throwable cause, ScriptExecutionContext.ScriptType scriptType) {
        super(message, cause);
        this.scriptType = scriptType;
    }

}