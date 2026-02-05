# 🚀 构建指南

## 前置要求

### 必需

- **Java 17** 或更高版本
- **Maven 3.6+**
- **Git**

### 推荐

- **[JetBrains Runtime (JBR)](https://github.com/JetBrains/JetBrainsRuntime)** - 为 Swing 应用优化的 JDK

> 💡 **为什么选择 JetBrains Runtime？**
> 
> JetBrains Runtime 是专门为 Swing/AWT 应用优化的 JDK 发行版，提供：
> - ✨ 更好的 Swing/AWT 渲染性能
> - 🎨 改进的字体渲染和 HiDPI 支持
> - 🐛 修复了标准 JDK 中的 Swing 相关 bug
> - 🚀 针对桌面应用优化的 GC 和性能调优
> 
> **下载地址**: [JetBrains Runtime Releases](https://github.com/JetBrains/JetBrainsRuntime/releases)

---

## 🔧 从源码构建

### 1. 克隆仓库

```bash
# GitHub
git clone https://github.com/lakernote/easy-postman.git
cd easy-postman

# 或 Gitee（国内镜像）
git clone https://gitee.com/lakernote/easy-postman.git
cd easy-postman
```

### 2. 构建项目

```bash
# 清理并打包
mvn clean package

# 跳过测试（更快）
mvn clean package -DskipTests
```

这将生成：
- `target/easy-postman-{版本号}.jar` - 可执行 JAR 文件

### 3. 运行应用

```bash
# 直接运行
java -jar target/easy-postman-*.jar

# 或使用自定义 JVM 选项
java -Xms512m -Xmx2g -jar target/easy-postman-*.jar
```

---

## 📦 生成原生安装包

### macOS

#### 要求
- macOS 10.13+（High Sierra 或更高版本）
- Xcode 命令行工具：`xcode-select --install`
- 支持 jpackage 的 JDK 17+

#### 构建 DMG

```bash
# 赋予脚本执行权限
chmod +x build/mac.sh

# 运行构建脚本
./build/mac.sh
```

**输出**: `target/EasyPostman-{版本号}-macos-{架构}.dmg`
- Apple Silicon (M1/M2/M3/M4): `macos-arm64.dmg`
- Intel: `macos-x86_64.dmg`

#### 手动构建

```bash
# Apple Silicon
jpackage \
  --input target \
  --name EasyPostman \
  --main-jar easy-postman-{版本号}.jar \
  --type dmg \
  --icon assets/mac/EasyPostman.icns \
  --app-version {版本号} \
  --vendor "laker" \
  --mac-package-name "EasyPostman"

# Intel
jpackage \
  --input target \
  --name EasyPostman \
  --main-jar easy-postman-{版本号}.jar \
  --type dmg \
  --icon assets/mac/EasyPostman.icns \
  --app-version {版本号} \
  --vendor "laker" \
  --mac-package-name "EasyPostman" \
  --java-options "--add-opens java.base/java.lang=ALL-UNNAMED"
```

---

### Windows

#### 要求
- Windows 10/11
- 支持 jpackage 的 JDK 17+
- [WiX Toolset 3.11+](https://wixtoolset.org/)（用于 MSI）
- [Inno Setup 6+](https://jrsoftware.org/isinfo.php)（用于 EXE 安装程序）

#### 构建 EXE 安装程序

```batch
# 运行构建脚本
build\win-exe.bat
```

**输出**:
- `target/EasyPostman-{版本号}-windows-x64.exe` - 安装程序
- `target/EasyPostman-{版本号}-windows-x64-portable.zip` - 便携版

#### 手动构建

```batch
# 构建安装程序
jpackage ^
  --input target ^
  --name EasyPostman ^
  --main-jar easy-postman-{版本号}.jar ^
  --type exe ^
  --icon assets\win\EasyPostman.ico ^
  --app-version {版本号} ^
  --vendor "laker" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

# 创建便携版 ZIP
mkdir target\portable
xcopy target\EasyPostman target\portable\EasyPostman\ /E /I
cd target\portable
powershell Compress-Archive -Path EasyPostman -DestinationPath ..\EasyPostman-{版本号}-windows-x64-portable.zip
cd ..\..
```

---

### Linux (Ubuntu/Debian)

#### 要求
- Ubuntu 18.04+ 或 Debian 10+
- 支持 jpackage 的 JDK 17+
- dpkg-dev: `sudo apt install dpkg-dev`

#### 构建 DEB 包

```bash
# 赋予脚本执行权限
chmod +x build/linux-deb.sh

# 运行构建脚本
./build/linux-deb.sh
```

**输出**: `target/easypostman_{版本号}_amd64.deb`

#### 手动构建

```bash
jpackage \
  --input target \
  --name easypostman \
  --main-jar easy-postman-{版本号}.jar \
  --type deb \
  --icon assets/linux/EasyPostman.png \
  --app-version {版本号} \
  --vendor "laker" \
  --linux-package-name "easypostman" \
  --linux-menu-group "Development" \
  --linux-shortcut
```

#### 安装 DEB 包

```bash
sudo dpkg -i target/easypostman_{版本号}_amd64.deb

# 如果缺少依赖
sudo apt-get install -f
```

---

### Linux (基于 RPM)

#### 要求
- Fedora/RHEL/CentOS
- 支持 jpackage 的 JDK 17+
- rpm-build: `sudo dnf install rpm-build`

#### 构建 RPM 包

```bash
# 赋予脚本执行权限
chmod +x build/linux-rpm.sh

# 运行构建脚本
./build/linux-rpm.sh
```

**输出**: `target/easypostman-{版本号}-1.x86_64.rpm`

---

## 🔍 高级构建选项

### 自定义 JVM 选项

编辑启动脚本或使用 jpackage `--java-options`：

```bash
jpackage \
  --java-options "-Xms512m" \
  --java-options "-Xmx2g" \
  --java-options "-Dfile.encoding=UTF-8" \
  # ... 其他选项
```

### 使用 jlink 创建自定义运行时

创建仅包含所需模块的最小 JRE：

```bash
# 列出所需模块
jdeps --print-module-deps target/easy-postman-{版本号}.jar

# 创建自定义运行时
jlink \
  --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.sql,java.xml \
  --output target/custom-runtime \
  --strip-debug \
  --compress 2 \
  --no-header-files \
  --no-man-pages

# 使用自定义运行时打包
jpackage \
  --runtime-image target/custom-runtime \
  # ... 其他选项
```

---

## 🐛 故障排除

### macOS: "应用已损坏，无法打开"

```bash
# 移除隔离属性
sudo xattr -rd com.apple.quarantine /Applications/EasyPostman.app
```

### Windows: "Windows 已保护你的电脑"

点击"更多信息" → "仍要运行"。这是未签名应用的 SmartScreen 警告。

### Linux: 缺少依赖

```bash
# Ubuntu/Debian
sudo apt-get install -f

# Fedora/RHEL
sudo dnf install <缺少的包>
```

### 构建失败，提示 "jpackage: command not found"

确保使用的是 JDK 17+（不是 JRE）且 jpackage 在 PATH 中：

```bash
# 检查 Java 版本
java -version

# 检查 jpackage
jpackage --version

# 如果未找到，使用完整路径
$JAVA_HOME/bin/jpackage --version
```

---

## 📚 其他资源

- [jpackage 文档](https://docs.oracle.com/en/java/javase/17/jpackage/)
- [Maven 文档](https://maven.apache.org/guides/)
- [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime)

---

## 💬 需要帮助？

- 💬 GitHub 讨论区: [https://github.com/lakernote/easy-postman/discussions](https://github.com/lakernote/easy-postman/discussions)
- 🐛 报告问题: [https://github.com/lakernote/easy-postman/issues](https://github.com/lakernote/easy-postman/issues)
- 👥 微信群: 添加 **lakernote** 加入
