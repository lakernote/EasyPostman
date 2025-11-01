@echo off
REM EasyPostman Update Script
REM This script will backup, update and restart the application

REM Wait to ensure file handles are released
timeout /t 1 /nobreak > nul

REM Backup current version
move /Y "{{CURRENT_JAR_PATH}}" "{{BACKUP_JAR_PATH}}" > nul 2>&1

REM Install new version
move /Y "{{DOWNLOADED_JAR_PATH}}" "{{CURRENT_JAR_PATH}}"
if errorlevel 1 (
    REM Update failed, restoring backup
    move /Y "{{BACKUP_JAR_PATH}}" "{{CURRENT_JAR_PATH}}"
    goto END
)

REM Create temporary VBScript to start application and cleanup
set "VBSCRIPT=%TEMP%\start_easypostman_%RANDOM%.vbs"
(
echo Set WshShell = CreateObject^("WScript.Shell"^)
echo WshShell.Run "javaw -jar ""{{CURRENT_JAR_PATH}}""", 0, False
echo WScript.Sleep 500
echo Set fso = CreateObject^("Scripting.FileSystemObject"^)
echo On Error Resume Next
echo fso.DeleteFile "{{BACKUP_JAR_PATH}}", True
echo fso.DeleteFile "{{SCRIPT_PATH}}", True
echo fso.DeleteFile WScript.ScriptFullName, True
) > "%VBSCRIPT%"

REM Check if VBScript was created successfully
if not exist "%VBSCRIPT%" (
    goto END
)

REM Start VBScript in background
start "" wscript.exe "%VBSCRIPT%"


:END
exit

