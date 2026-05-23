# Performance JMeter Plan Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Compile the performance Swing tree into immutable JMeter-style plan objects and execute it through separated thread-group, controller, timer, and sampler runtime classes.

**Architecture:** Keep `PerformanceExecutionEngine` as the facade, but move plan compilation into `plan`, thread-group scheduling into `PerformanceThreadGroupRunner`, iteration context creation into `PerformanceIterationContextFactory`, and controller/sampler walking into `PerformancePlanExecutor` plus `PerformanceSamplerExecutor`.

**Tech Stack:** Java 17, Swing `DefaultMutableTreeNode` compatibility boundary, TestNG, Maven.

---

### Task 1: Immutable Plan Model And Compiler

**Files:**
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceTestPlan.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceThreadGroupPlan.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformancePlanElement.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceLoopController.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceTimerElement.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceRequestSampler.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceProtocolStageElement.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan/PerformanceTestPlanCompiler.java`
- Test: `easy-postman-app/src/test/java/com/laker/postman/panel/performance/plan/PerformanceTestPlanCompilerTest.java`

- [ ] Write compiler tests for enabled thread groups, disabled nodes, nested loop/timer/request elements, and protocol stage preservation.
- [ ] Implement immutable records and compiler.
- [ ] Run `mvn -q -pl easy-postman-app -am -Dtest=PerformanceTestPlanCompilerTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test`.

### Task 2: Controller And Sampler Runtime

**Files:**
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformanceIterationContextFactory.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePlanExecutor.java`
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformanceSamplerExecutor.java`
- Modify: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformanceVirtualUserCoordinator.java`
- Test: `easy-postman-app/src/test/java/com/laker/postman/panel/performance/PerformancePlanExecutorTest.java`

- [ ] Write tests proving loop controllers execute nested request samplers, timers sleep through an injectable sleeper, disabled runtime elements are not present after compile, and CSV rows are selected by virtual user index.
- [ ] Implement iteration context factory, plan executor, sampler executor, and minimal virtual-user accessors needed by the factory.
- [ ] Run `mvn -q -pl easy-postman-app -am -Dtest=PerformancePlanExecutorTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test`.

### Task 3: Thread Group Runner Extraction

**Files:**
- Create: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformanceThreadGroupRunner.java`
- Modify: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformanceExecutionEngine.java`
- Test: `easy-postman-app/src/test/java/com/laker/postman/panel/performance/PerformanceThreadGroupRunnerTest.java`
- Modify: `easy-postman-app/src/test/java/com/laker/postman/panel/performance/PerformanceExecutionEngineTest.java`

- [ ] Move FIXED/RAMP_UP/SPIKE/STAIRS scheduling out of `PerformanceExecutionEngine`.
- [ ] Keep `PerformanceExecutionEngine.runJMeterTreeWithProgress(...)` compiling the tree and delegating to the runner.
- [ ] Preserve static helpers currently tested from `PerformanceExecutionEngine` by moving or forwarding them.
- [ ] Run `mvn -q -pl easy-postman-app -am -Dtest=PerformanceThreadGroupRunnerTest,PerformanceExecutionEngineTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test`.

### Task 4: Estimate And Documentation Integration

**Files:**
- Modify: `easy-postman-app/src/main/java/com/laker/postman/panel/performance/threadgroup/PerformanceThreadGroupPlanner.java`
- Modify: `docs/performance-load-test-design.md`
- Test: existing performance test set.

- [ ] Add plan-based request estimation while preserving the tree-based public method.
- [ ] Update performance design docs to describe compiled plan execution.
- [ ] Run the focused performance tests and `mvn -q -pl easy-postman-app -am -DskipTests compile`.

