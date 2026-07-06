package com.laker.postman.service.collections;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.service.variable.VariableResolver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class CollectionRequestExecutionScopeResolverTest {

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        RequestExecutionContext.clearCurrentScope();
        CollectionTreeRootRegistry.clear();
    }

    @Test
    public void shouldResolveLatestScopeFromActiveCollectionTree() {
        HttpRequestItem item = request("request-latest");
        registerCollectionRequest(item, List.of(new Variable(true, "testname", "888")));

        var scope = CollectionRequestExecutionScopeResolver.resolveCurrentScope(item);

        assertTrue(scope.isPresent());
        assertEquals(scope.get().getGroupVariable("testname"), "888");
    }

    @Test
    public void shouldReturnPresentEmptyScopeWhenRequestExistsWithoutGroupVariables() {
        HttpRequestItem item = request("request-empty");
        registerCollectionRequest(item, List.of());

        var scope = CollectionRequestExecutionScopeResolver.resolveCurrentScope(item.getId());

        assertTrue(scope.isPresent());
        assertTrue(scope.get().getGroupVariables().isEmpty());
    }

    @Test
    public void syncCurrentScopeShouldOverwriteStaleVariableWhenCollectionRequestExists() {
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromGroupVariables(Map.of("testname", "333")));
        HttpRequestItem item = request("request-sync");
        registerCollectionRequest(item, List.of(new Variable(true, "testname", "888")));

        assertTrue(CollectionRequestExecutionScopeResolver.syncCurrentScope(item));

        assertEquals(VariableResolver.resolveVariable("testname"), "888");
    }

    @Test
    public void syncCurrentScopeOrEmptyShouldClearStaleScopeWhenRequestIsMissing() {
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromGroupVariables(Map.of("testname", "333")));

        CollectionRequestExecutionScopeResolver.syncCurrentScopeOrEmpty("missing");

        assertNull(VariableResolver.resolveVariable("testname"));
    }

    @Test
    public void syncCurrentScopeShouldPreserveCallerScopeWhenRequestIsMissing() {
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromGroupVariables(Map.of("testname", "333")));

        assertFalse(CollectionRequestExecutionScopeResolver.syncCurrentScope("missing"));

        assertEquals(VariableResolver.resolveVariable("testname"), "333");
    }

    private static HttpRequestItem request(String id) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(id);
        return item;
    }

    private static void registerCollectionRequest(HttpRequestItem item, List<Variable> variables) {
        RequestGroup group = new RequestGroup("Group");
        group.setVariables(variables);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(item));
        rootNode.add(groupNode);
        CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);
    }
}
