package com.laker.postman.startup;

import com.laker.postman.common.constants.AppConstants;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import com.laker.postman.plugin.host.AppRequestCollectionImportService;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;

/**
 * GUI 启动分支的宿主运行时初始化。
 */
@UtilityClass
class GuiStartupBootstrap {

    void initBeanFactory() {
        BeanFactory.init(AppConstants.BASE_PACKAGE);
    }

    void initPluginRuntime() {
        // 插件加载期间可能查询宿主桥接服务，因此必须先注册宿主能力再初始化插件运行时。
        PluginRuntime.getRegistry().registerService(
                RequestCollectionImportService.class,
                new AppRequestCollectionImportService()
        );
        PluginRuntime.initialize();
    }
}
