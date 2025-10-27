# SSL 客户端证书配置说明

## 功能概述

EasyPostman 现已支持 SSL 客户端证书（mTLS - mutual TLS）认证。当访问需要客户端证书验证的 API 时，可以根据不同的主机名或 URL 配置相应的客户端证书。

## 支持的证书格式

### 1. PFX/P12 格式（推荐）
- 单一文件包含证书和私钥
- 通常带有密码保护
- 文件扩展名：`.pfx`, `.p12`

### 2. PEM 格式
- 证书文件和私钥文件分开
- 证书文件扩展名：`.pem`, `.crt`
- 私钥文件扩展名：`.key`, `.pem`

## 使用方法

### 1. 打开客户端证书设置

在主菜单中选择：**设置 (Settings)** → **客户端证书 (Client Certificates)**

### 2. 添加证书配置

点击 **添加证书** 按钮，填写以下信息：

- **名称**（可选）：为此证书配置命名，便于识别
- **主机名**（必填）：目标服务器的主机名
  - 精确匹配：`api.example.com`
  - 通配符匹配：`*.example.com`（匹配所有 example.com 的子域名）
- **端口**：目标端口号
  - `0` 或留空：匹配所有端口
  - 指定端口号（如 `8443`）：仅匹配该端口
- **证书类型**：选择 PFX/P12 或 PEM
- **证书文件**（必填）：选择证书文件路径
- **私钥文件**（PEM 格式时必填）：选择私钥文件路径
- **密码**（可选）：证书或私钥的密码
- **启用**：勾选以启用此证书配置

### 3. 证书匹配规则

当发送 HTTPS 请求时，系统会自动查找匹配的客户端证书：

1. 按配置顺序检查每个证书
2. 第一个匹配的**已启用**证书将被使用
3. 匹配规则：
   - 主机名必须匹配（支持通配符）
   - 端口号必须匹配（或证书配置为匹配所有端口）

### 4. 证书管理

- **编辑**：选中证书后点击编辑按钮修改配置
- **删除**：选中证书后点击删除按钮移除配置
- **启用/禁用**：直接在表格中勾选或取消勾选 Enabled 列

## 配置示例

### 示例 1：为特定 API 配置证书

```
名称：Production API Certificate
主机名：api.prod.example.com
端口：443
证书类型：PFX
证书文件：/path/to/client-cert.pfx
密码：your-certificate-password
启用：✓
```

### 示例 2：为所有子域名配置证书

```
名称：Development Environment
主机名：*.dev.example.com
端口：0 (匹配所有端口)
证书类型：PEM
证书文件：/path/to/cert.pem
私钥文件：/path/to/key.pem
启用：✓
```

## 证书文件存储位置

客户端证书配置保存在：
- **Windows**: `C:\Users\{用户名}\.easy_postman\client_certificates.json`
- **macOS/Linux**: `~/.easy_postman/client_certificates.json`

> ⚠️ 注意：配置文件包含证书路径和密码（明文），请注意保护此文件的安全。

## 常见问题

### Q1: 支持哪些证书格式？
A: 目前支持 PFX/P12（推荐）和 PEM 格式。暂不支持加密的 PEM 私钥。

### Q2: 如何生成测试证书？
A: 可以使用 OpenSSL 工具生成：
```bash
# 生成 PFX 格式
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365
openssl pkcs12 -export -out client-cert.pfx -inkey key.pem -in cert.pem

# PEM 格式直接使用 key.pem 和 cert.pem
```

### Q3: 证书不生效怎么办？
A: 请检查：
1. 证书配置是否已启用
2. 主机名和端口是否匹配
3. 证书文件路径是否正确
4. 证书密码是否正确
5. 查看日志文件了解详细错误信息

### Q4: 如何验证证书是否被使用？
A: 发送请求后，在日志中会显示类似以下信息：
```
[INFO] Using client certificate for host: api.example.com (Production API Certificate)
```

## 技术实现

- 使用 Java KeyStore API 加载和管理证书
- 通过 OkHttp SSLSocketFactory 配置客户端证书
- 支持 TLS 1.2 和 TLS 1.3 协议
- 自动匹配最合适的证书配置

## 参考资料

- [Postman 客户端证书文档](https://learning.postman.com/docs/sending-requests/authorization/certificates/)
- [mTLS 介绍](https://en.wikipedia.org/wiki/Mutual_authentication)
- [OpenSSL 证书管理](https://www.openssl.org/docs/man1.1.1/man1/openssl.html)

