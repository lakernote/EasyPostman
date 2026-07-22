package com.laker.postman.workspace.cli;

import com.laker.postman.collection.model.CollectionDocument;

@FunctionalInterface
public interface WorkspaceRunPlanner {
    WorkspaceRunPlan plan(WorkspaceRunWorkspace workspace, CollectionDocument document) throws Exception;
}
