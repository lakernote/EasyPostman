package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.variable.VariableType;

record RequestVariableUsage(String name, String value, VariableType type) {
    boolean defined() {
        return type != null && value != null;
    }
}
