package com.laker.postman.startup;

import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import com.laker.postman.plugin.host.HeadlessRequestCollectionImportService;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicBoolean;

@UtilityClass
public class HeadlessStartupBootstrap {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void initRuntime() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        // headless 压测只需要插件脚本扩展，不启动 app IOC，避免把工作区/GUI bean 扫描带入 worker。
        PluginRuntime.getRegistry().registerService(
                RequestCollectionImportService.class,
                new HeadlessRequestCollectionImportService()
        );
        PluginRuntime.initialize();
    }
}
