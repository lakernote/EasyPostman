package com.laker.postman.ioc;

/**
 * Bean销毁接口
 * 实现此接口的Bean会在容器关闭时自动调用destroy方法
 * 这是@PreDestroy注解的接口方式替代
 */
public interface DisposableBean {

    /**
     * Bean销毁前调用
     * 可以用于释放资源、关闭连接等清理工作
     *
     * @throws Exception 如果销毁过程中出现错误
     */
    void destroy() throws Exception;
}

