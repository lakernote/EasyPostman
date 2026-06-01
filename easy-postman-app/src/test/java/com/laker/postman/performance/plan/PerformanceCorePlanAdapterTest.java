package com.laker.postman.performance.plan;

import com.laker.postman.performance.core.config.CsvDataSetData;
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
}
