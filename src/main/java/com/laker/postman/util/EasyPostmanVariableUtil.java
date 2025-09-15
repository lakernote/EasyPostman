package com.laker.postman.util;

import com.laker.postman.model.Environment;
import com.laker.postman.model.VariableSegment;
import com.laker.postman.service.EnvironmentService;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasyPostmanVariableUtil {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    public static List<VariableSegment> getVariableSegments(String value) {
        List<VariableSegment> segments = new ArrayList<>();
        if (value == null) return segments;
        Matcher m = VARIABLE_PATTERN.matcher(value);
        while (m.find()) {
            segments.add(new VariableSegment(m.start(), m.end(), m.group(1)));
        }
        return segments;
    }

    public static boolean isVariableDefined(String varName) {
        if (varName == null) return false;
        if (EnvironmentService.getTemporaryVariable(varName) != null) {
            return true;
        }
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        return activeEnv != null && activeEnv.getVariable(varName) != null;
    }

    public static String getVariableValue(String varName) {
        if (varName == null) return null;
        Object temp = EnvironmentService.getTemporaryVariable(varName);
        if (temp != null) return temp.toString();
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariable(varName) != null) {
            Object v = activeEnv.getVariable(varName);
            return v == null ? null : v.toString();
        }
        return null;
    }
}