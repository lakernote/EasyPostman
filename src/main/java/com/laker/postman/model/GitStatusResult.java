package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Git状态结果封装
 */
public class GitStatusResult {
    public List<String> added = new ArrayList<>();
    public List<String> changed = new ArrayList<>();
    public List<String> modified = new ArrayList<>();
    public List<String> missing = new ArrayList<>();
    public List<String> removed = new ArrayList<>();
    public List<String> untracked = new ArrayList<>();
    public List<String> uncommitted = new ArrayList<>();
}