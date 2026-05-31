package com.laker.postman.request.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestAuthTypes {
    public static final String AUTH_TYPE_INHERIT = AuthType.INHERIT.getConstant();
    public static final String AUTH_TYPE_NONE = AuthType.NONE.getConstant();
    public static final String AUTH_TYPE_BASIC = AuthType.BASIC.getConstant();
    public static final String AUTH_TYPE_BEARER = AuthType.BEARER.getConstant();
    public static final String AUTH_TYPE_DIGEST = AuthType.DIGEST.getConstant();
}
