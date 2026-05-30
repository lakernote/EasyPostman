package com.laker.postman.panel.performance.plan;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Value
public class PerformancePlanWorkspace {
    String activePlanId;
    List<PerformanceSavedPlan> plans;

    @Builder
    public PerformancePlanWorkspace(String activePlanId, List<PerformanceSavedPlan> plans) {
        List<PerformanceSavedPlan> safePlans = new ArrayList<>();
        if (plans != null) {
            for (PerformanceSavedPlan plan : plans) {
                if (plan != null) {
                    safePlans.add(plan);
                }
            }
        }
        PerformanceSavedPlan activePlan = findPlan(safePlans, activePlanId);
        if (activePlan == null && !safePlans.isEmpty()) {
            activePlan = safePlans.get(0);
        }
        this.activePlanId = activePlan == null ? null : activePlan.getId();
        this.plans = Collections.unmodifiableList(safePlans);
    }

    public PerformanceSavedPlan getActivePlan() {
        PerformanceSavedPlan activePlan = findPlan(plans, activePlanId);
        if (activePlan != null) {
            return activePlan;
        }
        return plans.isEmpty() ? null : plans.get(0);
    }

    public PerformancePlanConfiguration getActiveConfiguration() {
        PerformanceSavedPlan activePlan = getActivePlan();
        return activePlan == null ? null : activePlan.toConfiguration();
    }

    public PerformancePlanWorkspace withActivePlanId(String newActivePlanId) {
        return PerformancePlanWorkspace.builder()
                .activePlanId(newActivePlanId)
                .plans(plans)
                .build();
    }

    public PerformancePlanWorkspace updateActiveConfiguration(PerformancePlanConfiguration configuration,
                                                              String fallbackPlanName) {
        PerformanceSavedPlan activePlan = getActivePlan();
        String targetId = activePlan == null ? UUID.randomUUID().toString() : activePlan.getId();
        String targetName = activePlan == null ? fallbackPlanName : activePlan.getName();
        PerformanceSavedPlan updatedPlan = PerformanceSavedPlan.fromConfiguration(targetId, targetName, configuration);
        List<PerformanceSavedPlan> updatedPlans = new ArrayList<>();
        boolean replaced = false;
        for (PerformanceSavedPlan plan : plans) {
            if (targetId.equals(plan.getId())) {
                updatedPlans.add(updatedPlan);
                replaced = true;
            } else {
                updatedPlans.add(plan);
            }
        }
        if (!replaced) {
            updatedPlans.add(updatedPlan);
        }
        return PerformancePlanWorkspace.builder()
                .activePlanId(targetId)
                .plans(updatedPlans)
                .build();
    }

    private static PerformanceSavedPlan findPlan(List<PerformanceSavedPlan> plans, String planId) {
        if (planId == null || plans == null) {
            return null;
        }
        for (PerformanceSavedPlan plan : plans) {
            if (planId.equals(plan.getId())) {
                return plan;
            }
        }
        return null;
    }
}
