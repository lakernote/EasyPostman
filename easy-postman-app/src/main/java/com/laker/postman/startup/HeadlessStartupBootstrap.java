package com.laker.postman.startup;

import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import com.laker.postman.plugin.host.HeadlessRequestCollectionImportService;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HeadlessStartupBootstrap {

    public void initRuntime() {
        if (PluginRuntime.isInitialized()) {
            return;
        }
        synchronized (HeadlessStartupBootstrap.class) {
            if (PluginRuntime.isInitialized()) {
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
}
