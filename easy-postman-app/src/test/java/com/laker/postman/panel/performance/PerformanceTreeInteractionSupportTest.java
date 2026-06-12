package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.CardLayout;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;

public class PerformanceTreeInteractionSupportTest extends AbstractSwingUiTest {

    @Test
    public void installShouldBindMacDeleteKeyToDeleteAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("root", NodeType.ROOT));
            DefaultMutableTreeNode request = new DefaultMutableTreeNode(new PerformanceTreeNode("request", NodeType.REQUEST));
            root.add(request);
            DefaultTreeModel treeModel = new DefaultTreeModel(root);
            JTree tree = new JTree(treeModel);
            AtomicReference<DefaultMutableTreeNode> currentRequest = new AtomicReference<>();
            PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(treeModel);

            PerformanceTreeInteractionSupport support = new PerformanceTreeInteractionSupport(
                    new JPanel(),
                    tree,
                    treeModel,
                    new CardLayout(),
                    new JPanel(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    treeSupport,
                    ignored -> {
                    },
                    ignored -> {
                    },
                    ignored -> {
                    },
                    ignored -> {
                    },
                    (node, treeNode) -> {
                    },
                    () -> {
                    },
                    currentRequest::get,
                    currentRequest::set,
                    "empty",
                    "threadGroup",
                    "csvDataSet",
                    "loop",
                    "simple",
                    "condition",
                    "while",
                    "onceOnly",
                    "request",
                    "assertion",
                    "extractor",
                    "timer",
                    "sseConnect",
                    "sseRead",
                    "wsConnect",
                    "wsSend",
                    "wsRead",
                    "wsClose"
            );

            support.install();

            InputMap inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
            assertEquals(
                    inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
                    "deletePerformanceNode"
            );
            assertEquals(
                    inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)),
                    "deletePerformanceNode"
            );
        });
    }
}
