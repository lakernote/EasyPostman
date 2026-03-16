# Plugins Layout

这里放“具体插件”和插件管理相关模块：

- `plugin-manager`: 插件目录、安装、市场与管理门面
- `plugin-redis`: Redis 插件
- `plugin-kafka`: Kafka 插件模块
- `plugin-decompiler`: Java 反编译插件
- `plugin-git`: Git 能力插件

共享插件平台层仍放在根目录，便于直接识别依赖关系：

- `easy-postman-plugin-api`
- `easy-postman-plugin-runtime`
