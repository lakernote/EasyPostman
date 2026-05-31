package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.EnumSet;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeActionPolicyTest {

    @Test
    public void rootSelectionShouldOnlyExposeRootActions() {
        TreeFixture fixture = new TreeFixture(RequestItemProtocolEnum.HTTP);
        PerformanceTreeActionPolicy policy = new PerformanceTreeActionPolicy(fixture.treeSupport);

        EnumSet<PerformanceTreeAction> actions = policy.actionsForSingleSelection(fixture.root, List.of());

        assertTrue(actions.contains(PerformanceTreeAction.ADD_THREAD_GROUP));
        assertFalse(actions.contains(PerformanceTreeAction.ENABLE));
        assertFalse(actions.contains(PerformanceTreeAction.DISABLE));
        assertFalse(actions.contains(PerformanceTreeAction.RENAME));
        assertFalse(actions.contains(PerformanceTreeAction.DELETE));
    }

    @Test
    public void disabledWebSocketConnectSelectionShouldExposeScenarioActionsAndEnable() {
        TreeFixture fixture = new TreeFixture(RequestItemProtocolEnum.WEBSOCKET);
        DefaultMutableTreeNode wsConnect = node(NodeType.WS_CONNECT, false);
        fixture.request.add(wsConnect);
        PerformanceTreeActionPolicy policy = new PerformanceTreeActionPolicy(fixture.treeSupport);

        EnumSet<PerformanceTreeAction> actions = policy.actionsForSingleSelection(wsConnect, List.of());

        assertTrue(actions.contains(PerformanceTreeAction.ADD_WS_CONNECT));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_WS_SEND));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_WS_READ));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_WS_CLOSE));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_LOOP));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_TIMER));
        assertTrue(actions.contains(PerformanceTreeAction.COPY));
        assertTrue(actions.contains(PerformanceTreeAction.DELETE));
        assertTrue(actions.contains(PerformanceTreeAction.ENABLE));
        assertFalse(actions.contains(PerformanceTreeAction.DISABLE));
        assertFalse(actions.contains(PerformanceTreeAction.RENAME));
        assertTrue(policy.canSetEnabled(wsConnect, true));
        assertFalse(policy.canSetEnabled(wsConnect, false));
        assertFalse(policy.canRename(wsConnect));
    }

    @Test
    public void sseReadSelectionShouldExposeStageActionsAssertionAndDisable() {
        TreeFixture fixture = new TreeFixture(RequestItemProtocolEnum.SSE);
        DefaultMutableTreeNode sseRead = node(NodeType.SSE_READ, true);
        fixture.request.add(sseRead);
        PerformanceTreeActionPolicy policy = new PerformanceTreeActionPolicy(fixture.treeSupport);

        EnumSet<PerformanceTreeAction> actions = policy.actionsForSingleSelection(sseRead, List.of());

        assertTrue(actions.contains(PerformanceTreeAction.ADD_SSE_CONNECT));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_SSE_READ));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_ASSERTION));
        assertTrue(actions.contains(PerformanceTreeAction.ADD_EXTRACTOR));
        assertTrue(actions.contains(PerformanceTreeAction.DISABLE));
        assertFalse(actions.contains(PerformanceTreeAction.ENABLE));
        assertFalse(actions.contains(PerformanceTreeAction.RENAME));
    }

    @Test
    public void mixedMultiSelectionShouldExposeOnlySelectionWideActions() {
        TreeFixture fixture = new TreeFixture(RequestItemProtocolEnum.HTTP);
        DefaultMutableTreeNode disabledRequest = node(NodeType.REQUEST, false);
        fixture.threadGroup.add(disabledRequest);
        PerformanceTreeActionPolicy policy = new PerformanceTreeActionPolicy(fixture.treeSupport);

        EnumSet<PerformanceTreeAction> actions = policy.actionsForMultiSelection(new TreePath[]{
                new TreePath(fixture.request.getPath()),
                new TreePath(disabledRequest.getPath())
        });

        assertTrue(actions.contains(PerformanceTreeAction.COPY));
        assertTrue(actions.contains(PerformanceTreeAction.DELETE));
        assertTrue(actions.contains(PerformanceTreeAction.ENABLE));
        assertTrue(actions.contains(PerformanceTreeAction.DISABLE));
        assertFalse(actions.contains(PerformanceTreeAction.ADD_REQUEST));
        assertFalse(actions.contains(PerformanceTreeAction.PASTE));
        assertFalse(actions.contains(PerformanceTreeAction.RENAME));
    }

    private static DefaultMutableTreeNode node(NodeType type, boolean enabled) {
        PerformanceTreeNode data = new PerformanceTreeNode(type.name(), type);
        data.enabled = enabled;
        return new DefaultMutableTreeNode(data);
    }

    private static HttpRequestItem requestItem(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setProtocol(protocol);
        return item;
    }

    private static final class TreeFixture {
        private final DefaultMutableTreeNode root = node(NodeType.ROOT, true);
        private final DefaultMutableTreeNode threadGroup = node(NodeType.THREAD_GROUP, true);
        private final PerformanceTreeNode requestData = new PerformanceTreeNode("request", NodeType.REQUEST, requestItem(RequestItemProtocolEnum.HTTP));
        private final DefaultMutableTreeNode request = new DefaultMutableTreeNode(requestData);
        private final PerformanceTreeSupport treeSupport;

        private TreeFixture(RequestItemProtocolEnum protocol) {
            requestData.httpRequestItem = requestItem(protocol);
            root.add(threadGroup);
            threadGroup.add(request);
            treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));
        }
    }
}
