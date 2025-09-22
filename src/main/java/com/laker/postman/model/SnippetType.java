package com.laker.postman.model;

public enum SnippetType {
    // 前置脚本类别
    SET_REQUEST_VARIABLE(Category.PRE_SCRIPT, "snippet.setRequestVariable.title", "snippet.setRequestVariable.desc", "pm.setVariable('requestId', pm.generateUUID());\nconsole.log('已生成请求ID: ' + pm.getVariable('requestId'));"),
    SET_LOCAL_VARIABLE(Category.PRE_SCRIPT, "snippet.setLocalVariable.title", "snippet.setLocalVariable.desc", "pm.variables.set('tempKey', 'tempValue');\nconsole.log('已设置局部变量: ' + pm.variables.get('tempKey'));"),
    RANDOM_UUID(Category.PRE_SCRIPT, "snippet.randomUUID.title", "snippet.randomUUID.desc", "pm.environment.set('uuid', pm.generateUUID());\nconsole.log('已生成随机UUID: ' + pm.environment.get('uuid'));"),
    DYNAMIC_TIMESTAMP(Category.PRE_SCRIPT, "snippet.dynamicTimestamp.title", "snippet.dynamicTimestamp.desc", "pm.environment.set('timestamp', pm.getTimestamp());\nconsole.log('已生成时间戳: ' + pm.environment.get('timestamp'));"),
    SIGNATURE(Category.PRE_SCRIPT, "snippet.signature.title", "snippet.signature.desc", "// 假设需要生成签名\nvar timestamp = Date.now();\nvar appKey = pm.environment.get('appKey');\nvar appSecret = pm.environment.get('appSecret');\n\n// 构建待签名字符串\nvar stringToSign = 'appKey=' + appKey + '&timestamp=' + timestamp;\n\n// 计算签名 (使用SHA256)\nvar signature = SHA256(stringToSign + appSecret).toString();\n\n// 设置到环境变量\npm.environment.set('timestamp', timestamp);\npm.environment.set('signature', signature);\n\nconsole.log('已生成签名: ' + signature);"),
    DYNAMIC_PARAM(Category.PRE_SCRIPT, "snippet.dynamicParam.title", "snippet.dynamicParam.desc", "// 获取当前时间\nvar now = new Date();\n\n// 格式化为YYYY-MM-DD\nvar date = now.getFullYear() + '-' + \n    ('0' + (now.getMonth() + 1)).slice(-2) + '-' + \n    ('0' + now.getDate()).slice(-2);\n\n// 保存到环境变量\npm.environment.set('currentDate', date);\nconsole.log('当前日期: ' + date);"),
    JWT_PARSE(Category.PRE_SCRIPT, "snippet.jwtParse.title", "snippet.jwtParse.desc", "// 解析JWT Token\nvar token = pm.environment.get('jwt_token');\nif (token) {\n    // 分割Token\n    var parts = token.split('.');\n    if (parts.length === 3) {\n        // 解码payload部分(base64)\n        var payload = JSON.parse(atob(parts[1]));\n        console.log('Token解析结果:', payload);\n        // 可以提取特定字段\n        if (payload.exp) {\n            console.log('Token过期时间:', new Date(payload.exp * 1000));\n        }\n    }\n}"),
    ENCRYPT_REQUEST_DATA(Category.PRE_SCRIPT, "snippet.encryptRequestData.title", "snippet.encryptRequestData.desc", "// 获取请求体\nvar requestData = JSON.parse(request.body || '{}');\n\n// 假设需要加密某个字段\nif (requestData.password) {\n    // 使用MD5加密\n    requestData.password = MD5(requestData.password).toString();\n    console.log('密码已加密');\n    \n    // 更新请求体\n    pm.setVariable('encryptedBody', JSON.stringify(requestData));\n    // 注意：这里只设置了变量，实际请求体不会改变，需要在请求体中使用{{encryptedBody}}\n}"),
    DYNAMIC_HEADER(Category.PRE_SCRIPT, "snippet.dynamicHeader.title", "snippet.dynamicHeader.desc", "// 设置时间戳和签名等动态请求头\nvar timestamp = Date.now();\nvar nonce = Math.random().toString(36).substring(2, 15);\n\n// 设置到环境变量，以便在请求头中使用\npm.environment.set('req_timestamp', timestamp);\npm.environment.set('req_nonce', nonce);\n\nconsole.log('已设置请求时间戳: ' + timestamp);\nconsole.log('已设置请求随机数: ' + nonce);"),
    CONDITIONAL(Category.PRE_SCRIPT, "snippet.conditional.title", "snippet.conditional.desc", "// 根据环境判断使用不同的参数\nvar env = pm.environment.get('environment');\n\nif (env === 'production') {\n    pm.environment.set('base_url', 'https://api.example.com');\n    console.log('已切换到生产环境');\n} else if (env === 'staging') {\n    pm.environment.set('base_url', 'https://staging-api.example.com');\n    console.log('已切换到预发布环境');\n} else {\n    pm.environment.set('base_url', 'https://dev-api.example.com');\n    console.log('已切换到开发环境');\n}"),
    DYNAMIC_MODIFY_HEADER(Category.PRE_SCRIPT, "snippet.dynamicModifyHeader.title", "snippet.dynamicModifyHeader.desc", "// 获取并修改请求头\nif (pm.request && pm.request.headers) {\n    // 添加自定义请求头\n    pm.request.headers.add({\n        key: 'X-Custom-Header',\n        value: 'Custom-Value-' + Date.now()\n    });\n    \n    // 添加认证信息\n    pm.request.headers.add({\n        key: 'Authorization',\n        value: 'Bearer ' + pm.environment.get('token')\n    });\n    \n    console.log('已动态添加请求头');\n}\n"),
    DYNAMIC_MODIFY_BODY(Category.PRE_SCRIPT, "snippet.dynamicModifyBody.title", "snippet.dynamicModifyBody.desc", "// 动态修改 formData、urlencoded、formFiles、params\nif (pm.request.formData) {\n    pm.request.formData.add({ key: 'extraField', value: 'extraValue' });\n    console.log('已添加 formData 字段');\n}\nif (pm.request.urlencoded) {\n    pm.request.urlencoded.add({ key: 'token', value: pm.environment.get('token') });\n    console.log('已添加 urlencoded 字段');\n}\nif (pm.request.formFiles) {\n    pm.request.formFiles.add({ key: 'file', value: '/path/to/file' });\n    console.log('已添加 formFiles 字段');\n}\n\n// 注意：要在实际请求体/参数中使用相关变量或字段才会生效"),
    DYNAMIC_MODIFY_PARAM(Category.PRE_SCRIPT, "snippet.dynamicModifyParam.title", "snippet.dynamicModifyParam.desc", "// 动态添加到 params\n pm.request.params.add({ key: 'timestamp', value: Date.now() });\n "),

