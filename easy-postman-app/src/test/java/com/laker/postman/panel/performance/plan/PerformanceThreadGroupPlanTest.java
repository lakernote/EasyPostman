package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.config.CsvDataSetData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class PerformanceThreadGroupPlanTest {

    @Test(description = "运行时应按虚拟用户索引只取对应的 CSV 行副本")
    public void shouldReturnCsvRowForVirtualUserWithoutExposingPlanState() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                new CsvDataSetData(
                        "users.csv",
                        List.of("userId"),
                        List.of(Map.of("userId", "u1"), Map.of("userId", "u2"))
                ),
                List.of()
        );

        Map<String, String> firstRow = groupPlan.csvRowForVirtualUser(0);
        Map<String, String> wrappedRow = groupPlan.csvRowForVirtualUser(2);
        firstRow.put("userId", "mutated");

        assertEquals(firstRow.get("userId"), "mutated");
        assertEquals(wrappedRow.get("userId"), "u1");
        assertEquals(groupPlan.csvRowForVirtualUser(0).get("userId"), "u1");
        assertEquals(groupPlan.csvRowForVirtualUser(1).get("userId"), "u2");
    }
}
