@echo off
chcp 65001 > nul
echo EasyPostman 正在更新...
echo Updating EasyPostman...
echo.

REM 等待当前进程结束（最多等待 10 秒）
set COUNTER=0
:WAIT_LOOP
timeout /t 1 /nobreak > nul
tasklist | find /i "java.exe" > nul
if errorlevel 1 goto UPDATE
set /a COUNTER+=1
if %COUNTER% lss 10 goto WAIT_LOOP

REM 执行更新
:UPDATE
echo 正在备份当前版本 / Backing up current version...
move /Y "{{CURRENT_JAR_PATH}}" "{{BACKUP_JAR_PATH}}" > nul 2>&1

echo 正在安装新版本 / Installing new version...
move /Y "{{DOWNLOADED_JAR_PATH}}" "{{CURRENT_JAR_PATH}}"
if errorlevel 1 (
    echo 更新失败，正在恢复 / Update failed, restoring...
    move /Y "{{BACKUP_JAR_PATH}}" "{{CURRENT_JAR_PATH}}"
    goto END
)

REM 启动新版本
echo 正在启动新版本 / Starting new version...
start "" javaw -jar "{{CURRENT_JAR_PATH}}"

REM 清理
timeout /t 2 /nobreak > nul
del /F /Q "{{BACKUP_JAR_PATH}}" > nul 2>&1
del /F /Q "%~f0" > nul 2>&1

:END
exit

