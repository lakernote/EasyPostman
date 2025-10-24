package com.laker.postman.ioc.test;

import com.laker.postman.ioc.Autowired;
import com.laker.postman.ioc.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 测试循环依赖 - ServiceB
 */
@Component
@Getter
@Setter
@ToString(exclude = "serviceA") // 排除循环引用字段，避免 toString 栈溢出
public class ServiceB {

    @Autowired
    private ServiceA serviceA;

    public void doSomething() {
        System.out.println("ServiceB doing something, has ServiceA: " + (serviceA != null));
    }
}

