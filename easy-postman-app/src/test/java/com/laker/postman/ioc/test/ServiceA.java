package com.laker.postman.ioc.test;

import com.laker.postman.ioc.Autowired;
import com.laker.postman.ioc.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 测试循环依赖 - ServiceA
 */
@Component
@Getter
@Setter
@ToString(exclude = "serviceB") // 排除循环引用字段，避免 toString 栈溢出
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void doSomething() {
        System.out.println("ServiceA doing something, has ServiceB: " + (serviceB != null));
    }
}

