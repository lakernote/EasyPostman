package com.laker.postman.ioc.behavior;

import com.laker.postman.ioc.Component;

@Component
public class ConstructorCycleB {
    public ConstructorCycleB(ConstructorCycleA constructorCycleA) {
    }
}
