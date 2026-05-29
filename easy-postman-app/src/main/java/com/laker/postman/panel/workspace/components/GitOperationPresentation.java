package com.laker.postman.panel.workspace.components;

import com.laker.postman.model.GitOperation;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
public class GitOperationPresentation {

    public String getIconName(GitOperation operation) {
        return switch (operation) {
            case COMMIT -> "icons/git-commit.svg";
            case PUSH -> "icons/git-push.svg";
            case PULL -> "icons/git-pull.svg";
        };
    }

    public Color getColor(GitOperation operation) {
        return switch (operation) {
            case COMMIT -> new Color(34, 197, 94);
            case PUSH -> new Color(59, 130, 246);
            case PULL -> new Color(168, 85, 247);
        };
    }
}
