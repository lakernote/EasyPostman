package com.laker.postman.service.js;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.EnvironmentService;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

/**
 * 脚本执行服务
 * 提供统一的脚本执行入口，处理日志记录、异常处理、环境变量保存等
 */
@Slf4j
@UtilityClass
public class ScriptExecutionService {

    /**
     * 执行前置脚本
     *
     * @param prescript 前置脚本内容
     * @param bindings  变量绑定
     * @return 是否执行成功（失败时会弹窗提示）
     */
    public static boolean executePreScript(String prescript, Map<String, Object> bindings) {
        if (CharSequenceUtil.isBlank(prescript)) {
            return true;
        }

        try {
            ScriptExecutionContext context = ScriptExecutionContext.builder()
                    .script(prescript)
                    .scriptType(ScriptExecutionContext.ScriptType.PRE_REQUEST)
                    .bindings(bindings)
                    .outputCallback(output -> {
                        if (!output.isBlank()) {
                            ConsolePanel.appendLog("[PreScript Console]\n" + output);
                        }
                    })
                    .showErrorDialog(true)
                    .build();

            JsScriptExecutor.executeScript(context);
            return true;

        } catch (ScriptExecutionException ex) {
            log.error("PreScript execution failed", ex);
            ConsolePanel.appendLog("[PreScript Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);

            // 显示错误对话框
            String errorMsg = formatErrorMessage(ex);
            JOptionPane.showMessageDialog(
                    null,
                    errorMsg,
                    "前置脚本执行失败",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    /**
     * 执行后置脚本
     *
     * @param postscript 后置脚本内容
     * @param bindings   变量绑定
     */
    public static void executePostScript(String postscript, Map<String, Object> bindings) {
        if (CharSequenceUtil.isBlank(postscript)) {
            return;
        }

        try {
            ScriptExecutionContext context = ScriptExecutionContext.builder()
                    .script(postscript)
                    .scriptType(ScriptExecutionContext.ScriptType.POST_REQUEST)
                    .bindings(bindings)
                    .outputCallback(output -> {
                        if (!output.isBlank()) {
                            ConsolePanel.appendLog("[PostScript Console]\n" + output);
                        }
                    })
                    .showErrorDialog(false)
                    .build();

            JsScriptExecutor.executeScript(context);

            // 保存环境变量（如果脚本中修改了环境变量）
            saveEnvironmentIfNeeded();

        } catch (ScriptExecutionException ex) {
            log.error("PostScript execution failed", ex);
            ConsolePanel.appendLog("[PostScript Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);
        }
    }

    /**
     * 保存环境变量（如果有激活的环境）
     */
    private static void saveEnvironmentIfNeeded() {
        try {
            Environment activeEnv = EnvironmentService.getActiveEnvironment();
            if (activeEnv != null) {
                EnvironmentService.saveEnvironment(activeEnv);
                log.debug("Environment saved after script execution");
            }
        } catch (Exception e) {
            log.warn("Failed to save environment after script execution", e);
        }
    }

    /**
     * 格式化错误信息
     */
    private static String formatErrorMessage(ScriptExecutionException ex) {
        String message = Objects.requireNonNullElse(
                ex.getCause() != null ? ex.getCause().getMessage() : null,
                ex.getMessage()
        );
        return "脚本执行失败：\n\n" + message;
    }
}

