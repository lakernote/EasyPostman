package com.laker.postman.plugin.capture;

record CaptureSettings(String bindHost, int bindPort, boolean syncSystemProxy, String hostFilter) {
}
