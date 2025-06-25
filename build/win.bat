@echo off
SETLOCAL ENABLEEXTENSIONS

:: This script requires PowerShell to execute certain commands. Please ensure PowerShell is available on your system.

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

echo Building EasyTools version: %VERSION%

set APP_NAME=EasyPostman
set JAR_NAME=easy-tools-%VERSION%.jar
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
call mvn clean package -DskipTests
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

:: Generate a new ProductCode for each build
for /f %%i in ('powershell -Command "[guid]::NewGuid().ToString()"') do set PRODUCT_CODE=%%i

:: Debugging: Print the generated ProductCode
echo Generated ProductCode: %PRODUCT_CODE%

:: Step 4: Package with jpackage
if not exist "%ICON_DIR%" (
    echo Icon file not found: "%ICON_DIR%"
    pause
    exit /b 1
)

:: Debugging: Print the full jpackage command
echo Running jpackage with the following command:
echo jpackage ^
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
    --win-dir-chooser ^
    --win-per-user-install ^
    --win-menu-group "EasyTools" ^
    --win-help-url "https://gitee.com/lakernote/easy-postman" ^
    --java-options "-Xms128m" ^
    --java-options "-Xmx256m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-product-code "%PRODUCT_CODE%"

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
    --win-dir-chooser ^
    --win-per-user-install ^
    --win-menu-group "EasyTools" ^
    --win-help-url "https://gitee.com/lakernote/easy-postman" ^
    --java-options "-Xms128m" ^
    --java-options "-Xmx256m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-product-code "%PRODUCT_CODE%"
if errorlevel 1 (
    echo ERROR: jpackage failed
    pause
    exit /b 1
)

echo Build completed! Output path: %cd%\%OUTPUT_DIR%

ENDLOCAL