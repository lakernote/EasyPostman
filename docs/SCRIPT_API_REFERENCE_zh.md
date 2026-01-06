# Script API 参考手册

本文档提供了 EasyPostman 脚本功能的完整 API 参考。所有 API 均基于实际代码整理，确保准确可用。

## 目录

- [全局对象](#全局对象)
- [pm 对象](#pm-对象)
- [pm.environment - 环境变量](#pmenvironment---环境变量)
- [全局变量（Global Variables）](#全局变量global-variables)
- [pm.variables - 临时变量](#pmvariables---临时变量)
- [pm.request - 请求对象](#pmrequest---请求对象)
- [pm.response - 响应对象](#pmresponse---响应对象)
- [pm.cookies - Cookie 管理](#pmcookies---cookie-管理)
- [pm.expect - 断言](#pmexpect---断言)
- [pm.test - 测试](#pmtest---测试)
- [console - 控制台](#console---控制台)

---

## 全局对象

### pm
主要的 Postman API 对象，提供了所有脚本功能的访问入口。

---

## pm 对象

### 方法列表

| 方法 | 说明 | 示例 |
|------|------|------|
| `pm.test(name, fn)` | 定义一个测试 | `pm.test("状态码是 200", () => {})` |
| `pm.expect(value)` | 创建断言 | `pm.expect(200).to.equal(200)` |
| `pm.uuid()` | 生成 UUID | `pm.uuid()` |
| `pm.generateUUID()` | 生成 UUID（别名） | `pm.generateUUID()` |
| `pm.getTimestamp()` | 获取当前时间戳（毫秒） | `pm.getTimestamp()` |
| `pm.setVariable(key, value)` | 设置临时变量 | `pm.setVariable('userId', '123')` |
| `pm.getVariable(key)` | 获取临时变量 | `pm.getVariable('userId')` |
| `pm.setGlobalVariable(key, value)` | 设置全局变量（实际存储在环境变量中） | `pm.setGlobalVariable('baseUrl', 'https://api.com')` |
| `pm.getGlobalVariable(key)` | 获取全局变量（实际从环境变量读取） | `pm.getGlobalVariable('baseUrl')` |
| `pm.getResponseCookie(name)` | 获取响应中的 Cookie | `pm.getResponseCookie('sessionId')` |

---

## pm.environment - 环境变量

环境变量的读写操作。

### 方法列表

| 方法 | 参数 | 返回值 | 说明 | 示例 |
|------|------|--------|------|------|
| `get(key)` | key: String | String | 获取环境变量 | `pm.environment.get('token')` |
| `set(key, value)` | key: String, value: Any | void | 设置环境变量 | `pm.environment.set('token', 'abc123')` |
| `unset(key)` | key: String | void | 删除环境变量 | `pm.environment.unset('token')` |
| `has(key)` | key: String | Boolean | 检查环境变量是否存在 | `pm.environment.has('token')` |
| `clear()` | 无 | void | 清空所有环境变量 | `pm.environment.clear()` |

---

## 全局变量（Global Variables）

EasyPostman **没有独立的 `pm.globals` 对象**，但提供了全局变量方法作为替代。

### 重要说明

- ❌ **不支持**：`pm.globals.set()` / `pm.globals.get()` - 因为没有 `pm.globals` 对象
- ✅ **支持**：`pm.setGlobalVariable()` / `pm.getGlobalVariable()` - 直接调用方法
- 💡 **实现方式**：全局变量实际存储在环境变量中（内部实现相同）

### 方法列表

| 方法 | 参数 | 说明 | 示例 |
|------|------|------|------|
| `pm.setGlobalVariable(key, value)` | key: String<br>value: Any | 设置全局变量 | `pm.setGlobalVariable('baseUrl', 'https://api.com')` |
| `pm.getGlobalVariable(key)` | key: String | 获取全局变量 | `pm.getGlobalVariable('baseUrl')` |

### 使用示例

```javascript
// ✅ 正确用法 - 使用方法调用
pm.setGlobalVariable('apiKey', 'abc123');
pm.setGlobalVariable('timeout', 5000);

const apiKey = pm.getGlobalVariable('apiKey');
console.log('API Key:', apiKey);

// ❌ 错误用法 - pm.globals 对象不存在
// pm.globals.set('key', 'value');  // 报错！
// pm.globals.get('key');           // 报错！

// 💡 推荐：直接使用 pm.environment（效果相同）
pm.environment.set('apiKey', 'abc123');
const apiKey2 = pm.environment.get('apiKey');
```

### 注意事项

由于全局变量和环境变量在内部实现上是相同的，建议统一使用 `pm.environment` 以保持代码一致性。

---

## pm.variables - 临时变量

临时变量管理，仅在当前请求执行过程中有效（不会持久化）。

### 方法列表

| 方法 | 参数 | 返回值 | 说明 | 示例 |
|------|------|--------|------|------|
| `get(key)` | key: String | String | 获取临时变量 | `pm.variables.get('userId')` |
| `set(key, value)` | key: String, value: Any | void | 设置临时变量 | `pm.variables.set('userId', 123)` |
| `has(key)` | key: String | Boolean | 检查临时变量是否存在 | `pm.variables.has('userId')` |
| `unset(key)` | key: String | void | 删除临时变量 | `pm.variables.unset('userId')` |
| `clear()` | 无 | void | 清空所有临时变量 | `pm.variables.clear()` |
| `toObject()` | 无 | Object | 获取所有临时变量对象（键值对） | `pm.variables.toObject()` |

---

## pm.request - 请求对象

访问和操作当前 HTTP 请求的信息（主要在 Pre-request 脚本中使用）。

### 属性

| 属性 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `id` | String | 请求唯一标识 | `pm.request.id` |
| `url` | UrlWrapper | 请求 URL 对象 | `pm.request.url` |
| `urlStr` | String | 请求 URL 字符串 | `pm.request.urlStr` |
| `method` | String | HTTP 方法 | `pm.request.method` |
| `headers` | JsListWrapper | 请求头列表 | `pm.request.headers` |
| `body` | String | 请求体内容 | `pm.request.body` |
| `formData` | JsListWrapper | 表单数据列表（multipart） | `pm.request.formData` |
| `urlencoded` | JsListWrapper | URL 编码表单数据列表 | `pm.request.urlencoded` |
| `params` | JsListWrapper | URL 查询参数列表 | `pm.request.params` |
| `isMultipart` | Boolean | 是否为 multipart 请求 | `pm.request.isMultipart` |
| `followRedirects` | Boolean | 是否跟随重定向 | `pm.request.followRedirects` |
| `logEvent` | Boolean | 是否记录事件日志 | `pm.request.logEvent` |

### URL 对象方法

| 方法 | 返回值 | 说明 | 示例 |
|------|--------|------|------|
| `toString()` | String | 获取完整 URL 字符串 | `pm.request.url.toString()` |
| `getHost()` | String | 获取主机名 | `pm.request.url.getHost()` |
| `getPath()` | String | 获取路径 | `pm.request.url.getPath()` |
| `getQueryString()` | String | 获取查询字符串 | `pm.request.url.getQueryString()` |
| `getPathWithQuery()` | String | 获取路径和查询字符串 | `pm.request.url.getPathWithQuery()` |

### URL Query 对象

访问查询参数：`pm.request.url.query`

| 方法 | 返回值 | 说明 | 示例 |
|------|--------|------|------|
| `all()` | Array | 获取所有查询参数 | `pm.request.url.query.all()` |

### Headers/FormData/Urlencoded/Params 集合方法

这些集合都是 `JsListWrapper` 类型，支持以下方法：

| 方法 | 参数 | 返回值 | 说明 | 示例 |
|------|------|--------|------|------|
| `add(item)` | item: Object | void | 添加一项 | `pm.request.headers.add({key: 'X-Custom', value: 'test'})` |
| `remove(keyOrPredicate)` | key: String/Function | void | 删除一项 | `pm.request.headers.remove('X-Custom')` |
| `upsert(item)` | item: Object | void | 更新或插入一项 | `pm.request.headers.upsert({key: 'X-Custom', value: 'new'})` |
| `get(key)` | key: String | String | 获取指定键的值 | `pm.request.headers.get('Content-Type')` |
| `has(key)` | key: String | Boolean | 检查是否存在指定键 | `pm.request.headers.has('Authorization')` |
| `all()` | 无 | Array | 获取所有项 | `pm.request.headers.all()` |
| `count()` | 无 | Number | 获取项数 | `pm.request.headers.count()` |
| `clear()` | 无 | void | 清空所有项 | `pm.request.headers.clear()` |
| `each(callback)` | callback: Function | void | 遍历每一项 | `pm.request.headers.each(h => console.log(h))` |

### 使用示例

```javascript
// 添加请求头
pm.request.headers.add({
    key: "Authorization",
    value: "Bearer " + pm.environment.get("token")
});

// 添加查询参数
pm.request.params.add({
    key: "timestamp",
    value: Date.now().toString()
});

// 添加表单数据
pm.request.formData.add({
    key: "username",
    value: "john"
});

// 获取 URL 信息
console.log("Host:", pm.request.url.getHost());
console.log("Path:", pm.request.url.getPath());
```

---

## pm.response - 响应对象

访问 HTTP 响应的信息（仅在 Post-request 脚本中可用）。

### 属性

| 属性 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `code` | Number | HTTP 状态码 | `pm.response.code` |
| `status` | String | HTTP 状态文本 | `pm.response.status` |
| `headers` | Headers | 响应头对象 | `pm.response.headers` |
| `responseTime` | Number | 响应时间（毫秒） | `pm.response.responseTime` |
| `to` | ResponseAssertion | 链式断言语法支持 | `pm.response.to.have.status(200)` |
| `have` | ResponseAssertion | 链式断言语法支持 | `pm.response.to.have.header('Content-Type')` |
| `be` | ResponseAssertion | 链式断言语法支持 | 用于链式调用 |

### 方法列表

| 方法 | 返回值 | 说明 | 示例 |
|------|--------|------|------|
| `text()` | String | 获取响应体文本 | `pm.response.text()` |
| `json()` | Object | 获取响应体 JSON 对象 | `pm.response.json()` |
| `size()` | ResponseSize | 获取响应大小信息 | `pm.response.size()` |

### 响应断言方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `to.have.status(code)` | 断言状态码 | `pm.response.to.have.status(200)` |
| `to.have.header(name)` | 断言包含响应头 | `pm.response.to.have.header('Content-Type')` |
| `to.be.below(ms)` | 断言响应时间小于指定值 | `pm.expect(pm.response.responseTime).to.be.below(1000)` |

### Headers 对象方法

访问响应头：`pm.response.headers`

| 方法 | 参数 | 返回值 | 说明 | 示例 |
|------|------|--------|------|------|
| `get(name)` | name: String | String | 获取响应头值 | `pm.response.headers.get('Content-Type')` |
| `has(name)` | name: String | Boolean | 检查响应头是否存在 | `pm.response.headers.has('Set-Cookie')` |
| `count()` | 无 | Number | 获取响应头数量 | `pm.response.headers.count()` |
| `all()` | 无 | Array | 获取所有响应头 | `pm.response.headers.all()` |
| `each(callback)` | callback: Function | void | 遍历所有响应头 | `pm.response.headers.each(h => console.log(h))` |

### ResponseSize 对象

`pm.response.size()` 返回的对象包含以下属性：

| 属性 | 类型 | 说明 |
|------|------|------|
| `body` | Number | 响应体大小（字节） |
| `header` | Number | 响应头大小（字节） |
| `total` | Number | 总大小（字节） |

### 使用示例

```javascript
// 获取响应数据
const jsonData = pm.response.json();
console.log("Status:", pm.response.status);
console.log("Code:", pm.response.code);

// 断言状态码
pm.response.to.have.status(200);

// 断言响应头
pm.response.to.have.header('Content-Type');

// 获取响应头
const contentType = pm.response.headers.get('Content-Type');

// 获取响应大小
const size = pm.response.size();
console.log("响应体大小:", size.body, "bytes");
```

---

## pm.cookies - Cookie 管理

管理和访问 Cookie。

### 方法列表

| 方法 | 参数 | 返回值 | 说明 | 示例 |
|------|------|--------|------|------|
| `get(name)` | name: String | Cookie | 获取指定名称的 Cookie | `pm.cookies.get('sessionId')` |
| `set(cookie)` | cookie: Cookie/String | void | 设置 Cookie | `pm.cookies.set({name: 'key', value: 'val'})` |
| `getAll()` | 无 | Array | 获取所有 Cookie | `pm.cookies.getAll()` |
| `has(name)` | name: String | Boolean | 检查 Cookie 是否存在 | `pm.cookies.has('sessionId')` |
| `toObject()` | 无 | Object | 获取所有 Cookie 对象（键值对形式） | `pm.cookies.toObject()` |
| `jar()` | 无 | CookieJar | 获取 CookieJar 对象 | `pm.cookies.jar()` |

### CookieJar 对象

CookieJar 用于跨域管理 Cookie，通过 `pm.cookies.jar()` 获取。

#### 方法列表

| 方法 | 参数 | 说明 | 示例 |
|------|------|------|------|
| `set(url, cookie, callback)` | url: String<br>cookie: String/Object<br>callback: Function | 设置指定 URL 的 Cookie | `jar.set(url, 'key=value', callback)` |
| `get(url, name, callback)` | url: String<br>name: String<br>callback: Function | 获取指定 URL 的 Cookie | `jar.get(url, 'sessionId', callback)` |
| `getAll(url, callback)` | url: String<br>callback: Function | 获取指定 URL 的所有 Cookie | `jar.getAll(url, callback)` |
| `unset(url, name, callback)` | url: String<br>name: String<br>callback: Function | 删除指定 URL 的 Cookie | `jar.unset(url, 'sessionId', callback)` |
| `clear(url, callback)` | url: String<br>callback: Function | 清空指定 URL 的所有 Cookie | `jar.clear(url, callback)` |

### Cookie 对象属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `name` | String | Cookie 名称 |
| `value` | String | Cookie 值 |
| `domain` | String | Cookie 域 |
| `path` | String | Cookie 路径 |
| `expires` | String | 过期时间 |
| `maxAge` | Number | 最大存活时间（秒） |
| `httpOnly` | Boolean | 是否仅 HTTP |
| `secure` | Boolean | 是否安全传输（HTTPS） |
| `sameSite` | String | SameSite 属性 |

### 使用示例

```javascript
// 获取 Cookie
const sessionId = pm.cookies.get('sessionId');
if (sessionId) {
    console.log('Session ID:', sessionId.value);
}

// 设置 Cookie
pm.cookies.set({
    name: 'myToken',
    value: 'abc123',
    domain: 'example.com',
    path: '/'
});

// 检查 Cookie 是否存在
if (pm.cookies.has('sessionId')) {
    console.log('Session cookie exists');
}

// 获取所有 Cookie
const allCookies = pm.cookies.getAll();
console.log('Total cookies:', allCookies.length);

// 使用 CookieJar 跨域设置 Cookie
const jar = pm.cookies.jar();
jar.set('https://api.example.com', 'token=xyz', (error, cookie) => {
    if (error) {
        console.error('设置 cookie 失败:', error);
    } else {
        console.log('Cookie 设置成功:', cookie);
    }
});

// 使用 CookieJar 获取 Cookie
jar.get('https://api.example.com', 'token', (error, cookie) => {
    if (!error && cookie) {
        console.log('Token:', cookie.value);
    }
});
```

---

## pm.expect - 断言

使用链式断言进行测试（类 Chai.js 风格）。

### 链式语法支持

| 链式属性 | 说明 |
|---------|------|
| `to` | 链式连接词 |
| `be` | 链式连接词 |
| `have` | 链式连接词 |

### 支持的断言方法

| 断言 | 参数 | 说明 | 示例 |
|------|------|------|------|
| `equal(value)` | value: Any | 严格相等（深度比较） | `pm.expect(200).to.equal(200)` |
| `eql(value)` | value: Any | 深度相等（与 equal 相同） | `pm.expect({a: 1}).to.eql({a: 1})` |
| `include(substring)` | substring: String | 包含子串 | `pm.expect('hello world').to.include('hello')` |
| `property(key)` | key: String | 包含属性（仅支持 Map/Object） | `pm.expect(obj).to.have.property('id')` |
| `match(regex)` | regex: String/Pattern/RegExp | 匹配正则表达式 | `pm.expect('hello').to.match(/^h/)` |
| `below(number)` | number: Number | 数值小于指定值 | `pm.expect(5).to.be.below(10)` |

### 使用示例

```javascript
// 基本相等断言
pm.test("Status code is 200", function() {
    pm.expect(pm.response.code).to.equal(200);
});

// 深度相等断言
pm.test("Response data matches", function() {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.eql({status: "success"});
});

// 包含子串
pm.test("Response contains success", function() {
    pm.expect(pm.response.text()).to.include("success");
});

// 属性存在
pm.test("Response has userId property", function() {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('userId');
});

// 正则匹配
pm.test("Email format is correct", function() {
    pm.expect(email).to.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/);
});

// 数值比较
pm.test("Response time is acceptable", function() {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});
```

### 注意事项

- 当前实现支持的断言方法有限，主要包括：equal、eql、include、property、match、below
- 不支持：above、least、most、within、length、keys、members、true、false、null、undefined、ok、empty 等
- 如需更多断言功能，建议使用 pm.test 结合简单的 if 判断

---

## pm.test - 测试

定义和管理测试用例。

### 主要方法

#### pm.test(name, function)

定义一个测试用例。

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | String | 测试名称 |
| `function` | Function | 测试函数（可使用 pm.expect 或 pm.response 断言） |

#### pm.test.index()

获取所有测试结果（通常在测试执行完成后调用）。

| 返回值 | 说明 |
|--------|------|
| Array | 测试结果数组，每个元素包含：<br>- `id`: 测试 ID<br>- `name`: 测试名称<br>- `passed`: 是否通过（Boolean）<br>- `errorMessage`: 错误信息（失败时） |

### 使用示例

```javascript
// 定义测试 - 状态码检查
pm.test("状态码是 200", function () {
    pm.response.to.have.status(200);
});

// 定义测试 - 使用 pm.expect
pm.test("响应时间小于 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// 定义测试 - JSON 数据验证
pm.test("响应包含用户 ID", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('userId');
    pm.expect(jsonData.userId).to.equal(123);
});

// 定义测试 - 响应头检查
pm.test("响应包含 Content-Type", function () {
    pm.response.to.have.header('Content-Type');
});

// 获取所有测试结果
const results = pm.test.index();
results.forEach(function(result) {
    console.log(result.name + ": " + (result.passed ? "通过" : "失败"));
    if (!result.passed) {
        console.log("  错误:", result.errorMessage);
    }
});
```

### TestResult 对象结构

```javascript
{
    id: "uuid-string",           // 测试唯一标识
    name: "测试名称",             // 测试名称
    passed: true,                // 是否通过
    errorMessage: null           // 错误信息（passed 为 false 时有值）
}
```

---

## console - 控制台

输出调试信息。

### 方法列表

| 方法 | 参数 | 说明 | 示例 |
|------|------|------|------|
| `log(message, ...)` | message: Any | 输出日志 | `console.log('Hello', 'World')` |
| `info(message, ...)` | message: Any | 输出信息 | `console.info('Info message')` |
| `warn(message, ...)` | message: Any | 输出警告 | `console.warn('Warning message')` |
| `error(message, ...)` | message: Any | 输出错误 | `console.error('Error message')` |

---

## 完整示例

### Pre-request Script 示例

```javascript
// 1. 设置环境变量
pm.environment.set('timestamp', Date.now());
pm.environment.set('requestId', pm.uuid());

// 2. 设置临时变量
pm.variables.set('localVar', 'tempValue');

// 3. 添加请求头
pm.request.headers.add({
    key: 'X-Request-Time',
    value: new Date().toISOString()
});

pm.request.headers.add({
    key: 'X-Request-ID',
    value: pm.environment.get('requestId')
});

// 4. 添加查询参数
pm.request.params.add({
    key: 'timestamp',
    value: pm.getTimestamp().toString()
});

// 5. 修改 URL 编码表单数据
pm.request.urlencoded.add({
    key: 'username',
    value: 'john'
});

// 6. 添加 multipart 表单数据
pm.request.formData.add({
    key: 'userId',
    value: '123'
});

// 7. 输出调试信息
console.log('Request URL:', pm.request.url.toString());
console.log('Request Method:', pm.request.method);
console.log('Request ID:', pm.environment.get('requestId'));
```

### Post-request Script 示例

```javascript
// 1. 状态码测试
pm.test("状态码是 200", function () {
    pm.response.to.have.status(200);
});

// 2. 响应时间测试
pm.test("响应时间小于 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// 3. 响应头测试
pm.test("响应包含 Content-Type", function () {
    pm.response.to.have.header('Content-Type');
});

const contentType = pm.response.headers.get('Content-Type');
console.log('Content-Type:', contentType);

// 4. JSON 结构测试
pm.test("响应包含正确的数据结构", function () {
    const jsonData = pm.response.json();
    
    // 检查属性存在
    pm.expect(jsonData).to.have.property('status');
    pm.expect(jsonData).to.have.property('data');
    
    // 检查值
    pm.expect(jsonData.status).to.equal('success');
});

// 5. 字符串包含测试
pm.test("响应体包含 success", function () {
    pm.expect(pm.response.text()).to.include('success');
});

// 6. 正则匹配测试
pm.test("响应包含有效的 email", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.email).to.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/);
});

// 7. 保存响应数据到环境变量
const responseData = pm.response.json();
if (responseData.token) {
    pm.environment.set('authToken', responseData.token);
    console.log('Token saved:', responseData.token);
}

if (responseData.userId) {
    pm.environment.set('userId', responseData.userId);
}

// 8. Cookie 管理
pm.test("检查 session cookie", function () {
    pm.expect(pm.cookies.has('sessionId')).to.equal(true);
});

const sessionCookie = pm.cookies.get('sessionId');
if (sessionCookie) {
    console.log('Session ID:', sessionCookie.value);
    pm.environment.set('sessionId', sessionCookie.value);
}

// 9. 从响应头获取 Cookie
const responseCookie = pm.getResponseCookie('JSESSIONID');
if (responseCookie) {
    console.log('JSESSIONID:', responseCookie.value);
    pm.environment.set('jsessionId', responseCookie.value);
}

// 10. 使用 CookieJar 跨域设置 Cookie
const jar = pm.cookies.jar();
jar.set('https://api.example.com', 'custom_token=xyz123', function (error, cookie) {
    if (error) {
        console.error('设置 cookie 失败:', error);
    } else {
        console.log('Cookie 设置成功');
    }
});

// 11. 获取响应大小信息
const size = pm.response.size();
console.log('响应体大小:', size.body, 'bytes');
console.log('响应头大小:', size.header, 'bytes');
console.log('总大小:', size.total, 'bytes');

// 12. 遍历所有响应头
console.log('所有响应头:');
pm.response.headers.each(function(header) {
    console.log('  ' + header.key + ': ' + header.value);
});

// 13. 获取所有测试结果
const testResults = pm.test.index();
console.log('测试结果统计:');
let passCount = 0;
let failCount = 0;
testResults.forEach(function(result) {
    if (result.passed) {
        passCount++;
    } else {
        failCount++;
        console.log('失败的测试:', result.name, '-', result.errorMessage);
    }
});
console.log('通过:', passCount, '失败:', failCount);
```

### 数据提取和链式操作示例

```javascript
// 从 JSON 响应中提取嵌套数据
pm.test("提取用户信息", function () {
    const jsonData = pm.response.json();
    
    // 假设响应结构：{ data: { user: { id: 123, name: "John" } } }
    pm.expect(jsonData).to.have.property('data');
    
    const userData = jsonData.data.user;
    pm.expect(userData).to.have.property('id');
    pm.expect(userData).to.have.property('name');
    
    // 保存到环境变量
    pm.environment.set('currentUserId', userData.id.toString());
    pm.environment.set('currentUserName', userData.name);
});

// 处理数组响应
pm.test("处理数组数据", function () {
    const jsonData = pm.response.json();
    
    // 假设响应是数组
    pm.expect(Array.isArray(jsonData.items)).to.equal(true);
    
    // 检查第一个元素
    if (jsonData.items.length > 0) {
        const firstItem = jsonData.items[0];
        pm.expect(firstItem).to.have.property('id');
        
        // 保存第一个项的 ID
        pm.environment.set('firstItemId', firstItem.id.toString());
    }
});
```

### 使用内置库示例

```javascript
// 使用 CryptoJS 进行加密
var CryptoJS = require('crypto-js');
var encrypted = CryptoJS.AES.encrypt('secret message', 'secret-key').toString();
pm.environment.set('encrypted', encrypted);

// 使用 Lodash 处理数据
var _ = require('lodash');
var data = [1, 2, 3, 4, 5];
var filtered = _.filter(data, function(n) { return n > 2; });
console.log('Filtered:', filtered); // [3, 4, 5]

// 使用 Moment 处理日期
var moment = require('moment');
var now = moment().format('YYYY-MM-DD HH:mm:ss');
pm.environment.set('currentTime', now);
console.log('Current time:', now);
```

---

## 注意事项

1. **作用域限制**
   - Pre-request 脚本中无法访问 `pm.response`
   - `pm.response` 仅在 Post-request 脚本中可用

2. **变量类型**
   - `pm.environment` 用于持久化变量存储
   - `pm.variables` 是临时变量，仅在当前请求生命周期内有效
   - **没有 `pm.globals` 对象**，但可以使用 `pm.setGlobalVariable()` 和 `pm.getGlobalVariable()` 方法
   - 全局变量实际上也存储在环境变量中（内部实现相同）
   - 环境变量会被持久化保存到文件，临时变量不会

3. **断言限制**
   - 当前仅支持有限的断言方法：`equal`、`eql`、`include`、`property`、`match`、`below`
   - 不支持完整的 Chai.js 断言库（如 `above`、`length`、`keys`、`true`、`false` 等）
   - 建议使用简单的 if 判断配合 `throw new Error()` 来实现复杂断言

4. **Cookie 管理**
   - `pm.cookies` 提供当前请求域的 Cookie 访问
   - `pm.cookies.jar()` 可以跨域管理 Cookie，需要完整的 URL
   - Cookie 操作是异步的，使用回调函数处理结果

5. **类型转换**
   - 使用 `pm.response.json()` 前确保响应是合法的 JSON 格式
   - 环境变量存储时会自动转换为字符串
   - 使用 `.toString()` 确保数值类型正确转换

6. **集合操作**
   - `pm.request.headers`、`formData`、`urlencoded`、`params` 都是 `JsListWrapper` 类型
   - 对这些集合的修改会直接影响实际发送的请求（仅在 Pre-request 中有效）
   - 使用 `add()`、`remove()`、`upsert()` 进行集合操作

7. **内置库**
   - 支持 `crypto-js`、`lodash`、`moment` 三个内置库
   - 使用 `require('library-name')` 加载库
   - 库代码会被缓存，重复加载不会影响性能

8. **不支持的功能**
   - ❌ `pm.sendRequest()` - 不支持在脚本中发送 HTTP 请求
   - ❌ `pm.iterationData` - 不支持迭代数据（但支持 CSV 数据驱动）
   - ❌ `pm.info` - 不支持请求元信息访问
   - ❌ 完整的 Chai.js 断言库

---

## 快速参考

### 常用 API 速查

```javascript
// ===== 环境变量 =====
pm.environment.set('key', 'value')        // 设置
pm.environment.get('key')                 // 获取
pm.environment.has('key')                 // 检查
pm.environment.unset('key')               // 删除
pm.environment.clear()                    // 清空

// ===== 全局变量（实际存储在环境中）=====
pm.setGlobalVariable('key', 'value')      // 设置全局变量
pm.getGlobalVariable('key')               // 获取全局变量

// ===== 临时变量 =====
pm.variables.set('key', 'value')          // 设置
pm.variables.get('key')                   // 获取
pm.variables.has('key')                   // 检查
pm.variables.unset('key')                 // 删除

// ===== 请求操作 (Pre-request) =====
pm.request.headers.add({key, value})      // 添加请求头
pm.request.params.add({key, value})       // 添加查询参数
pm.request.formData.add({key, value})     // 添加表单数据
pm.request.url.toString()                 // 获取 URL

// ===== 响应访问 (Post-request) =====
pm.response.code                          // 状态码
pm.response.status                        // 状态文本
pm.response.responseTime                  // 响应时间
pm.response.text()                        // 响应文本
pm.response.json()                        // 响应 JSON
pm.response.headers.get('name')           // 获取响应头
pm.response.size()                        // 响应大小

// ===== Cookie =====
pm.cookies.get('name')                    // 获取 Cookie
pm.cookies.set({name, value})             // 设置 Cookie
pm.cookies.has('name')                    // 检查 Cookie
pm.getResponseCookie('name')              // 从响应获取

// ===== 测试断言 =====
pm.test("测试名", function() {            // 定义测试
    pm.response.to.have.status(200)       // 断言状态码
    pm.expect(value).to.equal(expected)   // 相等断言
    pm.expect(str).to.include(substr)     // 包含断言
    pm.expect(obj).to.have.property('k')  // 属性断言
    pm.expect(str).to.match(/regex/)      // 正则断言
    pm.expect(num).to.be.below(max)       // 数值断言
})

// ===== 工具方法 =====
pm.uuid()                                 // 生成 UUID
pm.getTimestamp()                         // 获取时间戳
console.log(message)                      // 输出日志

// ===== 内置库 =====
var CryptoJS = require('crypto-js')       // 加密库
var _ = require('lodash')                 // 工具库
var moment = require('moment')            // 日期库
```

---

## 参考资源

- [Postman 官方文档](https://learning.postman.com/docs/writing-scripts/intro-to-scripts/)
- [ChaiJS 断言库](https://www.chaijs.com/api/bdd/)

