package com.laker.postman.plugin.manager.market;

import java.net.HttpURLConnection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 控制插件安装任务的取消状态，并在取消时主动断开下载连接。
 */
public class PluginInstallController {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile HttpURLConnection activeConnection;

    public void cancel() {
        cancelled.set(true);
        HttpURLConnection connection = activeConnection;
        if (connection != null) {
            connection.disconnect();
        }
    }

    public boolean isCancelled() {
        return cancelled.get() || Thread.currentThread().isInterrupted();
    }

    void attachConnection(HttpURLConnection connection) {
        activeConnection = connection;
        checkCancelled();
    }

    void detachConnection(HttpURLConnection connection) {
        if (activeConnection == connection) {
            activeConnection = null;
        }
    }

    void checkCancelled() {
        if (isCancelled()) {
            throw new CancellationException("Plugin installation cancelled");
        }
    }
}
