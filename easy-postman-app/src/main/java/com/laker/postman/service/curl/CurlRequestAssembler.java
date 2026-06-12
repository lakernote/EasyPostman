package com.laker.postman.service.curl;

import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
class CurlRequestAssembler {

    private static final String CONTENT_TYPE = "Content-Type";

    static CurlRequest assemble(CurlCommandOptions options) {
        CurlRequest req = new CurlRequest();
        req.url = options.url;
        req.method = options.method;
        req.followRedirects = options.followRedirects;
        req.authType = options.authType;
        req.authUsername = options.authUsername;
        req.authPassword = options.authPassword;
        if (!options.warnings.isEmpty()) {
            req.warnings = new ArrayList<>(options.warnings);
        }

        if (!options.headersList.isEmpty()) {
            req.headersList = new ArrayList<>(options.headersList);
        }
        if (options.hasFormData()) {
            req.formDataList = new ArrayList<>(options.formDataList);
            if (req.method == null) {
                req.method = "POST";
            }
            if (!CurlHeaderSupport.hasEnabledHeader(req.headersList, CONTENT_TYPE)) {
                if (req.headersList == null) {
                    req.headersList = new ArrayList<>();
                }
                req.headersList.add(new HttpHeader(true, CONTENT_TYPE, "multipart/form-data"));
            }
        }
        if (options.hasDataParams() && req.method == null && !options.forceGet) {
            req.method = "POST";
        }

        CurlDataMapper.apply(req, options);

        if (req.method == null) {
            req.method = "GET";
        }
        CurlUrlSupport.addQueryParams(req);
        CurlHeaderSupport.filterRestrictedWebSocketHeaders(req);
        return req;
    }
}
