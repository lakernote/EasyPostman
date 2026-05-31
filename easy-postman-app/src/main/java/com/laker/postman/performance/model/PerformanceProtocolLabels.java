package com.laker.postman.performance.model;

import com.laker.postman.performance.core.model.PerformanceProtocol;


import com.laker.postman.util.I18nUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceProtocolLabels {
    public String displayName(PerformanceProtocol protocol) {
        return I18nUtil.getMessage(protocol.getMessageKey());
    }
}
