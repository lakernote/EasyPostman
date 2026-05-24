package com.laker.postman.panel.performance.assertion;

public class AssertionData {
    public String type = AssertionType.RESPONSE_CODE.getStorageValue();
    public String content = "";
    public String operator = "=";
    public String value = "200";
}
