package com.laker.postman.panel.collections.editor;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.model.Variable;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreeDocumentMapper;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.collections.CollectionDocumentRegistry;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.service.collections.CollectionTreeRootRegistry;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.service.variable.VariableResolver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class RequestEditorSelectedScopeTest {

    @AfterMethod
    public void clearScopeAndCollectionRoot() {
        RequestExecutionContext.clearCurrentScope();
        CollectionTreeRootRegistry.clear();
        CollectionDocumentRegistry.registerDocumentSupplier(com.laker.postman.collection.model.CollectionDocument::empty);
    }

    @Test
    public void selectingRequestTabShouldRefreshVariableResolutionScopeFromCollectionTree() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RequestGroup group = groupWithVariable("testname", "2222");
            registerCollectionTree(group, request("request-1"));
            RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromGroupVariables(Map.of(
                    "testname", "1111"
            )));

            RequestEditorPanel editorPanel = createRequestEditorPanel();
            JTabbedPane tabbedPane = editorPanel.getTabbedPane();
            tabbedPane.addTab("Other", new JPanel());
            tabbedPane.addTab("Request", requestTab("request-1"));

            tabbedPane.setSelectedIndex(0);
            tabbedPane.setSelectedIndex(1);

            assertEquals(VariableResolver.resolveVariable("testname"), "2222");
        });
    }

    @Test
    public void reselectingRequestTabShouldUseLatestEditedGroupVariables() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RequestGroup group = groupWithVariable("testname", "1111");
            registerCollectionTree(group, request("request-1"));

            RequestEditorPanel editorPanel = createRequestEditorPanel();
            JTabbedPane tabbedPane = editorPanel.getTabbedPane();
            tabbedPane.addTab("Request", requestTab("request-1"));
            tabbedPane.addTab("Other", new JPanel());
            tabbedPane.setSelectedIndex(0);
            assertEquals(VariableResolver.resolveVariable("testname"), "1111");

            group.setVariables(List.of(new Variable(true, "testname", "2222")));
            tabbedPane.setSelectedIndex(1);
            tabbedPane.setSelectedIndex(0);

            assertEquals(VariableResolver.resolveVariable("testname"), "2222");
        });
    }

    private static RequestEditorPanel createRequestEditorPanel() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            RequestEditorPanel panel = new RequestEditorPanel();
            panel.setAutoInitializeSelectedTabOnTabAdd(false);
            panel.setStartupRestoreSelectingLastTab(true);
            panel.initUI();
            return panel;
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static RequestEditSubPanel requestTab(String requestId) {
        return new RequestEditSubPanel(requestId, RequestItemProtocolEnum.HTTP, true);
    }

    private static RequestGroup groupWithVariable(String key, String value) {
        RequestGroup group = new RequestGroup("Group");
        group.setVariables(List.of(new Variable(true, key, value)));
        return group;
    }

    private static HttpRequestItem request(String requestId) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(requestId);
        item.setName("Request");
        item.setMethod("GET");
        item.setProtocol(RequestItemProtocolEnum.HTTP);
        return item;
    }

    private static void registerCollectionTree(RequestGroup group, HttpRequestItem request) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(request));
        rootNode.add(groupNode);
        CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);
        CollectionDocumentRegistry.registerDocumentSupplier(() -> SwingCollectionTreeDocumentMapper.fromRoot(rootNode));
    }
}
