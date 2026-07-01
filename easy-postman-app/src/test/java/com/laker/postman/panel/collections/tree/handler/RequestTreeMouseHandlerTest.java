package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RequestTreeMouseHandlerTest extends AbstractSwingUiTest {

    @Test
    public void singleClickOnGroupRowShouldToggleExpansion() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TreeFixture fixture = createTreeFixture();
            RecordingOpenActions openActions = new RecordingOpenActions();
            RequestTreeMouseHandler handler = new RequestTreeMouseHandler(
                    fixture.tree(),
                    null,
                    new RequestTreeCoordinator(fixture.tree(), null),
                    openActions
            );

            handler.mousePressed(singleLeftClickOnRow(fixture.tree(), fixture.groupPath()));

            assertTrue(fixture.tree().isExpanded(fixture.groupPath()));
            assertEquals(openActions.transientGroupOpenCount(), 1);

            handler.mousePressed(singleLeftClickOnRow(fixture.tree(), fixture.groupPath()));

            assertFalse(fixture.tree().isExpanded(fixture.groupPath()));
            assertEquals(openActions.transientGroupOpenCount(), 2);
        });
    }

    private static TreeFixture createTreeFixture() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CollectionTreePanel.ROOT);
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(new RequestGroup("Group"));
        groupNode.add(CollectionTreeNodes.requestNode(new HttpRequestItem()));
        root.add(groupNode);

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setToggleClickCount(0);
        tree.setRowHeight(28);
        tree.setSize(320, 120);

        return new TreeFixture(tree, new TreePath(groupNode.getPath()));
    }

    private static MouseEvent singleLeftClickOnRow(JTree tree, TreePath path) {
        Rectangle rowBounds = tree.getRowBounds(tree.getRowForPath(path));
        return new MouseEvent(
                tree,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                rowBounds.x + 80,
                rowBounds.y + rowBounds.height / 2,
                1,
                false,
                MouseEvent.BUTTON1
        );
    }

    private record TreeFixture(JTree tree, TreePath groupPath) {
    }

    private static class RecordingOpenActions implements RequestTreeOpenActions {
        private final AtomicInteger transientGroupOpens = new AtomicInteger();

        @Override
        public void openTransientRequest(HttpRequestItem item) {
        }

        @Override
        public void openFixedRequest(HttpRequestItem item) {
        }

        @Override
        public void openTransientGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
            transientGroupOpens.incrementAndGet();
        }

        @Override
        public void openFixedGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        }

        @Override
        public void openTransientSavedResponse(SavedResponse savedResponse) {
        }

        @Override
        public void openFixedSavedResponse(SavedResponse savedResponse) {
        }

        int transientGroupOpenCount() {
            return transientGroupOpens.get();
        }
    }
}
