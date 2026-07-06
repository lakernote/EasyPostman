package com.laker.postman.panel.performance;

import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeNodeCommandSupportTest {

    @Test
    public void setSelectedNodesEnabledShouldUpdateNodesAndSaveOnce() {
        TreeFixture fixture = new TreeFixture();

        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));
        fixture.commandSupport.setSelectedNodesEnabled(false);

        assertFalse(((PerformanceTreeNode) fixture.request.getUserObject()).enabled);
        assertEquals(fixture.saveCount.get(), 1);
    }

    @Test
    public void deleteActionShouldClearCurrentRequestWhenDeleted() {
        TreeFixture fixture = new TreeFixture();
        fixture.currentRequest.set(fixture.request);
        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));
        fixture.commandSupport.setDeleteConfirmationAction(count -> true);

        fixture.commandSupport.createDeleteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete"));

        assertEquals(fixture.threadGroup.getChildCount(), 0);
        assertEquals(fixture.currentRequest.get(), null);
        assertEquals(fixture.saveCount.get(), 1);
    }

    @Test
    public void deleteActionShouldNotDeleteWhenConfirmationRejected() {
        TreeFixture fixture = new TreeFixture();
        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));
        AtomicInteger confirmCount = new AtomicInteger();
        fixture.commandSupport.setDeleteConfirmationAction(count -> {
            confirmCount.set(count);
            return false;
        });

        fixture.commandSupport.createDeleteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete"));

        assertEquals(confirmCount.get(), 1);
        assertEquals(fixture.threadGroup.getChildCount(), 1);
        assertEquals(fixture.saveCount.get(), 0);
    }

    @Test
    public void deleteActionShouldConfirmOnlyTopLevelDeletableNodes() {
        TreeFixture fixture = new TreeFixture();
        DefaultMutableTreeNode timer = new DefaultMutableTreeNode(new PerformanceTreeNode("timer", NodeType.TIMER));
        fixture.request.add(timer);
        fixture.tree.setSelectionPaths(new TreePath[]{
                new TreePath(fixture.root.getPath()),
                new TreePath(fixture.request.getPath()),
                new TreePath(timer.getPath())
        });
        AtomicInteger confirmCount = new AtomicInteger();
        fixture.commandSupport.setDeleteConfirmationAction(count -> {
            confirmCount.set(count);
            return false;
        });

        fixture.commandSupport.createDeleteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete"));

        assertEquals(confirmCount.get(), 1);
        assertEquals(fixture.threadGroup.getChildCount(), 1);
        assertEquals(fixture.saveCount.get(), 0);
    }

    @Test
    public void copyAndPasteActionsShouldPersistSelectionAndSaveAfterPaste() {
        TreeFixture fixture = new TreeFixture();
        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));

        fixture.commandSupport.createCopyAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy"));
        fixture.tree.setSelectionPath(new TreePath(fixture.threadGroup.getPath()));
        fixture.commandSupport.createPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "paste"));

        assertEquals(fixture.persistCount.get(), 2);
        assertEquals(fixture.threadGroup.getChildCount(), 2);
        DefaultMutableTreeNode pasted = (DefaultMutableTreeNode) fixture.threadGroup.getChildAt(1);
        assertSame(((PerformanceTreeNode) pasted.getUserObject()).type, NodeType.REQUEST);
        assertTrue(fixture.saveCount.get() >= 1);
    }

    @Test
    public void addSelectedRequestsShouldStorePerformanceOwnedRequestCopy() throws Exception {
        TreeFixture fixture = new TreeFixture();
        HttpRequestItem selectedRequest = new HttpRequestItem();
        selectedRequest.setId("selected-request");
        selectedRequest.setName("Selected Request");
        selectedRequest.setUrl("https://example.test/selected");
        selectedRequest.setProtocol(RequestItemProtocolEnum.HTTP);

        Method method = PerformanceTreeNodeCommandSupport.class.getDeclaredMethod(
                "addSelectedRequests",
                DefaultMutableTreeNode.class,
                List.class
        );
        method.setAccessible(true);
        method.invoke(fixture.commandSupport, fixture.threadGroup, List.of(selectedRequest));

        DefaultMutableTreeNode addedNode = (DefaultMutableTreeNode) fixture.threadGroup.getChildAt(1);
        PerformanceTreeNode addedData = (PerformanceTreeNode) addedNode.getUserObject();
        assertNotSame(addedData.httpRequestItem, selectedRequest);
        assertEquals(addedData.httpRequestItem.getUrl(), "https://example.test/selected");
        assertSame(fixture.switchedRequest.get(), addedData.httpRequestItem);
    }

    private static final class TreeFixture {
        private final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("root", NodeType.ROOT));
        private final DefaultMutableTreeNode threadGroup = new DefaultMutableTreeNode(new PerformanceTreeNode("group", NodeType.THREAD_GROUP));
        private final DefaultMutableTreeNode request = new DefaultMutableTreeNode(new PerformanceTreeNode("request", NodeType.REQUEST));
        private final DefaultTreeModel treeModel;
        private final JTree tree;
        private final CardLayout propertyCardLayout = new CardLayout();
        private final JPanel propertyPanel = new JPanel(propertyCardLayout);
        private final AtomicInteger saveCount = new AtomicInteger();
        private final AtomicInteger persistCount = new AtomicInteger();
        private final AtomicReference<DefaultMutableTreeNode> currentRequest = new AtomicReference<>();
        private final AtomicReference<HttpRequestItem> switchedRequest = new AtomicReference<>();
        private final PerformanceTreeNodeCommandSupport commandSupport;

        private TreeFixture() {
            root.add(threadGroup);
            threadGroup.add(request);
            treeModel = new DefaultTreeModel(root);
            tree = new JTree(treeModel);
            propertyPanel.add(new JPanel(), "request");
            PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(treeModel);
            commandSupport = new PerformanceTreeNodeCommandSupport(
                    new JPanel(),
                    tree,
                    treeModel,
                    propertyCardLayout,
                    propertyPanel,
                    treeSupport,
                    persistCount::incrementAndGet,
                    switchedRequest::set,
                    currentRequest::get,
                    currentRequest::set,
                    saveCount::incrementAndGet,
                    "request"
            );
        }
    }
}
