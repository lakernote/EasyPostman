package com.laker.postman.panel.performance.tree;

import com.laker.postman.performance.core.model.NodeType;


import com.laker.postman.panel.performance.PerformanceTreeRules;
import com.laker.postman.panel.performance.PerformanceTreeSnapshot;
import com.laker.postman.panel.performance.model.PerformanceTreeNode;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class PerformanceTreeClipboardSupport {

    private final DefaultTreeModel treeModel;
    private final PerformanceTreeStructureSupport structureSupport;

    public boolean hasCopyableNodes(TreePath[] selectedPaths) {
        return !copyableTopLevelNodes(selectedPaths).isEmpty();
    }

    public boolean hasDeletableNodes(TreePath[] selectedPaths) {
        return !deletableTopLevelNodes(selectedPaths).isEmpty();
    }

    public List<DefaultMutableTreeNode> copyNodes(TreePath[] selectedPaths) {
        List<DefaultMutableTreeNode> nodes = copyableTopLevelNodes(selectedPaths);
        List<DefaultMutableTreeNode> copies = new ArrayList<>(nodes.size());
        for (DefaultMutableTreeNode node : nodes) {
            copies.add(copyTreeNode(node));
        }
        return copies;
    }

    public List<DefaultMutableTreeNode> deleteNodes(TreePath[] selectedPaths) {
        List<DefaultMutableTreeNode> nodes = deletableTopLevelNodes(selectedPaths);
        for (DefaultMutableTreeNode node : nodes) {
            if (node.getParent() != null) {
                treeModel.removeNodeFromParent(node);
            }
        }
        return nodes;
    }

    public boolean canPasteNodes(DefaultMutableTreeNode targetNode, List<DefaultMutableTreeNode> copiedNodes) {
        return resolvePasteLocation(targetNode, copiedNodes) != null;
    }

    public List<DefaultMutableTreeNode> pasteNodes(JTree performanceTree,
                                                   DefaultMutableTreeNode targetNode,
                                                   List<DefaultMutableTreeNode> copiedNodes) {
        PasteLocation pasteLocation = resolvePasteLocation(targetNode, copiedNodes);
        if (pasteLocation == null) {
            return List.of();
        }

        List<DefaultMutableTreeNode> pastedNodes = new ArrayList<>(copiedNodes.size());
        int insertIndex = pasteLocation.getIndex();
        for (DefaultMutableTreeNode copiedNode : copiedNodes) {
            DefaultMutableTreeNode pastedNode = copyTreeNode(copiedNode);
            treeModel.insertNodeInto(pastedNode, pasteLocation.getParent(),
                    Math.min(insertIndex, pasteLocation.getParent().getChildCount()));
            pastedNodes.add(pastedNode);
            insertIndex++;
        }

        Object root = treeModel.getRoot();
        if (root instanceof DefaultMutableTreeNode rootNode) {
            structureSupport.syncAllRequestStructures(rootNode);
        }
        if (performanceTree != null) {
            performanceTree.expandPath(new TreePath(pasteLocation.getParent().getPath()));
            TreePath[] pastedPaths = pastedNodes.stream()
                    .map(node -> new TreePath(node.getPath()))
                    .toArray(TreePath[]::new);
            performanceTree.setSelectionPaths(pastedPaths);
        }
        return pastedNodes;
    }

    private List<DefaultMutableTreeNode> copyableTopLevelNodes(TreePath[] selectedPaths) {
        return topLevelNodes(selectedPaths, this::isCopyableNode);
    }

    private List<DefaultMutableTreeNode> deletableTopLevelNodes(TreePath[] selectedPaths) {
        return topLevelNodes(selectedPaths, this::isDeletableNode);
    }

    private List<DefaultMutableTreeNode> topLevelNodes(TreePath[] selectedPaths,
                                                       Predicate<DefaultMutableTreeNode> nodeFilter) {
        if (selectedPaths == null || selectedPaths.length == 0) {
            return List.of();
        }
        List<DefaultMutableTreeNode> selectedNodes = new ArrayList<>();
        for (TreePath path : selectedPaths) {
            if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) {
                continue;
            }
            if (nodeFilter.test(node)) {
                selectedNodes.add(node);
            }
        }
        selectedNodes.sort(this::compareTreeOrder);

        List<DefaultMutableTreeNode> topLevelNodes = new ArrayList<>();
        for (DefaultMutableTreeNode node : selectedNodes) {
            if (!hasSelectedAncestor(node, selectedNodes)) {
                topLevelNodes.add(node);
            }
        }
        return topLevelNodes;
    }

    private boolean isDeletableNode(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return false;
        }
        return nodeData.type != NodeType.ROOT;
    }

    private boolean isCopyableNode(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return false;
        }
        return nodeData.type != NodeType.ROOT;
    }

    private boolean hasSelectedAncestor(DefaultMutableTreeNode node, List<DefaultMutableTreeNode> selectedNodes) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        while (parent != null) {
            if (selectedNodes.contains(parent)) {
                return true;
            }
            parent = (DefaultMutableTreeNode) parent.getParent();
        }
        return false;
    }

    private int compareTreeOrder(DefaultMutableTreeNode first, DefaultMutableTreeNode second) {
        TreePath firstPath = new TreePath(first.getPath());
        TreePath secondPath = new TreePath(second.getPath());
        int firstCount = firstPath.getPathCount();
        int secondCount = secondPath.getPathCount();
        int sharedCount = Math.min(firstCount, secondCount);
        for (int i = 0; i < sharedCount; i++) {
            Object firstComponent = firstPath.getPathComponent(i);
            Object secondComponent = secondPath.getPathComponent(i);
            if (firstComponent == secondComponent) {
                continue;
            }
            DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) firstComponent;
            DefaultMutableTreeNode secondNode = (DefaultMutableTreeNode) secondComponent;
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) firstNode.getParent();
            return Integer.compare(parent.getIndex(firstNode), parent.getIndex(secondNode));
        }
        return Integer.compare(firstCount, secondCount);
    }

    private DefaultMutableTreeNode copyTreeNode(DefaultMutableTreeNode source) {
        return PerformanceTreeSnapshot.copyForPaste(source);
    }

    private PasteLocation resolvePasteLocation(DefaultMutableTreeNode targetNode, List<DefaultMutableTreeNode> copiedNodes) {
        if (targetNode == null || copiedNodes == null || copiedNodes.isEmpty()) {
            return null;
        }
        PasteLocation webSocketConnectLocation = resolveWebSocketConnectPasteLocation(targetNode, copiedNodes);
        if (webSocketConnectLocation != null) {
            return webSocketConnectLocation;
        }
        if (canAcceptAll(targetNode, copiedNodes)) {
            return new PasteLocation(targetNode, targetNode.getChildCount());
        }
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) targetNode.getParent();
        if (parentNode != null && canAcceptAll(parentNode, copiedNodes)) {
            return new PasteLocation(parentNode, parentNode.getIndex(targetNode) + 1);
        }
        return null;
    }

    private boolean canAcceptAll(DefaultMutableTreeNode parentNode, List<DefaultMutableTreeNode> children) {
        for (DefaultMutableTreeNode child : children) {
            if (!PerformanceTreeRules.canAcceptChild(parentNode, child)) {
                return false;
            }
        }
        return true;
    }

    private PasteLocation resolveWebSocketConnectPasteLocation(DefaultMutableTreeNode targetNode,
                                                              List<DefaultMutableTreeNode> copiedNodes) {
        boolean containsWebSocketConnect = copiedNodes.stream()
                .anyMatch(node -> PerformanceTreeRules.isNodeType(node, NodeType.WS_CONNECT));
        if (!containsWebSocketConnect) {
            return null;
        }
        DefaultMutableTreeNode requestNode = structureSupport.resolveWebSocketConnectParent(targetNode);
        if (requestNode == null || !canAcceptAll(requestNode, copiedNodes)) {
            return null;
        }
        return new PasteLocation(requestNode, structureSupport.resolveWebSocketConnectInsertIndex(requestNode, targetNode));
    }

    @Value
    private static class PasteLocation {
        DefaultMutableTreeNode parent;
        int index;
    }
}
