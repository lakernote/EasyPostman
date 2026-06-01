package com.laker.postman.panel.sidebar;

import com.laker.postman.service.js.JsScriptExecutor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConsoleScriptOutputAdapter {

    public static JsScriptExecutor.OutputCallback outputCallback() {
        return new JsScriptExecutor.OutputCallback() {
            @Override
            public void onOutput(String output) {
                ConsolePanel.appendLog(output);
            }

            @Override
            public void onOutput(String output, JsScriptExecutor.ConsoleType consoleType) {
                ConsolePanel.appendLog(output, toConsoleLogType(consoleType));
            }
        };
    }

    private static ConsolePanel.LogType toConsoleLogType(JsScriptExecutor.ConsoleType consoleType) {
        return switch (consoleType) {
            case ERROR -> ConsolePanel.LogType.ERROR;
            case WARN -> ConsolePanel.LogType.WARN;
            case INFO -> ConsolePanel.LogType.INFO;
            case DEBUG -> ConsolePanel.LogType.DEBUG;
            case LOG -> ConsolePanel.LogType.INFO;
        };
    }
}
