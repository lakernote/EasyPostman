package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpRequestSaveMerger {

    public HttpRequestItem mergeExisting(HttpRequestItem persisted, HttpRequestItem edited) {
        if (edited == null) {
            return null;
        }
        if (persisted == null) {
            return edited;
        }

        edited.setName(persisted.getName());
        if (persisted.getResponse() != null && !persisted.getResponse().isEmpty()) {
            edited.setResponse(persisted.getResponse());
        }
        return edited;
    }
}
