package com.laker.postman.workspace.cli;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;

import java.util.List;

public record WorkspaceRunSelectedRequest(
        HttpRequestItem request,
        List<RequestGroup> groupChain,
        String path
) {
    public WorkspaceRunSelectedRequest {
        groupChain = groupChain == null ? List.of() : List.copyOf(groupChain);
    }
}
