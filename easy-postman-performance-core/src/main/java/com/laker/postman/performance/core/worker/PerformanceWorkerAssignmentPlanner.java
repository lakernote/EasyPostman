package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.plan.PerformanceCorePlanDocumentCompiler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;

import java.util.ArrayList;
import java.util.List;

public class PerformanceWorkerAssignmentPlanner {

    public List<PerformanceWorkerAssignment> plan(PerformanceRunPlan runPlan,
                                                  List<PerformanceWorkerEndpoint> endpoints,
                                                  String runId) {
        // 按虚拟用户切片，而不是把完整线程组复制到每台 worker；这样总并发量与 GUI 单机语义一致。
        List<PerformanceWorkerEndpoint> safeEndpoints = endpoints == null ? List.of() : endpoints;
        PerformanceTestPlan testPlan = PerformanceCorePlanDocumentCompiler.compile(
                runPlan == null ? null : runPlan.getTestPlan()
        );
        List<PerformanceWorkerAssignment> assignments = new ArrayList<>();
        for (int workerIndex = 0; workerIndex < safeEndpoints.size(); workerIndex++) {
            List<PerformanceWorkerThreadGroupAssignment> threadGroups = new ArrayList<>();
            for (int groupIndex = 0; groupIndex < testPlan.getThreadGroups().size(); groupIndex++) {
                PerformanceThreadGroupPlan groupPlan = testPlan.getThreadGroups().get(groupIndex);
                int totalUsers = maxThreadCount(groupPlan.getThreadGroupData());
                int first = firstVirtualUserIndex(totalUsers, safeEndpoints.size(), workerIndex);
                int count = virtualUserCount(totalUsers, safeEndpoints.size(), workerIndex);
                if (count > 0) {
                    threadGroups.add(new PerformanceWorkerThreadGroupAssignment(
                            String.valueOf(groupIndex),
                            groupIndex,
                            first,
                            count
                    ));
                }
            }
            assignments.add(PerformanceWorkerAssignment.builder()
                    .runId(runId)
                    .workerId("worker-" + (workerIndex + 1))
                    .assignmentId(runId + "-assignment-" + (workerIndex + 1))
                    .endpoint(safeEndpoints.get(workerIndex))
                    .threadGroups(threadGroups)
                    .build());
        }
        return assignments;
    }

    static int maxThreadCount(ThreadGroupData tg) {
        ThreadGroupData safeTg = tg == null ? new ThreadGroupData() : tg;
        safeTg.normalize();
        return switch (safeTg.threadMode) {
            case FIXED -> safeTg.numThreads;
            case RAMP_UP -> safeTg.rampUpEndThreads;
            case SPIKE -> safeTg.spikeMaxThreads;
            case STAIRS -> safeTg.stairsEndThreads;
        };
    }

    private static int virtualUserCount(int totalUsers, int workerCount, int workerIndex) {
        if (totalUsers <= 0 || workerCount <= 0 || workerIndex < 0 || workerIndex >= workerCount) {
            return 0;
        }
        int base = totalUsers / workerCount;
        int remainder = totalUsers % workerCount;
        return base + (workerIndex < remainder ? 1 : 0);
    }

    private static int firstVirtualUserIndex(int totalUsers, int workerCount, int workerIndex) {
        int first = 0;
        for (int i = 0; i < workerIndex; i++) {
            first += virtualUserCount(totalUsers, workerCount, i);
        }
        return first;
    }
}
