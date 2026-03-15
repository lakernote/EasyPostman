package com.laker.postman.ioc;

/**
 * Bean初始化接口
 * 实现此接口的Bean会在属性注入完成后自动调用afterPropertiesSet方法
 * 这是@PostConstruct注解的接口方式替代
 */
public interface InitializingBean {
    
    /**
     * Bean的所有属性被设置后调用
     * 可以用于初始化资源、验证配置等
     * 
     * @throws Exception 如果初始化失败
     */
    void afterPropertiesSet() throws Exception;
}
