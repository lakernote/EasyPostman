package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Git working tree file change metadata for diff views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitFileChange {
    private String path;
    private Type type;

    public enum Type {
        ADDED,
        MODIFIED,
        DELETED,
        UNTRACKED,
        CONFLICTING
    }
}
