package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Git 操作结果封装。
 */
public class GitOperationResult {
    public boolean success = false;
    public String message = "";
    public List<String> affectedFiles = new ArrayList<>();
    public String operationType = "";
    public String details = "";
}
