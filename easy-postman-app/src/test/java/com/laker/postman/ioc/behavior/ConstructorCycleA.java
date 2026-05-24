package com.laker.postman.ioc.behavior;

import com.laker.postman.ioc.Component;

@Component
public class ConstructorCycleA {
    public ConstructorCycleA(ConstructorCycleB constructorCycleB) {
    }
}
