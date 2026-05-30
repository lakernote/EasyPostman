package com.laker.postman.startup;

import com.laker.postman.common.constants.AppConstants;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import com.laker.postman.plugin.host.AppRequestCollectionImportService;
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
        BeanFactory.init(AppConstants.BASE_PACKAGE);
        PluginRuntime.getRegistry().registerService(
                RequestCollectionImportService.class,
                new AppRequestCollectionImportService()
        );
        PluginRuntime.initialize();
    }
}
