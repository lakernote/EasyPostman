package com.laker.postman.service.curl;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.TransportAuth;
import lombok.experimental.UtilityClass;

@UtilityClass
class CurlRequestExporter {

    static String toCurl(PreparedRequest preparedRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("curl");
        if (preparedRequest.method != null && !"GET".equalsIgnoreCase(preparedRequest.method)) {
            sb.append(" -X ").append(preparedRequest.method.toUpperCase());
        }

        TransportAuth auth = preparedRequest.transportAuth;
        if (auth != null && auth.isDigest() && !isBlank(auth.username)) {
            sb.append(" --digest");
            sb.append(" --user ").append(ShellArgumentEscaper.escape(auth.username + ":" + (auth.password == null ? "" : auth.password)));
        }

        if (preparedRequest.url != null) {
            sb.append(" ").append(ShellArgumentEscaper.escape(preparedRequest.url));
        }
        if (preparedRequest.headersList != null) {
            for (var header : preparedRequest.headersList) {
                if (header.isEnabled()) {
                    sb.append(" -H ").append(ShellArgumentEscaper.escape(header.getKey() + ": " + header.getValue()));
                }
            }
        }
        if (preparedRequest.body != null && !preparedRequest.body.isEmpty()) {
            sb.append(" --data ").append(ShellArgumentEscaper.escape(preparedRequest.body));
        }
        if (preparedRequest.urlencodedList != null && !preparedRequest.urlencodedList.isEmpty()) {
            for (var encoded : preparedRequest.urlencodedList) {
                if (encoded.isEnabled()) {
                    sb.append(" --data-urlencode ").append(ShellArgumentEscaper.escape(encoded.getKey() + "=" + encoded.getValue()));
                }
            }
        }
        if (preparedRequest.formDataList != null && !preparedRequest.formDataList.isEmpty()) {
            for (var formData : preparedRequest.formDataList) {
                if (formData.isEnabled()) {
                    if (formData.isText()) {
                        sb.append(" -F ").append(ShellArgumentEscaper.escape(formData.getKey() + "=" + formData.getValue()));
                    } else if (formData.isFile()) {
                        sb.append(" -F ").append(ShellArgumentEscaper.escape(formData.getKey() + "=@" + formData.getValue()));
                    }
                }
            }
        }
        return sb.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
