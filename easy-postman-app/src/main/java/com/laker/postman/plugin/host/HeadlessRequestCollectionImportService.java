package com.laker.postman.plugin.host;

import com.laker.postman.model.RequestImportDraft;
import com.laker.postman.model.RequestImportResult;
import com.laker.postman.plugin.api.service.RequestCollectionImportService;

import java.util.List;

public class HeadlessRequestCollectionImportService implements RequestCollectionImportService {

    @Override
    public RequestImportResult importRequests(List<RequestImportDraft> requests) {
        return RequestImportResult.unavailable();
    }
}
