package com.laker.postman.collection;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;

public class CollectionInheritanceTest {

    @Test
    public void shouldApplyGroupChainWithoutMutatingRequest() {
        RequestGroup outer = group("Outer", "Bearer Token", "outer-token", "x-trace", "outer");
        outer.setPrescript("outerPre();");
        outer.setPostscript("outerPost();");

        RequestGroup inner = group("Inner", AuthType.INHERIT.getConstant(), "", "x-trace", "inner");
        inner.setPrescript("innerPre();");
        inner.setPostscript("innerPost();");

        HttpRequestItem request = new HttpRequestItem();
        request.setName("Get Orders");
        request.setAuthType(AuthType.INHERIT.getConstant());
        request.setPrescript("requestPre();");
        request.setPostscript("requestPost();");
        request.setHeadersList(List.of(header("x-trace", "request"), header("accept", "application/json")));

        HttpRequestItem merged = CollectionInheritance.apply(request, List.of(outer, inner));

        assertNotSame(merged, request);
        assertEquals(merged.getAuthType(), "Bearer Token");
        assertEquals(merged.getAuthToken(), "outer-token");
        assertEquals(merged.getHeadersList().size(), 2);
        assertEquals(merged.getHeadersList().get(0).getValue(), "request");
        assertEquals(merged.getHeadersList().get(1).getKey(), "accept");
        assertEquals(merged.getPrescript(), """
                // === Outer PreScript ===

                outerPre();

                // === Inner PreScript ===

                innerPre();

                // === 请求级脚本 ===

                requestPre();""");
        assertEquals(merged.getPostscript(), """
                // === 请求级脚本 ===

                requestPost();

                // === Inner PostScript ===

                innerPost();

                // === Outer PostScript ===

                outerPost();""");
        assertEquals(request.getAuthType(), AuthType.INHERIT.getConstant());
    }

    @Test
    public void shouldMergeGroupVariablesWithInnerGroupWinning() {
        RequestGroup outer = new RequestGroup("Outer");
        outer.setVariables(List.of(variable("tenant", "outer"), variable("region", "us")));
        RequestGroup inner = new RequestGroup("Inner");
        inner.setVariables(List.of(variable("tenant", "inner")));

        List<Variable> variables = CollectionInheritance.mergeGroupVariables(List.of(outer, inner));

        assertEquals(variables.size(), 2);
        assertEquals(variables.get(0).getKey(), "tenant");
        assertEquals(variables.get(0).getValue(), "inner");
        assertEquals(variables.get(1).getKey(), "region");
    }

    @Test
    public void shouldMergeOnlyEnabledVariablesForExecutionWithoutChangingEditableMerge() {
        RequestGroup outer = new RequestGroup("Outer");
        outer.setVariables(List.of(variable("tenant", "outer"), variable("region", "us")));
        RequestGroup inner = new RequestGroup("Inner");
        Variable disabledTenant = variable("tenant", "disabled-inner");
        disabledTenant.setEnabled(false);
        inner.setVariables(List.of(disabledTenant, variable("role", "admin")));

        List<Variable> editableVariables = CollectionInheritance.mergeGroupVariables(List.of(outer, inner));
        List<Variable> executableVariables = CollectionInheritance.mergeEnabledGroupVariables(List.of(outer, inner));

        assertEquals(editableVariables.size(), 3);
        assertEquals(editableVariables.get(0).getValue(), "disabled-inner");
        assertEquals(executableVariables.size(), 3);
        assertEquals(executableVariables.get(0).getValue(), "outer");
        assertEquals(executableVariables.get(1).getValue(), "us");
        assertEquals(executableVariables.get(2).getValue(), "admin");
    }

    @Test
    public void shouldReturnOriginalRequestWhenNoGroupsExist() {
        HttpRequestItem request = new HttpRequestItem();

        HttpRequestItem merged = CollectionInheritance.apply(request, List.of());

        assertEquals(merged, request);
        assertNull(CollectionInheritance.apply(null, List.of()));
    }

    @Test
    public void shouldPreserveRequestLevelTransportSettingsWhenApplyingInheritanceChain() {
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-1");
        request.setName("Request 1");
        request.setMethod("GET");
        request.setUrl("https://api.example.com/data");
        request.setFollowRedirects(Boolean.FALSE);
        request.setCookieJarEnabled(Boolean.FALSE);
        request.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_1_1);
        request.setRequestTimeoutMs(4321);

        HttpRequestItem merged = CollectionInheritance.apply(request, List.of(new RequestGroup("Parent")));

        assertEquals(merged.getFollowRedirects(), Boolean.FALSE);
        assertEquals(merged.getCookieJarEnabled(), Boolean.FALSE);
        assertEquals(merged.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_1_1);
        assertEquals(merged.getRequestTimeoutMs(), Integer.valueOf(4321));
    }

    private static RequestGroup group(String name, String authType, String token, String headerKey, String headerValue) {
        RequestGroup group = new RequestGroup(name);
        group.setAuthType(authType);
        group.setAuthToken(token);
        group.setHeaders(List.of(header(headerKey, headerValue)));
        return group;
    }

    private static HttpHeader header(String key, String value) {
        HttpHeader header = new HttpHeader();
        header.setKey(key);
        header.setValue(value);
        return header;
    }

    private static Variable variable(String key, String value) {
        Variable variable = new Variable();
        variable.setKey(key);
        variable.setValue(value);
        return variable;
    }
}
