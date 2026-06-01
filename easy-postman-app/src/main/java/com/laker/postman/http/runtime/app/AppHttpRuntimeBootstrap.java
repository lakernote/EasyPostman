package com.laker.postman.http.runtime.app;

import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.ssl.SSLConfigurationUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AppHttpRuntimeBootstrap {

    public static void configure() {
        HttpRuntimeSettingsProvider.set(new AppHttpRuntimeSettingsAdapter());
        SSLConfigurationUtil.setClientCertificateProvider(new AppClientCertificateProvider());
    }
}
