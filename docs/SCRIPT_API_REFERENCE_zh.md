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
- [外部数据源脚本 API](#外部数据源脚本-api)
- [console - 控制台](#console---控制台)
- [内置 JavaScript 库](#内置-javascript-库)
- [完整示例](#完整示例)
- [注意事项](#注意事项)
- [快速参考](#快速参考)
- [参考资源](#参考资源)

---

## 全局对象

### pm

主要的 Postman API 对象，提供了所有脚本功能的访问入口。

---

## pm 对象

### 方法列表

| 方法                                 | 说明                 | 示例                                                   |
|------------------------------------|--------------------|------------------------------------------------------|
| `pm.test(name, fn)`                | 定义一个测试             | `pm.test("状态码是 200", () => {})`                      |
| `pm.expect(value)`                 | 创建断言               | `pm.expect(200).to.equal(200)`                       |
| `pm.uuid()`                        | 生成 UUID            | `pm.uuid()`                                          |
| `pm.generateUUID()`                | 生成 UUID（别名）        | `pm.generateUUID()`                                  |
| `pm.getTimestamp()`                | 获取当前时间戳（毫秒）        | `pm.getTimestamp()`                                  |
| `pm.setVariable(key, value)`       | 设置临时变量             | `pm.setVariable('userId', '123')`                    |
| `pm.getVariable(key)`              | 获取临时变量             | `pm.getVariable('userId')`                           |
| `pm.setGlobalVariable(key, value)` | 设置全局变量（实际存储在环境变量中） | `pm.setGlobalVariable('baseUrl', 'https://api.com')` |
| `pm.getGlobalVariable(key)`        | 获取全局变量（实际从环境变量读取）  | `pm.getGlobalVariable('baseUrl')`                    |
| `pm.getResponseCookie(name)`       | 获取响应中的 Cookie      | `pm.getResponseCookie('sessionId')`                  |

### 外部数据源对象

| 对象 | 说明 | 常用方法 |
|------|------|----------|
| `pm.redis` | Redis 读写与查询 | `execute(options)`、`query(options)` |
| `pm.kafka` | Kafka 查询、发消息、消费消息 | `listTopics(options)`、`send(options)`、`poll(options)` |
| `pm.es` / `pm.elasticsearch` | Elasticsearch 请求与查询 | `request(options)`、`query(options)` |
| `pm.influx` / `pm.influxdb` | InfluxDB Flux / InfluxQL 查询与写入 | `query(options)`、`write(options)`、`request(options)` |

---

## pm.environment - 环境变量

环境变量的读写操作。

### 方法列表

| 方法                | 参数                      | 返回值     | 说明         | 示例                                      |
|-------------------|-------------------------|---------|------------|-----------------------------------------|
| `get(key)`        | key: String             | String  | 获取环境变量     | `pm.environment.get('token')`           |
| `set(key, value)` | key: String, value: Any | void    | 设置环境变量     | `pm.environment.set('token', 'abc123')` |
| `unset(key)`      | key: String             | void    | 删除环境变量     | `pm.environment.unset('token')`         |
| `has(key)`        | key: String             | Boolean | 检查环境变量是否存在 | `pm.environment.has('token')`           |
| `clear()`         | 无                       | void    | 清空所有环境变量   | `pm.environment.clear()`                |

---

## 全局变量（Global Variables）

EasyPostman **没有独立的 `pm.globals` 对象**，但提供了全局变量方法作为替代。

### 重要说明

- ❌ **不支持**：`pm.globals.set()` / `pm.globals.get()` - 因为没有 `pm.globals` 对象
- ✅ **支持**：`pm.setGlobalVariable()` / `pm.getGlobalVariable()` - 直接调用方法
- 💡 **实现方式**：全局变量实际存储在环境变量中（内部实现相同）

### 方法列表

| 方法                                 | 参数                        | 说明     | 示例                                                   |
|------------------------------------|---------------------------|--------|------------------------------------------------------|
| `pm.setGlobalVariable(key, value)` | key: String<br>value: Any | 设置全局变量 | `pm.setGlobalVariable('baseUrl', 'https://api.com')` |
| `pm.getGlobalVariable(key)`        | key: String               | 获取全局变量 | `pm.getGlobalVariable('baseUrl')`                    |

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

| 方法                | 参数                      | 返回值     | 说明              | 示例                                |
|-------------------|-------------------------|---------|-----------------|-----------------------------------|
| `get(key)`        | key: String             | String  | 获取临时变量          | `pm.variables.get('userId')`      |
| `set(key, value)` | key: String, value: Any | void    | 设置临时变量          | `pm.variables.set('userId', 123)` |
| `has(key)`        | key: String             | Boolean | 检查临时变量是否存在      | `pm.variables.has('userId')`      |
| `unset(key)`      | key: String             | void    | 删除临时变量          | `pm.variables.unset('userId')`    |
| `clear()`         | 无                       | void    | 清空所有临时变量        | `pm.variables.clear()`            |
| `toObject()`      | 无                       | Object  | 获取所有临时变量对象（键值对） | `pm.variables.toObject()`         |

---

## pm.request - 请求对象

访问和操作当前 HTTP 请求的信息（主要在 Pre-request 脚本中使用）。

### 属性

| 属性                | 类型            | 说明                | 示例                           |
|-------------------|---------------|-------------------|------------------------------|
| `id`              | String        | 请求唯一标识            | `pm.request.id`              |
| `url`             | UrlWrapper    | 请求 URL 对象         | `pm.request.url`             |
| `urlStr`          | String        | 请求 URL 字符串        | `pm.request.urlStr`          |
| `method`          | String        | HTTP 方法           | `pm.request.method`          |
| `headers`         | JsListWrapper | 请求头列表             | `pm.request.headers`         |
| `body`            | String        | 请求体内容             | `pm.request.body`            |
| `formData`        | JsListWrapper | 表单数据列表（multipart） | `pm.request.formData`        |
| `urlencoded`      | JsListWrapper | URL 编码表单数据列表      | `pm.request.urlencoded`      |
| `params`          | JsListWrapper | URL 查询参数列表        | `pm.request.params`          |
| `isMultipart`     | Boolean       | 是否为 multipart 请求  | `pm.request.isMultipart`     |
| `followRedirects` | Boolean       | 是否跟随重定向           | `pm.request.followRedirects` |
| `logEvent`        | Boolean       | 是否记录事件日志          | `pm.request.logEvent`        |

### URL 对象方法

| 方法                   | 返回值    | 说明           | 示例                                  |
|----------------------|--------|--------------|-------------------------------------|
| `toString()`         | String | 获取完整 URL 字符串 | `pm.request.url.toString()`         |
| `getHost()`          | String | 获取主机名        | `pm.request.url.getHost()`          |
| `getPath()`          | String | 获取路径         | `pm.request.url.getPath()`          |
| `getQueryString()`   | String | 获取查询字符串      | `pm.request.url.getQueryString()`   |
| `getPathWithQuery()` | String | 获取路径和查询字符串   | `pm.request.url.getPathWithQuery()` |

### URL Query 对象

访问查询参数：`pm.request.url.query`

| 方法      | 返回值   | 说明       | 示例                           |
|---------|-------|----------|------------------------------|
| `all()` | Array | 获取所有查询参数 | `pm.request.url.query.all()` |

### Headers/FormData/Urlencoded/Params 集合方法

这些集合都是 `JsListWrapper` 类型，支持以下方法：

| 方法                       | 参数                   | 返回值     | 说明        | 示例                                                           |
|--------------------------|----------------------|---------|-----------|--------------------------------------------------------------|
| `add(item)`              | item: Object         | void    | 添加一项      | `pm.request.headers.add({key: 'X-Custom', value: 'test'})`   |
| `remove(keyOrPredicate)` | key: String/Function | void    | 删除一项      | `pm.request.headers.remove('X-Custom')`                      |
| `upsert(item)`           | item: Object         | void    | 更新或插入一项   | `pm.request.headers.upsert({key: 'X-Custom', value: 'new'})` |
| `get(key)`               | key: String          | String  | 获取指定键的值   | `pm.request.headers.get('Content-Type')`                     |
| `has(key)`               | key: String          | Boolean | 检查是否存在指定键 | `pm.request.headers.has('Authorization')`                    |
| `all()`                  | 无                    | Array   | 获取所有项     | `pm.request.headers.all()`                                   |
| `count()`                | 无                    | Number  | 获取项数      | `pm.request.headers.count()`                                 |
| `clear()`                | 无                    | void    | 清空所有项     | `pm.request.headers.clear()`                                 |
| `each(callback)`         | callback: Function   | void    | 遍历每一项     | `pm.request.headers.each(h => console.log(h))`               |

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

| 属性             | 类型                | 说明        | 示例                                           |
|----------------|-------------------|-----------|----------------------------------------------|
| `code`         | Number            | HTTP 状态码  | `pm.response.code`                           |
| `status`       | String            | HTTP 状态文本 | `pm.response.status`                         |
| `headers`      | Headers           | 响应头对象     | `pm.response.headers`                        |
| `responseTime` | Number            | 响应时间（毫秒）  | `pm.response.responseTime`                   |
| `to`           | ResponseAssertion | 链式断言语法支持  | `pm.response.to.have.status(200)`            |
| `have`         | ResponseAssertion | 链式断言语法支持  | `pm.response.to.have.header('Content-Type')` |
| `be`           | ResponseAssertion | 链式断言语法支持  | 用于链式调用                                       |

### 方法列表

| 方法       | 返回值          | 说明            | 示例                   |
|----------|--------------|---------------|----------------------|
| `text()` | String       | 获取响应体文本       | `pm.response.text()` |
| `json()` | Object       | 获取响应体 JSON 对象 | `pm.response.json()` |
| `size()` | ResponseSize | 获取响应大小信息      | `pm.response.size()` |

### 响应断言方法

| 方法                     | 说明          | 示例                                                      |
|------------------------|-------------|---------------------------------------------------------|
| `to.have.status(code)` | 断言状态码       | `pm.response.to.have.status(200)`                       |
| `to.have.header(name)` | 断言包含响应头     | `pm.response.to.have.header('Content-Type')`            |
| `to.be.below(ms)`      | 断言响应时间小于指定值 | `pm.expect(pm.response.responseTime).to.be.below(1000)` |

### Headers 对象方法

访问响应头：`pm.response.headers`

| 方法               | 参数                 | 返回值     | 说明        | 示例                                              |
|------------------|--------------------|---------|-----------|-------------------------------------------------|
| `get(name)`      | name: String       | String  | 获取响应头值    | `pm.response.headers.get('Content-Type')`       |
| `has(name)`      | name: String       | Boolean | 检查响应头是否存在 | `pm.response.headers.has('Set-Cookie')`         |
| `count()`        | 无                  | Number  | 获取响应头数量   | `pm.response.headers.count()`                   |
| `all()`          | 无                  | Array   | 获取所有响应头   | `pm.response.headers.all()`                     |
| `each(callback)` | callback: Function | void    | 遍历所有响应头   | `pm.response.headers.each(h => console.log(h))` |

### ResponseSize 对象

`pm.response.size()` 返回的对象包含以下属性：

| 属性       | 类型     | 说明        |
|----------|--------|-----------|
| `body`   | Number | 响应体大小（字节） |
| `header` | Number | 响应头大小（字节） |
| `total`  | Number | 总大小（字节）   |

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

| 方法            | 参数                    | 返回值       | 说明                    | 示例                                            |
|---------------|-----------------------|-----------|-----------------------|-----------------------------------------------|
| `get(name)`   | name: String          | Cookie    | 获取指定名称的 Cookie        | `pm.cookies.get('sessionId')`                 |
| `set(cookie)` | cookie: Cookie/String | void      | 设置 Cookie             | `pm.cookies.set({name: 'key', value: 'val'})` |
| `getAll()`    | 无                     | Array     | 获取所有 Cookie           | `pm.cookies.getAll()`                         |
| `has(name)`   | name: String          | Boolean   | 检查 Cookie 是否存在        | `pm.cookies.has('sessionId')`                 |
| `toObject()`  | 无                     | Object    | 获取所有 Cookie 对象（键值对形式） | `pm.cookies.toObject()`                       |
| `jar()`       | 无                     | CookieJar | 获取 CookieJar 对象       | `pm.cookies.jar()`                            |

### CookieJar 对象

CookieJar 用于跨域管理 Cookie，通过 `pm.cookies.jar()` 获取。

#### 方法列表

| 方法                           | 参数                                                         | 说明                  | 示例                                      |
|------------------------------|------------------------------------------------------------|---------------------|-----------------------------------------|
| `set(url, cookie, callback)` | url: String<br>cookie: String/Object<br>callback: Function | 设置指定 URL 的 Cookie   | `jar.set(url, 'key=value', callback)`   |
| `get(url, name, callback)`   | url: String<br>name: String<br>callback: Function          | 获取指定 URL 的 Cookie   | `jar.get(url, 'sessionId', callback)`   |
| `getAll(url, callback)`      | url: String<br>callback: Function                          | 获取指定 URL 的所有 Cookie | `jar.getAll(url, callback)`             |
| `unset(url, name, callback)` | url: String<br>name: String<br>callback: Function          | 删除指定 URL 的 Cookie   | `jar.unset(url, 'sessionId', callback)` |
| `clear(url, callback)`       | url: String<br>callback: Function                          | 清空指定 URL 的所有 Cookie | `jar.clear(url, callback)`              |

### Cookie 对象属性

| 属性         | 类型      | 说明            |
|------------|---------|---------------|
| `name`     | String  | Cookie 名称     |
| `value`    | String  | Cookie 值      |
| `domain`   | String  | Cookie 域      |
| `path`     | String  | Cookie 路径     |
| `expires`  | String  | 过期时间          |
| `maxAge`   | Number  | 最大存活时间（秒）     |
| `httpOnly` | Boolean | 是否仅 HTTP      |
| `secure`   | Boolean | 是否安全传输（HTTPS） |
| `sameSite` | String  | SameSite 属性   |

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

| 链式属性   | 说明    |
|--------|-------|
| `to`   | 链式连接词 |
| `be`   | 链式连接词 |
| `have` | 链式连接词 |

### 支持的断言方法

| 断言                   | 参数                           | 说明                   | 示例                                             |
|----------------------|------------------------------|----------------------|------------------------------------------------|
| `equal(value)`       | value: Any                   | 严格相等（深度比较）           | `pm.expect(200).to.equal(200)`                 |
| `eql(value)`         | value: Any                   | 深度相等（与 equal 相同）     | `pm.expect({a: 1}).to.eql({a: 1})`             |
| `include(substring)` | substring: String            | 包含子串                 | `pm.expect('hello world').to.include('hello')` |
| `property(key)`      | key: String                  | 包含属性（仅支持 Map/Object） | `pm.expect(obj).to.have.property('id')`        |
| `match(regex)`       | regex: String/Pattern/RegExp | 匹配正则表达式              | `pm.expect('hello').to.match(/^h/)`            |
| `below(number)`      | number: Number               | 数值小于指定值              | `pm.expect(5).to.be.below(10)`                 |

### 使用示例

```javascript
// 基本相等断言
pm.test("Status code is 200", function () {
    pm.expect(pm.response.code).to.equal(200);
});

// 深度相等断言
pm.test("Response data matches", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.eql({status: "success"});
});

// 包含子串
pm.test("Response contains success", function () {
    pm.expect(pm.response.text()).to.include("success");
});

// 属性存在
pm.test("Response has userId property", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('userId');
});

// 正则匹配
pm.test("Email format is correct", function () {
    pm.expect(email).to.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/);
});

// 数值比较
pm.test("Response time is acceptable", function () {
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

| 参数         | 类型       | 说明                                   |
|------------|----------|--------------------------------------|
| `name`     | String   | 测试名称                                 |
| `function` | Function | 测试函数（可使用 pm.expect 或 pm.response 断言） |

#### pm.test.index()

获取所有测试结果（通常在测试执行完成后调用）。

| 返回值   | 说明                                                                                                            |
|-------|---------------------------------------------------------------------------------------------------------------|
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
results.forEach(function (result) {
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
        name
:
    "测试名称",             // 测试名称
        passed
:
    true,                // 是否通过
        errorMessage
:
    null           // 错误信息（passed 为 false 时有值）
}
```

---

## 外部数据源脚本 API

可以在 Pre-request Script 或 Test Script 中直接访问 Redis、Kafka、Elasticsearch、InfluxDB，并结合 `pm.test()` / `pm.expect()` 进行断言。

### pm.redis

#### 方法列表

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `execute(options)` | `host`、`port`、`db`、`command`、`key`、`args`、`value` | Any | 执行一次 Redis 命令 |
| `query(options)` | 同 `execute(options)` | Any | `execute()` 的别名 |

#### 示例：写入并断言 Redis

```javascript
pm.redis.execute({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'SET',
    key: 'user:1001',
    value: JSON.stringify({id: 1001, name: 'Alice'})
});

const redisValue = pm.redis.query({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'GET',
    key: 'user:1001'
});

pm.test('Redis 写入成功', function () {
    pm.expect(redisValue).to.include('Alice');
});
```

### pm.kafka

#### 方法列表

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `listTopics(options)` | `bootstrapServers` 等 | `Array<String>` | 获取 topic 列表 |
| `send(options)` | `bootstrapServers`、`topic`、`key`、`value`、`headers` | Object | 发送消息 |
| `poll(options)` | `bootstrapServers`、`topic`、`groupId`、`pollTimeoutMs` 等 | Array | 拉取消息 |

#### 示例：发消息并断言 Kafka

```javascript
const sendResp = pm.kafka.send({
    bootstrapServers: 'localhost:9092',
    topic: 'orders',
    key: 'order-1001',
    value: JSON.stringify({orderId: 1001, status: 'CREATED'})
});

pm.test('Kafka 发送成功', function () {
    pm.expect(sendResp.topic).to.equal('orders');
    pm.expect(sendResp.offset).to.be.least(0);
});
```

### pm.es / pm.elasticsearch

#### 方法列表

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `request(options)` | `baseUrl`、`method`、`path`、`body`、`headers` | Object | 执行一次 ES HTTP 请求 |
| `query(options)` | 同 `request(options)` | Object | `request()` 的别名 |

#### 示例：写入并断言 Elasticsearch

```javascript
const indexResp = pm.es.request({
    baseUrl: 'http://localhost:9200',
    method: 'POST',
    path: '/orders/_doc/order-1001',
    body: JSON.stringify({orderId: 1001, status: 'CREATED'})
});

pm.test('ES 写入成功', function () {
    pm.expect(indexResp.code).to.be.within(200, 201);
});
```

### pm.influx / pm.influxdb

支持两种模式：

- `version: 'v1'` 或 `mode: 'influxql'`：使用 InfluxQL（默认走 `/query`、`/write`）
- `version: 'v2'` 或 `mode: 'flux'`：使用 Flux / InfluxDB 2.x（走 `/api/v2/query`、`/api/v2/write`）

#### 方法列表

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `query(options)` | `baseUrl`、`version`、`db/org`、`query`、`token` 等 | Object | 查询 InfluxDB |
| `write(options)` | `baseUrl`、`version`、`db/org/bucket`、`lineProtocol`、`precision` | Object | 写入 line protocol 数据 |
| `request(options)` | `baseUrl`、`path`、`method`、`body`、`headers` | Object | 发送原始 HTTP 请求 |

#### 常用参数

| 参数 | 适用版本 | 说明 |
|------|----------|------|
| `baseUrl` | v1 / v2 | InfluxDB 地址，默认 `http://localhost:8086` |
| `version` / `mode` | v1 / v2 | `v1` / `influxql` 或 `v2` / `flux` |
| `db` / `database` | v1 | 数据库名 |
| `org` | v2 | 组织名 |
| `bucket` | v2 写入 | bucket 名 |
| `token` | v2 | `Authorization: Token xxx` |
| `username` / `password` | v1 | 通过 `u/p` 参数传递 |
| `query` | v1 / v2 | InfluxQL 或 Flux 查询语句 |
| `lineProtocol` | v1 / v2 写入 | 写入的 line protocol 文本 |
| `precision` | v1 / v2 写入 | 时间精度，默认 `ms` |

#### 示例：InfluxDB 1.x 查询并断言

```javascript
const queryResp = pm.influx.query({
    baseUrl: 'http://localhost:8086',
    version: 'v1',
    db: 'metrics',
    username: 'root',
    password: 'root',
    query: 'SELECT * FROM cpu ORDER BY time DESC LIMIT 1'
});

pm.test('InfluxQL 查询成功', function () {
    pm.expect(queryResp.code).to.equal(200);
    pm.expect(queryResp.json).to.exist();
});
```

#### 示例：InfluxDB 1.x 写入并断言

```javascript
const writeResp = pm.influxdb.write({
    baseUrl: 'http://localhost:8086',
    version: 'v1',
    db: 'metrics',
    username: 'root',
    password: 'root',
    precision: 'ms',
    lineProtocol: 'api_requests,service=order count=1i ' + Date.now()
});

pm.test('Influx 写入成功', function () {
    pm.expect(writeResp.code).to.equal(204);
});
```

#### 示例：InfluxDB 2.x Flux 查询

```javascript
const resp = pm.influx.query({
    baseUrl: 'http://localhost:8086',
    version: 'v2',
    org: 'demo-org',
    token: pm.environment.get('influxToken'),
    query: 'from(bucket: "metrics") |> range(start: -5m) |> limit(n: 1)'
});

pm.test('Influx Flux 查询成功', function () {
    pm.expect(resp.code).to.equal(200);
    pm.expect(resp.body).to.include('_value');
});
```

---

## console - 控制台

输出调试信息。

### 方法列表

| 方法                    | 参数           | 说明   | 示例                                |
|-----------------------|--------------|------|-----------------------------------|
| `log(message, ...)`   | message: Any | 输出日志 | `console.log('Hello', 'World')`   |
| `info(message, ...)`  | message: Any | 输出信息 | `console.info('Info message')`    |
| `warn(message, ...)`  | message: Any | 输出警告 | `console.warn('Warning message')` |
| `error(message, ...)` | message: Any | 输出错误 | `console.error('Error message')`  |

---

## 内置 JavaScript 库

EasyPostman 内置了三个常用的 JavaScript 库，这些库**已预加载到全局作用域**，可以直接使用，无需 `require()`。

### 支持的库列表

| 库名         | 全局变量名       | 版本       | 说明                                    | 官方文档                                      |
|------------|-------------|----------|---------------------------------------|--------------------------------------------|
| crypto-js  | `CryptoJS`  | 4.1.1    | 加密库，支持 AES、DES、MD5、SHA、HMAC 等多种加密算法 | [crypto-js](https://cryptojs.gitbook.io/docs/) |
| lodash     | `_`         | 4.17.21  | JavaScript 实用工具库，提供丰富的函数式编程辅助方法      | [lodash](https://lodash.com/docs/)          |
| moment     | `moment`    | 2.29.4   | 日期时间处理库，用于格式化、解析和操作日期时间             | [moment.js](https://momentjs.com/docs/)     |

### 使用方式

```javascript
// ✅ 推荐：直接使用全局变量（无需 require）
var hash = CryptoJS.MD5('message').toString();
var randomNum = _.random(1, 100);
var now = moment().format('YYYY-MM-DD HH:mm:ss');

// ✅ 也支持：使用 require() 加载（兼容 Postman）
var CryptoJS = require('crypto-js');
var _ = require('lodash');
var moment = require('moment');
```

### crypto-js - 加密库

提供多种加密和哈希算法，常用于生成签名、加密敏感数据等场景。

#### 常用功能

| 功能          | 方法                                  | 说明            | 示例                                                   |
|-------------|-------------------------------------|---------------|------------------------------------------------------|
| MD5 哈希      | `CryptoJS.MD5(message)`             | 生成 MD5 哈希     | `CryptoJS.MD5('text').toString()`                    |
| SHA1 哈希     | `CryptoJS.SHA1(message)`            | 生成 SHA1 哈希    | `CryptoJS.SHA1('text').toString()`                   |
| SHA256 哈希   | `CryptoJS.SHA256(message)`          | 生成 SHA256 哈希  | `CryptoJS.SHA256('text').toString()`                 |
| HMAC-SHA256 | `CryptoJS.HmacSHA256(message, key)` | HMAC 签名       | `CryptoJS.HmacSHA256('data', 'secret').toString()`   |
| AES 加密      | `CryptoJS.AES.encrypt(msg, key)`    | AES 对称加密      | `CryptoJS.AES.encrypt('text', 'secret').toString()`  |
| AES 解密      | `CryptoJS.AES.decrypt(cipher, key)` | AES 解密        | `CryptoJS.AES.decrypt(cipher, 'secret').toString()`  |
| Base64 编码   | `CryptoJS.enc.Base64.stringify()`   | Base64 编码     | `CryptoJS.enc.Base64.stringify(wordArray)`           |
| 随机字节        | `CryptoJS.lib.WordArray.random(n)`  | 生成 n 字节随机数据   | `CryptoJS.lib.WordArray.random(16).toString()`       |

#### 使用示例

```javascript
// 直接使用全局变量 CryptoJS（无需 require）

// MD5 哈希
var md5 = CryptoJS.MD5('password123').toString();
pm.environment.set('passwordHash', md5);

// HMAC-SHA256 签名
var data = 'userId=123&timestamp=' + Date.now();
var signature = CryptoJS.HmacSHA256(data, 'secret-key').toString();
pm.environment.set('signature', signature);

// AES 加密解密
var encrypted = CryptoJS.AES.encrypt('sensitive data', 'my-key').toString();
var decrypted = CryptoJS.AES.decrypt(encrypted, 'my-key').toString(CryptoJS.enc.Utf8);
```

### lodash - 工具库

提供数组、对象、字符串等数据类型的实用操作方法，简化数据处理逻辑。

#### 常用功能

| 分类   | 方法                          | 说明           | 示例                                      |
|------|-----------------------------|--------------|-----------------------------------------|
| 数组   | `_.random(min, max)`        | 生成随机数        | `_.random(1, 100)`                      |
|      | `_.shuffle(array)`          | 打乱数组         | `_.shuffle([1, 2, 3])`                  |
|      | `_.sample(array)`           | 随机取一个元素      | `_.sample(['a', 'b', 'c'])`             |
|      | `_.uniq(array)`             | 数组去重         | `_.uniq([1, 2, 2, 3])`                  |
| 集合   | `_.map(collection, fn)`     | 映射转换         | `_.map([1,2], n => n*2)`                |
|      | `_.filter(collection, fn)`  | 过滤           | `_.filter([1,2,3], n => n>1)`           |
|      | `_.find(collection, fn)`    | 查找第一个匹配项     | `_.find(users, {role: 'admin'})`        |
|      | `_.groupBy(collection, fn)` | 分组           | `_.groupBy(users, 'role')`              |
|      | `_.sortBy(collection, fn)`  | 排序           | `_.sortBy(users, 'age')`                |
| 对象   | `_.pick(object, keys)`      | 提取指定属性       | `_.pick(user, ['id', 'name'])`          |
|      | `_.omit(object, keys)`      | 排除指定属性       | `_.omit(user, ['password'])`            |
|      | `_.merge(obj1, obj2)`       | 深度合并对象       | `_.merge({a:1}, {b:2})`                 |
|      | `_.cloneDeep(object)`       | 深度克隆         | `_.cloneDeep(complexObject)`            |
| 字符串  | `_.camelCase(string)`       | 转驼峰命名        | `_.camelCase('hello-world')`            |
|      | `_.snakeCase(string)`       | 转蛇形命名        | `_.snakeCase('helloWorld')`             |
|      | `_.capitalize(string)`      | 首字母大写        | `_.capitalize('hello')`                 |
| 其他   | `_.times(n, fn)`            | 执行 n 次       | `_.times(3, i => console.log(i))`       |
|      | `_.debounce(fn, wait)`      | 防抖函数         | `_.debounce(func, 300)`                 |

#### 使用示例

```javascript
// 直接使用全局变量 _（无需 require）

// 生成随机测试数据
var userId = _.random(10000, 99999);
var status = _.sample(['pending', 'approved', 'rejected']);

// 数组操作
var nums = [1, 2, 3, 4, 5];
var doubled = _.map(nums, n => n * 2);
var filtered = _.filter(nums, n => n > 2);

// 对象操作
var user = {id: 1, name: 'John', password: 'secret', age: 30};
var safeUser = _.omit(user, ['password']);
pm.environment.set('user', JSON.stringify(safeUser));
```

### moment - 日期时间库

强大的日期时间处理库，支持格式化、解析、计算和验证日期。

#### 常用功能

| 功能       | 方法                                  | 说明             | 示例                                          |
|----------|-------------------------------------|----------------|---------------------------------------------|
| 获取当前时间   | `moment()`                          | 创建当前时间对象       | `moment()`                                  |
| 格式化      | `moment().format(format)`           | 格式化日期          | `moment().format('YYYY-MM-DD HH:mm:ss')`    |
| ISO格式    | `moment().toISOString()`            | 转 ISO 8601 格式  | `moment().toISOString()`                    |
| 时间戳      | `moment().valueOf()`                | 获取毫秒时间戳        | `moment().valueOf()`                        |
|          | `moment().unix()`                   | 获取秒级时间戳        | `moment().unix()`                           |
| 解析日期     | `moment(str, format)`               | 解析字符串为日期       | `moment('2024-01-01', 'YYYY-MM-DD')`        |
| 日期加减     | `moment().add(n, unit)`             | 增加时间           | `moment().add(7, 'days')`                   |
|          | `moment().subtract(n, unit)`        | 减少时间           | `moment().subtract(1, 'months')`            |
| 日期比较     | `moment().isBefore(date)`           | 是否在之前          | `moment().isBefore('2025-01-01')`           |
|          | `moment().isAfter(date)`            | 是否在之后          | `moment().isAfter('2023-01-01')`            |
|          | `moment().isSame(date)`             | 是否相同           | `moment().isSame('2024-01-01', 'day')`      |
| 时间差      | `moment().diff(date, unit)`         | 计算时间差          | `moment().diff('2024-01-01', 'days')`       |
| 开始/结束时间 | `moment().startOf(unit)`            | 获取单位开始时间       | `moment().startOf('day')`                   |
|          | `moment().endOf(unit)`              | 获取单位结束时间       | `moment().endOf('month')`                   |
| 验证       | `moment(str, format, true).isValid()` | 验证日期是否有效       | `moment('2024-13-01', 'YYYY-MM-DD', true).isValid()` |

#### 使用示例

```javascript
// 直接使用全局变量 moment（无需 require）

// 生成各种时间格式
pm.environment.set('currentDate', moment().format('YYYY-MM-DD'));
pm.environment.set('currentTime', moment().format('YYYY-MM-DD HH:mm:ss'));
pm.environment.set('timestamp', moment().valueOf().toString());
pm.environment.set('isoTime', moment().toISOString());

// 日期计算
var tomorrow = moment().add(1, 'days').format('YYYY-MM-DD');
var lastMonth = moment().subtract(1, 'months').format('YYYY-MM');
var startOfDay = moment().startOf('day').valueOf();
var endOfDay = moment().endOf('day').valueOf();
```

### 组合使用示例

```javascript
// 直接使用全局变量（无需 require）
// CryptoJS、_（lodash）、moment 都已预加载

// 生成带签名的 API 请求参数
var params = {
    userId: pm.environment.get('userId') || '123',
    timestamp: moment().valueOf().toString(),
    nonce: CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex),
    action: 'getUserInfo'
};

// 按键名排序并拼接签名字符串
var sortedKeys = _.keys(params).sort();
var signString = _.map(sortedKeys, key => key + '=' + params[key]).join('&');

// 生成 HMAC-SHA256 签名
var secretKey = pm.environment.get('secretKey') || 'default-secret';
var signature = CryptoJS.HmacSHA256(signString, secretKey).toString();

// 保存到环境变量
pm.environment.set('requestTimestamp', params.timestamp);
pm.environment.set('requestNonce', params.nonce);
pm.environment.set('requestSignature', signature);

console.log('请求参数已准备完成');
console.log('签名字符串:', signString);
console.log('签名:', signature);
```

---

## 完整示例

### Pre-request Script 示例

#### 示例 1：基础请求准备

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

#### 示例 2：JWT Token 认证

```javascript
// 检查 token 是否存在
const token = pm.environment.get('authToken');

if (token) {
    // 添加 Bearer Token
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
    console.log('已添加认证 Token');
} else {
    console.warn('警告：未找到认证 Token，请先登录');
}

// 添加 API Key（如果需要）
const apiKey = pm.environment.get('apiKey');
if (apiKey) {
    pm.request.headers.upsert({
        key: 'X-API-Key',
        value: apiKey
    });
}
```

#### 示例 3：动态生成签名（HMAC-SHA256）

```javascript
var CryptoJS = require('crypto-js');

// 获取请求信息
const timestamp = Date.now().toString();
const method = pm.request.method;
const path = pm.request.url.getPath();
const secretKey = pm.environment.get('secretKey') || 'default-secret';

// 生成签名字符串
const signString = method + '\n' + path + '\n' + timestamp;
console.log('签名字符串:', signString);

// 计算 HMAC-SHA256 签名
const signature = CryptoJS.HmacSHA256(signString, secretKey).toString();
console.log('生成的签名:', signature);

// 添加签名相关请求头
pm.request.headers.upsert({
    key: 'X-Timestamp',
    value: timestamp
});

pm.request.headers.upsert({
    key: 'X-Signature',
    value: signature
});

pm.request.headers.upsert({
    key: 'X-App-Id',
    value: pm.environment.get('appId') || 'default-app'
});
```

#### 示例 4：动态数据生成器

```javascript
var moment = require('moment');
var _ = require('lodash');

// 生成随机用户数据
const randomUser = {
    id: pm.uuid(),
    username: 'user_' + _.random(10000, 99999),
    email: 'test_' + Date.now() + '@example.com',
    phone: '138' + _.random(10000000, 99999999),
    createTime: moment().format('YYYY-MM-DD HH:mm:ss'),
    age: _.random(18, 60)
};

// 保存到环境变量供后续使用
pm.environment.set('testUserId', randomUser.id);
pm.environment.set('testUserEmail', randomUser.email);
pm.environment.set('testUsername', randomUser.username);

// 如果是 JSON 请求体，可以动态修改
console.log('生成的测试用户:', JSON.stringify(randomUser, null, 2));

// 生成随机订单号
const orderId = 'ORD' + moment().format('YYYYMMDDHHmmss') + _.random(1000, 9999);
pm.environment.set('testOrderId', orderId);
console.log('订单号:', orderId);
```

#### 示例 5：条件请求修改

```javascript
// 根据环境变量决定请求配置
const env = pm.environment.get('currentEnv') || 'dev';

// 根据环境设置不同的 baseURL
const baseUrls = {
    'dev': 'https://dev-api.example.com',
    'test': 'https://test-api.example.com',
    'prod': 'https://api.example.com'
};

pm.environment.set('baseUrl', baseUrls[env]);
console.log('当前环境:', env, '- API 地址:', baseUrls[env]);

// 根据请求方法添加不同的请求头
if (pm.request.method === 'POST' || pm.request.method === 'PUT') {
    pm.request.headers.upsert({
        key: 'Content-Type',
        value: 'application/json'
    });
}

// 为特定路径添加额外参数
if (pm.request.url.getPath().includes('/api/v2/')) {
    pm.request.params.add({
        key: 'version',
        value: '2.0'
    });
}
```

#### 示例 6：请求数据校验

```javascript
// 检查必要的环境变量
const requiredVars = ['baseUrl', 'apiKey', 'userId'];
const missingVars = [];

requiredVars.forEach(function(varName) {
    if (!pm.environment.get(varName)) {
        missingVars.push(varName);
    }
});

if (missingVars.length > 0) {
    console.error('错误：缺少必要的环境变量:', missingVars.join(', '));
    throw new Error('缺少环境变量: ' + missingVars.join(', '));
}

// 检查请求头是否完整
if (!pm.request.headers.has('Content-Type')) {
    console.warn('警告：缺少 Content-Type 请求头');
}

console.log('✓ 环境变量校验通过');
```

#### 示例 7：批量操作与数据处理

```javascript
var _ = require('lodash');

// 批量添加自定义请求头
const customHeaders = [
    { key: 'X-Client-Version', value: '1.0.0' },
    { key: 'X-Platform', value: 'web' },
    { key: 'X-Device-ID', value: pm.environment.get('deviceId') || pm.uuid() },
    { key: 'X-Session-ID', value: pm.environment.get('sessionId') || pm.uuid() }
];

customHeaders.forEach(function(header) {
    pm.request.headers.upsert(header);
});

// 批量添加查询参数
const commonParams = {
    'appId': pm.environment.get('appId') || 'default',
    'lang': 'zh-CN',
    'timezone': 'Asia/Shanghai',
    'platform': 'web'
};

_.forEach(commonParams, function(value, key) {
    pm.request.params.add({ key: key, value: value });
});

console.log('已添加', customHeaders.length, '个请求头');
console.log('已添加', Object.keys(commonParams).length, '个查询参数');
```

#### 示例 8：URL 参数加密

```javascript
var CryptoJS = require('crypto-js');

// 获取敏感参数
const userId = pm.environment.get('userId');
const secretKey = pm.environment.get('encryptKey') || 'default-key';

if (userId) {
    // 加密用户 ID
    const encryptedUserId = CryptoJS.AES.encrypt(userId, secretKey).toString();
    
    // 使用加密后的值
    pm.request.params.add({
        key: 'uid',
        value: encodeURIComponent(encryptedUserId)
    });
    
    console.log('原始 userId:', userId);
    console.log('加密后:', encryptedUserId.substring(0, 20) + '...');
}

// Base64 编码
const credentials = pm.environment.get('username') + ':' + pm.environment.get('password');
const base64Credentials = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(credentials));

pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Basic ' + base64Credentials
});
```

#### 示例 9：请求重试机制准备

```javascript
// 设置重试计数器
let retryCount = pm.environment.get('retryCount');
if (!retryCount) {
    retryCount = 0;
}
pm.environment.set('retryCount', retryCount);

// 添加重试标识
pm.request.headers.upsert({
    key: 'X-Retry-Count',
    value: retryCount.toString()
});

// 设置超时时间（根据重试次数递增）
const baseTimeout = 5000;
const timeout = baseTimeout * (retryCount + 1);
pm.environment.set('currentTimeout', timeout);

console.log('重试次数:', retryCount, '- 超时时间:', timeout + 'ms');
```

#### 示例 10：模拟数据填充（用于测试）

```javascript
var _ = require('lodash');
var moment = require('moment');

// 生成模拟订单数据
const mockOrder = {
    orderId: 'TEST_' + moment().format('YYYYMMDDHHmmss') + _.random(1000, 9999),
    customerId: pm.environment.get('testUserId') || 'CUST_' + _.random(10000, 99999),
    products: _.times(_.random(1, 5), function(n) {
        return {
            productId: 'PROD_' + _.random(1000, 9999),
            quantity: _.random(1, 10),
            price: _.round(_.random(10, 1000, true), 2)
        };
    }),
    totalAmount: 0,
    orderTime: moment().toISOString(),
    status: 'pending'
};

// 计算总金额
mockOrder.totalAmount = _.sumBy(mockOrder.products, function(p) {
    return p.quantity * p.price;
});
mockOrder.totalAmount = _.round(mockOrder.totalAmount, 2);

// 保存模拟数据
pm.environment.set('mockOrderData', JSON.stringify(mockOrder));
console.log('生成的模拟订单:', JSON.stringify(mockOrder, null, 2));
```

### Post-request Script 示例

#### 示例 1：基础响应验证

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
```

#### 示例 2：登录接口完整测试

```javascript
pm.test("登录请求成功", function () {
    pm.response.to.have.status(200);
});

pm.test("登录响应时间合理", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

const jsonData = pm.response.json();

pm.test("登录返回正确的数据结构", function () {
    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData).to.have.property('message');
    pm.expect(jsonData).to.have.property('data');
    pm.expect(jsonData.code).to.equal(200);
});

pm.test("返回了认证令牌", function () {
    pm.expect(jsonData.data).to.have.property('token');
    pm.expect(jsonData.data).to.have.property('refreshToken');
    pm.expect(jsonData.data.token).to.match(/^[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+$/); // JWT 格式
});

pm.test("返回了用户信息", function () {
    pm.expect(jsonData.data).to.have.property('userInfo');
    pm.expect(jsonData.data.userInfo).to.have.property('userId');
    pm.expect(jsonData.data.userInfo).to.have.property('username');
});

// 保存认证信息
if (jsonData.code === 200 && jsonData.data) {
    pm.environment.set('authToken', jsonData.data.token);
    pm.environment.set('refreshToken', jsonData.data.refreshToken);
    pm.environment.set('currentUserId', jsonData.data.userInfo.userId);
    pm.environment.set('currentUsername', jsonData.data.userInfo.username);
    
    console.log('✓ 登录成功，用户:', jsonData.data.userInfo.username);
    console.log('✓ Token 已保存');
}
```

#### 示例 3：数据列表接口测试

```javascript
const jsonData = pm.response.json();

pm.test("获取列表成功", function () {
    pm.response.to.have.status(200);
    pm.expect(jsonData.code).to.equal(200);
});

pm.test("列表数据结构正确", function () {
    pm.expect(jsonData.data).to.have.property('list');
    pm.expect(jsonData.data).to.have.property('total');
    pm.expect(jsonData.data).to.have.property('pageNum');
    pm.expect(jsonData.data).to.have.property('pageSize');
});

pm.test("列表数据有效", function () {
    pm.expect(Array.isArray(jsonData.data.list)).to.equal(true);
    pm.expect(jsonData.data.total).to.be.a('number');
    pm.expect(jsonData.data.list.length).to.be.below(jsonData.data.pageSize + 1);
});

// 验证列表项结构
if (jsonData.data.list.length > 0) {
    pm.test("列表项包含必要字段", function () {
        const firstItem = jsonData.data.list[0];
        pm.expect(firstItem).to.have.property('id');
        pm.expect(firstItem).to.have.property('name');
        pm.expect(firstItem).to.have.property('createTime');
    });
    
    // 保存第一项的 ID 供后续测试使用
    pm.environment.set('firstItemId', jsonData.data.list[0].id);
    console.log('总记录数:', jsonData.data.total);
    console.log('当前页记录数:', jsonData.data.list.length);
}
```

#### 示例 4：错误处理和重试逻辑

```javascript
const statusCode = pm.response.code;
const retryCount = parseInt(pm.environment.get('retryCount') || '0');
const maxRetries = 3;

if (statusCode === 200) {
    // 成功，重置重试计数
    pm.environment.set('retryCount', '0');
    
    pm.test("请求成功", function () {
        pm.response.to.have.status(200);
    });
    
    console.log('✓ 请求成功');
    
} else if (statusCode === 401) {
    // 未授权，可能 token 过期
    pm.test("认证失败 - Token 可能已过期", function () {
        pm.expect(statusCode).to.equal(401);
    });
    
    console.error('✗ 认证失败，请重新登录');
    pm.environment.unset('authToken');
    
} else if (statusCode === 429) {
    // 请求过于频繁
    pm.test("请求限流", function () {
        pm.expect(statusCode).to.equal(429);
    });
    
    const retryAfter = pm.response.headers.get('Retry-After') || '60';
    console.warn('⚠ 请求过于频繁，建议等待', retryAfter, '秒后重试');
    
} else if (statusCode >= 500) {
    // 服务器错误，可以重试
    if (retryCount < maxRetries) {
        pm.environment.set('retryCount', (retryCount + 1).toString());
        console.warn('⚠ 服务器错误，准备重试 (' + (retryCount + 1) + '/' + maxRetries + ')');
    } else {
        console.error('✗ 达到最大重试次数，放弃重试');
        pm.environment.set('retryCount', '0');
    }
} else {
    // 其他错误
    pm.test("请求失败 - 状态码: " + statusCode, function () {
        const jsonData = pm.response.json();
        console.error('错误信息:', jsonData.message || '未知错误');
    });
}
```

#### 示例 5：Cookie 和 Session 管理

```javascript
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

// 检查所有 Cookie
console.log('=== 所有 Cookie ===');
const allCookies = pm.cookies.getAll();
allCookies.forEach(function(cookie) {
    console.log(cookie.name + ':', cookie.value);
});
```

#### 示例 6：性能监控和统计

```javascript
// 11. 获取响应大小信息
const size = pm.response.size();
console.log('响应体大小:', size.body, 'bytes');
console.log('响应头大小:', size.header, 'bytes');
console.log('总大小:', size.total, 'bytes');

// 性能统计
const responseTime = pm.response.responseTime;
pm.environment.set('lastResponseTime', responseTime.toString());

// 计算平均响应时间
let totalTime = parseFloat(pm.environment.get('totalResponseTime') || '0');
let requestCount = parseInt(pm.environment.get('requestCount') || '0');

totalTime += responseTime;
requestCount += 1;

pm.environment.set('totalResponseTime', totalTime.toString());
pm.environment.set('requestCount', requestCount.toString());

const avgResponseTime = totalTime / requestCount;
console.log('本次响应时间:', responseTime, 'ms');
console.log('平均响应时间:', avgResponseTime.toFixed(2), 'ms');
console.log('请求总数:', requestCount);

// 性能等级判断
let performanceLevel = '';
if (responseTime < 100) {
    performanceLevel = '优秀';
} else if (responseTime < 300) {
    performanceLevel = '良好';
} else if (responseTime < 1000) {
    performanceLevel = '一般';
} else {
    performanceLevel = '较慢';
}

pm.test("响应性能: " + performanceLevel + " (" + responseTime + "ms)", function () {
    console.log('性能等级:', performanceLevel);
});
```

#### 示例 7：复杂数据验证

```javascript
const jsonData = pm.response.json();

pm.test("验证嵌套数据结构", function () {
    // 多层嵌套验证
    pm.expect(jsonData).to.have.property('data');
    pm.expect(jsonData.data).to.have.property('user');
    pm.expect(jsonData.data.user).to.have.property('profile');
    pm.expect(jsonData.data.user.profile).to.have.property('address');
});

// 数组遍历验证
if (jsonData.data && jsonData.data.items) {
    pm.test("所有商品都有价格", function () {
        jsonData.data.items.forEach(function(item, index) {
            pm.expect(item).to.have.property('price');
            pm.expect(item.price).to.be.a('number');
            pm.expect(item.price).to.be.below(100000);
            console.log('商品' + (index + 1) + ':', item.name, '- 价格:', item.price);
        });
    });
    
    // 计算总价
    let totalPrice = 0;
    jsonData.data.items.forEach(function(item) {
        totalPrice += item.price * item.quantity;
    });
    
    pm.test("总价计算正确", function () {
        pm.expect(totalPrice).to.equal(jsonData.data.totalAmount);
    });
    
    console.log('商品总价:', totalPrice);
}
```

#### 示例 8：响应头详细分析

```javascript
// 12. 遍历所有响应头
console.log('所有响应头:');
pm.response.headers.each(function (header) {
    console.log('  ' + header.key + ': ' + header.value);
});

// 验证安全相关响应头
pm.test("检查安全响应头", function () {
    const securityHeaders = [
        'X-Content-Type-Options',
        'X-Frame-Options',
        'X-XSS-Protection'
    ];
    
    securityHeaders.forEach(function(headerName) {
        if (pm.response.headers.has(headerName)) {
            console.log('✓ 包含安全头:', headerName);
        } else {
            console.warn('⚠ 缺少安全头:', headerName);
        }
    });
});

// 检查缓存策略
if (pm.response.headers.has('Cache-Control')) {
    const cacheControl = pm.response.headers.get('Cache-Control');
    console.log('缓存策略:', cacheControl);
}

// 检查 CORS 设置
if (pm.response.headers.has('Access-Control-Allow-Origin')) {
    const cors = pm.response.headers.get('Access-Control-Allow-Origin');
    console.log('CORS 设置:', cors);
}
```

#### 示例 9：测试结果统计和报告

```javascript
// 13. 获取所有测试结果
const testResults = pm.test.index();
console.log('测试结果统计:');
let passCount = 0;
let failCount = 0;
testResults.forEach(function (result) {
    if (result.passed) {
        passCount++;
    } else {
        failCount++;
        console.log('失败的测试:', result.name, '-', result.errorMessage);
    }
});
console.log('通过:', passCount, '失败:', failCount);

// 保存测试统计
pm.environment.set('lastTestPassCount', passCount.toString());
pm.environment.set('lastTestFailCount', failCount.toString());

// 累计统计
let totalPass = parseInt(pm.environment.get('totalTestPass') || '0');
let totalFail = parseInt(pm.environment.get('totalTestFail') || '0');

totalPass += passCount;
totalFail += failCount;

pm.environment.set('totalTestPass', totalPass.toString());
pm.environment.set('totalTestFail', totalFail.toString());

const successRate = (totalPass / (totalPass + totalFail) * 100).toFixed(2);
console.log('累计测试通过率:', successRate + '%');

// 生成测试报告摘要
console.log('=== 测试报告摘要 ===');
console.log('本次测试: 通过', passCount, '/ 失败', failCount);
console.log('累计测试: 通过', totalPass, '/ 失败', totalFail);
console.log('成功率:', successRate + '%');
```

#### 示例 10：数据提取和传递（API 链式调用）

```javascript
// 从 JSON 响应中提取嵌套数据
pm.test("提取用户信息", function () {
    const jsonData = pm.response.json();

    // 假设响应结构：{ data: { user: { id: 123, name: "John" } } }
    pm.expect(jsonData).to.have.property('data');

    const userData = jsonData.data.user;
    pm.expect(userData).to.have.property('id');
    pm.expect(userData).to.have.property('name');

    // 保存到环境变量供下一个请求使用
    pm.environment.set('currentUserId', userData.id.toString());
    pm.environment.set('currentUserName', userData.name);
    
    // 如果有权限信息，也保存
    if (userData.roles) {
        pm.environment.set('userRoles', JSON.stringify(userData.roles));
    }
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
    
    // 保存整个列表的 ID 数组
    const itemIds = jsonData.items.map(function(item) {
        return item.id;
    });
    pm.environment.set('allItemIds', JSON.stringify(itemIds));
    console.log('提取了', itemIds.length, '个 ID');
});
```

#### 示例 11：响应数据解密和验证

```javascript
var CryptoJS = require('crypto-js');

const jsonData = pm.response.json();

pm.test("响应包含加密数据", function () {
    pm.expect(jsonData).to.have.property('encryptedData');
});

// 解密响应数据
if (jsonData.encryptedData) {
    const secretKey = pm.environment.get('encryptKey') || 'default-key';
    
    try {
        const decryptedBytes = CryptoJS.AES.decrypt(jsonData.encryptedData, secretKey);
        const decryptedText = decryptedBytes.toString(CryptoJS.enc.Utf8);
        const decryptedData = JSON.parse(decryptedText);
        
        console.log('✓ 数据解密成功');
        console.log('解密后的数据:', decryptedData);
        
        // 验证解密后的数据
        pm.test("解密后的数据有效", function () {
            pm.expect(decryptedData).to.have.property('userId');
            pm.expect(decryptedData).to.have.property('balance');
        });
        
        // 保存解密后的数据
        pm.environment.set('decryptedUserId', decryptedData.userId);
        pm.environment.set('userBalance', decryptedData.balance.toString());
        
    } catch (error) {
        console.error('✗ 解密失败:', error.message);
        pm.test("数据解密失败", function () {
            throw new Error('解密失败: ' + error.message);
        });
    }
}
```

#### 示例 12：业务逻辑验证

```javascript
var _ = require('lodash');
var moment = require('moment');

const jsonData = pm.response.json();

// 订单状态验证
pm.test("订单状态有效", function () {
    const validStatuses = ['pending', 'processing', 'completed', 'cancelled'];
    pm.expect(validStatuses).to.include(jsonData.order.status);
});

// 日期格式验证
pm.test("日期格式正确", function () {
    const createTime = jsonData.order.createTime;
    pm.expect(moment(createTime, moment.ISO_8601, true).isValid()).to.equal(true);
});

// 金额计算验证
pm.test("订单金额计算正确", function () {
    const items = jsonData.order.items;
    let calculatedTotal = _.sumBy(items, function(item) {
        return item.price * item.quantity;
    });
    
    // 加上运费
    calculatedTotal += jsonData.order.shippingFee || 0;
    
    // 减去折扣
    calculatedTotal -= jsonData.order.discount || 0;
    
    calculatedTotal = _.round(calculatedTotal, 2);
    
    pm.expect(calculatedTotal).to.equal(jsonData.order.totalAmount);
    console.log('计算金额:', calculatedTotal, '订单金额:', jsonData.order.totalAmount);
});

// 库存验证
pm.test("商品库存充足", function () {
    jsonData.order.items.forEach(function(item) {
        pm.expect(item.quantity).to.be.below(item.stock + 1);
        if (item.quantity > item.stock * 0.8) {
            console.warn('⚠ 商品', item.name, '库存不足，剩余:', item.stock);
        }
    });
});
```

### 使用内置库示例

#### 示例 1：CryptoJS 加密库

```javascript
// 直接使用全局变量 CryptoJS（无需 require）
// var CryptoJS = require('crypto-js'); // 可选，也支持这种方式

// 1. AES 加密/解密
const message = 'secret message';
const secretKey = 'my-secret-key-123';

// 加密
const encrypted = CryptoJS.AES.encrypt(message, secretKey).toString();
pm.environment.set('encrypted', encrypted);
console.log('AES 加密:', encrypted);

// 解密
const decrypted = CryptoJS.AES.decrypt(encrypted, secretKey);
const decryptedText = decrypted.toString(CryptoJS.enc.Utf8);
console.log('AES 解密:', decryptedText);

// 2. MD5 哈希
const password = 'myPassword123';
const md5Hash = CryptoJS.MD5(password).toString();
console.log('MD5 哈希:', md5Hash);
pm.environment.set('passwordHash', md5Hash);

// 3. SHA256 哈希
const sha256Hash = CryptoJS.SHA256(password).toString();
console.log('SHA256 哈希:', sha256Hash);

// 4. HMAC-SHA256 签名
const timestamp = Date.now().toString();
const data = 'userId=123&timestamp=' + timestamp;
const hmacKey = 'my-hmac-key';
const signature = CryptoJS.HmacSHA256(data, hmacKey).toString();
console.log('HMAC-SHA256 签名:', signature);

// 5. Base64 编码/解码
const text = 'Hello World';
const base64Encoded = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
console.log('Base64 编码:', base64Encoded);

const base64Decoded = CryptoJS.enc.Base64.parse(base64Encoded).toString(CryptoJS.enc.Utf8);
console.log('Base64 解码:', base64Decoded);

// 6. 生成随机字符串
const randomBytes = CryptoJS.lib.WordArray.random(16);
const randomString = randomBytes.toString(CryptoJS.enc.Hex);
console.log('随机字符串:', randomString);
pm.environment.set('nonce', randomString);
```

#### 示例 2：Lodash 数据处理库

```javascript
// 直接使用全局变量 _（无需 require）
// var _ = require('lodash'); // 可选，也支持这种方式

// 1. 数组操作
const numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// 过滤
const filtered = _.filter(numbers, function (n) {
    return n > 5;
});
console.log('过滤结果:', filtered); // [6, 7, 8, 9, 10]

// 映射
const doubled = _.map(numbers, function(n) {
    return n * 2;
});
console.log('映射结果:', doubled);

// 求和
const sum = _.sum(numbers);
console.log('数组求和:', sum);

// 取平均值
const avg = _.mean(numbers);
console.log('平均值:', avg);

// 2. 对象操作
const user = {
    id: 123,
    name: 'John Doe',
    email: 'john@example.com',
    age: 30,
    city: 'Beijing'
};

// 提取属性值
const values = _.values(user);
console.log('对象值:', values);

// 提取属性名
const keys = _.keys(user);
console.log('对象键:', keys);

// 挑选特定属性
const picked = _.pick(user, ['id', 'name', 'email']);
console.log('挑选属性:', picked);

// 省略特定属性
const omitted = _.omit(user, ['age', 'city']);
console.log('省略属性:', omitted);

// 3. 集合操作
const users = [
    { id: 1, name: 'Alice', age: 25, role: 'admin' },
    { id: 2, name: 'Bob', age: 30, role: 'user' },
    { id: 3, name: 'Charlie', age: 35, role: 'user' },
    { id: 4, name: 'David', age: 28, role: 'admin' }
];

// 查找
const admin = _.find(users, { role: 'admin' });
console.log('第一个管理员:', admin);

// 过滤
const admins = _.filter(users, { role: 'admin' });
console.log('所有管理员:', admins);

// 分组
const grouped = _.groupBy(users, 'role');
console.log('按角色分组:', grouped);

// 排序
const sorted = _.sortBy(users, ['age']);
console.log('按年龄排序:', sorted);

// 提取特定字段
const names = _.map(users, 'name');
console.log('所有姓名:', names);

// 统计
const sumAges = _.sumBy(users, 'age');
console.log('年龄总和:', sumAges);

// 4. 字符串操作
const str = 'hello world';
const capitalized = _.capitalize(str);
console.log('首字母大写:', capitalized);

const camelCase = _.camelCase('hello-world-example');
console.log('驼峰命名:', camelCase); // helloWorldExample

const snakeCase = _.snakeCase('helloWorldExample');
console.log('蛇形命名:', snakeCase); // hello_world_example

// 5. 随机数生成
const randomNum = _.random(1, 100);
console.log('随机整数:', randomNum);

const randomFloat = _.random(1.5, 5.5, true);
console.log('随机浮点数:', randomFloat);

// 6. 去重
const duplicates = [1, 2, 2, 3, 4, 4, 5];
const unique = _.uniq(duplicates);
console.log('去重结果:', unique);

// 7. 数组操作
const arr1 = [1, 2, 3];
const arr2 = [3, 4, 5];

// 交集
const intersection = _.intersection(arr1, arr2);
console.log('交集:', intersection); // [3]

// 并集
const union = _.union(arr1, arr2);
console.log('并集:', union); // [1, 2, 3, 4, 5]

// 差集
const difference = _.difference(arr1, arr2);
console.log('差集:', difference); // [1, 2]

// 8. 深度克隆
const original = { a: 1, b: { c: 2 } };
const cloned = _.cloneDeep(original);
cloned.b.c = 999;
console.log('原始对象:', original.b.c); // 2
console.log('克隆对象:', cloned.b.c); // 999

// 9. 合并对象
const obj1 = { a: 1, b: 2 };
const obj2 = { b: 3, c: 4 };
const merged = _.merge({}, obj1, obj2);
console.log('合并结果:', merged); // { a: 1, b: 3, c: 4 }

// 10. 防抖和节流（在循环中使用）
const processData = _.debounce(function(data) {
    console.log('处理数据:', data);
}, 300);

// 11. 链式调用
const result = _(numbers)
    .filter(function(n) { return n % 2 === 0; })
    .map(function(n) { return n * n; })
    .sum();
console.log('偶数平方和:', result);
```

#### 示例 3：Moment.js 日期时间库

```javascript
// 直接使用全局变量 moment（无需 require）
// var moment = require('moment'); // 可选，也支持这种方式

// 1. 获取当前时间
const now = moment();
console.log('当前时间:', now.format('YYYY-MM-DD HH:mm:ss'));

// 2. 格式化日期
const formatted = moment().format('YYYY-MM-DD HH:mm:ss');
pm.environment.set('currentTime', formatted);
console.log('格式化时间:', formatted);

// ISO 8601 格式
const iso = moment().toISOString();
console.log('ISO 格式:', iso);

// Unix 时间戳
const timestamp = moment().unix();
console.log('Unix 时间戳:', timestamp);

// 毫秒时间戳
const milliseconds = moment().valueOf();
console.log('毫秒时间戳:', milliseconds);

// 3. 解析日期
const parsed1 = moment('2024-01-01', 'YYYY-MM-DD');
console.log('解析日期:', parsed1.format('YYYY年MM月DD日'));

const parsed2 = moment('01/15/2024', 'MM/DD/YYYY');
console.log('美式日期:', parsed2.format('YYYY-MM-DD'));

// 从时间戳解析
const fromTimestamp = moment(1704067200000);
console.log('时间戳解析:', fromTimestamp.format('YYYY-MM-DD HH:mm:ss'));

// 4. 日期计算
const tomorrow = moment().add(1, 'days');
console.log('明天:', tomorrow.format('YYYY-MM-DD'));

const nextWeek = moment().add(1, 'weeks');
console.log('下周:', nextWeek.format('YYYY-MM-DD'));

const nextMonth = moment().add(1, 'months');
console.log('下月:', nextMonth.format('YYYY-MM-DD'));

const yesterday = moment().subtract(1, 'days');
console.log('昨天:', yesterday.format('YYYY-MM-DD'));

// 5. 日期比较
const date1 = moment('2024-01-01');
const date2 = moment('2024-12-31');

console.log('date1 在 date2 之前:', date1.isBefore(date2)); // true
console.log('date1 在 date2 之后:', date1.isAfter(date2)); // false
console.log('日期相同:', date1.isSame(date2)); // false

// 6. 日期差异
const start = moment('2024-01-01');
const end = moment('2024-12-31');

const diffDays = end.diff(start, 'days');
console.log('相差天数:', diffDays);

const diffMonths = end.diff(start, 'months');
console.log('相差月数:', diffMonths);

const diffYears = end.diff(start, 'years');
console.log('相差年数:', diffYears);

// 7. 开始和结束时间
const startOfDay = moment().startOf('day');
console.log('今天开始:', startOfDay.format('YYYY-MM-DD HH:mm:ss'));

const endOfDay = moment().endOf('day');
console.log('今天结束:', endOfDay.format('YYYY-MM-DD HH:mm:ss'));

const startOfMonth = moment().startOf('month');
console.log('本月开始:', startOfMonth.format('YYYY-MM-DD'));

const endOfMonth = moment().endOf('month');
console.log('本月结束:', endOfMonth.format('YYYY-MM-DD'));

// 8. 相对时间
const aWeekAgo = moment().subtract(7, 'days');
console.log('一周前:', aWeekAgo.fromNow()); // 7 days ago

const inThreeDays = moment().add(3, 'days');
console.log('三天后:', inThreeDays.fromNow()); // in 3 days

// 9. 验证日期
const validDate = moment('2024-01-01', 'YYYY-MM-DD', true).isValid();
console.log('日期有效:', validDate); // true

const invalidDate = moment('2024-13-01', 'YYYY-MM-DD', true).isValid();
console.log('日期无效:', invalidDate); // false

// 10. 实用场景：生成各种时间格式
pm.environment.set('dateYMD', moment().format('YYYY-MM-DD'));
pm.environment.set('dateYMDHMS', moment().format('YYYY-MM-DD HH:mm:ss'));
pm.environment.set('dateISO', moment().toISOString());
pm.environment.set('timestamp', moment().valueOf().toString());
pm.environment.set('dateChina', moment().format('YYYY年MM月DD日 HH时mm分ss秒'));

// 11. 时区处理（如果支持）
const utc = moment.utc();
console.log('UTC 时间:', utc.format('YYYY-MM-DD HH:mm:ss'));

// 12. 业务场景：生成时间范围
const today = moment().startOf('day');
const todayEnd = moment().endOf('day');

pm.environment.set('queryStartTime', today.valueOf().toString());
pm.environment.set('queryEndTime', todayEnd.valueOf().toString());

console.log('查询开始时间:', today.format('YYYY-MM-DD HH:mm:ss'));
console.log('查询结束时间:', todayEnd.format('YYYY-MM-DD HH:mm:ss'));
```

#### 示例 4：组合使用多个库

```javascript
var _ = require('lodash');
var moment = require('moment');
var CryptoJS = require('crypto-js');

// 场景：生成带签名的 API 请求

// 1. 准备请求参数
const params = {
    userId: pm.environment.get('userId') || '123',
    timestamp: moment().valueOf().toString(),
    nonce: CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex),
    action: 'getUserInfo'
};

// 2. 按键名排序参数
const sortedKeys = _.keys(params).sort();
console.log('排序后的键:', sortedKeys);

// 3. 拼接签名字符串
const signString = _.map(sortedKeys, function(key) {
    return key + '=' + params[key];
}).join('&');
console.log('签名字符串:', signString);

// 4. 生成签名
const secretKey = pm.environment.get('secretKey') || 'default-secret';
const signature = CryptoJS.HmacSHA256(signString, secretKey).toString();
console.log('签名:', signature);

// 5. 保存到环境变量
pm.environment.set('requestTimestamp', params.timestamp);
pm.environment.set('requestNonce', params.nonce);
pm.environment.set('requestSignature', signature);

// 6. 打印完整请求参数
const fullParams = _.assign({}, params, { signature: signature });
console.log('完整请求参数:', JSON.stringify(fullParams, null, 2));
```

#### 示例 5：数据模拟生成器

```javascript
var _ = require('lodash');
var moment = require('moment');

// 生成批量测试数据
const mockUsers = _.times(10, function(index) {
    return {
        id: 1000 + index,
        username: 'user_' + _.random(10000, 99999),
        email: 'test' + index + '@example.com',
        age: _.random(18, 60),
        gender: _.sample(['male', 'female']),
        city: _.sample(['Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen']),
        registerTime: moment().subtract(_.random(1, 365), 'days').format('YYYY-MM-DD HH:mm:ss'),
        lastLoginTime: moment().subtract(_.random(0, 30), 'days').format('YYYY-MM-DD HH:mm:ss'),
        isActive: _.sample([true, false]),
        score: _.round(_.random(0, 100, true), 2)
    };
});

console.log('生成了', mockUsers.length, '个模拟用户');
console.log('示例用户:', JSON.stringify(mockUsers[0], null, 2));

// 保存第一个用户信息
pm.environment.set('testUserId', mockUsers[0].id.toString());
pm.environment.set('testUsername', mockUsers[0].username);
pm.environment.set('mockUsersData', JSON.stringify(mockUsers));

// 统计信息
const avgAge = _.meanBy(mockUsers, 'age');
const avgScore = _.meanBy(mockUsers, 'score');
const activeCount = _.filter(mockUsers, { isActive: true }).length;

console.log('平均年龄:', _.round(avgAge, 1));
console.log('平均得分:', _.round(avgScore, 2));
console.log('活跃用户数:', activeCount);
```

---

## 高级代码片段示例

### 代码片段 1：OAuth 2.0 Token 自动刷新

```javascript
// Pre-request Script: 自动检查和刷新 Token
const tokenExpireTime = pm.environment.get('tokenExpireTime');
const currentTime = Date.now();

// 检查 token 是否过期（提前5分钟刷新）
if (!tokenExpireTime || currentTime > (parseInt(tokenExpireTime) - 300000)) {
    console.log('Token 即将过期或已过期，需要刷新');
    // 在实际环境中，这里应该触发刷新 token 的逻辑
    // 由于不支持 pm.sendRequest，建议在测试流程中手动添加刷新 token 的请求
} else {
    const token = pm.environment.get('authToken');
    if (token) {
        pm.request.headers.upsert({
            key: 'Authorization',
            value: 'Bearer ' + token
        });
        console.log('✓ Token 有效，已添加认证头');
    }
}
```

### 代码片段 2：接口限流处理（本地计数）

```javascript
// Pre-request Script: 本地请求限流检查
const lastRequestTime = pm.environment.get('lastRequestTime');
const minInterval = 100; // 最小请求间隔（毫秒）

if (lastRequestTime) {
    const timeSinceLastRequest = Date.now() - parseInt(lastRequestTime);
    if (timeSinceLastRequest < minInterval) {
        const waitTime = minInterval - timeSinceLastRequest;
        console.warn('⚠ 请求过于频繁，建议等待', waitTime, 'ms');
    }
}

pm.environment.set('lastRequestTime', Date.now().toString());
```

### 代码片段 3：动态环境切换

```javascript
// Pre-request Script: 根据变量切换环境配置
const env = pm.environment.get('ENV') || 'dev';

const configs = {
    dev: {
        baseUrl: 'https://dev-api.example.com',
        timeout: 10000,
        debug: true
    },
    test: {
        baseUrl: 'https://test-api.example.com',
        timeout: 8000,
        debug: true
    },
    staging: {
        baseUrl: 'https://staging-api.example.com',
        timeout: 5000,
        debug: false
    },
    prod: {
        baseUrl: 'https://api.example.com',
        timeout: 5000,
        debug: false
    }
};

const config = configs[env];
if (!config) {
    throw new Error('未知环境: ' + env);
}

// 应用配置
pm.environment.set('baseUrl', config.baseUrl);
pm.environment.set('timeout', config.timeout.toString());
pm.environment.set('debug', config.debug.toString());

// 添加环境标识头
pm.request.headers.upsert({
    key: 'X-Environment',
    value: env
});

console.log('✓ 已切换到', env, '环境');
console.log('  Base URL:', config.baseUrl);
console.log('  Timeout:', config.timeout);
```

### 代码片段 4：请求体动态模板填充

```javascript
// Pre-request Script: JSON 模板动态替换
var _ = require('lodash');
var moment = require('moment');

// 定义请求体模板（可以从环境变量获取）
const requestTemplate = {
    "requestId": "{{requestId}}",
    "timestamp": "{{timestamp}}",
    "userId": "{{userId}}",
    "action": "{{action}}",
    "data": {
        "startDate": "{{startDate}}",
        "endDate": "{{endDate}}",
        "pageNum": "{{pageNum}}",
        "pageSize": "{{pageSize}}"
    }
};

// 准备替换数据
const templateData = {
    requestId: pm.uuid(),
    timestamp: moment().valueOf(),
    userId: pm.environment.get('userId') || '123',
    action: 'queryOrders',
    startDate: moment().subtract(7, 'days').format('YYYY-MM-DD'),
    endDate: moment().format('YYYY-MM-DD'),
    pageNum: 1,
    pageSize: 20
};

// 替换模板
let requestBodyStr = JSON.stringify(requestTemplate);
_.forEach(templateData, function(value, key) {
    const placeholder = '{{' + key + '}}';
    requestBodyStr = requestBodyStr.replace(new RegExp(placeholder, 'g'), value);
});

const requestBody = JSON.parse(requestBodyStr);
console.log('生成的请求体:', JSON.stringify(requestBody, null, 2));

// 保存到环境变量供请求使用
pm.environment.set('dynamicRequestBody', JSON.stringify(requestBody));
```

### 代码片段 5：批量参数验证器

```javascript
// Pre-request Script: 请求参数验证
const validationRules = {
    userId: {
        required: true,
        type: 'string',
        pattern: /^[0-9]+$/,
        message: 'userId 必须是数字字符串'
    },
    email: {
        required: false,
        type: 'string',
        pattern: /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/,
        message: 'email 格式不正确'
    },
    age: {
        required: false,
        type: 'number',
        min: 0,
        max: 150,
        message: 'age 必须在 0-150 之间'
    }
};

const params = {
    userId: pm.environment.get('userId'),
    email: pm.environment.get('email'),
    age: parseInt(pm.environment.get('age'))
};

const errors = [];

// 执行验证
Object.keys(validationRules).forEach(function(key) {
    const rule = validationRules[key];
    const value = params[key];
    
    // 必填校验
    if (rule.required && (!value || value === '')) {
        errors.push(key + ' 是必填项');
        return;
    }
    
    if (value !== undefined && value !== null && value !== '') {
        // 类型校验
        if (rule.type === 'number' && typeof value !== 'number') {
            errors.push(key + ' 必须是数字类型');
        }
        
        // 正则校验
        if (rule.pattern && !rule.pattern.test(String(value))) {
            errors.push(rule.message || key + ' 格式不正确');
        }
        
        // 范围校验
        if (rule.type === 'number') {
            if (rule.min !== undefined && value < rule.min) {
                errors.push(key + ' 不能小于 ' + rule.min);
            }
            if (rule.max !== undefined && value > rule.max) {
                errors.push(key + ' 不能大于 ' + rule.max);
            }
        }
    }
});

if (errors.length > 0) {
    console.error('参数验证失败:');
    errors.forEach(function(error) {
        console.error('  ✗', error);
    });
    throw new Error('参数验证失败: ' + errors.join('; '));
} else {
    console.log('✓ 所有参数验证通过');
}
```

### 代码片段 6：响应数据提取链（链式调用场景）

```javascript
// Post-request Script: 从复杂嵌套响应中提取数据
var _ = require('lodash');

const jsonData = pm.response.json();

pm.test("响应结构正确", function () {
    pm.response.to.have.status(200);
    pm.expect(jsonData.code).to.equal(0);
});

// 使用 lodash 从复杂结构提取数据
const extractedData = {
    // 提取用户 ID 列表
    userIds: _.map(_.get(jsonData, 'data.users', []), 'id'),
    
    // 提取第一个用户的详细信息
    firstUser: _.get(jsonData, 'data.users[0]', null),
    
    // 提取所有管理员
    admins: _.filter(_.get(jsonData, 'data.users', []), { role: 'admin' }),
    
    // 计算总金额
    totalAmount: _.sumBy(_.get(jsonData, 'data.orders', []), 'amount'),
    
    // 按状态分组订单
    ordersByStatus: _.groupBy(_.get(jsonData, 'data.orders', []), 'status'),
    
    // 提取分页信息
    pagination: _.pick(_.get(jsonData, 'data', {}), ['pageNum', 'pageSize', 'total'])
};

console.log('提取的数据:', JSON.stringify(extractedData, null, 2));

// 保存关键信息到环境变量
if (extractedData.firstUser) {
    pm.environment.set('firstUserId', extractedData.firstUser.id);
    pm.environment.set('firstUserName', extractedData.firstUser.name);
}

if (extractedData.userIds.length > 0) {
    pm.environment.set('userIdList', JSON.stringify(extractedData.userIds));
}

pm.environment.set('totalAmount', extractedData.totalAmount.toString());

console.log('✓ 提取了', extractedData.userIds.length, '个用户ID');
console.log('✓ 总金额:', extractedData.totalAmount);
```

### 代码片段 7：性能基准测试

```javascript
// Post-request Script: 性能监控和基准对比
var _ = require('lodash');

const responseTime = pm.response.responseTime;
const endpoint = pm.request.url.getPath();

// 定义性能基准（毫秒）
const benchmarks = {
    '/api/user/login': 500,
    '/api/user/profile': 300,
    '/api/orders/list': 1000,
    '/api/products/search': 800,
    'default': 1000
};

const benchmark = benchmarks[endpoint] || benchmarks['default'];

// 性能等级评估
let performanceGrade = '';
let performanceScore = 100;

if (responseTime <= benchmark * 0.3) {
    performanceGrade = 'A+ (优秀)';
    performanceScore = 100;
} else if (responseTime <= benchmark * 0.5) {
    performanceGrade = 'A (良好)';
    performanceScore = 90;
} else if (responseTime <= benchmark * 0.8) {
    performanceGrade = 'B (一般)';
    performanceScore = 75;
} else if (responseTime <= benchmark) {
    performanceGrade = 'C (及格)';
    performanceScore = 60;
} else if (responseTime <= benchmark * 1.5) {
    performanceGrade = 'D (较慢)';
    performanceScore = 40;
} else {
    performanceGrade = 'F (缓慢)';
    performanceScore = 20;
}

// 记录性能数据
const perfKey = 'perf_' + endpoint.replace(/\//g, '_');
let perfHistory = pm.environment.get(perfKey);
if (!perfHistory) {
    perfHistory = [];
} else {
    try {
        perfHistory = JSON.parse(perfHistory);
    } catch (e) {
        perfHistory = [];
    }
}

perfHistory.push(responseTime);

// 只保留最近10次记录
if (perfHistory.length > 10) {
    perfHistory = _.takeRight(perfHistory, 10);
}

pm.environment.set(perfKey, JSON.stringify(perfHistory));

// 计算平均响应时间
const avgResponseTime = _.mean(perfHistory);
const minResponseTime = _.min(perfHistory);
const maxResponseTime = _.max(perfHistory);

// 输出性能报告
console.log('=== 性能测试报告 ===');
console.log('接口:', endpoint);
console.log('本次响应时间:', responseTime, 'ms');
console.log('性能等级:', performanceGrade, '(得分:', performanceScore + ')');
console.log('性能基准:', benchmark, 'ms');
console.log('历史平均:', _.round(avgResponseTime, 2), 'ms');
console.log('最快响应:', minResponseTime, 'ms');
console.log('最慢响应:', maxResponseTime, 'ms');

// 创建性能测试
pm.test("性能测试: " + performanceGrade, function () {
    pm.expect(responseTime).to.be.below(benchmark);
});

// 如果性能下降，发出警告
if (responseTime > avgResponseTime * 1.5) {
    console.warn('⚠️ 警告: 本次响应时间比平均值慢了', _.round((responseTime / avgResponseTime - 1) * 100, 1) + '%');
}
```

### 代码片段 8：数据一致性校验

```javascript
// Post-request Script: 复杂业务逻辑验证
var _ = require('lodash');
var moment = require('moment');

const jsonData = pm.response.json();

pm.test("订单数据一致性校验", function () {
    if (!jsonData.data || !jsonData.data.order) {
        throw new Error('订单数据不存在');
    }
    
    const order = jsonData.data.order;
    
    // 1. 金额计算验证
    const calculatedSubtotal = _.sumBy(order.items, function(item) {
        return item.price * item.quantity;
    });
    
    pm.expect(_.round(calculatedSubtotal, 2)).to.equal(order.subtotal);
    console.log('✓ 小计金额正确:', order.subtotal);
    
    // 2. 总金额验证（小计 + 运费 - 折扣）
    const calculatedTotal = _.round(
        order.subtotal + (order.shippingFee || 0) - (order.discount || 0),
        2
    );
    pm.expect(calculatedTotal).to.equal(order.totalAmount);
    console.log('✓ 总金额正确:', order.totalAmount);
    
    // 3. 状态转换逻辑验证
    const validStatusFlow = {
        'pending': ['processing', 'cancelled'],
        'processing': ['shipped', 'cancelled'],
        'shipped': ['delivered', 'return_requested'],
        'delivered': ['completed', 'return_requested'],
        'return_requested': ['refunding', 'delivered'],
        'refunding': ['refunded'],
        'completed': [],
        'cancelled': [],
        'refunded': []
    };
    
    const currentStatus = order.status;
    console.log('✓ 当前订单状态:', currentStatus);
    
    // 4. 日期逻辑验证
    if (order.createTime && order.updateTime) {
        const createTime = moment(order.createTime);
        const updateTime = moment(order.updateTime);
        
        pm.expect(updateTime.isSameOrAfter(createTime)).to.equal(true);
        console.log('✓ 更新时间不早于创建时间');
    }
    
    // 5. 商品数量验证
    pm.expect(order.items.length).to.be.above(0);
    
    order.items.forEach(function(item, index) {
        // 数量必须大于0
        pm.expect(item.quantity).to.be.above(0);
        
        // 价格必须大于等于0
        pm.expect(item.price).to.be.below(999999);
        
        // SKU 不能为空
        pm.expect(item.sku).to.be.a('string');
        pm.expect(item.sku.length).to.be.above(0);
        
        console.log('✓ 商品', (index + 1), '验证通过:', item.name);
    });
});

// 6. 数据完整性检查
pm.test("必填字段检查", function () {
    const requiredFields = [
        'orderId',
        'userId',
        'status',
        'totalAmount',
        'createTime',
        'items'
    ];
    
    const order = jsonData.data.order;
    requiredFields.forEach(function(field) {
        pm.expect(order).to.have.property(field);
        pm.expect(order[field]).to.not.equal(null);
        pm.expect(order[field]).to.not.equal(undefined);
        console.log('✓ 字段', field, '存在且有值');
    });
});
```

### 代码片段 9：自定义断言函数库

```javascript
// Post-request Script: 创建可复用的断言函数

// 定义自定义断言函数库
const customAssert = {
    // 断言数组包含特定元素
    arrayContains: function(array, element, message) {
        const contains = array.indexOf(element) !== -1;
        if (!contains) {
            throw new Error(message || '数组不包含元素: ' + element);
        }
        return true;
    },
    
    // 断言字符串长度在范围内
    stringLengthBetween: function(str, min, max, message) {
        if (str.length < min || str.length > max) {
            throw new Error(message || '字符串长度应在 ' + min + '-' + max + ' 之间');
        }
        return true;
    },
    
    // 断言日期在范围内
    dateInRange: function(dateStr, startDate, endDate, message) {
        const date = new Date(dateStr);
        const start = new Date(startDate);
        const end = new Date(endDate);
        
        if (date < start || date > end) {
            throw new Error(message || '日期不在指定范围内');
        }
        return true;
    },
    
    // 断言对象结构匹配
    objectMatchesSchema: function(obj, schema, message) {
        for (let key in schema) {
            if (!obj.hasOwnProperty(key)) {
                throw new Error(message || '对象缺少字段: ' + key);
            }
            
            const expectedType = schema[key];
            const actualType = typeof obj[key];
            
            if (actualType !== expectedType && !(expectedType === 'array' && Array.isArray(obj[key]))) {
                throw new Error(message || '字段 ' + key + ' 类型错误，期望: ' + expectedType + '，实际: ' + actualType);
            }
        }
        return true;
    },
    
    // 断言枚举值
    isOneOf: function(value, allowedValues, message) {
        if (allowedValues.indexOf(value) === -1) {
            throw new Error(message || '值必须是以下之一: ' + allowedValues.join(', '));
        }
        return true;
    },
    
    // 断言数值范围
    numberInRange: function(num, min, max, message) {
        if (num < min || num > max) {
            throw new Error(message || '数值应在 ' + min + '-' + max + ' 之间');
        }
        return true;
    }
};

// 使用自定义断言
const jsonData = pm.response.json();

pm.test("自定义断言测试", function () {
    const user = jsonData.data.user;
    
    // 测试对象结构
    customAssert.objectMatchesSchema(user, {
        id: 'string',
        name: 'string',
        age: 'number',
        email: 'string',
        roles: 'array'
    });
    console.log('✓ 对象结构匹配');
    
    // 测试枚举值
    customAssert.isOneOf(user.status, ['active', 'inactive', 'suspended']);
    console.log('✓ 状态值有效');
    
    // 测试数值范围
    customAssert.numberInRange(user.age, 0, 150);
    console.log('✓ 年龄在有效范围内');
    
    // 测试字符串长度
    customAssert.stringLengthBetween(user.name, 1, 50);
    console.log('✓ 名称长度有效');
    
    // 测试数组包含
    customAssert.arrayContains(user.roles, 'user');
    console.log('✓ 角色列表包含基础角色');
});
```

### 代码片段 10：多语言国际化测试

```javascript
// Pre-request Script: 国际化多语言测试
const languages = ['zh-CN', 'en-US', 'ja-JP', 'ko-KR', 'fr-FR'];
const currentLang = pm.environment.get('testLanguage') || 'zh-CN';

// 设置语言请求头
pm.request.headers.upsert({
    key: 'Accept-Language',
    value: currentLang
});

pm.request.headers.upsert({
    key: 'X-Locale',
    value: currentLang
});

console.log('当前测试语言:', currentLang);

// Post-request Script: 验证多语言响应
pm.test("多语言响应验证", function () {
    const jsonData = pm.response.json();
    const lang = pm.request.headers.get('Accept-Language') || 'zh-CN';
    
    // 检查响应中的语言字段
    if (jsonData.message) {
        console.log('响应消息 (' + lang + '):', jsonData.message);
        
        // 验证中文响应包含中文字符
        if (lang === 'zh-CN') {
            pm.expect(jsonData.message).to.match(/[\u4e00-\u9fa5]/);
        }
        
        // 验证英文响应只包含ASCII字符
        if (lang === 'en-US') {
            pm.expect(jsonData.message).to.match(/^[\x00-\x7F]*$/);
        }
    }
});
```

### 代码片段 11：幂等性测试

```javascript
// Post-request Script: 幂等性验证（需要多次执行同一请求）
const requestId = pm.environment.get('idempotencyTestId');
const responseBody = pm.response.text();
const responseHash = CryptoJS.MD5(responseBody).toString();

if (!requestId) {
    // 第一次请求，保存请求ID和响应哈希
    pm.environment.set('idempotencyTestId', pm.uuid());
    pm.environment.set('idempotencyResponseHash', responseHash);
    pm.environment.set('idempotencyCount', '1');
    console.log('幂等性测试：第1次请求，已保存响应基准');
} else {
    // 后续请求，验证响应是否一致
    const savedHash = pm.environment.get('idempotencyResponseHash');
    let count = parseInt(pm.environment.get('idempotencyCount') || '1');
    count++;
    pm.environment.set('idempotencyCount', count.toString());
    
    pm.test("幂等性验证 - 第" + count + "次请求", function () {
        pm.expect(responseHash).to.equal(savedHash);
        console.log('✓ 响应与第1次请求一致');
    });
    
    console.log('幂等性测试：已执行', count, '次请求');
    
    // 完成5次测试后清理
    if (count >= 5) {
        pm.environment.unset('idempotencyTestId');
        pm.environment.unset('idempotencyResponseHash');
        pm.environment.unset('idempotencyCount');
        console.log('✓ 幂等性测试完成，已清理测试数据');
    }
}
```

### 代码片段 12：响应时间统计图表数据生成

```javascript
// Post-request Script: 收集性能数据用于图表展示
var moment = require('moment');
var _ = require('lodash');

const endpoint = pm.request.url.getPath();
const responseTime = pm.response.responseTime;
const timestamp = moment().format('YYYY-MM-DD HH:mm:ss');

// 获取历史数据
let perfData = pm.environment.get('performanceChartData');
if (!perfData) {
    perfData = [];
} else {
    try {
        perfData = JSON.parse(perfData);
    } catch (e) {
        perfData = [];
    }
}

// 添加新数据点
perfData.push({
    endpoint: endpoint,
    responseTime: responseTime,
    timestamp: timestamp,
    statusCode: pm.response.code,
    success: pm.response.code === 200
});

// 保留最近100条记录
if (perfData.length > 100) {
    perfData = _.takeRight(perfData, 100);
}

pm.environment.set('performanceChartData', JSON.stringify(perfData));

// 按接口分组统计
const groupedByEndpoint = _.groupBy(perfData, 'endpoint');
const stats = {};

_.forEach(groupedByEndpoint, function(records, endpoint) {
    const times = _.map(records, 'responseTime');
    stats[endpoint] = {
        count: records.length,
        avg: _.round(_.mean(times), 2),
        min: _.min(times),
        max: _.max(times),
        p50: _.round(times.sort()[Math.floor(times.length * 0.5)], 2),
        p95: _.round(times.sort()[Math.floor(times.length * 0.95)], 2),
        successRate: _.round(_.filter(records, { success: true }).length / records.length * 100, 2)
    };
});

console.log('=== 性能统计数据 ===');
console.log(JSON.stringify(stats, null, 2));

// 保存统计数据
pm.environment.set('performanceStats', JSON.stringify(stats));
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
    - ✅ **推荐**：直接使用全局变量 `CryptoJS`、`_`、`moment`（已预加载）
    - ✅ **也支持**：使用 `require('library-name')` 加载（兼容 Postman）
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
pm.test("测试名", function () {            // 定义测试
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

// ===== 内置库（全局变量，直接使用）=====
CryptoJS.MD5('text').toString()           // 加密库（全局变量 CryptoJS）
_.random(1, 100)                          // 工具库（全局变量 _）
moment().format('YYYY-MM-DD')             // 日期库（全局变量 moment）

// 也支持 require()（兼容 Postman）
var CryptoJS = require('crypto-js')
var _ = require('lodash')
var moment = require('moment')
```

---

## 参考资源

- [Postman 官方文档](https://learning.postman.com/docs/writing-scripts/intro-to-scripts/)
- [ChaiJS 断言库](https://www.chaijs.com/api/bdd/)
