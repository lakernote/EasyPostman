package com.laker.postman.performance.plan;

import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.plan.PerformanceOnceOnlyController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceWhileController;
import com.laker.postman.performance.core.plan.PerformanceSimpleController;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class PerformanceCorePlanAdapterTest {

    @Test
    public void shouldPreserveVirtualUserOffsetWhenAdaptingPartitionedPlan() {
        PerformanceTestPlan partitionedPlan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan(
                        "group",
                        new ThreadGroupData(),
                        new CsvDataSetData("users", List.of("user"), List.of(
                                Map.of("user", "u0"),
                                Map.of("user", "u1"),
                                Map.of("user", "u2")
                        )),
                        List.of(),
                        2
                )
        ));

        PerformanceTestPlan appPlan = PerformanceCorePlanAdapter.toExecutablePlan(partitionedPlan);

        PerformanceThreadGroupPlan group = appPlan.getThreadGroups().get(0);
        assertEquals(group.getVirtualUserIndexOffset(), 2);
        assertEquals(group.csvRowForVirtualUser(0).get("user"), "u2");
    }

    @Test
    public void shouldPreserveConditionDataAcrossCoreAndAppAdapters() {
        ConditionData conditionData = new ConditionData();
        conditionData.expression = "{{enabled}} == true";
        PerformancePlanNode appCondition = PerformancePlanNode.builder()
                .name("condition")
                .type(NodeType.CONDITION)
                .conditionData(conditionData)
                .build();

        PerformanceCorePlanNode coreNode = PerformanceCorePlanAdapter.toCoreNode(appCondition);
        PerformancePlanNode appNode = PerformanceCorePlanAdapter.toAppNode(coreNode);

        assertEquals(coreNode.getConditionData().expression, "{{enabled}} == true");
        assertEquals(appNode.getConditionData().expression, "{{enabled}} == true");
    }

    @Test
    public void shouldPreserveWhileDataAcrossCoreAndAppAdapters() {
        WhileData whileData = new WhileData();
        whileData.expression = "{{retryCount}} < 3";
        whileData.intervalMs = 250;
        whileData.timeoutMs = 15000;
        whileData.maxIterations = 20;
        PerformancePlanNode appWhile = PerformancePlanNode.builder()
                .name("while")
                .type(NodeType.WHILE)
                .whileData(whileData)
                .build();

        PerformanceCorePlanNode coreNode = PerformanceCorePlanAdapter.toCoreNode(appWhile);
        PerformancePlanNode appNode = PerformanceCorePlanAdapter.toAppNode(coreNode);

        assertEquals(coreNode.getWhileData().expression, "{{retryCount}} < 3");
        assertEquals(coreNode.getWhileData().intervalMs, 250);
        assertEquals(coreNode.getWhileData().timeoutMs, 15000);
        assertEquals(coreNode.getWhileData().maxIterations, 20);
        assertEquals(appNode.getWhileData().expression, "{{retryCount}} < 3");
        assertEquals(appNode.getWhileData().intervalMs, 250);
        assertEquals(appNode.getWhileData().timeoutMs, 15000);
        assertEquals(appNode.getWhileData().maxIterations, 20);
    }

    @Test
    public void shouldAdaptExecutableSimpleControllerChildren() {
        PerformanceTestPlan corePlan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan(
                        "group",
                        new ThreadGroupData(),
                        List.of(new PerformanceSimpleController("simple", List.of()))
                )
        ));

        PerformanceTestPlan appPlan = PerformanceCorePlanAdapter.toExecutablePlan(corePlan);

        PerformancePlanElement element = appPlan.getThreadGroups().get(0).getElements().get(0);
        PerformanceSimpleController simple = (PerformanceSimpleController) element;
        assertEquals(simple.getName(), "simple");
        assertEquals(simple.getIterationCount(), 1);
    }

    @Test
    public void shouldAdaptExecutableConditionControllerChildren() {
        ConditionData conditionData = new ConditionData();
        conditionData.expression = "{{enabled}} == true";
        PerformanceTestPlan corePlan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan(
                        "group",
                        new ThreadGroupData(),
                        List.of(new PerformanceConditionController("condition", conditionData, List.of()))
                )
        ));

        PerformanceTestPlan appPlan = PerformanceCorePlanAdapter.toExecutablePlan(corePlan);

        PerformancePlanElement element = appPlan.getThreadGroups().get(0).getElements().get(0);
        PerformanceConditionController condition = (PerformanceConditionController) element;
        assertEquals(condition.getConditionData().expression, "{{enabled}} == true");
    }

    @Test
    public void shouldAdaptExecutableWhileControllerChildren() {
        WhileData whileData = new WhileData();
        whileData.expression = "{{retryCount}} < 3";
        whileData.intervalMs = 100;
        PerformanceTestPlan corePlan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan(
                        "group",
                        new ThreadGroupData(),
                        List.of(new PerformanceWhileController("while", whileData, List.of()))
                )
        ));

        PerformanceTestPlan appPlan = PerformanceCorePlanAdapter.toExecutablePlan(corePlan);

        PerformancePlanElement element = appPlan.getThreadGroups().get(0).getElements().get(0);
        PerformanceWhileController whileController = (PerformanceWhileController) element;
        assertEquals(whileController.getWhileData().expression, "{{retryCount}} < 3");
        assertEquals(whileController.getWhileData().intervalMs, 100);
    }

    @Test
    public void shouldAdaptExecutableOnceOnlyControllerChildren() {
        PerformanceTestPlan corePlan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan(
                        "group",
                        new ThreadGroupData(),
                        List.of(new PerformanceOnceOnlyController("once only", List.of()))
                )
        ));

        PerformanceTestPlan appPlan = PerformanceCorePlanAdapter.toExecutablePlan(corePlan);

        PerformancePlanElement element = appPlan.getThreadGroups().get(0).getElements().get(0);
        PerformanceOnceOnlyController onceOnly = (PerformanceOnceOnlyController) element;
        assertEquals(onceOnly.getName(), "once only");
    }
}
