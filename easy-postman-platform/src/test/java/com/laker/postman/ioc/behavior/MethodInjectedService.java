package com.laker.postman.ioc.behavior;

import com.laker.postman.ioc.Autowired;
import com.laker.postman.ioc.Component;

@Component
public class MethodInjectedService {
    private MethodInjectedDependency dependency;

    @Autowired
    public void configure(MethodInjectedDependency dependency) {
        this.dependency = dependency;
    }

    public MethodInjectedDependency getDependency() {
        return dependency;
    }
}
