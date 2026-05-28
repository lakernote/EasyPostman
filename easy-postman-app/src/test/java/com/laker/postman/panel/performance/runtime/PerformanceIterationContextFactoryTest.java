package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.runtime.PerformanceVirtualUserCoordinator;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.service.variable.ExecutionVariableContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class PerformanceIterationContextFactoryTest {

    @Test
    public void shouldResolveIterationDataThroughProvider() {
        PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
        PerformanceIterationContextFactory factory = new PerformanceIterationContextFactory(
                (groupPlan, virtualUserIndex) -> Map.of("user", groupPlan.getName() + "-" + virtualUserIndex),
                virtualUsers
        );
        List<ExecutionVariableContext> contexts = new ArrayList<>();
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of()
        );

        virtualUsers.newThread("test-vu", (active, total) -> {
        }, 1, () -> contexts.add(factory.create(groupPlan, 3))).run();

        assertEquals(contexts.size(), 1);
        assertEquals(contexts.get(0).getIterationData().get("user"), "group-0");
    }

    @Test(description = "每个 Thread Group 使用自己的 CSV Data Set，虚拟用户索引按组从 0 开始")
    public void shouldBindCsvRowsByThreadGroupLocalVirtualUser() {
        PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
        PerformanceIterationContextFactory factory = new PerformanceIterationContextFactory(virtualUsers);
        PerformanceThreadGroupPlan firstGroup = groupWithCsv("first", "first-users.csv", "u1", "u2");
        PerformanceThreadGroupPlan secondGroup = groupWithCsv("second", "second-users.csv", "u301", "u302");
        AtomicInteger firstGroupUserIndex = new AtomicInteger();
        AtomicInteger secondGroupUserIndex = new AtomicInteger();
        List<ExecutionVariableContext> firstContexts = new ArrayList<>();
        List<ExecutionVariableContext> secondContexts = new ArrayList<>();

        virtualUsers.newThread("first-vu", (active, total) -> {
        }, 2, firstGroupUserIndex::getAndIncrement, () -> firstContexts.add(factory.create(firstGroup, 1))).run();
        virtualUsers.newThread("first-vu", (active, total) -> {
        }, 2, firstGroupUserIndex::getAndIncrement, () -> firstContexts.add(factory.create(firstGroup, 1))).run();
        virtualUsers.newThread("second-vu", (active, total) -> {
        }, 2, secondGroupUserIndex::getAndIncrement, () -> secondContexts.add(factory.create(secondGroup, 1))).run();

        assertEquals(firstContexts.get(0).getIterationData().get("userId"), "u1");
        assertEquals(firstContexts.get(1).getIterationData().get("userId"), "u2");
        assertEquals(secondContexts.get(0).getIterationData().get("userId"), "u301");
    }

    private static PerformanceThreadGroupPlan groupWithCsv(String name,
                                                           String sourceName,
                                                           String firstUser,
                                                           String secondUser) {
        return new PerformanceThreadGroupPlan(
                name,
                new ThreadGroupData(),
                new CsvDataSetData(
                        sourceName,
                        List.of("userId"),
                        List.of(Map.of("userId", firstUser), Map.of("userId", secondUser))
                ),
                List.of()
        );
    }
}
