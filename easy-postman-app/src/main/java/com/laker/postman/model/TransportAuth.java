package com.laker.postman.model;

public class TransportAuth {
    public String type;
    public String username;
    public String password;

    public TransportAuth(String type, String username, String password) {
        this.type = type;
        this.username = username;
        this.password = password;
    }

    public boolean isDigest() {
        return AuthType.DIGEST.getConstant().equals(type);
    }

    public TransportAuth shallowCopy() {
        return new TransportAuth(type, username, password);
    }
}
