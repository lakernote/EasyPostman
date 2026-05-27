package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeSelectionSupportTest extends AbstractSwingUiTest {

    @Test
    public void requestSelectionShouldSyncStructureSwitchEditorAndTrackCurrentRequest() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TreeFixture fixture = new TreeFixture();
            AtomicInteger syncCount = new AtomicInteger();
            AtomicReference<HttpRequestItem> switchedItem = new AtomicReference<>();
            AtomicReference<DefaultMutableTreeNode> currentRequest = new AtomicReference<>();
            PerformanceTreeSelectionSupport support = fixture.createSelectionSupport(
                    ignored -> {
                    },
                    switchedItem::set,
                    (node, treeNode) -> syncCount.incrementAndGet(),
                    currentRequest::set
            );
            support.install();

            fixture.tree.setSelectionPath(new TreePath(fixture.requestNode.getPath()));

            assertEquals(syncCount.get(), 1);
            assertSame(switchedItem.get(), fixture.requestItem);
            assertSame(currentRequest.get(), fixture.requestNode);
            assertTrue(fixture.requestCard.isVisible());
            assertFalse(fixture.emptyCard.isVisible());
        });
    }

    @Test
    public void changingSelectionShouldPersistPreviousRequestAndShowEmptyForUnknownNode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TreeFixture fixture = new TreeFixture();
            AtomicReference<DefaultMutableTreeNode> savedRequest = new AtomicReference<>();
            AtomicReference<DefaultMutableTreeNode> currentRequest = new AtomicReference<>();
            PerformanceTreeSelectionSupport support = fixture.createSelectionSupport(
                    savedRequest::set,
                    ignored -> {
                    },
                    (node, treeNode) -> {
                    },
                    currentRequest::set
            );
            support.install();

            fixture.tree.setSelectionPath(new TreePath(fixture.requestNode.getPath()));
            fixture.tree.setSelectionPath(new TreePath(fixture.unknownNode.getPath()));

            assertSame(savedRequest.get(), fixture.requestNode);
            assertEquals(currentRequest.get(), null);
            assertTrue(fixture.emptyCard.isVisible());
            assertFalse(fixture.requestCard.isVisible());
        });
    }

    @Test
    public void webSocketConnectSelectionShouldEditConnectStageNode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TreeFixture fixture = new TreeFixture();
            fixture.requestItem.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            JMeterTreeNode connectData = new JMeterTreeNode("connect", NodeType.WS_CONNECT);
            connectData.webSocketPerformanceData = new WebSocketPerformanceData();
            DefaultMutableTreeNode connectNode = new DefaultMutableTreeNode(connectData);
            fixture.requestNode.add(connectNode);
            RecordingWebSocketStagePropertyPanel wsConnectPanel = new RecordingWebSocketStagePropertyPanel();
            PerformanceTreeSelectionSupport support = fixture.createSelectionSupport(
                    ignored -> {
                    },
                    ignored -> {
                    },
                    (node, treeNode) -> {
                    },
                    ignored -> {
                    },
                    wsConnectPanel
            );
            support.install();

            fixture.tree.setSelectionPath(new TreePath(connectNode.getPath()));

            assertSame(wsConnectPanel.lastNode, connectData);
            assertTrue(fixture.wsConnectCard.isVisible());
        });
    }

    @Test
    public void webSocketConnectSelectionShouldInitializeStageConfigFromRequestDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TreeFixture fixture = new TreeFixture();
            fixture.requestItem.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            JMeterTreeNode requestData = (JMeterTreeNode) fixture.requestNode.getUserObject();
            requestData.webSocketPerformanceData = new WebSocketPerformanceData();
            requestData.webSocketPerformanceData.connectTimeoutMs = 4321;
            JMeterTreeNode connectData = new JMeterTreeNode("connect", NodeType.WS_CONNECT);
            DefaultMutableTreeNode connectNode = new DefaultMutableTreeNode(connectData);
            fixture.requestNode.add(connectNode);
            RecordingWebSocketStagePropertyPanel wsConnectPanel = new RecordingWebSocketStagePropertyPanel();
            PerformanceTreeSelectionSupport support = fixture.createSelectionSupport(
                    ignored -> {
                    },
                    ignored -> {
                    },
                    (node, treeNode) -> {
                    },
                    ignored -> {
                    },
                    wsConnectPanel
            );
            support.install();

            fixture.tree.setSelectionPath(new TreePath(connectNode.getPath()));

            assertSame(wsConnectPanel.lastNode, connectData);
            assertEquals(connectData.webSocketPerformanceData.connectTimeoutMs, 4321);
            assertNotSame(connectData.webSocketPerformanceData, requestData.webSocketPerformanceData);
        });
    }

    private static final class TreeFixture {
        private static final String EMPTY_CARD = "empty";
        private static final String REQUEST_CARD = "request";
        private static final String WS_CONNECT_CARD = "wsConnect";

        private final HttpRequestItem requestItem = new HttpRequestItem();
        private final DefaultMutableTreeNode requestNode;
        private final DefaultMutableTreeNode unknownNode;
        private final JTree tree;
        private final DefaultTreeModel treeModel;
        private final CardLayout cardLayout = new CardLayout();
        private final JPanel propertyPanel = new JPanel(cardLayout);
        private final JPanel emptyCard = new JPanel();
        private final JPanel requestCard = new JPanel();
        private final JPanel wsConnectCard = new JPanel();

        private TreeFixture() {
            requestItem.setName("request");
            JMeterTreeNode requestData = new JMeterTreeNode("request", NodeType.REQUEST, requestItem);
            requestNode = new DefaultMutableTreeNode(requestData);
            unknownNode = new DefaultMutableTreeNode("unknown");
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("root", NodeType.ROOT));
            root.add(requestNode);
            root.add(unknownNode);
            treeModel = new DefaultTreeModel(root);
            tree = new JTree(treeModel);
            emptyCard.add(new JLabel("empty"));
            requestCard.add(new JLabel("request"));
            wsConnectCard.add(new JLabel("wsConnect"));
            propertyPanel.add(emptyCard, EMPTY_CARD);
            propertyPanel.add(requestCard, REQUEST_CARD);
            propertyPanel.add(wsConnectCard, WS_CONNECT_CARD);
            cardLayout.show(propertyPanel, EMPTY_CARD);
        }

        private PerformanceTreeSelectionSupport createSelectionSupport(
                java.util.function.Consumer<DefaultMutableTreeNode> saveRequestAction,
                java.util.function.Consumer<HttpRequestItem> switchRequestEditorAction,
                java.util.function.BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction,
                java.util.function.Consumer<DefaultMutableTreeNode> currentRequestSetter) {
            return createSelectionSupport(
                    saveRequestAction,
                    switchRequestEditorAction,
                    syncRequestStructureAction,
                    currentRequestSetter,
                    null
            );
        }

        private PerformanceTreeSelectionSupport createSelectionSupport(
                java.util.function.Consumer<DefaultMutableTreeNode> saveRequestAction,
                java.util.function.Consumer<HttpRequestItem> switchRequestEditorAction,
                java.util.function.BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction,
                java.util.function.Consumer<DefaultMutableTreeNode> currentRequestSetter,
                WebSocketStagePropertyPanel wsConnectPanel) {
            return new PerformanceTreeSelectionSupport(
                    tree,
                    treeModel,
                    cardLayout,
                    propertyPanel,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    wsConnectPanel,
                    null,
                    null,
                    null,
                    new PerformanceTreeSupport(treeModel),
                    saveRequestAction,
                    ignored -> {
                    },
                    ignored -> {
                    },
                    switchRequestEditorAction,
                    syncRequestStructureAction,
                    currentRequestSetter,
                    EMPTY_CARD,
                    "threadGroup",
                    "loop",
                    REQUEST_CARD,
                    "assertion",
                    "extractor",
                    "timer",
                    "sseConnect",
                    "sseAwait",
                    WS_CONNECT_CARD,
                    "wsSend",
                    "wsAwait",
                    "wsClose"
            );
        }
    }

    private static final class RecordingWebSocketStagePropertyPanel extends WebSocketStagePropertyPanel {
        private JMeterTreeNode lastNode;

        private RecordingWebSocketStagePropertyPanel() {
            super(Stage.CONNECT);
        }

        @Override
        public void setNode(JMeterTreeNode node) {
            lastNode = node;
        }
    }
}