    // 断言类别
    ASSERT_STATUS_200(Category.ASSERT, "snippet.assertStatus200.title", "snippet.assertStatus200.desc", "pm.test('Status code is 200', function () {\n    pm.response.to.have.status(200);\n});"),
    ASSERT_BODY_CONTAINS(Category.ASSERT, "snippet.assertBodyContains.title", "snippet.assertBodyContains.desc", "pm.test('Body contains string', function () {\n    pm.expect(pm.response.text()).to.include('success');\n});"),
    ASSERT_JSON_VALUE(Category.ASSERT, "snippet.assertJsonValue.title", "snippet.assertJsonValue.desc", "pm.test('JSON value check', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData.code).to.eql(0);\n});"),
    ASSERT_HEADER_PRESENT(Category.ASSERT, "snippet.assertHeaderPresent.title", "snippet.assertHeaderPresent.desc", "pm.test('Header is present', function () {\n    pm.response.to.have.header('Content-Type');\n});"),
    ASSERT_RESPONSE_TIME(Category.ASSERT, "snippet.assertResponseTime.title", "snippet.assertResponseTime.desc", "pm.test('Response time is less than 1000ms', function () {\n    pm.expect(pm.response.responseTime).to.be.below(1000);\n});"),
    ASSERT_FIELD_EXISTS(Category.ASSERT, "snippet.assertFieldExists.title", "snippet.assertFieldExists.desc", "pm.test('字段存在', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData).to.have.property('data');\n});"),
    ASSERT_ARRAY_LENGTH(Category.ASSERT, "snippet.assertArrayLength.title", "snippet.assertArrayLength.desc", "pm.test('数组长度为3', function () {\n    var arr = pm.response.json().list;\n    pm.expect(arr.length).to.eql(3);\n});"),
    ASSERT_REGEX(Category.ASSERT, "snippet.assertRegex.title", "snippet.assertRegex.desc", "pm.test('Body正则匹配', function () {\n    pm.expect(pm.response.text()).to.match(/success/);\n});"),

