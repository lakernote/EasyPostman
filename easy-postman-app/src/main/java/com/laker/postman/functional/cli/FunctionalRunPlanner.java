package com.laker.postman.functional.cli;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.workspace.cli.WorkspaceRequestCatalog;
import com.laker.postman.workspace.cli.WorkspaceRunPlan;
import com.laker.postman.workspace.cli.WorkspaceRunPlanner;
import com.laker.postman.workspace.cli.WorkspaceRunSelectedRequest;
import com.laker.postman.workspace.cli.WorkspaceRunWorkspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FunctionalRunPlanner implements WorkspaceRunPlanner {

    @Override
    public WorkspaceRunPlan plan(WorkspaceRunWorkspace workspace, CollectionDocument document) {
        FunctionalRunConfig config = FunctionalRunConfigLoader.load(
                workspace.directory().resolve("functional_config.json")
        );
        List<WorkspaceRunSelectedRequest> allRequests = WorkspaceRequestCatalog.flatten(document.getRoots());
        List<WorkspaceRunSelectedRequest> requests = selectRequests(allRequests, config.requestIds());
        return new WorkspaceRunPlan(
                requests,
                WorkspaceRequestCatalog.collectionNames(requests),
                config.iterationData(),
                "FUNCTIONAL_CONFIG",
                iterationDataSource(config)
        );
    }

    private static List<WorkspaceRunSelectedRequest> selectRequests(
            List<WorkspaceRunSelectedRequest> allRequests,
            List<String> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            throw new IllegalArgumentException("functional_config.json contains no selected requests");
        }
        Map<String, WorkspaceRunSelectedRequest> requestsById = new LinkedHashMap<>();
        for (WorkspaceRunSelectedRequest request : allRequests) {
            if (request.request().getId() != null && !request.request().getId().isBlank()) {
                requestsById.putIfAbsent(request.request().getId(), request);
            }
        }

        List<WorkspaceRunSelectedRequest> selected = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String requestId : requestIds) {
            WorkspaceRunSelectedRequest request = requestsById.get(requestId);
            if (request == null) {
                missing.add(requestId);
            } else {
                selected.add(request);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "functional_config.json references missing request ID(s): " + String.join(", ", missing)
            );
        }
        return List.copyOf(selected);
    }

    private static String iterationDataSource(FunctionalRunConfig config) {
        if (config.iterationData().isEmpty()) {
            return "<none>";
        }
        return config.iterationDataSource().isBlank()
                ? "functional_config.json"
                : "functional_config.json (" + config.iterationDataSource() + ")";
    }
}
