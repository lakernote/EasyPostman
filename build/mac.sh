#!/bin/bash

# 获取项目根目录路径（包含 pom.xml）
PROJECT_ROOT=$(cd "$(dirname "$0")/.."; pwd)

# 切换到项目根目录并读取版本号
cd "$PROJECT_ROOT" || exit 1
# 直接从 pom.xml 解析版本号
VERSION=$(grep -m 1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "🔧 开始打包 EasyTools 版本: $VERSION"
APP_NAME="EasyPostman"
JAR_NAME="easy-tools-$VERSION.jar"
MAIN_CLASS="com.laker.postman.App"
ICON_DIR="assets/mac/EasyPostman.icns"
OUTPUT_DIR="dist"

# 检查 JDK 版本是否 >= 17
JAVA_VERSION=$(java -version 2>&1 | grep version | awk '{print substr($3, 2, 3)}' | tr -d '"')
if (( $(echo "$JAVA_VERSION < 17" | bc -l) )); then
    echo "❌ 需要 JDK 17 或更高版本。当前版本: $JAVA_VERSION"
    exit 1
fi

# Step 1: 清理 & 构建项目
echo "🚀 开始构建项目..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ Maven 构建失败，请检查错误日志"
    exit 1
fi

# Step 2: 创建最小运行时 jlink
echo "⚙️ 使用 jlink 创建最小化运行时..."
rm -rf target/runtime
jlink \
    --add-modules java.base,java.desktop,java.logging,jdk.unsupported,java.naming,java.net.http,java.prefs,java.sql,java.security.sasl,java.security.jgss,jdk.crypto.ec,java.management,java.management.rmi,jdk.crypto.cryptoki \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2 \
    --output target/runtime

if [ $? -ne 0 ]; then
    echo "❌ jlink 创建失败"
    exit 1
fi

# Step 3: 准备输出目录
mkdir -p ${OUTPUT_DIR}

# Step 3.5: 创建仅包含主 jar 的临时目录
DIST_INPUT_DIR="target/dist-input"
rm -rf ${DIST_INPUT_DIR}
mkdir -p ${DIST_INPUT_DIR}
cp target/${JAR_NAME} ${DIST_INPUT_DIR}/

# Step 4: 使用 jpackage 打包 DMG
# --input 只需要包含最终运行所需的 jar 包和依赖。
echo "📦 使用 jpackage 打包 macOS 应用..."
jpackage \
    --input ${DIST_INPUT_DIR} \
    --main-jar ${JAR_NAME} \
    --main-class ${MAIN_CLASS} \
    --runtime-image target/runtime \
    --type dmg \
    --name "${APP_NAME}" \
    --app-version "${VERSION}" \
    --dest "${OUTPUT_DIR}" \
    --icon "${ICON_DIR}" \
    --vendor "Laker" \
    --copyright "© 2025 Laker" \
    --java-options "-Xms128m" \
    --java-options "-Xmx256m" \
    --java-options "-Dfile.encoding=UTF-8"

# 完成提示
echo "🎉 打包完成！输出路径：$(pwd)/${OUTPUT_DIR}"
