package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeMenuVisibilitySupportTest {

    @Test
    public void rootSelectionShouldOnlyShowAddThreadGroupAndPasteWhenAllowed() {
        TreeFixture fixture = new TreeFixture();
        PerformanceTreeMenuItems items = newMenuItems();
        PerformanceTreeMenuVisibilitySupport support = new PerformanceTreeMenuVisibilitySupport(
                fixture.treeSupport,
                List::of
        );

        support.configureSingleSelectionMenu(fixture.root, items);

        assertTrue(items.addThreadGroup().isVisible());
        assertFalse(items.addRequest().isVisible());
        assertFalse(items.copyNode().isVisible());
        assertFalse(items.renameNode().isVisible());
        assertFalse(items.deleteNode().isVisible());
        assertFalse(items.enableNode().isVisible());
        assertFalse(items.disableNode().isVisible());
    }

    @Test
    public void threadGroupSelectionShouldShowContainerActionsAndDisableForEnabledNode() {
        TreeFixture fixture = new TreeFixture();
        PerformanceTreeMenuItems items = newMenuItems();
        PerformanceTreeMenuVisibilitySupport support = new PerformanceTreeMenuVisibilitySupport(
                fixture.treeSupport,
                List::of
        );

        support.configureSingleSelectionMenu(fixture.threadGroup, items);

        assertFalse(items.addThreadGroup().isVisible());
        assertTrue(items.addRequest().isVisible());
        assertTrue(items.addLoop().isVisible());
        assertTrue(items.copyNode().isVisible());
        assertTrue(items.renameNode().isVisible());
        assertTrue(items.deleteNode().isVisible());
        assertFalse(items.enableNode().isVisible());
        assertTrue(items.disableNode().isVisible());
    }

    @Test
    public void multiSelectionShouldHideAddRenamePasteAndExposeMixedEnableState() {
        TreeFixture fixture = new TreeFixture();
        PerformanceTreeMenuItems items = newMenuItems();
        PerformanceTreeMenuVisibilitySupport support = new PerformanceTreeMenuVisibilitySupport(
                fixture.treeSupport,
                List::of
        );

        support.configureMultiSelectionMenu(
                new TreePath[]{
                        new TreePath(fixture.request.getPath()),
                        new TreePath(fixture.disabledRequest.getPath())
                },
                items
        );

        assertFalse(items.addRequest().isVisible());
        assertFalse(items.addLoop().isVisible());
        assertFalse(items.pasteNode().isVisible());
        assertFalse(items.renameNode().isVisible());
        assertTrue(items.copyNode().isVisible());
        assertTrue(items.deleteNode().isVisible());
        assertTrue(items.enableNode().isVisible());
        assertTrue(items.disableNode().isVisible());
    }

    @Test
    public void disabledWebSocketConnectSelectionShouldShowEnableWithScenarioActions() {
        TreeFixture fixture = new TreeFixture();
        fixture.requestData.httpRequestItem = requestItem(RequestItemProtocolEnum.WEBSOCKET);
        DefaultMutableTreeNode wsConnect = node(NodeType.WS_CONNECT, false);
        fixture.request.add(wsConnect);
        PerformanceTreeMenuItems items = newMenuItems();
        PerformanceTreeMenuVisibilitySupport support = new PerformanceTreeMenuVisibilitySupport(
                fixture.treeSupport,
                List::of
        );

        support.configureSingleSelectionMenu(wsConnect, items);

        assertTrue(items.addLoop().isVisible());
        assertTrue(items.addWsConnect().isVisible());
        assertTrue(items.addWsSend().isVisible());
        assertTrue(items.addWsRead().isVisible());
        assertTrue(items.addWsClose().isVisible());
        assertTrue(items.addTimer().isVisible());
        assertTrue(items.copyNode().isVisible());
        assertTrue(items.deleteNode().isVisible());
        assertTrue(items.enableNode().isVisible());
        assertFalse(items.disableNode().isVisible());
        assertFalse(items.renameNode().isVisible());
    }

    @Test
    public void sseStageSelectionShouldShowEnableOrDisableToggle() {
        TreeFixture fixture = new TreeFixture();
        fixture.requestData.httpRequestItem = requestItem(RequestItemProtocolEnum.SSE);
        DefaultMutableTreeNode disabledConnect = node(NodeType.SSE_CONNECT, false);
        DefaultMutableTreeNode enabledRead = node(NodeType.SSE_READ, true);
        fixture.request.add(disabledConnect);
        fixture.request.add(enabledRead);
        PerformanceTreeMenuVisibilitySupport support = new PerformanceTreeMenuVisibilitySupport(
                fixture.treeSupport,
                List::of
        );

        PerformanceTreeMenuItems connectItems = newMenuItems();
        support.configureSingleSelectionMenu(disabledConnect, connectItems);

        assertTrue(connectItems.addSseConnect().isVisible());
        assertTrue(connectItems.addSseRead().isVisible());
        assertTrue(connectItems.enableNode().isVisible());
        assertFalse(connectItems.disableNode().isVisible());
        assertFalse(connectItems.renameNode().isVisible());

        PerformanceTreeMenuItems readItems = newMenuItems();
        support.configureSingleSelectionMenu(enabledRead, readItems);

        assertTrue(readItems.addSseConnect().isVisible());
        assertTrue(readItems.addSseRead().isVisible());
        assertTrue(readItems.addAssertion().isVisible());
        assertTrue(readItems.addExtractor().isVisible());
        assertFalse(readItems.enableNode().isVisible());
        assertTrue(readItems.disableNode().isVisible());
        assertFalse(readItems.renameNode().isVisible());
    }

    private static PerformanceTreeMenuItems newMenuItems() {
        return new PerformanceTreeMenuItems(
                new JMenuItem("addThreadGroup"),
                new JMenuItem("addCsvDataSet"),
                new JMenuItem("addRequest"),
                new JMenuItem("addLoop"),
                new JMenuItem("addSseConnect"),
                new JMenuItem("addSseRead"),
                new JMenuItem("addWsConnect"),
                new JMenuItem("addWsSend"),
                new JMenuItem("addWsRead"),
                new JMenuItem("addWsClose"),
                new JMenuItem("addAssertion"),
                new JMenuItem("addExtractor"),
                new JMenuItem("addTimer"),
                new JMenuItem("enable"),
                new JMenuItem("disable"),
                new JMenuItem("copy"),
                new JMenuItem("paste"),
                new JMenuItem("rename"),
                new JMenuItem("delete")
        );
    }

    private static HttpRequestItem requestItem(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setProtocol(protocol);
        return item;
    }

    private static DefaultMutableTreeNode node(NodeType type, boolean enabled) {
        JMeterTreeNode data = new JMeterTreeNode(type.name(), type);
        data.enabled = enabled;
        return new DefaultMutableTreeNode(data);
    }

    private static final class TreeFixture {
        private final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("root", NodeType.ROOT));
        private final DefaultMutableTreeNode threadGroup = new DefaultMutableTreeNode(new JMeterTreeNode("group", NodeType.THREAD_GROUP));
        private final JMeterTreeNode requestData = new JMeterTreeNode("request", NodeType.REQUEST);
        private final DefaultMutableTreeNode request = new DefaultMutableTreeNode(requestData);
        private final DefaultMutableTreeNode disabledRequest;
        private final PerformanceTreeSupport treeSupport;

        private TreeFixture() {
            JMeterTreeNode disabledRequestData = new JMeterTreeNode("disabled", NodeType.REQUEST);
            disabledRequestData.enabled = false;
            disabledRequest = new DefaultMutableTreeNode(disabledRequestData);
            root.add(threadGroup);
            threadGroup.add(request);
            threadGroup.add(disabledRequest);
            treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));
        }
    }
}
