package com.laker.postman.service;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.*;
import com.laker.postman.plugin.bridge.GitPluginService;
import com.laker.postman.plugin.bridge.GitPluginServices;
import com.laker.postman.util.SystemUtil;
import com.laker.postman.util.WorkspaceStorageUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 工作区服务类
 * 负责工作区的创建、管理、Git操作等核心功能
 */
@Slf4j
public class WorkspaceService {

    private static final String WORKSPACE_NOT_FOUND_MSG = "Workspace not found: ";

    private static WorkspaceService instance;
    private List<Workspace> workspaces = new ArrayList<>();
    @Getter
    private Workspace currentWorkspace;

    private WorkspaceService() {
        loadWorkspaces();
        migrateDefaultWorkspaceIfNeeded();
    }

    public static synchronized WorkspaceService getInstance() {
        if (instance == null) {
            instance = new WorkspaceService();
        }
        return instance;
    }

    /**
     * 获取所有工作区
     */
    public List<Workspace> getAllWorkspaces() {
        return new ArrayList<>(workspaces);
    }

    /**
     * 创建新工作区
     */
    public void createWorkspace(Workspace workspace) throws Exception {
        if (WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            throw new IllegalArgumentException("Cannot create the default workspace");
        }
        validateWorkspace(workspace);

        // 生成唯一ID
        workspace.setId(UUID.randomUUID().toString());
        workspace.setCreatedAt(System.currentTimeMillis());
        workspace.setUpdatedAt(System.currentTimeMillis());

        // 对于非Git克隆模式，创建本地目录
        if (workspace.getType() != WorkspaceType.GIT || workspace.getGitRepoSource() != GitRepoSource.CLONED) {
            Path workspacePath = Paths.get(workspace.getPath());
            if (!Files.exists(workspacePath)) {
                Files.createDirectories(workspacePath);
            }
        }

        // 根据工作区类型执行相应操作
        if (workspace.getType() == WorkspaceType.GIT) {
            handleGitWorkspace(workspace);
        }

        // 添加到工作区列表并保存
        workspaces.add(workspace);
        saveWorkspaces();

        log.info("Created workspace: {} ({})", workspace.getName(), workspace.getType());
    }

    /**
     * 处理Git工作区创建
     */
    private void handleGitWorkspace(Workspace workspace) throws Exception {
        requireGitService().prepareGitWorkspace(workspace);
    }

