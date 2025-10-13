#!/bin/bash

# CentOS/RedHat 系统 RPM 包打包脚本

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

# 获取项目根目录路径（包含 pom.xml）
PROJECT_ROOT=$(cd "$(dirname "$0")/.."; pwd)

# 切换到项目根目录并读取版本号
cd "$PROJECT_ROOT" || exit 1
# 直接从 pom.xml 解析版本号
VERSION=$(grep -m 1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "🔧 开始打包 EasyPostman RPM 包，版本: $VERSION"
APP_NAME="EasyPostman"
JAR_NAME="easy-postman-$VERSION.jar"
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
if [ ! -f "target/$JAR_NAME" ]; then
    echo "❌ 构建未生成 jar 包: target/$JAR_NAME"
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

# Step 4: 创建仅包含主 jar 的临时目录
DIST_INPUT_DIR="target/dist-input"
rm -rf ${DIST_INPUT_DIR}
mkdir -p ${DIST_INPUT_DIR}
cp target/${JAR_NAME} ${DIST_INPUT_DIR}/

# Step 5: 使用 jpackage 打包 RPM
echo "📦 使用 jpackage 打包 RPM 包..."
jpackage \
    --input ${DIST_INPUT_DIR} \
    --main-jar ${JAR_NAME} \
    --main-class ${MAIN_CLASS} \
    --runtime-image target/runtime \
    --type rpm \
    --name "easypostman" \
    --app-version "${VERSION}" \
    --dest "${OUTPUT_DIR}" \
    --vendor "Laker" \
    --copyright "© 2025 Laker" \
    --description "A modern API testing tool similar to Postman" \
    --linux-shortcut \
    --linux-menu-group "Development" \
    --linux-app-category "Development" \
    --linux-rpm-license-type "MIT" \
    --java-options "-Xms256m" \
    --java-options "-Xmx512m" \
    --java-options "-Dfile.encoding=UTF-8"

if [ $? -ne 0 ]; then
    echo "❌ jpackage 打包失败"
    exit 1
fi

# 完成提示
echo "🎉 RPM 包打包完成！输出路径：$(pwd)/${OUTPUT_DIR}"
echo "📝 安装命令: sudo rpm -ivh ${OUTPUT_DIR}/easypostman-${VERSION}-1.x86_64.rpm"
echo "📝 或使用: sudo yum install ${OUTPUT_DIR}/easypostman-${VERSION}-1.x86_64.rpm"
echo "📝 卸载命令: sudo rpm -e easypostman"

