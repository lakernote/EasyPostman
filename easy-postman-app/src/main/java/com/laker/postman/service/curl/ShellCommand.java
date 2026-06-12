package com.laker.postman.service.curl;

import java.util.List;

record ShellCommand(List<String> argv, List<CurlParseWarning> warnings) {
}
