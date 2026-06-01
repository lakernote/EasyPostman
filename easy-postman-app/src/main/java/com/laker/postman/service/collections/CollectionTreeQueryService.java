package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class CollectionTreeQueryService {

    public static HttpRequestItem findLastPersistedRequest(List<HttpRequestItem> requestItems) {
        if (requestItems == null || requestItems.isEmpty()) {
            return null;
        }
        for (int i = requestItems.size() - 1; i >= 0; i--) {
            HttpRequestItem item = requestItems.get(i);
            if (!item.isNewRequest()) {
                return item;
            }
        }
        return null;
    }
}
