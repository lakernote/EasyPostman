package com.laker.postman.panel.collections.right;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;

public class RequestEditPanelSaveTest {

    @Test(description = "新建请求保存弹框取消时保存流程返回 false")
    public void saveNewRequestReturnsFalseWhenDialogCancelled() throws Exception {
        RequestEditPanel panel = createPanelThatCancelsSaveDialog();
        Method saveNewRequest = RequestEditPanel.class.getDeclaredMethod(
                "saveNewRequest", RequestCollectionsLeftPanel.class, HttpRequestItem.class);
        saveNewRequest.setAccessible(true);

        Object result = saveNewRequest.invoke(panel, createCollectionPanel(), new HttpRequestItem());

        assertEquals(result, Boolean.FALSE);
    }

    private RequestEditPanel createPanelThatCancelsSaveDialog() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new RequestEditPanel() {
                @Override
                protected void initUI() {
                }

                @Override
                protected void registerListeners() {
                }

                @Override
                public Object[] showGroupAndNameDialog(TreeModel groupTreeModel, String defaultName) {
                    return null;
                }
            };
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private RequestCollectionsLeftPanel createCollectionPanel() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new RequestCollectionsLeftPanel() {
                @Override
                protected void initUI() {
                }

                @Override
                protected void registerListeners() {
                }

                @Override
                public DefaultTreeModel getGroupTreeModel() {
                    return new DefaultTreeModel(new DefaultMutableTreeNode("root"));
                }
            };
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }
}