    /**
     * 版本兼容迁移：将旧版默认工作区数据从根目录迁移到 workspaces/default/
     *
     * <h3>旧版结构（v1）</h3>
     * <pre>
     *   ~/EasyPostman/
     *       collections.json      ← 默认工作区数据（旧位置）
     *       environments.json     ← 默认工作区数据（旧位置）
     *       README.md             ← 可选，旧位置
     *       .git/                 ← 若用户曾 git init（旧位置，不迁移）
     *       .gitignore            ← 旧白名单模式（不迁移，新目录用标准模式）
     *       workspaces/
     *           ws1/
     * </pre>
     *
     * <h3>新版结构（v2+）</h3>
     * <pre>
     *   ~/EasyPostman/
     *       workspaces/
     *           default/          ← 默认工作区（与其他工作区完全平级）
     *               collections.json
     *               environments.json
     *           ws1/
     * </pre>
     *
     * <h3>迁移策略</h3>
     * <ul>
     *   <li><b>collections.json / environments.json / README.md</b>：有旧无新则复制，幂等</li>
     *   <li><b>.git/</b>：绝不迁移。Git 仓库的 working tree 路径与目录绑定，
     *       直接复制会导致所有文件显示为 deleted/missing。
     *       若旧目录是 Git 工作区，在新目录重新 git init 并提交现有数据。</li>
     *   <li><b>.gitignore</b>：不迁移旧版白名单。新目录是独立子目录，
     *       由 createGitignore() 生成标准模式。</li>
     * </ul>
     */
    private void migrateDefaultWorkspaceIfNeeded() {
        try {
            String rootDir = SystemUtil.getEasyPostmanPath();
            Path newDefaultDir = Paths.get(ConfigPathConstants.DEFAULT_WORKSPACE_DIR);

            // 确保新目录存在
            Files.createDirectories(newDefaultDir);

            // 若迁移标记文件已存在，说明之前已完成迁移，直接返回，避免重复迁移
            Path migratedMarker = newDefaultDir.resolve(".migrated");
            if (Files.exists(migratedMarker)) {
                return;
            }

            log.info(" ======!!!!!! Migrating default workspace at: {}", rootDir);

            // 1. 迁移数据文件（collections.json / environments.json / README.md）
            //    .git/ 和 .gitignore 不在迁移范围内（见 Javadoc）
            String[] dataFiles = {"collections.json", "environments.json", "README.md"};
            boolean migrated = false;
            for (String fileName : dataFiles) {
                Path oldFile = Paths.get(rootDir, fileName);
                Path newFile = newDefaultDir.resolve(fileName);
                if (!Files.exists(newFile) && Files.exists(oldFile)) {
                    Files.copy(oldFile, newFile);
                    log.info(" ========!!!!!! Migrated default workspace file: {} -> {}", oldFile, newFile);
                    migrated = true;
                }
            }

            // 2. 处理 Git 仓库：旧目录有 .git/ 说明用户曾做过 git init
            Path oldGitDir = Paths.get(rootDir, ".git");
            Path newGitDir = newDefaultDir.resolve(".git");
            if (Files.exists(oldGitDir) && !Files.exists(newGitDir)) {
                Workspace defaultWs = workspaces.stream()
                        .filter(WorkspaceStorageUtil::isDefaultWorkspace)
                        .findFirst().orElse(null);
                if (defaultWs != null) {
                    defaultWs.setPath(ConfigPathConstants.DEFAULT_WORKSPACE_DIR);
                    try {
                        requireGitService().migrateDefaultWorkspaceGit(
                                defaultWs,
                                "Migrate default workspace to workspaces/default/");
                        migrated = true;
                        log.info("Re-initialized git repository in new default workspace dir: {}", newDefaultDir);
                    } catch (Exception ex) {
                        log.warn("Failed to re-init git in new default workspace dir, skipping git migration", ex);
                    }
                }
            }

            if (migrated) {
                log.info("Default workspace migration complete: {}", newDefaultDir);
            }

            // 3. 同步更新内存中所有默认工作区对象的 path
            //    （loadWorkspaces 从旧 workspaces.json 加载时可能读到旧路径）
            workspaces.stream()
                    .filter(WorkspaceStorageUtil::isDefaultWorkspace)
                    .forEach(ws -> ws.setPath(ConfigPathConstants.DEFAULT_WORKSPACE_DIR));

            // 4. 持久化：将更新后的 path 写回 workspaces.json
            //    否则下次启动 loadWorkspaces 仍读到旧路径，每次都重复触发迁移逻辑
            saveWorkspaces();

            // 5. 写入迁移标记文件，下次启动时检测到后直接跳过，避免重复迁移
            Files.writeString(migratedMarker, String.valueOf(System.currentTimeMillis()));
            log.info("Migration marker written: {}", migratedMarker);

        } catch (Exception e) {
            log.warn("Failed to migrate default workspace, will use new path directly", e);
        }
    }

