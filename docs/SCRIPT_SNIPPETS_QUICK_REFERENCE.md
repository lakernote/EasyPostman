# Script 代码片段速查表

本文档提供了常用脚本代码片段的快速索引，方便快速查找和复制使用。

## 📑 目录

### 基础操作
- [设置环境变量](#设置环境变量)
- [获取环境变量](#获取环境变量)
- [生成UUID](#生成uuid)
- [获取时间戳](#获取时间戳)
- [添加请求头](#添加请求头)
- [添加查询参数](#添加查询参数)

### 认证相关
- [Bearer Token 认证](#bearer-token-认证)
- [Basic 认证](#basic-认证)
- [API Key 认证](#api-key-认证)
- [HMAC 签名](#hmac-签名)
- [OAuth 2.0 Token 刷新](#oauth-20-token-刷新)

### 数据处理
- [解析 JSON 响应](#解析-json-响应)
- [提取嵌套数据](#提取嵌套数据)
- [数组遍历和过滤](#数组遍历和过滤)
- [数据转换](#数据转换)
- [JSON 模板填充](#json-模板填充)

### 测试断言
- [状态码断言](#状态码断言)
- [响应时间断言](#响应时间断言)
- [响应体断言](#响应体断言)
- [响应头断言](#响应头断言)
- [JSON 结构断言](#json-结构断言)

### 加密和编码
- [MD5 哈希](#md5-哈希)
- [SHA256 哈希](#sha256-哈希)
- [HMAC-SHA256 签名](#hmac-sha256-签名)
- [AES 加密/解密](#aes-加密解密)
- [Base64 编码/解码](#base64-编码解码)

### 日期时间
- [格式化当前时间](#格式化当前时间)
- [日期计算](#日期计算)
- [日期比较](#日期比较)
- [解析日期字符串](#解析日期字符串)

### Cookie 管理
- [获取 Cookie](#获取-cookie)
- [设置 Cookie](#设置-cookie)
- [跨域 Cookie 管理](#跨域-cookie-管理)

### 外部数据源
- [Redis 读写断言](#redis-读写断言)
- [Kafka 发消息断言](#kafka-发消息断言)
- [Elasticsearch 写入断言](#elasticsearch-写入断言)
- [InfluxDB 查询与写入断言](#influxdb-查询与写入断言)

### 实用工具
- [生成随机数据](#生成随机数据)
- [数据验证](#数据验证)
- [性能监控](#性能监控)
- [调试日志](#调试日志)

---

## 基础操作

### 设置环境变量
```javascript
pm.environment.set('key', 'value');
pm.environment.set('userId', '123');
pm.environment.set('token', 'abc123xyz');
```

### 获取环境变量
```javascript
const value = pm.environment.get('key');
const userId = pm.environment.get('userId');
const token = pm.environment.get('token');
```

### 生成UUID
```javascript
const uuid = pm.uuid();
pm.environment.set('requestId', uuid);
```

### 获取时间戳
```javascript
// 毫秒时间戳
const timestamp = pm.getTimestamp();
pm.environment.set('timestamp', timestamp);

// 秒级时间戳
const unixTime = Math.floor(Date.now() / 1000);
```

### 添加请求头
```javascript
// Pre-request Script
pm.request.headers.add({
    key: 'X-Custom-Header',
    value: 'custom-value'
});

// 更新或插入请求头
pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Bearer ' + pm.environment.get('token')
});
```

### 添加查询参数
```javascript
// Pre-request Script
pm.request.params.add({
    key: 'timestamp',
    value: Date.now().toString()
});

pm.request.params.add({
    key: 'userId',
    value: pm.environment.get('userId')
});
```

---

## 认证相关

### Bearer Token 认证
```javascript
// Pre-request Script
const token = pm.environment.get('authToken');
pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Bearer ' + token
});
```

### Basic 认证
```javascript
// Pre-request Script
const username = pm.environment.get('username');
const password = pm.environment.get('password');
const credentials = CryptoJS.enc.Base64.stringify(
    CryptoJS.enc.Utf8.parse(username + ':' + password)
);

pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Basic ' + credentials
});
```

### API Key 认证
```javascript
// Pre-request Script
const apiKey = pm.environment.get('apiKey');

// 方式1: 请求头
pm.request.headers.upsert({
    key: 'X-API-Key',
    value: apiKey
});

// 方式2: 查询参数
pm.request.params.add({
    key: 'apiKey',
    value: apiKey
});
```

### HMAC 签名
```javascript
// Pre-request Script
const timestamp = Date.now().toString();
const method = pm.request.method;
const path = pm.request.url.getPath();
const secretKey = pm.environment.get('secretKey');

// 生成签名字符串
const signString = method + '\n' + path + '\n' + timestamp;

// 计算 HMAC-SHA256
const signature = CryptoJS.HmacSHA256(signString, secretKey).toString();

pm.request.headers.upsert({
    key: 'X-Timestamp',
    value: timestamp
});

pm.request.headers.upsert({
    key: 'X-Signature',
    value: signature
});
```

### OAuth 2.0 Token 刷新
```javascript
// Pre-request Script
const tokenExpireTime = pm.environment.get('tokenExpireTime');
const currentTime = Date.now();

if (!tokenExpireTime || currentTime > (parseInt(tokenExpireTime) - 300000)) {
    console.warn('⚠ Token 即将过期，请手动刷新');
} else {
    const token = pm.environment.get('authToken');
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}
```

---

## 数据处理

### 解析 JSON 响应
```javascript
// Post-request Script
const jsonData = pm.response.json();
console.log('响应数据:', jsonData);

// 保存数据到环境变量
pm.environment.set('userId', jsonData.data.userId);
pm.environment.set('username', jsonData.data.username);
```

### 提取嵌套数据
```javascript
// Post-request Script
const jsonData = pm.response.json();

// 使用 lodash 安全提取
const userId = _.get(jsonData, 'data.user.id');
const userName = _.get(jsonData, 'data.user.profile.name');
const firstEmail = _.get(jsonData, 'data.user.emails[0]');

pm.environment.set('userId', userId);
console.log('用户名:', userName);
```

### 数组遍历和过滤
```javascript
// Post-request Script
const jsonData = pm.response.json();
const users = jsonData.data.users;

// 过滤活跃用户
const activeUsers = _.filter(users, { status: 'active' });
console.log('活跃用户数:', activeUsers.length);

// 提取所有用户ID
const userIds = _.map(users, 'id');
pm.environment.set('userIds', JSON.stringify(userIds));

// 按角色分组
const groupedByRole = _.groupBy(users, 'role');
console.log('按角色分组:', groupedByRole);
```

### 数据转换
```javascript
// Post-request Script
const jsonData = pm.response.json();

// 转换为指定格式
const transformed = _.map(jsonData.data.items, function(item) {
    return {
        id: item.id,
        name: item.name,
        price: _.round(item.price, 2),
        category: _.upperFirst(item.category)
    };
});

console.log('转换后的数据:', transformed);
```

### JSON 模板填充
```javascript
// Pre-request Script
const template = {
    "userId": "{{userId}}",
    "timestamp": "{{timestamp}}",
    "action": "{{action}}"
};

const data = {
    userId: pm.environment.get('userId'),
    timestamp: Date.now(),
    action: 'getUserInfo'
};

let json = JSON.stringify(template);
_.forEach(data, function(value, key) {
    json = json.replace('{{' + key + '}}', value);
});

console.log('填充后的JSON:', json);
```

---

## 测试断言

### 状态码断言
```javascript
// Post-request Script
pm.test("状态码是 200", function () {
    pm.response.to.have.status(200);
});

pm.test("状态码是 2xx", function () {
    pm.expect(pm.response.code).to.be.below(300);
    pm.expect(pm.response.code).to.be.above(199);
});
```

### 响应时间断言
```javascript
// Post-request Script
pm.test("响应时间小于 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

pm.test("响应时间小于 1 秒", function () {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});
```

### 响应体断言
```javascript
// Post-request Script
pm.test("响应包含成功标识", function () {
    pm.expect(pm.response.text()).to.include('success');
});

pm.test("响应体不为空", function () {
    pm.expect(pm.response.text().length).to.be.above(0);
});
```

### 响应头断言
```javascript
// Post-request Script
pm.test("响应包含 Content-Type", function () {
    pm.response.to.have.header('Content-Type');
});

pm.test("Content-Type 是 JSON", function () {
    const contentType = pm.response.headers.get('Content-Type');
    pm.expect(contentType).to.include('application/json');
});
```

### JSON 结构断言
```javascript
// Post-request Script
pm.test("响应包含必要字段", function () {
    const jsonData = pm.response.json();
    
    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData).to.have.property('message');
    pm.expect(jsonData).to.have.property('data');
    
    pm.expect(jsonData.code).to.equal(200);
    pm.expect(jsonData.data).to.have.property('userId');
});
```

---

## 加密和编码

### MD5 哈希
```javascript
// Pre-request Script
const password = 'myPassword123';
const md5Hash = CryptoJS.MD5(password).toString();

pm.environment.set('passwordHash', md5Hash);
console.log('MD5:', md5Hash);
```

### SHA256 哈希
```javascript
// Pre-request Script
const data = 'sensitive data';
const sha256Hash = CryptoJS.SHA256(data).toString();

pm.environment.set('dataHash', sha256Hash);
console.log('SHA256:', sha256Hash);
```

### HMAC-SHA256 签名
```javascript
// Pre-request Script
const data = 'userId=123&timestamp=' + Date.now();
const secretKey = pm.environment.get('secretKey');
const signature = CryptoJS.HmacSHA256(data, secretKey).toString();

pm.environment.set('signature', signature);
console.log('签名:', signature);
```

### AES 加密/解密
```javascript
// 加密
const message = 'secret message';
const key = 'my-secret-key';
const encrypted = CryptoJS.AES.encrypt(message, key).toString();
pm.environment.set('encrypted', encrypted);

// 解密
const encryptedData = pm.environment.get('encrypted');
const decrypted = CryptoJS.AES.decrypt(encryptedData, key);
const decryptedText = decrypted.toString(CryptoJS.enc.Utf8);
console.log('解密结果:', decryptedText);
```

### Base64 编码/解码
```javascript
// 编码
const text = 'Hello World';
const base64 = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
console.log('Base64:', base64);

// 解码
const decoded = CryptoJS.enc.Base64.parse(base64).toString(CryptoJS.enc.Utf8);
console.log('解码:', decoded);
```

---

## 日期时间

### 格式化当前时间
```javascript
// 各种格式
pm.environment.set('dateYMD', moment().format('YYYY-MM-DD'));
pm.environment.set('dateTime', moment().format('YYYY-MM-DD HH:mm:ss'));
pm.environment.set('isoTime', moment().toISOString());
pm.environment.set('timestamp', moment().valueOf().toString());
pm.environment.set('unixTime', moment().unix().toString());

console.log('当前时间:', moment().format('YYYY-MM-DD HH:mm:ss'));
```

### 日期计算
```javascript
// 加减日期
const tomorrow = moment().add(1, 'days').format('YYYY-MM-DD');
const nextWeek = moment().add(7, 'days').format('YYYY-MM-DD');
const nextMonth = moment().add(1, 'months').format('YYYY-MM-DD');

const yesterday = moment().subtract(1, 'days').format('YYYY-MM-DD');
const lastWeek = moment().subtract(7, 'days').format('YYYY-MM-DD');

pm.environment.set('startDate', lastWeek);
pm.environment.set('endDate', tomorrow);
```

### 日期比较
```javascript
const date1 = moment('2024-01-01');
const date2 = moment('2024-12-31');

console.log('date1 < date2:', date1.isBefore(date2));
console.log('date1 > date2:', date1.isAfter(date2));
console.log('date1 == date2:', date1.isSame(date2));

// 计算天数差
const diffDays = date2.diff(date1, 'days');
console.log('相差天数:', diffDays);
```

### 解析日期字符串
```javascript
// 解析各种格式
const date1 = moment('2024-01-01', 'YYYY-MM-DD');
const date2 = moment('01/15/2024', 'MM/DD/YYYY');
const date3 = moment('2024年1月1日', 'YYYY年M月D日');

// 验证日期
const isValid = moment('2024-13-01', 'YYYY-MM-DD', true).isValid();
console.log('日期有效:', isValid); // false

// 获取时间范围
const startOfDay = moment().startOf('day').valueOf();
const endOfDay = moment().endOf('day').valueOf();

pm.environment.set('startTimestamp', startOfDay.toString());
pm.environment.set('endTimestamp', endOfDay.toString());
```

---

## Cookie 管理

### 获取 Cookie
```javascript
// Post-request Script
const sessionCookie = pm.cookies.get('sessionId');
if (sessionCookie) {
    console.log('Session ID:', sessionCookie.value);
    pm.environment.set('sessionId', sessionCookie.value);
}

// 检查 Cookie 是否存在
if (pm.cookies.has('authToken')) {
    console.log('认证 Cookie 存在');
}

// 获取所有 Cookie
const allCookies = pm.cookies.getAll();
console.log('Cookie 总数:', allCookies.length);
```

### 设置 Cookie
```javascript
// Post-request Script
pm.cookies.set({
    name: 'customToken',
    value: 'abc123',
    domain: 'example.com',
    path: '/',
    httpOnly: true,
    secure: true
});
```

### 跨域 Cookie 管理
```javascript
// Post-request Script
const jar = pm.cookies.jar();

// 设置 Cookie
jar.set('https://api.example.com', 'token=xyz123', function(error, cookie) {
    if (error) {
        console.error('设置失败:', error);
    } else {
        console.log('Cookie 已设置');
    }
});

// 获取 Cookie
jar.get('https://api.example.com', 'token', function(error, cookie) {
    if (!error && cookie) {
        console.log('Token:', cookie.value);
        pm.environment.set('apiToken', cookie.value);
    }
});
```

---

## 外部数据源

### Redis 读写断言
```javascript
pm.redis.execute({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'SET',
    key: 'order:1001',
    value: '{"id":1001,"status":"CREATED"}'
});

const value = pm.redis.query({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'GET',
    key: 'order:1001'
});

pm.test('Redis value exists', function () {
    pm.expect(value).to.include('CREATED');
});
```

### Kafka 发消息断言
```javascript
const resp = pm.kafka.send({
    bootstrapServers: 'localhost:9092',
    topic: 'orders',
    key: 'order-1001',
    value: '{"id":1001,"status":"CREATED"}'
});

pm.test('Kafka send success', function () {
    pm.expect(resp.topic).to.equal('orders');
    pm.expect(resp.offset).to.be.least(0);
});
```

### Elasticsearch 写入断言
```javascript
const resp = pm.es.request({
    baseUrl: 'http://localhost:9200',
    method: 'POST',
    path: '/orders/_doc/order-1001',
    body: JSON.stringify({id: 1001, status: 'CREATED'})
});

pm.test('ES index success', function () {
    pm.expect(resp.code).to.be.within(200, 201);
});
```

### InfluxDB 查询与写入断言
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

pm.test('Influx write success', function () {
    pm.expect(writeResp.code).to.equal(204);
});

const queryResp = pm.influx.query({
    baseUrl: 'http://localhost:8086',
    version: 'v1',
    db: 'metrics',
    username: 'root',
    password: 'root',
    query: 'SELECT * FROM cpu ORDER BY time DESC LIMIT 1'
});

pm.test('Influx query success', function () {
    pm.expect(queryResp.code).to.equal(200);
    pm.expect(queryResp.json).to.exist();
});
```

---

## 实用工具

### 生成随机数据
```javascript
// Pre-request Script

// 随机整数
const randomId = _.random(10000, 99999);
const randomAge = _.random(18, 60);

// 随机选择
const randomStatus = _.sample(['pending', 'approved', 'rejected']);
const randomCity = _.sample(['Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen']);

// 随机UUID
const uuid = pm.uuid();

// 随机字符串
const randomStr = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex);

// 随机邮箱
const randomEmail = 'test_' + Date.now() + '@example.com';

// 随机手机号
const randomPhone = '138' + _.random(10000000, 99999999);

pm.environment.set('testUserId', randomId.toString());
pm.environment.set('testEmail', randomEmail);
pm.environment.set('testPhone', randomPhone);

console.log('生成的随机数据:');
console.log('  ID:', randomId);
console.log('  Email:', randomEmail);
console.log('  Phone:', randomPhone);
```

### 数据验证
```javascript
// Pre-request Script
const userId = pm.environment.get('userId');
const email = pm.environment.get('email');

// 验证必填参数
if (!userId) {
    throw new Error('userId 不能为空');
}

// 验证邮箱格式
const emailPattern = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
if (email && !emailPattern.test(email)) {
    throw new Error('邮箱格式不正确');
}

// 验证数值范围
const age = parseInt(pm.environment.get('age'));
if (age && (age < 0 || age > 150)) {
    throw new Error('年龄必须在 0-150 之间');
}

console.log('✓ 参数验证通过');
```

### 性能监控
```javascript
// Post-request Script
const responseTime = pm.response.responseTime;

// 记录响应时间
console.log('响应时间:', responseTime, 'ms');

// 性能等级
if (responseTime < 100) {
    console.log('性能: 优秀 ⭐⭐⭐⭐⭐');
} else if (responseTime < 300) {
    console.log('性能: 良好 ⭐⭐⭐⭐');
} else if (responseTime < 1000) {
    console.log('性能: 一般 ⭐⭐⭐');
} else {
    console.log('性能: 较慢 ⭐⭐');
}

// 累计统计
let totalTime = parseFloat(pm.environment.get('totalResponseTime') || '0');
let count = parseInt(pm.environment.get('requestCount') || '0');

totalTime += responseTime;
count += 1;

pm.environment.set('totalResponseTime', totalTime.toString());
pm.environment.set('requestCount', count.toString());

console.log('平均响应时间:', (totalTime / count).toFixed(2), 'ms');
console.log('请求总数:', count);
```

### 调试日志
```javascript
// 详细的调试信息
console.log('=== 请求信息 ===');
console.log('URL:', pm.request.url.toString());
console.log('Method:', pm.request.method);
console.log('Headers:', pm.request.headers.all());

// Post-request Script
console.log('=== 响应信息 ===');
console.log('Status:', pm.response.code, pm.response.status);
console.log('Time:', pm.response.responseTime, 'ms');
console.log('Size:', pm.response.size().total, 'bytes');

// 条件日志
const debug = pm.environment.get('debug') === 'true';
if (debug) {
    console.log('Debug 模式: 开启');
    console.log('响应体:', pm.response.text());
}

// 错误日志
if (pm.response.code !== 200) {
    console.error('✗ 请求失败');
    console.error('状态码:', pm.response.code);
    console.error('响应:', pm.response.text());
}
```

---

## 常用组合模式

### 完整的请求准备流程
```javascript
// Pre-request Script - 综合示例

// 1. 生成请求ID
const requestId = pm.uuid();
pm.environment.set('requestId', requestId);

// 2. 生成时间戳
const timestamp = Date.now();
pm.environment.set('timestamp', timestamp.toString());

// 3. 准备认证
const token = pm.environment.get('authToken');
if (token) {
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}

// 4. 添加通用请求头
pm.request.headers.upsert({
    key: 'X-Request-ID',
    value: requestId
});

pm.request.headers.upsert({
    key: 'X-Timestamp',
    value: timestamp.toString()
});

// 5. 添加查询参数
pm.request.params.add({
    key: 'timestamp',
    value: timestamp.toString()
});

// 6. 生成签名
const secretKey = pm.environment.get('secretKey');
const signData = requestId + timestamp + secretKey;
const signature = CryptoJS.HmacSHA256(signData, secretKey).toString();

pm.request.headers.upsert({
    key: 'X-Signature',
    value: signature
});

console.log('✓ 请求准备完成');
console.log('  Request ID:', requestId);
console.log('  Timestamp:', timestamp);
console.log('  Signature:', signature.substring(0, 16) + '...');
```

### 完整的响应验证流程
```javascript
// Post-request Script - 综合示例

// 1. 基础验证
pm.test("请求成功", function () {
    pm.response.to.have.status(200);
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

// 2. 解析响应
const jsonData = pm.response.json();

// 3. 业务验证
pm.test("业务响应正确", function () {
    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData.code).to.equal(200);
    pm.expect(jsonData).to.have.property('data');
});

// 4. 提取数据
if (jsonData.code === 200 && jsonData.data) {
    const userId = _.get(jsonData, 'data.userId');
    const userName = _.get(jsonData, 'data.userName');
    const token = _.get(jsonData, 'data.token');
    
    if (userId) pm.environment.set('userId', userId);
    if (userName) pm.environment.set('userName', userName);
    if (token) pm.environment.set('authToken', token);
    
    console.log('✓ 数据已提取并保存');
}

// 5. 性能记录
const responseTime = pm.response.responseTime;
console.log('响应时间:', responseTime, 'ms');

// 6. 测试结果统计
const results = pm.test.index();
const passCount = _.filter(results, { passed: true }).length;
const failCount = _.filter(results, { passed: false }).length;

console.log('测试通过:', passCount, '/ 失败:', failCount);
```

---

## 快速复制模板

### Pre-request 基础模板
```javascript
// Pre-request Script

// 环境变量
pm.environment.set('timestamp', Date.now().toString());
pm.environment.set('requestId', pm.uuid());

// 认证
const token = pm.environment.get('authToken');
if (token) {
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}

// 通用请求头
pm.request.headers.upsert({
    key: 'X-Request-ID',
    value: pm.environment.get('requestId')
});

console.log('✓ Pre-request 完成');
```

### Post-request 基础模板
```javascript
// Post-request Script

// 状态码检查
pm.test("状态码正确", function () {
    pm.response.to.have.status(200);
});

// 响应时间检查
pm.test("响应时间合理", function () {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});

// 解析响应
const jsonData = pm.response.json();

// 业务验证
pm.test("业务响应正确", function () {
    pm.expect(jsonData.code).to.equal(200);
});

// 提取数据
if (jsonData.data) {
    // 根据需要提取数据
    console.log('响应数据:', jsonData.data);
}

console.log('✓ Post-request 完成');
```

---

## 提示

1. **直接使用全局变量**：`CryptoJS`、`_`（lodash）、`moment` 已预加载，无需 `require()`
2. **也支持 require()**：`var CryptoJS = require('crypto-js')` 也可以使用（兼容 Postman）
3. **善用 console.log**：调试时输出关键信息
4. **环境变量命名**：使用有意义的名称，如 `authToken`、`userId`
5. **错误处理**：关键操作使用 try-catch 包裹
6. **性能优化**：避免在循环中执行重复计算

---

**更多详细信息请参考：** [SCRIPT_API_REFERENCE_zh.md](./SCRIPT_API_REFERENCE_zh.md)
