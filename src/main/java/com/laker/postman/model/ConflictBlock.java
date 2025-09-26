package com.laker.postman.model;

import lombok.Getter;

import java.util.List;

@Getter
public class ConflictBlock {
    public int begin;
    public int end;
    public List<String> baseLines;
    public List<String> localLines;
    public List<String> remoteLines;

    public ConflictBlock(int begin, int end, List<String> baseLines, List<String> localLines, List<String> remoteLines) {
        this.begin = begin;
        this.end = end;
        this.baseLines = baseLines;
        this.localLines = localLines;
        this.remoteLines = remoteLines;
    }

}