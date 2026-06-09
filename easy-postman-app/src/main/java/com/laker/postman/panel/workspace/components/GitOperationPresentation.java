package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.constants.ModernColors;
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
            case COMMIT -> ModernColors.getGitCommit();
            case PUSH -> ModernColors.getGitPush();
            case PULL -> ModernColors.getGitPull();
        };
    }
}