    // 提取类别
    EXTRACT_JSON_TO_ENV(Category.EXTRACT, "snippet.extractJsonToEnv.title", "snippet.extractJsonToEnv.desc", "var jsonData = pm.response.json();\npm.environment.set('token', jsonData.token);"),
    EXTRACT_HEADER_TO_ENV(Category.EXTRACT, "snippet.extractHeaderToEnv.title", "snippet.extractHeaderToEnv.desc", "var token = pm.response.headers.get('X-Token');\npm.environment.set('token', token);"),
    EXTRACT_REGEX_TO_ENV(Category.EXTRACT, "snippet.extractRegexToEnv.title", "snippet.extractRegexToEnv.desc", "var match = pm.response.text().match(/token=(\\w+)/);\nif (match) {\n    pm.environment.set('token', match[1]);\n}"),

    // 局部变量管理
    LOCAL_SET(Category.LOCAL_VAR, "snippet.localSet.title", "snippet.localSet.desc", "pm.variables.set('key', 'value');\nconsole.log('已设置局部变量: ' + pm.variables.get('key'));"),
    LOCAL_GET(Category.LOCAL_VAR, "snippet.localGet.title", "snippet.localGet.desc", "var value = pm.variables.get('key');\nconsole.log('局部变量值: ' + value);"),
    LOCAL_HAS(Category.LOCAL_VAR, "snippet.localHas.title", "snippet.localHas.desc", "if (pm.variables.has('key')) {\n    console.log('局部变量存在');\n} else {\n    console.log('局部变量不存在');\n}"),
    LOCAL_UNSET(Category.LOCAL_VAR, "snippet.localUnset.title", "snippet.localUnset.desc", "pm.variables.unset('key');\nconsole.log('已删除局部变量: key');"),
    LOCAL_CLEAR(Category.LOCAL_VAR, "snippet.localClear.title", "snippet.localClear.desc", "pm.variables.clear();\nconsole.log('已清空所有局部变量');"),
    LOCAL_BATCH_SET(Category.LOCAL_VAR, "snippet.localBatchSet.title", "snippet.localBatchSet.desc", "var data = {\n    'userId': '12345',\n    'sessionId': pm.generateUUID(),\n    'timestamp': Date.now()\n};\n\nObject.keys(data).forEach(function(key) {\n    pm.variables.set(key, data[key]);\n});\n\nconsole.log('已批量设置局部变量');"),
    LOCAL_FOREACH(Category.LOCAL_VAR, "snippet.localForeach.title", "snippet.localForeach.desc", "// 注意：pm.variables 本身不提供遍历方法\n// 这里演示通过已知的变量名列表进行遍历\nvar knownKeys = ['userId', 'sessionId', 'timestamp'];\n\nknownKeys.forEach(function(key) {\n    if (pm.variables.has(key)) {\n        console.log(key + ': ' + pm.variables.get(key));\n    }\n});"),
    LOCAL_CONDITIONAL_SET(Category.LOCAL_VAR, "snippet.localConditionalSet.title", "snippet.localConditionalSet.desc", "// 根据条件设置局部变量\nvar userRole = pm.environment.get('userRole');\n\nif (userRole === 'admin') {\n    pm.variables.set('permissions', 'all');\n} else if (userRole === 'user') {\n    pm.variables.set('permissions', 'read');\n} else {\n    pm.variables.set('permissions', 'none');\n}\n\nconsole.log('已根据用户角色设置权限: ' + pm.variables.get('permissions'));"),
    LOCAL_DEFAULT(Category.LOCAL_VAR, "snippet.localDefault.title", "snippet.localDefault.desc", "// 获取局部变量，如果不存在则使用默认值\nvar userId = pm.variables.get('userId') || 'defaultUser';\nvar timeout = pm.variables.get('timeout') || '5000';\n\nconsole.log('用户ID: ' + userId);\nconsole.log('超时时间: ' + timeout);"),

    // 环境变量管理
    ENV_SET(Category.ENV_VAR, "snippet.envSet.title", "snippet.envSet.desc", "pm.environment.set('key', 'value');"),
    ENV_GET(Category.ENV_VAR, "snippet.envGet.title", "snippet.envGet.desc", "pm.environment.get('key');"),
    ENV_UNSET(Category.ENV_VAR, "snippet.envUnset.title", "snippet.envUnset.desc", "pm.environment.unset('key');"),
    ENV_CLEAR(Category.ENV_VAR, "snippet.envClear.title", "snippet.envClear.desc", "pm.environment.clear();"),

    // 其他操作
    FOREACH_ARRAY(Category.OTHER, "snippet.foreachArray.title", "snippet.foreachArray.desc", "var arr = pm.response.json().list;\narr.forEach(function(item) {\n    // 处理每个item\n});"),
    IF_ELSE(Category.OTHER, "snippet.ifElse.title", "snippet.ifElse.desc", "if (pm.response.code === 200) {\n    // 成功逻辑\n} else {\n    // 失败逻辑\n}"),
    PRINT_LOG(Category.OTHER, "snippet.printLog.title", "snippet.printLog.desc", "console.log('日志内容');"),

