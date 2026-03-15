package com.laker.postman.ioc;

/**
 * 对象工厂接口
 * 用于延迟创建对象，支持循环依赖解决
 */
@FunctionalInterface
public interface ObjectFactory<T> {

    /**
     * 获取对象实例
     *
     * @return 对象实例
     * @throws Exception 如果获取失败
     */
    T getObject() throws Exception;
}

