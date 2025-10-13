# Linux 系统打包说明

## 概述

本项目提供了两个 Linux 打包脚本：
- `build/linux-deb.sh` - 用于 Ubuntu/Debian 系统的 DEB 包
- `build/linux-rpm.sh` - 用于 CentOS/RedHat/Fedora 系统的 RPM 包

## 前置要求

### 1. 系统要求

#### Ubuntu/Debian 系统（打包 DEB）
```bash
# 安装必需的工具
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk maven fakeroot
```

#### CentOS/RedHat 系统（打包 RPM）
```bash
# CentOS 7/8
sudo yum install -y java-17-openjdk java-17-openjdk-devel maven rpm-build

# 或者使用 dnf (CentOS 8+/Fedora)
sudo dnf install -y java-17-openjdk java-17-openjdk-devel maven rpm-build
```

### 2. 验证环境
```bash
# 检查 Java 版本（需要 >= 17）
java -version

# 检查 Maven
mvn -version

# 检查 jlink 和 jpackage（JDK 17+ 自带）
jlink --version
jpackage --version
```

## 打包步骤

### Ubuntu/Debian 系统 - 打包 DEB

1. **克隆项目代码**
   ```bash
   git clone https://gitee.com/lakernote/easy-postman.git
   cd easy-postman
   ```

2. **运行打包脚本**
   ```bash
   ./build/linux-deb.sh
   ```

3. **生成的文件**
   ```
   dist/easypostman_2.1.8_amd64.deb
   ```

4. **安装 DEB 包**
   ```bash
   # 安装
   sudo dpkg -i dist/easypostman_2.1.8_amd64.deb
   
   # 如果有依赖问题，运行：
   sudo apt-get install -f
   
   # 卸载
   sudo dpkg -r easypostman
   ```

### CentOS/RedHat 系统 - 打包 RPM

1. **克隆项目代码**
   ```bash
   git clone https://gitee.com/lakernote/easy-postman.git
   cd easy-postman
   ```

2. **运行打包脚本**
   ```bash
   ./build/linux-rpm.sh
   ```

3. **生成的文件**
   ```
   dist/easypostman-2.1.8-1.x86_64.rpm
   ```

4. **安装 RPM 包**
   ```bash
   # 使用 rpm 安装
   sudo rpm -ivh dist/easypostman-2.1.8-1.x86_64.rpm
   
   # 或使用 yum/dnf（会自动处理依赖）
   sudo yum install dist/easypostman-2.1.8-1.x86_64.rpm
   # 或
   sudo dnf install dist/easypostman-2.1.8-1.x86_64.rpm
   
   # 卸载
   sudo rpm -e easypostman
   # 或
   sudo yum remove easypostman
   ```

## 打包过程说明

两个脚本都会执行以下步骤：

1. **环境检查** - 验证 Java、Maven、jlink、jpackage 是否已安装
2. **版本读取** - 从 `pom.xml` 自动读取项目版本号
3. **Maven 构建** - 执行 `mvn clean package` 生成 fat JAR
4. **创建运行时** - 使用 `jlink` 创建精简的 JRE 运行时
5. **打包应用** - 使用 `jpackage` 创建系统安装包

## 打包特性

### DEB 包特性
- ✅ 自动创建桌面快捷方式
- ✅ 添加到应用程序菜单（开发工具分类）
- ✅ 包含精简的 JRE 运行时（无需系统安装 Java）
- ✅ 自动配置内存参数（256MB-512MB）
- ✅ UTF-8 编码支持

### RPM 包特性
- ✅ 自动创建桌面快捷方式
- ✅ 添加到应用程序菜单（开发工具分类）
- ✅ 包含精简的 JRE 运行时（无需系统安装 Java）
- ✅ 自动配置内存参数（256MB-512MB）
- ✅ UTF-8 编码支持
- ✅ MIT 许可证声明

## 安装后使用

### 启动应用

1. **通过应用程序菜单**
   - 在系统菜单中找到 "Development" 或 "开发工具" 分类
   - 点击 "EasyPostman" 图标启动

2. **通过命令行**
   ```bash
   easypostman
   ```

3. **查看安装位置**
   ```bash
   # DEB 包
   dpkg -L easypostman
   
   # RPM 包
   rpm -ql easypostman
   ```

## 常见问题

### 1. 打包失败：找不到 jpackage

**问题**: `jpackage: command not found`

**解决方案**: 
```bash
# 确保安装了完整的 JDK 17+（不是 JRE）
# Ubuntu/Debian
sudo apt-get install openjdk-17-jdk

# CentOS/RedHat
sudo yum install java-17-openjdk-devel
```

### 2. 安装后无法启动

**问题**: 双击图标无反应

**解决方案**:
```bash
# 查看错误日志
journalctl --user -f

# 或直接从终端运行查看错误
/opt/easypostman/bin/EasyPostMan
```

### 3. 权限问题

**问题**: `Permission denied`

**解决方案**:
```bash
# 确保脚本有执行权限
chmod +x build/linux-deb.sh build/linux-rpm.sh
```

### 4. Maven 构建失败

**问题**: 依赖下载失败

**解决方案**:
```bash
# 清理 Maven 缓存重试
rm -rf ~/.m2/repository
./build/linux-deb.sh  # 或 ./build/linux-rpm.sh
```

## 跨平台打包总结

| 平台 | 脚本 | 输出格式 | 包管理器 |
|------|------|----------|----------|
| macOS | `build/mac.sh` | DMG | 手动安装 |
| Windows | `build/win.bat` | MSI | 双击安装 |
| Ubuntu/Debian | `build/linux-deb.sh` | DEB | dpkg/apt |
| CentOS/RedHat | `build/linux-rpm.sh` | RPM | rpm/yum/dnf |

## 技术细节

### 包含的 Java 模块
```
java.base, java.desktop, java.logging, jdk.unsupported, 
java.naming, java.net.http, java.prefs, java.sql,
java.security.sasl, java.security.jgss, jdk.crypto.ec,
java.management, java.management.rmi, jdk.crypto.cryptoki
```

### JVM 参数
```
-Xms256m          # 初始堆内存
-Xmx512m          # 最大堆内存
-Dfile.encoding=UTF-8  # 文件编码
```

### 安装路径

- **DEB**: `/opt/easypostman/`
- **RPM**: `/opt/easypostman/`
