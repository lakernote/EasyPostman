package com.laker.postman.service.curl;

import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
class CurlMultipartBodyParser {

    private static final String CONTENT_TYPE = "Content-Type";

    static void parseInto(CurlRequest req, String data) {
        try {
            String contentType = null;
            if (req.headersList != null) {
                for (HttpHeader h : req.headersList) {
                    if (h.isEnabled() && CONTENT_TYPE.equalsIgnoreCase(h.getKey())) {
                        contentType = h.getValue();
                        break;
                    }
                }
            }
            if (contentType == null || !contentType.contains("boundary=")) {
                throw new IllegalArgumentException("Content-Type missing or invalid");
            }
            String boundary = "--" + contentType.split("boundary=")[1];

            String[] parts = data.split(Pattern.quote(boundary));
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || part.equals("--")) {
                    continue;
                }

                Matcher dispositionMatcher = Pattern.compile("Content-Disposition: form-data;\\s*(.+)").matcher(part);
                if (!dispositionMatcher.find()) {
                    continue;
                }
                String disposition = dispositionMatcher.group(1);

                Matcher nameMatcher = Pattern.compile("name=\"([^\"]+)\"").matcher(disposition);
                if (!nameMatcher.find()) {
                    continue;
                }
                String fieldName = nameMatcher.group(1);

                Matcher fileNameMatcher = Pattern.compile("filename=\"([^\"]+)\"").matcher(disposition);
                if (req.formDataList == null) {
                    req.formDataList = new ArrayList<>();
                }
                if (fileNameMatcher.find()) {
                    String fileName = fileNameMatcher.group(1);
                    req.formDataList.add(new HttpFormData(true, fieldName, HttpFormData.TYPE_FILE, fileName));
                } else {
                    int valueStart = part.indexOf("\r\n\r\n") + 4;
                    String value = part.substring(valueStart).trim();
                    req.formDataList.add(new HttpFormData(true, fieldName, HttpFormData.TYPE_TEXT, value));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse multipart/form-data body", e);
        }
    }
}
