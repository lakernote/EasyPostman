package com.laker.postman.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestBodyTypes {
    public static final String BODY_TYPE_NONE = "none";
    public static final String BODY_TYPE_FORM_DATA = "form-data";
    public static final String BODY_TYPE_FORM_URLENCODED = "x-www-form-urlencoded";
    public static final String BODY_TYPE_RAW = "raw";

    public static final String RAW_TYPE_JSON = "JSON";
    public static final String RAW_TYPE_TEXT = "Text";
    public static final String RAW_TYPE_XML = "XML";
}
