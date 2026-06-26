package com.laker.postman.request.compare;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class HttpRequestDirtyComparator {

    public static boolean isDirty(HttpRequestItem original,
                                  HttpRequestItem current,
                                  List<HttpHeader> generatedDefaultHeaders) {
        if (original == current) {
            return false;
        }
        if (original == null || current == null) {
            return true;
        }

        HttpRequestEditNormalizer.NormalizedRequest normalizedOriginal =
                HttpRequestEditNormalizer.original(original);
        HttpRequestEditNormalizer.NormalizedRequest normalizedCurrent =
                HttpRequestEditNormalizer.currentComparedToOriginal(
                        current,
                        normalizedOriginal,
                        generatedDefaultHeaders
                );

        HttpRequestEditSnapshot originalSnapshot = HttpRequestEditSnapshot.from(normalizedOriginal);
        HttpRequestEditSnapshot currentSnapshot = HttpRequestEditSnapshot.from(normalizedCurrent);
        return !originalSnapshot.equals(currentSnapshot);
    }
}
