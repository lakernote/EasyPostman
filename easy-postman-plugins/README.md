# Easy Postman Plugins

这里放官方插件模块和插件管理模块：

- `plugin-manager`: catalog 解析、在线/离线安装、插件管理门面
- `plugin-redis`: Redis 插件
- `plugin-kafka`: Kafka 插件
- `plugin-git`: Git 工作区插件
- `plugin-decompiler`: Java 反编译插件

共享平台层仍放在根目录：

- `easy-postman-plugin-api`
- `easy-postman-plugin-runtime`

常用命令：

```bash
mvn -pl easy-postman-app,easy-postman-plugins/plugin-redis -am clean package -DskipTests
mvn clean package -DskipTests
```

更多说明见：

- `docs/PLUGINS_zh.md`
- `docs/BUILD_zh.md`
