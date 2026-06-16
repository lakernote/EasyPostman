package com.laker.postman.panel.performance.component;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeCellRendererStructureTest extends AbstractSwingUiTest {

    @Test
    public void requestNodeShouldUseCollectionStyleMethodPrefixInsteadOfProtocolIcon() {
        HttpRequestItem item = new HttpRequestItem();
        item.setMethod("GET");
        item.setProtocol(RequestItemProtocolEnum.HTTP);
        PerformanceTreeNode nodeData = new PerformanceTreeNode("GET Example", NodeType.REQUEST, item);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(nodeData);
        JTree tree = new JTree(new DefaultTreeModel(treeNode));

        JLabel label = (JLabel) new PerformanceTreeCellRenderer()
                .getTreeCellRendererComponent(tree, treeNode, false, false, true, 0, false);

        assertNull(label.getIcon());
        assertTrue(label.getText().startsWith("<html><nobr>"));
        assertTrue(label.getText().contains(">GET</span>"));
        assertTrue(label.getText().contains("GET Example"));
    }
}
