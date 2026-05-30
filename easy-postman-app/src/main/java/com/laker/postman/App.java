package com.laker.postman;

import com.laker.postman.startup.AppLauncher;

/**
 * 应用主入口。
 */
public final class App {

    public static void main(String[] args) {
        int exitCode = AppLauncher.launch(args);
        if (exitCode != AppLauncher.GUI_STARTED) {
            System.exit(exitCode);
        }
    }
}
