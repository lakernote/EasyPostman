package com.laker.postman.collection.cli;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.workspace.cli.WorkspaceRequestCatalog;
import com.laker.postman.workspace.cli.WorkspaceRunPlan;
import com.laker.postman.workspace.cli.WorkspaceRunPlanner;
import com.laker.postman.workspace.cli.WorkspaceRunSelectedRequest;
import com.laker.postman.workspace.cli.WorkspaceRunWorkspace;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CollectionRunPlanner implements WorkspaceRunPlanner {
    private final List<String> collections;
    private final List<String> folders;

    CollectionRunPlanner(List<String> collections, List<String> folders) {
        this.collections = collections == null ? List.of() : List.copyOf(collections);
        this.folders = folders == null ? List.of() : List.copyOf(folders);
    }

    @Override
    public WorkspaceRunPlan plan(WorkspaceRunWorkspace workspace, CollectionDocument document) {
        List<CollectionNode> selectedCollections = selectCollections(document, collections);
        List<WorkspaceRunSelectedRequest> allRequests = WorkspaceRequestCatalog.flatten(selectedCollections);
        if (allRequests.isEmpty()) {
            throw new IllegalArgumentException("No runnable requests found in selected collection(s)");
        }

        List<WorkspaceRunSelectedRequest> requests = selectFolders(allRequests, folders);
        if (!folders.isEmpty() && requests.isEmpty()) {
            throw new IllegalArgumentException("No requests matched folder(s): " + String.join(", ", folders));
        }
        return new WorkspaceRunPlan(
                requests,
                selectedCollections.stream()
                        .map(CollectionNode::asGroup)
                        .map(RequestGroup::getName)
                        .toList(),
                List.of(),
                "COLLECTIONS",
                "<none>"
        );
    }

    private static List<CollectionNode> selectCollections(CollectionDocument document, List<String> selectors) {
        List<CollectionNode> roots = document.getRoots();
        if (selectors == null || selectors.isEmpty()) {
            return roots;
        }

        Set<String> unmatched = new LinkedHashSet<>(selectors);
        List<CollectionNode> selected = new ArrayList<>();
        for (CollectionNode root : roots) {
            RequestGroup group = root.asGroup();
            boolean matches = selectors.stream()
                    .anyMatch(selector -> selector.equals(group.getName()) || selector.equals(group.getId()));
            if (matches) {
                selected.add(root);
                unmatched.remove(group.getName());
                unmatched.remove(group.getId());
            }
        }
        if (!unmatched.isEmpty()) {
            String available = roots.stream()
                    .map(CollectionNode::asGroup)
                    .map(RequestGroup::getName)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("<none>");
            throw new IllegalArgumentException(
                    "Collection not found: " + String.join(", ", unmatched)
                            + ". Available collections: " + available
            );
        }
        return List.copyOf(selected);
    }

    private static List<WorkspaceRunSelectedRequest> selectFolders(
            List<WorkspaceRunSelectedRequest> requests,
            List<String> folders) {
        if (folders == null || folders.isEmpty()) {
            return requests;
        }
        return requests.stream()
                .filter(request -> request.groupChain().stream()
                        .skip(1)
                        .map(RequestGroup::getName)
                        .anyMatch(folders::contains))
                .toList();
    }
}
