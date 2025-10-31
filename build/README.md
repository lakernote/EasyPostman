# 构建脚本修改说明 - 使用固定 JAR 文件名

## 修改内容

所有构建脚本（mac.sh, win.bat, linux-deb.sh, linux-rpm.sh）已修改为使用固定的 JAR 文件名：`easy-postman.jar`

## 修改前后对比

### 修改前
```bash
JAR_NAME="easy-postman-$VERSION.jar"  # 例如: easy-postman-3.1.0.jar
```

### 修改后
```bash
JAR_NAME_WITH_VERSION="easy-postman-$VERSION.jar"  # Maven 构建的文件名
JAR_NAME="easy-postman.jar"                         # jpackage 使用的固定名称
```

## 构建流程

```
1. Maven 构建 → easy-postman-3.1.0.jar
2. 脚本复制重命名 → easy-postman.jar
3. jpackage 打包 → 使用 easy-postman.jar
4. 最终配置 → app.classpath=$APPDIR/easy-postman.jar
```

## 优势

✅ **JAR 更新无需修改配置** - 配置文件中的路径固定为 `easy-postman.jar`  
✅ **更新脚本简化** - 始终替换同一个文件名  
✅ **避免版本号混淆** - 文件名不包含版本号，版本信息在 MANIFEST 中  

## 影响的文件

- `build/mac.sh` ✅
- `build/win.bat` ✅  
- `build/linux-deb.sh` ✅
- `build/linux-rpm.sh` ✅

## 测试

```bash
# macOS
cd build && bash mac.sh

# 检查生成的配置
cat dist/EasyPostman.app/Contents/app/EasyPostman.cfg
# 应该看到: app.classpath=$APPDIR/easy-postman.jar
```

## 注意事项

⚠️ 下次构建时，jpackage 配置会自动使用 `easy-postman.jar`  
⚠️ JAR 自动更新功能会继续正常工作（文件名固定）  
⚠️ 所有平台保持一致的命名规则

---
**修改日期**: 2025-10-31  
**影响版本**: 3.1.7+

