@echo off
SETLOCAL ENABLEEXTENSIONS

:: Step 1: Locate project root directory
cd /d "%~dp0%\.."
set PROJECT_ROOT=%cd%

echo Current directory: %cd%
if not exist "pom.xml" (
    echo ERROR: pom.xml not found in current directory!
    echo Expected location: %cd%\pom.xml
    pause
    exit /b 1
)

:: Step 2: Get version from pom.xml using PowerShell
FOR /F "tokens=* USEBACKQ" %%F IN (`
    powershell -Command "[regex]::Match((Get-Content '%cd%\pom.xml' -Raw), '<version>(.*?)</version>').Groups[1].Value"
`) DO SET VERSION=%%F

if not defined VERSION (
    echo Failed to get version from pom.xml
    echo Please make sure:
    echo   1. pom.xml exists at: %cd%\pom.xml
    echo   2. It contains a valid <version>1.0.0</version> tag
    pause
    exit /b 1
)

echo Building EasyPostman version: %VERSION%

set APP_NAME=EasyPostman
set JAR_NAME=easy-postman-%VERSION%.jar
set MAIN_CLASS=com.laker.postman.App
set ICON_DIR=assets\win\EasyPostman.ico
set OUTPUT_DIR=dist

:: Check Java version >= 17
for /f tokens^=2^ delims^=^" %%a in ('java -version 2^>^&1 ^| findstr version') do set "JVER=%%a"
for /f tokens^=1^ delims^=.^" %%b in ("%JVER%") do set "MAJOR_JVER=%%b"

if %MAJOR_JVER% lss 17 (
    echo ERROR: JDK 17 or higher is required. Current version: %JVER%
    exit /b 1
)

:: Step 1: Clean and build project
call mvn clean package -DskipTests=false
if errorlevel 1 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)

:: Step 2: Create runtime with jlink
if exist target\runtime rd /s /q target\runtime
jlink ^
    --add-modules java.base,java.desktop,java.logging,jdk.unsupported,java.naming,java.net.http,java.prefs,java.sql,java.security.sasl,java.security.jgss,jdk.crypto.ec,java.management,java.management.rmi,jdk.crypto.cryptoki ^
    --strip-debug ^
    --no-header-files ^
    --no-man-pages ^
    --compress=2 ^
    --output target\runtime
if errorlevel 1 (
    echo ERROR: jlink failed
    pause
    exit /b 1
)

:: Step 3: Prepare output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: Step 3.5: Create temp dir with only main jar
set DIST_INPUT_DIR=target\dist-input
if exist %DIST_INPUT_DIR% rd /s /q %DIST_INPUT_DIR%
mkdir %DIST_INPUT_DIR%
copy target\%JAR_NAME% %DIST_INPUT_DIR%\
if errorlevel 1 (
    echo Failed to copy jar file
    pause
    exit /b 1
)

:: Step 4: Copy WiX configuration to remember installation path
set RESOURCE_DIR=target\jpackage-resources
if not exist "%RESOURCE_DIR%" mkdir "%RESOURCE_DIR%"

:: Copy unified WiX configuration file
set WIX_CONFIG=build\wix-install-path-memory.wxs
if not exist "%WIX_CONFIG%" (
    echo ERROR: WiX configuration file not found: %WIX_CONFIG%
    pause
    exit /b 1
)

copy "%WIX_CONFIG%" "%RESOURCE_DIR%\main.wxs"
if errorlevel 1 (
    echo Failed to copy WiX configuration file
    pause
    exit /b 1
)

echo WiX configuration copied (install path memory enabled)

:: Step 5: Package with jpackage
if not exist "%ICON_DIR%" (
    echo Icon file not found: "%ICON_DIR%"
    pause
    exit /b 1
)

jpackage ^
    --input %DIST_INPUT_DIR% ^
    --main-jar %JAR_NAME% ^
    --main-class %MAIN_CLASS% ^
    --runtime-image target\runtime ^
    --type msi ^
    --name "%APP_NAME%" ^
    --app-version "%VERSION%" ^
    --dest "%OUTPUT_DIR%" ^
    --icon "%ICON_DIR%" ^
    --vendor "Laker" ^
    --copyright "© 2025 Laker" ^
    --win-shortcut ^
    --win-menu ^
    --description "A modern API testing tool similar to Postman" ^
    --win-upgrade-uuid "28607609-97b7-4212-9285-04ef64a4946c" ^
    --win-dir-chooser ^
    --win-per-user-install ^
    --win-menu-group "EasyTools" ^
    --win-help-url "https://gitee.com/lakernote/easy-postman" ^
    --resource-dir "%RESOURCE_DIR%" ^
    --java-options "-Xms256m" ^
    --java-options "-Xmx512m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --java-options "-Djavax.accessibility.assistive_technologies="

if errorlevel 1 (
    echo ERROR: jpackage failed
    pause
    exit /b 1
)

echo Build completed! Output path: %cd%\%OUTPUT_DIR%

ENDLOCAL