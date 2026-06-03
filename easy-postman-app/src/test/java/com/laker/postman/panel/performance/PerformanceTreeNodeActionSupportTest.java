package com.laker.postman.panel.performance;

import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeNodeActionSupportTest {

    @Test
    public void setSelectedNodesEnabledShouldUpdateNodesAndSaveOnce() {
        TreeFixture fixture = new TreeFixture();

        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));
        fixture.actionSupport.setSelectedNodesEnabled(false);

        assertFalse(((PerformanceTreeNode) fixture.request.getUserObject()).enabled);
        assertEquals(fixture.saveCount.get(), 1);
    }

    @Test
    public void deleteActionShouldClearCurrentRequestWhenDeleted() {
        TreeFixture fixture = new TreeFixture();
        fixture.currentRequest.set(fixture.request);
        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));
        fixture.actionSupport.setDeleteConfirmationAction(count -> true);

        fixture.actionSupport.createDeleteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete"));

        assertEquals(fixture.threadGroup.getChildCount(), 0);
        assertEquals(fixture.currentRequest.get(), null);
        assertEquals(fixture.saveCount.get(), 1);
    }

    @Test
    public void deleteActionShouldNotDeleteWhenConfirmationRejected() {
        TreeFixture fixture = new TreeFixture();
        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));
        AtomicInteger confirmCount = new AtomicInteger();
        fixture.actionSupport.setDeleteConfirmationAction(count -> {
            confirmCount.set(count);
            return false;
        });

        fixture.actionSupport.createDeleteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete"));

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
        fixture.actionSupport.setDeleteConfirmationAction(count -> {
            confirmCount.set(count);
            return false;
        });

        fixture.actionSupport.createDeleteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete"));

        assertEquals(confirmCount.get(), 1);
        assertEquals(fixture.threadGroup.getChildCount(), 1);
        assertEquals(fixture.saveCount.get(), 0);
    }

    @Test
    public void copyAndPasteActionsShouldPersistSelectionAndSaveAfterPaste() {
        TreeFixture fixture = new TreeFixture();
        fixture.tree.setSelectionPath(new TreePath(fixture.request.getPath()));

        fixture.actionSupport.createCopyAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy"));
        fixture.tree.setSelectionPath(new TreePath(fixture.threadGroup.getPath()));
        fixture.actionSupport.createPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "paste"));

        assertEquals(fixture.persistCount.get(), 2);
        assertEquals(fixture.threadGroup.getChildCount(), 2);
        DefaultMutableTreeNode pasted = (DefaultMutableTreeNode) fixture.threadGroup.getChildAt(1);
        assertSame(((PerformanceTreeNode) pasted.getUserObject()).type, NodeType.REQUEST);
        assertTrue(fixture.saveCount.get() >= 1);
    }

    private static final class TreeFixture {
        private final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("root", NodeType.ROOT));
        private final DefaultMutableTreeNode threadGroup = new DefaultMutableTreeNode(new PerformanceTreeNode("group", NodeType.THREAD_GROUP));
        private final DefaultMutableTreeNode request = new DefaultMutableTreeNode(new PerformanceTreeNode("request", NodeType.REQUEST));
        private final DefaultTreeModel treeModel;
        private final JTree tree;
        private final AtomicInteger saveCount = new AtomicInteger();
        private final AtomicInteger persistCount = new AtomicInteger();
        private final AtomicReference<DefaultMutableTreeNode> currentRequest = new AtomicReference<>();
        private final PerformanceTreeNodeActionSupport actionSupport;

        private TreeFixture() {
            root.add(threadGroup);
            threadGroup.add(request);
            treeModel = new DefaultTreeModel(root);
            tree = new JTree(treeModel);
            PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(treeModel);
            actionSupport = new PerformanceTreeNodeActionSupport(
                    new JPanel(),
                    tree,
                    treeModel,
                    new CardLayout(),
                    new JPanel(),
                    treeSupport,
                    persistCount::incrementAndGet,
                    ignored -> {
                    },
                    currentRequest::get,
                    currentRequest::set,
                    saveCount::incrementAndGet,
                    "request"
            );
        }
    }
}
