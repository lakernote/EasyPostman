package com.laker.postman.plugin.api.service;

import com.laker.postman.model.RequestImportDraft;
import com.laker.postman.model.RequestImportResult;

import java.util.List;

public interface RequestCollectionImportService {

    RequestImportResult importRequests(List<RequestImportDraft> requests);
}
