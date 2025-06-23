package com.laker.postman.panel.jmeter;

import cn.hutool.core.date.DateUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;

import java.util.Date;

// 结果树节点信息
public class ResultNodeInfo {
    String name;
    boolean success;
    String detail;
    PreparedRequest req;
    HttpResponse resp;

    ResultNodeInfo(String name, boolean success, String detail, PreparedRequest req, HttpResponse resp) {
        this.name = name;
        this.success = success;
        this.detail = detail;
        this.req = req;
        this.resp = resp;
    }

    @Override
    public String toString() {
        return DateUtil.format(new Date(), "HH:mm:ss.SSS") + " " + name;
    }
}