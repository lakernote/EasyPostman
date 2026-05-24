package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.tree.DefaultMutableTreeNode;

final class PerformanceProtocolStageValidator {

    private PerformanceProtocolStageValidator() {
    }

    static ValidationResult validate(DefaultMutableTreeNode requestNode, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET && !hasEnabledDirectChild(requestNode, NodeType.WS_CONNECT)) {
            return ValidationResult.invalid(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_WS_CONNECT_STAGE_REQUIRED));
        }
        if (protocol == PerformanceProtocol.SSE && (!hasEnabledDirectChild(requestNode, NodeType.SSE_CONNECT)
                || !hasEnabledDirectChild(requestNode, NodeType.SSE_AWAIT))) {
            return ValidationResult.invalid(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SSE_STAGE_REQUIRED));
        }
        return ValidationResult.ok();
    }

    static ValidationResult validate(PerformanceRequestSampler requestSampler, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET && !hasEnabledDirectChild(requestSampler, NodeType.WS_CONNECT)) {
            return ValidationResult.invalid(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_WS_CONNECT_STAGE_REQUIRED));
        }
        if (protocol == PerformanceProtocol.SSE && (!hasEnabledDirectChild(requestSampler, NodeType.SSE_CONNECT)
                || !hasEnabledDirectChild(requestSampler, NodeType.SSE_AWAIT))) {
            return ValidationResult.invalid(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SSE_STAGE_REQUIRED));
        }
        return ValidationResult.ok();
    }

    private static boolean hasEnabledDirectChild(DefaultMutableTreeNode parent, NodeType type) {
        if (parent == null || type == null) {
            return false;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof JMeterTreeNode node && node.type == type && node.enabled) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEnabledDirectChild(PerformanceRequestSampler requestSampler, NodeType type) {
        if (requestSampler == null || type == null) {
            return false;
        }
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element.getType() == type) {
                return true;
            }
        }
        return false;
    }

    record ValidationResult(boolean valid, String message) {
        private static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        private static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}
