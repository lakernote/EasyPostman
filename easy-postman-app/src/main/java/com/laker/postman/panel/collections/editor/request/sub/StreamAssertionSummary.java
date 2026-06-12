package com.laker.postman.panel.collections.editor.request.sub;

import cn.hutool.core.collection.CollUtil;
import com.laker.postman.script.model.TestResult;

import java.util.List;

final class StreamAssertionSummary {
    private final int passedCount;
    private final int totalCount;

    private StreamAssertionSummary(int passedCount, int totalCount) {
        this.passedCount = passedCount;
        this.totalCount = totalCount;
    }

    static StreamAssertionSummary from(List<TestResult> testResults) {
        if (CollUtil.isEmpty(testResults)) {
            return null;
        }
        int passed = 0;
        for (TestResult testResult : testResults) {
            if (testResult != null && testResult.passed) {
                passed++;
            }
        }
        return new StreamAssertionSummary(passed, testResults.size());
    }

    boolean passed() {
        return passedCount == totalCount;
    }

    int passedCount() {
        return passedCount;
    }

    int totalCount() {
        return totalCount;
    }

    @Override
    public String toString() {
        return passedCount + "/" + totalCount;
    }
}
