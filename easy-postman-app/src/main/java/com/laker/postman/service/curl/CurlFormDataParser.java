package com.laker.postman.service.curl;

import com.laker.postman.request.model.HttpFormData;
import lombok.experimental.UtilityClass;

@UtilityClass
class CurlFormDataParser {

    static HttpFormData parseOption(String formField) {
        if (formField == null) {
            return null;
        }
        int eqIdx = formField.indexOf('=');
        if (eqIdx <= 0) {
            return null;
        }

        String key = formField.substring(0, eqIdx);
        String value = formField.substring(eqIdx + 1);
        if (value.startsWith("@")) {
            return new HttpFormData(true, key, HttpFormData.TYPE_FILE, value.substring(1));
        }
        return new HttpFormData(true, key, HttpFormData.TYPE_TEXT, value);
    }
}
