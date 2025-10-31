@echo off
chcp 65001 > nul
echo EasyPostman 正在更新...
echo Updating EasyPostman...
echo.

REM 等待确保文件句柄释放
timeout /t 2 /nobreak > nul

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

REM 创建临时VBScript来启动应用并清理
set "VBSCRIPT=%TEMP%\start_easypostman_%RANDOM%.vbs"
echo Set WshShell = CreateObject("WScript.Shell") > "%VBSCRIPT%"
echo WshShell.Run "javaw -jar ""{{CURRENT_JAR_PATH}}""", 0, False >> "%VBSCRIPT%"
echo WScript.Sleep 500 >> "%VBSCRIPT%"
echo Set fso = CreateObject("Scripting.FileSystemObject") >> "%VBSCRIPT%"
echo On Error Resume Next >> "%VBSCRIPT%"
echo fso.DeleteFile "{{BACKUP_JAR_PATH}}", True >> "%VBSCRIPT%"
echo fso.DeleteFile "{{SCRIPT_PATH}}", True >> "%VBSCRIPT%"
echo fso.DeleteFile WScript.ScriptFullName, True >> "%VBSCRIPT%"

REM 后台启动VBScript并立即退出
start "" wscript.exe "%VBSCRIPT%"

:END
exit

