package com.laker.postman.service.js;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JS脚本执行器，使用GraalVM的Polyglot API执行JavaScript脚本。
 */
public class JsScriptExecutor {

    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false") // 禁用解释器模式警告
            .build();

    private JsScriptExecutor() {
        // 工具类不应该被实例化
    }

    /**
     * 执行JS脚本，自动注入所有变量、polyfill，并支持输出回调。
     *
     * @param script         脚本内容
     * @param bindings       需要注入的变量（变量名->对象），如request/env/postman/responseBody等
     * @param outputCallback 输出回调（可为null）
     */
    public static void executeScript(String script, Map<String, Object> bindings, OutputCallback outputCallback) {
        OutputStream outputStream = new OutputStream() {
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

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .out(outputStream)
                .err(outputStream)
                .engine(ENGINE)
                .build()) {
            injectBindings(context, bindings);
            JsPolyfillInjector.injectAll(context);
            context.eval("js", script);

            // 最后 flush 一次，防止遗漏未输出的内容
            outputStream.flush();
        } catch (IOException e) {
            if (outputCallback != null) {
                outputCallback.onOutput("Error executing script: " + e.getMessage());
            }
        }
    }

    private static void injectBindings(Context context, Map<String, Object> bindings) {
        if (bindings != null) {
            for (var entry : bindings.entrySet()) {
                context.getBindings("js").putMember(entry.getKey(), entry.getValue());
            }
        }
    }

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