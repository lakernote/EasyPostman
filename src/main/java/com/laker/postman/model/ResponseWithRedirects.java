package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;

public class ResponseWithRedirects {
    public HttpResponse finalResponse;
    public List<RedirectInfo> redirects = new ArrayList<>();
}