    /**
     * 验证工作区参数
     */
    private void validateWorkspace(Workspace workspace) throws IllegalArgumentException {
        if (workspace.getName() == null || workspace.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace name is required");
        }

        if (workspace.getPath() == null || workspace.getPath().trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace path is required");
        }

        if (workspace.getType() == WorkspaceType.GIT) {
            if (workspace.getGitRepoSource() == GitRepoSource.CLONED) {
                if (workspace.getGitRemoteUrl() == null || workspace.getGitRemoteUrl().trim().isEmpty()) {
                    throw new IllegalArgumentException("Git repository URL is required for cloning");
                }

                if (workspace.getGitAuthType() == GitAuthType.PASSWORD) {
                    if (workspace.getGitUsername() == null || workspace.getGitPassword() == null) {
                        throw new IllegalArgumentException("Username and password are required");
                    }
                } else if (workspace.getGitAuthType() == GitAuthType.TOKEN) {
                    if (workspace.getGitUsername() == null || workspace.getGitToken() == null) {
                        throw new IllegalArgumentException("Username and access token are required");
                    }
                }
            }
        }

        // 检查路径是否已被其他工作区使用
        String normalizedPath = Paths.get(workspace.getPath()).toAbsolutePath().normalize().toString();
        boolean pathExists = workspaces.stream()
                .anyMatch(w -> Paths.get(w.getPath()).toAbsolutePath().normalize().toString().equals(normalizedPath));

        if (pathExists) {
            throw new IllegalArgumentException("Path is already used by another workspace");
        }

        // 对于Git克隆模式，检查目标目录是否已存在且不为空
        if (workspace.getType() == WorkspaceType.GIT && workspace.getGitRepoSource() == GitRepoSource.CLONED) {
            Path targetPath = Paths.get(workspace.getPath());
            if (Files.exists(targetPath)) {
                try {
                    // 检查目录是否为空
                    boolean isEmpty = Files.list(targetPath).findAny().isEmpty();
                    if (!isEmpty) {
                        throw new IllegalArgumentException("Target directory is not empty: " + workspace.getPath());
                    }
                    // 如果目录存在但为空，删除它让Git克隆重新创建
                    Files.delete(targetPath);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Cannot access target directory: " + workspace.getPath());
                }
            }
        }
    }

