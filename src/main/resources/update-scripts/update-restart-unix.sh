#!/bin/bash

echo "EasyPostman 正在更新..."
echo "Updating EasyPostman..."
echo ""

# 等待当前进程结束
CURRENT_PID=$$
PARENT_PID=$PPID
echo "Update script PID: $CURRENT_PID, Parent PID: $PARENT_PID"

# 等待父进程结束（最多10秒）
for i in {1..20}; do
    if ! ps -p $PARENT_PID > /dev/null 2>&1; then
        echo "Parent process ended"
        break
    fi
    sleep 0.5
done

# 额外等待，确保所有资源释放
sleep 1

# 执行更新
echo "正在备份当前版本 / Backing up current version..."
mv -f "{{CURRENT_JAR_PATH}}" "{{BACKUP_JAR_PATH}}" 2>/dev/null

echo "正在安装新版本 / Installing new version..."
mv -f "{{DOWNLOADED_JAR_PATH}}" "{{CURRENT_JAR_PATH}}"
if [ $? -ne 0 ]; then
    echo "更新失败，正在恢复 / Update failed, restoring..."
    mv -f "{{BACKUP_JAR_PATH}}" "{{CURRENT_JAR_PATH}}"
    exit 1
fi

# 启动新版本
echo "正在启动新版本 / Starting new version..."

# {{PLATFORM_SPECIFIC_START}}

# 验证启动
if [ $APP_STARTED -eq 0 ]; then
    echo "新版本已启动 / New version started successfully"
else
    echo "启动失败 / Failed to start new version"
fi

# 清理
sleep 2
rm -f "{{BACKUP_JAR_PATH}}" 2>/dev/null
rm -f "{{SCRIPT_FILE_PATH}}" 2>/dev/null

exit 0

