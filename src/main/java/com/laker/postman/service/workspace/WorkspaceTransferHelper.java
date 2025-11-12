package com.laker.postman.service.workspace;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.dialog.WorkspaceSelectionDialog;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 工作区转移辅助类
 * 提供通用的工作区转移流程，减少代码重复
 * 所有转移场景使用统一的国际化消息键
 */
@Slf4j
public class WorkspaceTransferHelper {

    private WorkspaceTransferHelper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 转移数据到其他工作区的通用流程（显示成功消息）
     *
     * @param itemName       要转移的项目名称（用于日志和确认对话框）
     * @param transferAction 实际执行转移的操作 (selectedWorkspace, itemName) -> void
     * @return true 如果转移成功，false 如果用户取消或失败
     */
    public static boolean transferToWorkspace(String itemName, BiConsumer<Workspace, String> transferAction) {
        return transferToWorkspace(itemName, true, transferAction);
    }

    /**
     * 转移数据到其他工作区的通用流程（不显示成功消息）
     *
     * @param itemName       要转移的项目名称（用于日志和确认对话框）
     * @param transferAction 实际执行转移的操作 (selectedWorkspace, itemName) -> void
     * @return true 如果转移成功，false 如果用户取消或失败
     */
    public static boolean transferToWorkspaceQuiet(String itemName, BiConsumer<Workspace, String> transferAction) {
        return transferToWorkspace(itemName, false, transferAction);
    }

    /**
     * 转移数据到其他工作区的核心流程
     *
     * @param itemName           要转移的项目名称（用于日志和确认对话框）
     * @param showSuccessMessage 是否显示成功消息
     * @param transferAction     实际执行转移的操作 (selectedWorkspace, itemName) -> void
     * @return true 如果转移成功，false 如果用户取消或失败
     */
    private static boolean transferToWorkspace(
            String itemName,
            boolean showSuccessMessage,
            BiConsumer<Workspace, String> transferAction) {

        try {
            // 1. 获取可用的工作区列表
            List<Workspace> availableWorkspaces = getAvailableWorkspaces();
            if (availableWorkspaces.isEmpty()) {
                showNoWorkspaceAvailableMessage();
                return false;
            }

            // 2. 显示工作区选择对话框
            String dialogTitle = I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_SELECT_DIALOG_TITLE);
            Workspace selectedWorkspace = showWorkspaceSelectionDialog(dialogTitle, availableWorkspaces);
            if (selectedWorkspace == null) {
                return false; // 用户取消
            }

            // 3. 确认转移操作
            if (!confirmTransfer(itemName, selectedWorkspace.getName())) {
                return false; // 用户取消
            }

            // 4. 执行转移操作
            transferAction.accept(selectedWorkspace, itemName);

            // 5. 显示成功消息（如果需要）
            if (showSuccessMessage) {
                showSuccessMessage(selectedWorkspace.getName());
            }

            log.info("Successfully transferred '{}' to workspace '{}'", itemName, selectedWorkspace.getName());
            return true;

        } catch (Exception ex) {
            log.error("Transfer to workspace failed for '{}'", itemName, ex);
            showErrorMessage(ex.getMessage());
            return false;
        }
    }

    /**
     * 获取所有可用的工作区（排除当前工作区）
     */
    private static List<Workspace> getAvailableWorkspaces() {
        WorkspaceService workspaceService = WorkspaceService.getInstance();
        List<Workspace> allWorkspaces = workspaceService.getAllWorkspaces();
        Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

        return allWorkspaces.stream()
                .filter(w -> currentWorkspace == null || !w.getId().equals(currentWorkspace.getId()))
                .toList();
    }

    /**
     * 显示工作区选择对话框
     */
    private static Workspace showWorkspaceSelectionDialog(String title, List<Workspace> workspaces) {
        WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(title, workspaces);
        return dialog.showDialog();
    }

    /**
     * 确认转移操作
     */
    private static boolean confirmTransfer(String itemName, String targetWorkspaceName) {
        int confirm = JOptionPane.showConfirmDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_CONFIRM_MESSAGE, itemName, targetWorkspaceName),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return confirm == JOptionPane.YES_OPTION;
    }

    /**
     * 显示没有其他可用工作区的消息
     */
    private static void showNoWorkspaceAvailableMessage() {
        JOptionPane.showMessageDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_NO_OTHER_AVAILABLE),
                I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 显示成功消息
     */
    private static void showSuccessMessage(String workspaceName) {
        JOptionPane.showMessageDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_SUCCESS, workspaceName),
                I18nUtil.getMessage(MessageKeys.SUCCESS),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示错误消息
     */
    private static void showErrorMessage(String errorDetails) {
        JOptionPane.showMessageDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_FAIL, errorDetails),
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE);
    }

}