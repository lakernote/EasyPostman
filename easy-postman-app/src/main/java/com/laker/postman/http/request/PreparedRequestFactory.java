package com.laker.postman.http.request;

import com.laker.postman.http.runtime.mapper.PreparedRequestMapper;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.collections.InheritanceService;
import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;

/**
 * App-side request preparation facade.
 * Applies collection inheritance, then delegates DTO mapping to the headless HTTP runtime mapper.
 */
@UtilityClass
public class PreparedRequestFactory {

    private static final InheritanceService inheritanceService = new InheritanceService();

    public static void invalidateCache() {
        inheritanceService.invalidateCache();
    }

    public static void invalidateCacheForRequest(String requestId) {
        inheritanceService.invalidateCache(requestId);
    }

    public static PreparedRequest build(HttpRequestItem item) {
        return build(item, true);
    }

    public static PreparedRequest build(HttpRequestItem item, boolean useCache) {
        return buildEffectiveItem(resolveEffectiveItem(item, useCache));
    }

    public static PreparedRequest buildWithoutInheritance(HttpRequestItem item) {
        return buildEffectiveItem(item);
    }

    public static PreparedRequestMapper.DeferredAuthorization resolveDeferredAuthorization(HttpRequestItem item,
                                                                                           boolean useCache) {
        return resolveDeferredAuthorizationForEffectiveItem(resolveEffectiveItem(item, useCache));
    }

    public static PreparedRequestMapper.DeferredAuthorization resolveDeferredAuthorizationWithoutInheritance(HttpRequestItem item) {
        return resolveDeferredAuthorizationForEffectiveItem(item);
    }

    private static PreparedRequest buildEffectiveItem(HttpRequestItem effectiveItem) {
        return PreparedRequestMapper.map(effectiveItem, VariableResolver::resolve);
    }

    private static PreparedRequestMapper.DeferredAuthorization resolveDeferredAuthorizationForEffectiveItem(HttpRequestItem effectiveItem) {
        return PreparedRequestMapper.resolveDeferredAuthorization(effectiveItem, VariableResolver::resolve);
    }

    private static HttpRequestItem resolveEffectiveItem(HttpRequestItem item, boolean useCache) {
        return inheritanceService.applyInheritance(item, useCache);
    }
}
