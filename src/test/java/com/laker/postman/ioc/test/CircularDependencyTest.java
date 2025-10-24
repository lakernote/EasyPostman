package com.laker.postman.ioc.test;

import com.laker.postman.ioc.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;

import static org.testng.Assert.*;

/**
 * 循环依赖测试 - 使用 TestNG
 * 测试三级缓存是否能正确解决循环依赖问题
 */
@Slf4j
public class CircularDependencyTest {

    private ApplicationContext context;

    @BeforeMethod
    public void setUp() {
        log.info("========== 测试初始化 ==========");
        context = ApplicationContext.getInstance();
        context.clear();
    }

    @AfterMethod
    public void tearDown() {
        log.info("========== 测试清理 ==========");
        if (context != null) {
            context.clear();
        }
    }

    @Test(description = "完整的循环依赖解决流程测试")
    public void testCompleteCircularDependencyResolution() {
        log.info("========== 开始测试循环依赖解决（使用三级缓存） ==========");

        // 1. 扫描包，注册Bean
        log.info("1. 扫描并注册Bean...");
        context.scan("com.laker.postman.ioc.test");

        // 2. 获取ServiceA（此时会触发循环依赖）
        log.info("\n2. 获取ServiceA（会触发循环依赖解决）...");
        ServiceA serviceA = context.getBean(ServiceA.class);
        assertNotNull(serviceA, "ServiceA should not be null");
        log.info("✅ 成功获取 ServiceA: {}", serviceA);

        // 3. 获取ServiceB
        log.info("\n3. 获取ServiceB...");
        ServiceB serviceB = context.getBean(ServiceB.class);
        assertNotNull(serviceB, "ServiceB should not be null");
        log.info("✅ 成功获取 ServiceB: {}", serviceB);

        // 4. 验证循环依赖是否正确注入
        log.info("\n4. 验证循环依赖注入...");
        assertNotNull(serviceA.getServiceB(), "ServiceA.serviceB should not be null");
        assertNotNull(serviceB.getServiceA(), "ServiceB.serviceA should not be null");
        log.info("✅ ServiceA.serviceB 不为 null");
        log.info("✅ ServiceB.serviceA 不为 null");

        // 5. 验证是同一个实例（单例）
        log.info("\n5. 验证单例模式...");
        assertSame(serviceA.getServiceB(), serviceB, "ServiceA.serviceB should be the same instance as ServiceB");
        assertSame(serviceB.getServiceA(), serviceA, "ServiceB.serviceA should be the same instance as ServiceA");
        log.info("ServiceA.serviceB == ServiceB: true");
        log.info("ServiceB.serviceA == ServiceA: true");
        log.info("✅ 单例模式验证通过！");

        // 6. 测试功能是否正常
        log.info("\n6. 测试Bean功能...");
        serviceA.doSomething();
        serviceB.doSomething();

        // 7. 测试多次获取
        log.info("\n7. 测试多次获取相同Bean...");
        ServiceA serviceA2 = context.getBean("serviceA");
        ServiceB serviceB2 = context.getBean("serviceB");

        assertSame(serviceA, serviceA2, "Multiple gets should return the same ServiceA instance");
        assertSame(serviceB, serviceB2, "Multiple gets should return the same ServiceB instance");
        log.info("多次获取是否返回同一实例: true");
        log.info("✅ 多次获取测试通过！");

        log.info("\n========== 🎉 循环依赖测试全部通过！三级缓存工作正常！ ==========");
        log.info("\n总结：");
        log.info("✅ 成功解决了 ServiceA 和 ServiceB 之间的循环依赖");
        log.info("✅ 单例Bean只创建了一个实例");
        log.info("✅ 依赖注入正确完成");
        log.info("✅ 三级缓存机制正常工作");
    }

    @Test(description = "测试Bean注入不为null", priority = 1)
    public void testBeanInjectionNotNull() {
        log.info("========== 测试Bean注入不为null ==========");

        context.scan("com.laker.postman.ioc.test");

        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        assertNotNull(serviceA.getServiceB(), "ServiceA's serviceB field should be injected");
        assertNotNull(serviceB.getServiceA(), "ServiceB's serviceA field should be injected");

        log.info("✅ 所有依赖注入验证通过！");
    }

    @Test(description = "测试单例一致性", priority = 2)
    public void testSingletonConsistency() {
        log.info("========== 测试单例一致性 ==========");

        context.scan("com.laker.postman.ioc.test");

        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        // 验证循环引用的一致性
        assertSame(serviceA.getServiceB(), serviceB, "ServiceA's serviceB should reference the same ServiceB instance");
        assertSame(serviceB.getServiceA(), serviceA, "ServiceB's serviceA should reference the same ServiceA instance");

        // 验证深层引用
        assertSame(serviceA, serviceA.getServiceB().getServiceA(), "Deep circular reference should work");
        assertSame(serviceB, serviceB.getServiceA().getServiceB(), "Deep circular reference should work");

        log.info("✅ 单例一致性验证通过！");
    }

    @Test(description = "测试Bean功能正常", priority = 3)
    public void testBeanFunctionality() {
        log.info("========== 测试Bean功能正常 ==========");

        context.scan("com.laker.postman.ioc.test");

        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        // 调用方法，不应该抛出异常
        try {
            serviceA.doSomething();
            serviceB.doSomething();
            log.info("✅ Bean功能测试通过！");
        } catch (Exception e) {
            fail("Bean methods should not throw exception: " + e.getMessage());
        }
    }
}

