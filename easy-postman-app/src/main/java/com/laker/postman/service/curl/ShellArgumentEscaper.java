package com.laker.postman.service.curl;

import lombok.experimental.UtilityClass;

@UtilityClass
class ShellArgumentEscaper {

    static String escape(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
