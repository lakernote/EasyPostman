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
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class RequestTreeKeyboardHandlerTest extends AbstractSwingUiTest {

    @Test
    public void enterShouldOpenEmptyGroupLikeMouseSingleClick() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(CollectionTreePanel.ROOT);
            DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(new RequestGroup("Empty"));
            root.add(groupNode);
            JTree tree = new JTree(new DefaultTreeModel(root));
            tree.setRootVisible(false);
            tree.setSelectionPath(new TreePath(groupNode.getPath()));

            RecordingOpenActions openActions = new RecordingOpenActions();
            RequestTreeKeyboardHandler handler = new RequestTreeKeyboardHandler(
                    tree,
                    null,
                    new RequestTreeCoordinator(tree, null),
                    openActions
            );

            handler.keyPressed(new KeyEvent(
                    tree,
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    KeyEvent.VK_ENTER,
                    KeyEvent.CHAR_UNDEFINED
            ));

            assertEquals(openActions.transientGroupOpenCount(), 1);
        });
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
