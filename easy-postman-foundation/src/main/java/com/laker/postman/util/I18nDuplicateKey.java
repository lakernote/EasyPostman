package com.laker.postman.util;

import java.nio.file.Path;
import java.util.List;

public record I18nDuplicateKey(Path resourceFile, String key, List<Integer> lineNumbers) {

    public I18nDuplicateKey {
        lineNumbers = List.copyOf(lineNumbers);
    }
}
