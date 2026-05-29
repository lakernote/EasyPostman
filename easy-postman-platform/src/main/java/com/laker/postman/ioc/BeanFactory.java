package com.laker.postman.ioc;

import lombok.extern.slf4j.Slf4j;

/**
 * IOC容器工具类，提供便捷的访问方法
 */
@Slf4j
public class BeanFactory {

    private static final ApplicationContext context = ApplicationContext.getInstance();

    /**
     * 初始化IOC容器，扫描指定包
     */
    public static void init(String... basePackages) {
        context.scan(basePackages);
    }

    /**
     * 根据名称获取Bean
     */
    public static <T> T getBean(String beanName) {
        return context.getBean(beanName);
    }

    /**
     * 根据类型获取Bean
     */
    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    /**
     * 销毁所有Bean，调用@PreDestroy方法
     * 建议在应用程序关闭时调用此方法
     */
    public static void destroy() {
        log.info("Destroying IOC container...");
        context.destroy();
    }
}