    // 基本加密解密功能
    BASE64_ENCODE(Category.ENCODE, "snippet.base64Encode.title", "snippet.base64Encode.desc", "var encoded = btoa('Hello World');\nconsole.log(encoded); // SGVsbG8gV29ybGQ="),
    BASE64_DECODE(Category.ENCODE, "snippet.base64Decode.title", "snippet.base64Decode.desc", "var decoded = atob('SGVsbG8gV29ybGQ=');\nconsole.log(decoded); // Hello World"),
    URL_ENCODE(Category.ENCODE, "snippet.urlEncode.title", "snippet.urlEncode.desc", "var encoded = encodeURIComponent('Hello World!');\nconsole.log(encoded); // Hello%20World%21"),
    URL_DECODE(Category.ENCODE, "snippet.urlDecode.title", "snippet.urlDecode.desc", "var decoded = decodeURIComponent('Hello%20World%21');\nconsole.log(decoded); // Hello World!"),

    // 常用字符串操作
    STR_SUBSTRING(Category.STRING, "snippet.strSubstring.title", "snippet.strSubstring.desc", "var str = 'Hello World';\nvar sub = str.substring(0, 5);\nconsole.log(sub); // Hello"),
    STR_REPLACE(Category.STRING, "snippet.strReplace.title", "snippet.strReplace.desc", "var str = 'Hello World';\nvar newStr = str.replace('World', 'JavaScript');\nconsole.log(newStr); // Hello JavaScript"),
    STR_SPLIT(Category.STRING, "snippet.strSplit.title", "snippet.strSplit.desc", "var str = 'a,b,c,d';\nvar arr = str.split(',');\nconsole.log(arr); // ['a', 'b', 'c', 'd']"),

    // 日期时间处理
    GET_TIMESTAMP(Category.DATE, "snippet.getTimestamp.title", "snippet.getTimestamp.desc", "var timestamp = Date.now();\nconsole.log(timestamp); // 毫秒时间戳"),
    FORMAT_DATE(Category.DATE, "snippet.formatDate.title", "snippet.formatDate.desc", "var date = new Date();\nvar formatted = date.toISOString();\nconsole.log(formatted); // 如: 2023-01-01T12:00:00.000Z"),

    // JSON处理
    JSON_TO_OBJ(Category.JSON, "snippet.jsonToObj.title", "snippet.jsonToObj.desc", "var jsonString = '{\"name\":\"test\",\"value\":123}';\nvar obj = JSON.parse(jsonString);\nconsole.log(obj.name); // test"),
    OBJ_TO_JSON(Category.JSON, "snippet.objToJson.title", "snippet.objToJson.desc", "var obj = {name: 'test', value: 123};\nvar jsonString = JSON.stringify(obj);\nconsole.log(jsonString); // {\"name\":\"test\",\"value\":123}"),

    // 数组操作
    ARRAY_FILTER(Category.ARRAY, "snippet.arrayFilter.title", "snippet.arrayFilter.desc", "var arr = [1, 2, 3, 4, 5];\nvar filtered = arr.filter(function(item) {\n    return item > 3;\n});\nconsole.log(filtered); // [4, 5]"),
    ARRAY_MAP(Category.ARRAY, "snippet.arrayMap.title", "snippet.arrayMap.desc", "var arr = [1, 2, 3];\nvar mapped = arr.map(function(item) {\n    return item * 2;\n});\nconsole.log(mapped); // [2, 4, 6]"),

    // 正则表达式
    REGEX_EXTRACT(Category.REGEX, "snippet.regexExtract.title", "snippet.regexExtract.desc", "var str = 'My email is test@example.com';\nvar regex = /[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,4}/;\nvar email = str.match(regex)[0];\nconsole.log(email); // test@example.com"),

    // 计算与加密
    MD5(Category.ENCRYPT, "snippet.md5.title", "snippet.md5.desc", "var hash = MD5('Message').toString();\nconsole.log(hash);"),
    SHA256(Category.ENCRYPT, "snippet.sha256.title", "snippet.sha256.desc", "var hash = SHA256('Message').toString();\nconsole.log(hash);");

    public final Category type;
    public final String titleKey;
    public final String descKey;
    public final String code;

    SnippetType(Category type, String titleKey, String descKey, String code) {
        this.type = type;
        this.titleKey = titleKey;
        this.descKey = descKey;
        this.code = code;
    }
}
