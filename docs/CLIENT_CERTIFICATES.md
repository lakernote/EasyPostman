# SSL 客户端证书配置说明

## 功能概述

EasyPostman 现已支持 SSL 客户端证书（mTLS - mutual TLS）认证。当访问需要客户端证书验证的 API 时，可以根据不同的主机名或 URL 配置相应的客户端证书。

## 支持的证书格式

### 1. PFX/P12 格式（推荐）
- 单一文件包含证书和私钥
- 通常带有密码保护
- 文件扩展名：`.pfx`, `.p12`
- **优点**：文件管理简单，一个文件包含所有内容

### 2. PEM 格式
- 证书文件和私钥文件分开
- 证书文件扩展名：`.pem`, `.crt`
- 私钥文件扩展名：`.key`, `.pem`
- **注意**：目前仅支持未加密的私钥文件（不支持 ENCRYPTED PRIVATE KEY）

## 使用方法

### 1. 打开客户端证书设置

在主菜单中选择：**设置 (Settings)** → **客户端证书 (Client Certificates)**

### 2. 添加证书配置

点击 **添加证书** 按钮，填写以下信息：

- **名称**（可选）：为此证书配置命名，便于识别
- **主机名**（必填）：目标服务器的主机名
  - 精确匹配：`api.example.com`
  - 通配符匹配：`*.example.com`（匹配 api.example.com、test.example.com 等所有子域名，也匹配 example.com 本身）
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

#### 主机名匹配示例

| 证书配置 | 匹配的主机 | 不匹配的主机 |
|---------|-----------|------------|
| `api.example.com` | `api.example.com` | `test.example.com`, `example.com` |
| `*.example.com` | `api.example.com`, `test.example.com`, `example.com` | `api.test.example.com` |
| `example.com` | `example.com` | `api.example.com`, `www.example.com` |

## 配置示例

### 示例 1：单个 API 服务器（PFX 格式）

```
名称：Production API Client Cert
主机名：api.production.com
端口：443
证书类型：PFX
证书文件：/path/to/client-cert.pfx
密码：your-password
启用：✓
```

### 示例 2：多个子域名（通配符）

```
名称：Development Environment
主机名：*.dev.example.com
端口：0 (所有端口)
证书类型：PEM
证书文件：/path/to/cert.pem
私钥文件：/path/to/key.pem
密码：(留空)
启用：✓
```

### 示例 3：特定端口的服务

```
名称：Internal Service
主机名：internal.company.com
端口：8443
证书类型：PFX
证书文件：/path/to/internal.p12
密码：secret123
启用：✓
```

## 证书文件准备

### 从 PEM 转换为 PFX

如果您有 PEM 格式的证书和私钥，可以使用 OpenSSL 转换为 PFX：

```bash
openssl pkcs12 -export -out client-cert.pfx \
  -inkey private-key.pem \
  -in certificate.pem \
  -certfile ca-cert.pem
```

### 移除 PEM 私钥的密码保护

如果您的 PEM 私钥是加密的，需要先移除密码：

```bash
openssl rsa -in encrypted-key.pem -out unencrypted-key.pem
```

**警告**：未加密的私钥文件应妥善保管，建议使用 PFX 格式并设置密码。

## 安全注意事项

### 1. 证书文件安全
- 客户端证书和私钥非常敏感，请确保文件权限设置正确
- 建议使用带密码保护的 PFX/P12 格式
- 不要将证书文件提交到版本控制系统

### 2. 密码存储
- 证书密码以明文形式存储在配置文件中：`~/.easypostman/client_certificates.json`
- 该文件的访问权限应限制为当前用户
- 在共享计算机上使用时请格外小心

### 3. 证书有效期
- 定期检查证书的有效期
- 证书过期后需要更新配置

## 故障排查

### 证书未生效

1. **检查证书配置是否启用**
   - 确认证书配置中的"启用"选项已勾选

2. **检查主机名匹配**
   - 查看日志输出，确认主机名是否匹配
   - 使用通配符时注意大小写（匹配时会转换为小写）

3. **检查证书文件路径**
   - 确认证书文件存在且可读
   - PEM 格式需要同时提供证书文件和私钥文件

4. **检查密码**
   - PFX/P12 格式的证书通常需要密码
   - 密码错误会导致证书加载失败

5. **查看日志**
   - 日志中会显示证书加载和匹配的详细信息
   - 错误信息会帮助定位问题

### 常见错误

#### "Certificate file not found or not readable"
- 证书文件路径错误或文件不存在
- 文件权限不足，无法读取

#### "Failed to load client certificate"
- 证书文件格式错误
- 密码不正确（PFX/P12）
- PEM 私钥文件是加密的

#### "Encrypted PEM private keys are not supported"
- 您的 PEM 私钥文件是加密的
- 请移除密码保护或转换为 PFX 格式

## 技术细节

### 配置文件位置

- **macOS**: `~/.easypostman/client_certificates.json`
- **Windows**: `%USERPROFILE%\.easypostman\client_certificates.json`
- **Linux**: `~/.easypostman/client_certificates.json`

### 支持的算法

- **证书类型**: X.509
- **私钥算法**: RSA, EC (Elliptic Curve)
- **密钥存储**: PKCS#12 (PFX), PKCS#8 (PEM)

### mTLS 握手流程

1. 客户端向服务器发起 HTTPS 连接
2. 服务器发送证书并请求客户端证书
3. EasyPostman 根据主机名和端口查找匹配的客户端证书
4. 将客户端证书和私钥发送给服务器
5. 服务器验证客户端证书，完成双向认证

## 相关功能

- **SSL 验证模式**: 可以在请求设置中选择严格模式或宽松模式
- **证书信息查看**: 在请求响应中可以查看服务器证书信息
- **网络日志**: 可以查看 SSL 握手的详细过程

## 更新日志

- **v1.0.0**: 初始版本，支持 PFX/P12 和 PEM 格式
- **v1.0.1**: 修复通配符域名匹配逻辑，改进错误处理
- **v1.1.0** (计划): 支持加密的 PEM 私钥，密码加密存储

## 反馈与支持

如果您在使用客户端证书功能时遇到问题，请：

1. 查看日志输出以获取详细错误信息
2. 参考本文档的故障排查部分
3. 在 GitHub 提交 Issue 并提供详细信息（注意不要泄露敏感信息）

---

**提示**：客户端证书是企业级 API 认证的重要方式，正确配置可以提供更高的安全性。如果您的组织使用客户端证书认证，请咨询您的系统管理员获取正确的证书文件和配置信息。

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

