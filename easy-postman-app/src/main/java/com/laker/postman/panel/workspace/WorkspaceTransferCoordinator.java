package com.laker.postman.panel.workspace;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.dialog.WorkspaceSelectionDialog;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 工作区转移 UI 协调器。
 * 提供通用的工作区选择、确认和错误提示流程，减少面板层重复代码。
 */
@Slf4j
@UtilityClass
public class WorkspaceTransferCoordinator {

    /**
     * 转移数据到其他工作区的通用流程（显示成功消息）
     *
     * @param itemName       要转移的项目名称（用于日志和确认对话框）
     * @param transferAction 实际执行转移的操作 (selectedWorkspace, itemName) -> void
     */
    public static void transferToWorkspace(String itemName, BiConsumer<Workspace, String> transferAction) {
        try {
            List<Workspace> availableWorkspaces = getAvailableWorkspaces();
            if (availableWorkspaces.isEmpty()) {
                showNoWorkspaceAvailableMessage();
                return;
            }

            String dialogTitle = I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_SELECT_DIALOG_TITLE);
            Workspace selectedWorkspace = showWorkspaceSelectionDialog(dialogTitle, availableWorkspaces);
            if (selectedWorkspace == null) {
                return;
            }

            if (!confirmTransfer(itemName, selectedWorkspace.getName())) {
                return;
            }

            transferAction.accept(selectedWorkspace, itemName);
            log.info("Successfully transferred '{}' to workspace '{}'", itemName, selectedWorkspace.getName());

        } catch (Exception ex) {
            log.error("Transfer to workspace failed for '{}'", itemName, ex);
            showErrorMessage(ex.getMessage());
        }
    }

    private static List<Workspace> getAvailableWorkspaces() {
        WorkspaceService workspaceService = WorkspaceService.getInstance();
        List<Workspace> allWorkspaces = workspaceService.getAllWorkspaces();
        Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

        return allWorkspaces.stream()
                .filter(w -> currentWorkspace == null || !w.getId().equals(currentWorkspace.getId()))
                .toList();
    }

    private static Workspace showWorkspaceSelectionDialog(String title, List<Workspace> workspaces) {
        WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(title, workspaces);
        return dialog.showDialog();
    }

    private static boolean confirmTransfer(String itemName, String targetWorkspaceName) {
        int confirm = JOptionPane.showConfirmDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_CONFIRM_MESSAGE, itemName, targetWorkspaceName),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return confirm == JOptionPane.YES_OPTION;
    }

    private static void showNoWorkspaceAvailableMessage() {
        JOptionPane.showMessageDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_NO_OTHER_AVAILABLE),
                I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                JOptionPane.WARNING_MESSAGE);
    }

    private static void showErrorMessage(String errorDetails) {
        JOptionPane.showMessageDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_FAIL, errorDetails),
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE);
    }
}
