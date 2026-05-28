package com.laker.postman.performance.core.extractor;

public class ExtractorData {
    public String type = ExtractorType.JSON_PATH.getStorageValue();
    public String expression = "";
    public String variableName = "";
    public String defaultValue = "";
    public int matchIndex = 1;
    public int groupIndex = 1;
}
