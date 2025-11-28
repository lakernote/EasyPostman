package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JS脚本执行器，使用GraalVM的Polyglot API执行JavaScript脚本。
 * 提供统一的脚本执行入口，支持变量注入、polyfill、输出回调和错误处理。
 */
@Slf4j
@UtilityClass
public class JsScriptExecutor {

    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false") // 禁用解释器模式警告
            .build();

    /**
     * 执行JS脚本（使用上下文对象）
     *
     * @param context 脚本执行上下文
     * @throws ScriptExecutionException 脚本执行异常
     */
    public static void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        if (context == null || context.getScript() == null || context.getScript().isBlank()) {
            log.debug("Script is empty, skipping execution");
            return;
        }

        try {
            executeScript(context.getScript(), context.getBindings(), context.getOutputCallback());
            log.debug("Script executed successfully: {}", context.getScriptType().getDisplayName());
        } catch (Exception e) {
            String errorMsg = String.format("%s execution failed: %s",
                    context.getScriptType().getDisplayName(), e.getMessage());
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e, context.getScriptType());
        }
    }

    /**
     * 执行JS脚本，自动注入所有变量、polyfill，并支持输出回调。
     *
     * @param script         脚本内容
     * @param bindings       需要注入的变量（变量名->对象），如request/env/postman/responseBody等
     * @param outputCallback 输出回调（可为null）
     * @throws ScriptExecutionException 脚本执行异常
     */
    public static void executeScript(String script, Map<String, Object> bindings, OutputCallback outputCallback)
            throws ScriptExecutionException {
        if (script == null || script.isBlank()) {
            return;
        }
        OutputStream outputStream = createOutputStream(outputCallback);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .out(outputStream)
                .err(outputStream)
                .engine(ENGINE)
                .build()) {

            // 注入变量和polyfill
            injectBindings(context, bindings);
            JsPolyfillInjector.injectAll(context);

            // 执行脚本
            context.eval("js", script);

            // 最后 flush 一次，防止遗漏未输出的内容
            outputStream.flush();

        } catch (PolyglotException e) {
            // GraalVM特定异常
            String errorMsg = formatPolyglotError(e);
            log.error("Script execution error: {}", errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } catch (IOException e) {
            String errorMsg = "IO error during script execution: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during script execution: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        }
    }

    /**
     * 创建输出流，用于捕获脚本输出
     */
    private static OutputStream createOutputStream(OutputCallback outputCallback) {
        return new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) {
                buffer.write(b);
            }

            @Override
            public void flush() throws IOException {
                super.flush();
                if (buffer.size() == 0) {
                    return;
                }
                String chunk = bufferToString(buffer);
                buffer.reset(); // 清空缓冲区
                if (outputCallback != null && !chunk.isEmpty()) {
                    for (String line : chunk.split("\n")) {
                        if (!line.isEmpty()) {
                            outputCallback.onOutput(line);
                        }
                    }
                }
            }
        };
    }

    /**
     * 格式化Polyglot异常信息
     */
    private static String formatPolyglotError(PolyglotException e) {
        if (e.isHostException()) {
            return "Host exception: " + e.getMessage();
        }
        if (e.isGuestException()) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());
            if (e.getSourceLocation() != null) {
                sb.append(" (Line: ").append(e.getSourceLocation().getStartLine()).append(")");
            }
            return sb.toString();
        }
        return e.getMessage();
    }

    /**
     * 注入变量到JS上下文
     */
    private static void injectBindings(Context context, Map<String, Object> bindings) {
        if (bindings != null && !bindings.isEmpty()) {
            for (var entry : bindings.entrySet()) {
                try {
                    context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                    log.trace("Injected binding: {}", entry.getKey());
                } catch (Exception e) {
                    log.warn("Failed to inject binding: {}, error: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    /**
     * 将缓冲区转换为字符串
     */
    private static String bufferToString(ByteArrayOutputStream buffer) {
        return buffer.size() == 0 ? "" : buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * 输出回调接口
     */
    public interface OutputCallback {
        void onOutput(String output);
    }

}