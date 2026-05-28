package com.laker.postman.panel.performance.plan;

import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.panel.performance.PerformanceTreeSnapshot;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.request.PerformanceAuthType;
import com.laker.postman.panel.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.service.collections.CollectionTreeRootRegistry;
import com.laker.postman.service.variable.RequestExecutionScope;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PerformancePlanDocumentCompilerTest {

    @Test
    public void documentCompilerShouldNotExposeSwingTreeTypes() {
        assertFalse(hasParameterType(PerformancePlanDocumentCompiler.class, DefaultMutableTreeNode.class));
    }

    @Test
    public void shouldCompileRuntimePlanFromPureDocument() {
        LoopData loopData = new LoopData();
        loopData.iterations = 2;

        PerformancePlanNode request = PerformancePlanNode.builder()
                .name("request")
                .type(NodeType.REQUEST)
                .httpRequestItem(requestItem("request"))
                .build();
        PerformancePlanNode loop = PerformancePlanNode.builder()
                .name("loop")
                .type(NodeType.LOOP)
                .loopData(loopData)
                .children(List.of(request))
                .build();
        PerformancePlanNode disabledGroup = PerformancePlanNode.builder()
                .name("disabled group")
                .type(NodeType.THREAD_GROUP)
                .enabled(false)
                .threadGroupData(threadGroupData())
                .children(List.of(request))
                .build();
        PerformancePlanNode enabledGroup = PerformancePlanNode.builder()
                .name("enabled group")
                .type(NodeType.THREAD_GROUP)
                .threadGroupData(threadGroupData())
                .children(List.of(loop))
                .build();
        PerformancePlanDocument document = new PerformancePlanDocument(
                PerformancePlanNode.builder()
                        .name("plan")
                        .type(NodeType.ROOT)
                        .children(List.of(disabledGroup, enabledGroup))
                        .build()
        );

        PerformanceTestPlan plan = PerformancePlanDocumentCompiler.compile(document);

        assertEquals(plan.getThreadGroups().size(), 1);
        PerformanceThreadGroupPlan group = plan.getThreadGroups().get(0);
        assertEquals(group.getName(), "enabled group");
        assertEquals(group.getElements().size(), 1);
        assertTrue(group.getElements().get(0) instanceof PerformanceLoopController);
        PerformanceLoopController loopController = (PerformanceLoopController) group.getElements().get(0);
        assertEquals(loopController.getIterationCount(), 2);
        assertTrue(loopController.getElements().get(0) instanceof PerformanceRequestSampler);
    }

    @Test
    public void swingPlanAdapterShouldSnapshotRequestExecutionScopeFromCollectionTree() {
        HttpRequestItem collectionRequest = requestItem("scoped-request");
        collectionRequest.setAuthType(AuthType.INHERIT.getConstant());
        collectionRequest.setPrescript("pm.variables.set('requestPre', 'yes');");
        collectionRequest.setPostscript("pm.variables.set('requestPost', 'yes');");
        RequestGroup group = new RequestGroup("tenant group");
        group.setAuthType(AuthType.BEARER.getConstant());
        group.setAuthToken("group-token");
        group.setPrescript("pm.variables.set('groupPre', 'yes');");
        group.setPostscript("pm.variables.set('groupPost', 'yes');");
        group.setHeaders(new ArrayList<>(List.of(new HttpHeader(true, "X-Group", "group-value"))));
        group.setVariables(new ArrayList<>(List.of(new Variable(true, "tenantId", "collection-tenant"))));
        DefaultMutableTreeNode collectionRoot = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(collectionRequest));
        collectionRoot.add(groupNode);

        CollectionTreeRootRegistry.registerRootSupplier(() -> collectionRoot);
        try {
            DefaultMutableTreeNode performanceRequestNode = new DefaultMutableTreeNode(
                    new PerformanceTreeNode(
                            collectionRequest.getName(),
                            NodeType.REQUEST,
                            collectionRequest
                    )
            );

            PerformanceRequestSampler sampler = PerformancePlanDocumentCompiler.compileRequestSampler(
                    PerformanceSwingTreePlanAdapter.toDocumentNode(performanceRequestNode)
            );
            HttpRequestItem samplerRequest = sampler.getHttpRequestItem();

            assertEquals(sampler.getRequestExecutionScope().getGroupVariable("tenantId"), "collection-tenant");
            assertEquals(sampler.getRequestSnapshot().getExecutionScope().getGroupVariable("tenantId"), "collection-tenant");
            assertEquals(sampler.getRequestSnapshot().getAuthType(), PerformanceAuthType.BEARER);
            assertTrue(sampler.getRequestSnapshot().getHeaders().stream()
                    .anyMatch(header -> "X-Group".equals(header.getKey()) && "group-value".equals(header.getValue())));
            assertEquals(samplerRequest.getAuthType(), AuthType.BEARER.getConstant());
            assertEquals(samplerRequest.getAuthToken(), "group-token");
            assertTrue(samplerRequest.getHeadersList().stream()
                    .anyMatch(header -> "X-Group".equals(header.getKey()) && "group-value".equals(header.getValue())));
            assertTrue(samplerRequest.getPrescript().contains("groupPre"));
            assertTrue(samplerRequest.getPrescript().contains("requestPre"));
            assertTrue(samplerRequest.getPostscript().contains("groupPost"));
            assertTrue(samplerRequest.getPostscript().contains("requestPost"));
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }

    @Test
    public void swingPlanAdapterShouldNotApplyInheritanceTwiceForLoadedSnapshot() {
        HttpRequestItem collectionRequest = requestItem("loaded-snapshot-request");
        collectionRequest.setPrescript("pm.variables.set('requestPre', 'yes');");
        RequestGroup group = new RequestGroup("snapshot group");
        group.setPrescript("pm.variables.set('groupPre', 'yes');");
        DefaultMutableTreeNode collectionRoot = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(collectionRequest));
        collectionRoot.add(groupNode);

        CollectionTreeRootRegistry.registerRootSupplier(() -> collectionRoot);
        try {
            DefaultMutableTreeNode performanceRequestNode = new DefaultMutableTreeNode(
                    new PerformanceTreeNode(
                            collectionRequest.getName(),
                            NodeType.REQUEST,
                            collectionRequest
                    )
            );
            PerformancePlanNode firstSnapshot = PerformanceSwingTreePlanAdapter.toDocumentNode(performanceRequestNode);
            DefaultMutableTreeNode loadedTreeNode = PerformanceSwingTreePlanAdapter.toTreeNode(firstSnapshot);

            PerformancePlanNode secondSnapshot = PerformanceSwingTreePlanAdapter.toDocumentNode(loadedTreeNode);

            assertEquals(countOccurrences(secondSnapshot.getHttpRequestItem().getPrescript(), "groupPre"), 1);
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }

    @Test
    public void swingPlanAdapterShouldNotApplyInheritanceTwiceAfterExecutionSnapshotCopy() {
        HttpRequestItem collectionRequest = requestItem("execution-snapshot-request");
        collectionRequest.setPrescript("pm.variables.set('requestPre', 'yes');");
        RequestGroup group = new RequestGroup("snapshot group");
        group.setPrescript("pm.variables.set('groupPre', 'yes');");
        DefaultMutableTreeNode collectionRoot = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(collectionRequest));
        collectionRoot.add(groupNode);

        CollectionTreeRootRegistry.registerRootSupplier(() -> collectionRoot);
        try {
            DefaultMutableTreeNode performanceRequestNode = new DefaultMutableTreeNode(
                    new PerformanceTreeNode(
                            collectionRequest.getName(),
                            NodeType.REQUEST,
                            collectionRequest
                    )
            );
            PerformancePlanNode firstSnapshot = PerformanceSwingTreePlanAdapter.toDocumentNode(performanceRequestNode);
            DefaultMutableTreeNode loadedTreeNode = PerformanceSwingTreePlanAdapter.toTreeNode(firstSnapshot);
            DefaultMutableTreeNode executionSnapshot = PerformanceTreeSnapshot.copy(loadedTreeNode);

            PerformancePlanNode secondSnapshot = PerformanceSwingTreePlanAdapter.toDocumentNode(executionSnapshot);

            assertEquals(countOccurrences(secondSnapshot.getHttpRequestItem().getPrescript(), "groupPre"), 1);
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }

    @Test
    public void swingPlanAdapterShouldPreserveLoadedRequestExecutionScope() {
        PerformancePlanNode loadedRequest = PerformancePlanNode.builder()
                .name("Loaded Request")
                .type(NodeType.REQUEST)
                .httpRequestItem(requestItem("loaded-scope-request"))
                .requestExecutionScope(RequestExecutionScope.fromGroupVariables(Map.of("tenantId", "loaded-tenant")))
                .requestInheritanceSnapshot(true)
                .build();

        DefaultMutableTreeNode treeNode = PerformanceSwingTreePlanAdapter.toTreeNode(loadedRequest);
        PerformancePlanNode roundTripped = PerformanceSwingTreePlanAdapter.toDocumentNode(treeNode);

        assertNotNull(roundTripped.getRequestExecutionScope());
        assertEquals(roundTripped.getRequestExecutionScope().getGroupVariable("tenantId"), "loaded-tenant");
        assertNotNull(roundTripped.getRequestSnapshot());
        assertEquals(roundTripped.getRequestSnapshot().getExecutionScope().getGroupVariable("tenantId"), "loaded-tenant");
    }

    @Test
    public void swingPlanAdapterShouldPreserveLoadedRequestExecutionScopeAfterExecutionSnapshotCopy() {
        PerformancePlanNode loadedRequest = PerformancePlanNode.builder()
                .name("Loaded Request")
                .type(NodeType.REQUEST)
                .httpRequestItem(requestItem("loaded-scope-copy-request"))
                .requestExecutionScope(RequestExecutionScope.fromGroupVariables(Map.of("tenantId", "copied-tenant")))
                .requestInheritanceSnapshot(true)
                .build();

        DefaultMutableTreeNode treeNode = PerformanceSwingTreePlanAdapter.toTreeNode(loadedRequest);
        DefaultMutableTreeNode executionSnapshot = PerformanceTreeSnapshot.copy(treeNode);
        PerformancePlanNode roundTripped = PerformanceSwingTreePlanAdapter.toDocumentNode(executionSnapshot);

        assertNotNull(roundTripped.getRequestExecutionScope());
        assertEquals(roundTripped.getRequestExecutionScope().getGroupVariable("tenantId"), "copied-tenant");
    }

    private static boolean hasParameterType(Class<?> type, Class<?> parameterType) {
        boolean methodParameter = java.util.Arrays.stream(type.getMethods())
                .flatMap(method -> java.util.Arrays.stream(method.getParameterTypes()))
                .anyMatch(parameterType::equals);
        boolean constructorParameter = java.util.Arrays.stream(type.getConstructors())
                .flatMap(constructor -> java.util.Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(parameterType::equals);
        return methodParameter || constructorParameter;
    }

    private static ThreadGroupData threadGroupData() {
        ThreadGroupData data = new ThreadGroupData();
        data.numThreads = 1;
        data.useTime = false;
        data.loops = 1;
        return data;
    }

    private static HttpRequestItem requestItem(String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(name + "-id");
        item.setName(name);
        item.setProtocol(RequestItemProtocolEnum.HTTP);
        return item;
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while (value != null && (index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
