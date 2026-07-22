package com.laker.postman.workspace.cli;

import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;

import java.util.ArrayList;
import java.util.List;

public final class WorkspaceRequestCatalog {
    private WorkspaceRequestCatalog() {
    }

    public static List<WorkspaceRunSelectedRequest> flatten(List<CollectionNode> roots) {
        List<WorkspaceRunSelectedRequest> requests = new ArrayList<>();
        if (roots == null) {
            return requests;
        }
        for (CollectionNode root : roots) {
            collectRequests(root, List.of(), requests);
        }
        return List.copyOf(requests);
    }

    public static List<String> collectionNames(List<WorkspaceRunSelectedRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(WorkspaceRunSelectedRequest::groupChain)
                .filter(chain -> !chain.isEmpty())
                .map(chain -> chain.get(0).getName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    private static void collectRequests(CollectionNode node,
                                        List<RequestGroup> groupChain,
                                        List<WorkspaceRunSelectedRequest> requests) {
        if (node == null) {
            return;
        }
        if (node.isRequest() && node.getRequest() != null) {
            requests.add(new WorkspaceRunSelectedRequest(
                    node.getRequest(),
                    groupChain,
                    buildRequestPath(groupChain, node.getRequest().getName())
            ));
            return;
        }

        List<RequestGroup> childChain = groupChain;
        if (node.isGroup() && node.getGroup() != null) {
            childChain = new ArrayList<>(groupChain);
            childChain.add(node.getGroup());
        }
        for (CollectionNode child : node.getChildren()) {
            collectRequests(child, childChain, requests);
        }
    }

    private static String buildRequestPath(List<RequestGroup> groupChain, String requestName) {
        List<String> segments = new ArrayList<>();
        for (RequestGroup group : groupChain) {
            if (group != null && group.getName() != null && !group.getName().isBlank()) {
                segments.add(group.getName());
            }
        }
        segments.add(requestName == null || requestName.isBlank() ? "Unnamed request" : requestName);
        return String.join(" / ", segments);
    }
}