    /**
     * 切换工作区
     */
    public void switchWorkspace(String workspaceId) {
        Workspace workspace = workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));

        currentWorkspace = workspace;
        WorkspaceStorageUtil.saveCurrentWorkspace(workspaceId);
        log.info("Switched to workspace: {}", workspace.getName());
    }

    /**
     * 删除工作区
     */
    public void deleteWorkspace(String workspaceId) throws Exception {
        Workspace workspace = workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));
        if (WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            throw new IllegalArgumentException("Default workspace cannot be deleted");
        }
        // 删除工作区文件
        Path workspacePath = Paths.get(workspace.getPath());
        if (Files.exists(workspacePath)) {
            deleteDirectoryRecursively(workspacePath);
        }

        workspaces.removeIf(w -> w.getId().equals(workspaceId));

        // 如果删除的是当前工作区，切换到默认工作区
        if (currentWorkspace != null && currentWorkspace.getId().equals(workspaceId)) {
            // 优先切换到默认工作区，找不到则取列表第一个，确保 currentWorkspace 不为 null
            currentWorkspace = getDefaultWorkspace();
            if (currentWorkspace != null) {
                WorkspaceStorageUtil.saveCurrentWorkspace(currentWorkspace.getId());
            } else {
                log.warn("No workspace available after deletion, currentWorkspace is null");
            }
        }

        saveWorkspaces();
        log.info("Deleted workspace: {}", workspace.getName());
    }

    public Workspace getDefaultWorkspace() {
        return workspaces.stream()
                .filter(WorkspaceStorageUtil::isDefaultWorkspace)
                .findFirst()
                .orElse(workspaces.isEmpty() ? null : workspaces.get(0));
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            if (!file.delete()) {
                                log.warn("Failed to delete file: {}", file.getAbsolutePath());
                            }
                        });
            }
        }
    }

    /**
     * 重命名工作区
     */
    public void renameWorkspace(String workspaceId, String newName) {
        Workspace workspace = workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));
        if (WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            throw new IllegalArgumentException("Default workspace cannot be renamed");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace name cannot be empty");
        }

        workspace.setName(newName.trim());
        workspace.setUpdatedAt(System.currentTimeMillis());
        saveWorkspaces();

        log.info("Renamed workspace to: {}", newName);
    }

    /**
     * Git操作：拉取更新
     */
    public GitOperationResult pullUpdates(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().pullUpdates(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * Git操作：推送变更
     */
    public GitOperationResult pushChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().pushChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * 强制推送变更（覆盖远程变更）
     */
    public GitOperationResult forcePushChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().forcePushChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * 暂存本地变更
     */
    public GitOperationResult stashChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().stashChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * 恢复暂存的变更
     */
    public GitOperationResult popStashChanges(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().popStashChanges(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * 强制拉取更新（丢弃本地变更）
     */
    public GitOperationResult forcePullUpdates(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().forcePullUpdates(workspace);
        saveWorkspaces();
        return result;
    }

    /**
     * Git操作：提交变更
     */
    public GitOperationResult commitChanges(String workspaceId, String message) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().commitChanges(workspace, message);
        saveWorkspaces();
        return result;
    }

    /**
     * 获取两个 commit 之间的变更文件列表
     */
    public List<String> getChangedFilesBetweenCommits(String workspaceId, String oldCommitId, String newCommitId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getChangedFilesBetweenCommits(workspace, oldCommitId, newCommitId);
    }

    /**
     * 根据ID获取工作区
     */
    private Workspace getWorkspaceById(String workspaceId) {
        return workspaces.stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(WORKSPACE_NOT_FOUND_MSG + workspaceId));
    }

    /**
     * 加载工作区配置
     */
    private void loadWorkspaces() {
        try {
            workspaces = WorkspaceStorageUtil.loadWorkspaces();

            String currentWorkspaceId = WorkspaceStorageUtil.getCurrentWorkspace();
            if (currentWorkspaceId != null) {
                currentWorkspace = workspaces.stream()
                        .filter(w -> w.getId().equals(currentWorkspaceId))
                        .findFirst()
                        .orElse(null);
            }

            // 如果没有当前工作区但有工作区列表，设置第一个为当前工作区
            if (currentWorkspace == null && !workspaces.isEmpty()) {
                currentWorkspace = workspaces.get(0);
                WorkspaceStorageUtil.saveCurrentWorkspace(currentWorkspace.getId());
            }

            log.info("Loaded {} workspaces", workspaces.size());
        } catch (Exception e) {
            log.error("Failed to load workspaces", e);
            workspaces = new ArrayList<>();
            // 加载失败时也要保证内存中有默认工作区，避免后续逻辑空指针
            Workspace defaultWs = WorkspaceStorageUtil.getDefaultWorkspace();
            workspaces.add(defaultWs);
            currentWorkspace = defaultWs;
        }
    }

    /**
     * 保存工作区配置
     */
    private void saveWorkspaces() {
        try {
            WorkspaceStorageUtil.saveWorkspaces(workspaces);
        } catch (Exception e) {
            log.error("Failed to save workspaces", e);
        }
    }

    /**
     * 为 INITIALIZED 工作区添加远程仓库
     */
    public void addRemoteRepository(String workspaceId, String remoteUrl, String remoteBranch,
                                    GitAuthType authType, String username, String password, String token) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        requireGitService().addRemoteRepository(workspace, remoteUrl, remoteBranch, authType, username, password, token);
        saveWorkspaces();
    }

    /**
     * 获取远程仓库状态信息
     */
    public RemoteStatus getRemoteStatus(String workspaceId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getRemoteStatus(workspace);
    }

    /**
     * 更新工作区的 Git 认证信息
     */
    public void updateGitAuthentication(String workspaceId, GitAuthType authType,
                                        String username, String password, String token,
                                        String sshKeyPath, String sshPassphrase) {
        Workspace workspace = getWorkspaceById(workspaceId);
        if (workspace.getType() != WorkspaceType.GIT) {
            throw new IllegalStateException("Not a Git workspace");
        }

        if (workspace.getGitRemoteUrl() == null || workspace.getGitRemoteUrl().isEmpty()) {
            throw new IllegalStateException("Workspace does not have a remote repository configured");
        }

        // 更新工作区的认证信息
        workspace.setGitAuthType(authType);
        workspace.setGitUsername(username);

        if (authType == GitAuthType.PASSWORD) {
            workspace.setGitPassword(password);
            workspace.setGitToken(null);
            workspace.setSshPrivateKeyPath(null);
            workspace.setSshPassphrase(null);
        } else if (authType == GitAuthType.TOKEN) {
            workspace.setGitToken(token);
            workspace.setGitPassword(null);
            workspace.setSshPrivateKeyPath(null);
            workspace.setSshPassphrase(null);
        } else if (authType == GitAuthType.SSH_KEY) {
            workspace.setSshPrivateKeyPath(sshKeyPath);
            workspace.setSshPassphrase(sshPassphrase);
            workspace.setGitPassword(null);
            workspace.setGitToken(null);
            workspace.setGitUsername(null);
        } else if (authType == GitAuthType.NONE) {
            workspace.setGitPassword(null);
            workspace.setGitToken(null);
            workspace.setGitUsername(null);
            workspace.setSshPrivateKeyPath(null);
            workspace.setSshPassphrase(null);
        }

        workspace.setUpdatedAt(System.currentTimeMillis());
        saveWorkspaces();

        log.info("Updated Git authentication for workspace: {}, authType: {}", workspace.getName(), authType);
    }

    /**
     * 将本地工作区转换为Git工作区
     * 复用现有的Git初始化逻辑
     */
    public void convertLocalToGit(Workspace workspace, String localBranch) throws Exception {
        requireGitService().convertLocalToGit(workspace, localBranch);
        saveWorkspaces();
    }

    /**
     * 保存工作区顺序（拖拽排序后调用）
     */
    public void saveWorkspaceOrder(List<String> idOrder) {
        try {
            // 根据idOrder重新排序workspaces列表
            List<Workspace> sortedWorkspaces = new ArrayList<>();
            for (String id : idOrder) {
                for (Workspace ws : workspaces) {
                    if (ws.getId().equals(id)) {
                        sortedWorkspaces.add(ws);
                        break;
                    }
                }
            }

            // 添加可能遗漏的工作区（防止数据丢失）
            for (Workspace ws : workspaces) {
                if (!sortedWorkspaces.contains(ws)) {
                    sortedWorkspaces.add(ws);
                }
            }

            // 更新工作区列表
            workspaces = sortedWorkspaces;

            // 持久化到文件
            saveWorkspaces();

            log.debug("Workspace order saved successfully");
        } catch (Exception e) {
            log.error("Failed to save workspace order", e);
        }
    }

    /**
     * 获取工作区的 Git 提交历史
     *
     * @param workspaceId 工作区ID
     * @param maxCount    最大返回数量，0表示返回所有
     * @return Git 提交信息列表
     */
    public List<GitCommitInfo> getGitHistory(String workspaceId, int maxCount) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getGitHistory(workspace, maxCount);
    }

    /**
     * 恢复工作区到指定的 Git 提交版本
     * 使用 reset + commit 方式，保留完整的历史记录
     *
     * @param workspaceId  工作区ID
     * @param commitId     提交ID
     * @param createBackup 是否在恢复前创建备份提交（保存未提交的更改）
     * @return 操作结果
     */
    public GitOperationResult restoreToCommit(String workspaceId, String commitId, boolean createBackup) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        GitOperationResult result = requireGitService().restoreToCommit(workspace, commitId, createBackup);
        saveWorkspaces();
        return result;
    }

    /**
     * 查看指定提交的详细信息
     *
     * @param workspaceId 工作区ID
     * @param commitId    提交ID
     * @return 提交详细信息
     */
    public String getCommitDetails(String workspaceId, String commitId) throws Exception {
        Workspace workspace = getWorkspaceById(workspaceId);
        return requireGitService().getCommitDetails(workspace, commitId);
    }

    public GitStatusCheck checkGitStatus(Workspace workspace, GitOperation operation) {
        return requireGitService().checkGitStatus(workspace, operation);
    }

    public void clearSshCache(String privateKeyPath) {
        requireGitService().clearSshCache(privateKeyPath);
    }

    public boolean isGitPluginInstalled() {
        return GitPluginServices.isGitPluginInstalled();
    }

    private GitPluginService requireGitService() {
        return GitPluginServices.requireGitService();
    }
}
