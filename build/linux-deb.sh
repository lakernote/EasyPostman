#!/bin/bash

# Ubuntu/Debian 系统 DEB 包打包脚本

# 检查 java 是否可用
if ! command -v java &> /dev/null; then
    echo "❌ 未检测到 java，请先安装 JDK 17+ 并配置环境变量。"
    exit 1
fi
# 检查 mvn 是否可用
if ! command -v mvn &> /dev/null; then
    echo "❌ 未检测到 mvn，请先安装 Maven 并配置环境变量。"
    exit 1
fi
# 检查 jlink 是否可用
if ! command -v jlink &> /dev/null; then
    echo "❌ 未检测到 jlink，请确认 JDK 17+ 已正确安装。"
    exit 1
fi
# 检查 jpackage 是否可用
if ! command -v jpackage &> /dev/null; then
    echo "❌ 未检测到 jpackage，请确认 JDK 17+ 已正确安装。"
    exit 1
fi
# 检查 objcopy 是否可用（jlink 需要）
if ! command -v objcopy &> /dev/null; then
    echo "❌ 未检测到 objcopy，请先安装 binutils："
    echo "   sudo apt-get install binutils"
    exit 1
fi

# 获取项目根目录路径（包含 pom.xml）
PROJECT_ROOT=$(cd "$(dirname "$0")/.."; pwd)

# 切换到项目根目录并读取版本号
cd "$PROJECT_ROOT" || exit 1
# 直接从 pom.xml 解析版本号
VERSION=$(grep -m 1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "🔧 开始打包 EasyPostman DEB 包，版本: $VERSION"
APP_NAME="EasyPostman"
JAR_NAME_WITH_VERSION="easy-postman-$VERSION.jar"
JAR_NAME="easy-postman.jar"  # 固定名称，不带版本号
MAIN_CLASS="com.laker.postman.App"
ICON_DIR="assets/linux/EasyPostman.png"
OUTPUT_DIR="dist"

# 检查 JDK 版本是否 >= 17
JAVA_VERSION=$(java -version 2>&1 | grep version | awk '{print substr($3, 2, 3)}' | tr -d '"')
if (( $(echo "$JAVA_VERSION < 17" | bc -l) )); then
    echo "❌ 需要 JDK 17 或更高版本。当前版本: $JAVA_VERSION"
    exit 1
fi

# Step 1: 清理 & 构建项目
echo "🚀 开始构建项目..."
mvn clean package -DskipTests=false
if [ $? -ne 0 ]; then
    echo "❌ Maven 构建失败，请检查错误日志"
    exit 1
fi
# 检查 jar 包是否生成成功
if [ ! -f "target/$JAR_NAME_WITH_VERSION" ]; then
    echo "❌ 构建未生成 jar 包: target/$JAR_NAME_WITH_VERSION"
    exit 1
fi

# 重命名 JAR 为固定名称（方便 jpackage 配置和后续更新）
echo "📝 重命名 JAR: $JAR_NAME_WITH_VERSION -> $JAR_NAME"
cp "target/$JAR_NAME_WITH_VERSION" "target/$JAR_NAME"

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

# Step 4: 创建仅包含主 jar 的临时目录
DIST_INPUT_DIR="target/dist-input"
rm -rf ${DIST_INPUT_DIR}
mkdir -p ${DIST_INPUT_DIR}
cp target/${JAR_NAME} ${DIST_INPUT_DIR}/

# Step 5: 使用 jpackage 打包 DEB
echo "📦 使用 jpackage 打包 DEB 包..."
jpackage \
    --input ${DIST_INPUT_DIR} \
    --main-jar ${JAR_NAME} \
    --main-class ${MAIN_CLASS} \
    --runtime-image target/runtime \
    --type deb \
    --name "${APP_NAME}" \
    --app-version "${VERSION}" \
    --dest "${OUTPUT_DIR}" \
    --vendor "Laker" \
    --copyright "© 2025 Laker" \
    --description "A modern API testing tool similar to Postman" \
    --linux-shortcut \
    --linux-menu-group "Development" \
    --linux-app-category "Development" \
    --java-options "-Xms512m" \
    --java-options "-Xmx1g" \
    --java-options "-XX:MaxMetaspaceSize=256m" \
    --java-options "-XX:MetaspaceSize=128m" \
    --java-options "-XX:MaxDirectMemorySize=256m" \
    --java-options "-XX:+UseG1GC" \
    --java-options "-XX:MaxGCPauseMillis=200" \
    --java-options "-XX:InitiatingHeapOccupancyPercent=45" \
    --java-options "-XX:+UseStringDeduplication" \
    --java-options "-XX:+HeapDumpOnOutOfMemoryError" \
    --java-options "-XX:HeapDumpPath=./dumps" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "-Dawt.useSystemAAFontSettings=on" \
    --java-options "-Dswing.aatext=true" \
    --java-options "-Djava.net.preferIPv4Stack=true" \
    --java-options "-Dhttp.keepAlive=true" \
    --java-options "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"

if [ $? -ne 0 ]; then
    echo "❌ jpackage 打包失败"
    exit 1
fi

# 完成提示
echo "🎉 DEB 包打包完成！输出路径：$(pwd)/${OUTPUT_DIR}"
echo "📝 安装命令: sudo dpkg -i ${OUTPUT_DIR}/EasyPostman_${VERSION}-1_amd64.deb"
echo "📝 或使用: sudo apt install ${OUTPUT_DIR}/EasyPostman_${VERSION}-1_amd64.deb"
echo "📝 卸载命令: sudo dpkg -r easypostman"
