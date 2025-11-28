package com.laker.postman.model;

public enum SnippetType {
    // Pre-script category
    SET_REQUEST_VARIABLE(Category.PRE_SCRIPT, "snippet.setRequestVariable.title", "snippet.setRequestVariable.desc", "pm.setVariable('requestId', pm.generateUUID());\nconsole.log('Generated request ID: ' + pm.getVariable('requestId'));"),
    SET_LOCAL_VARIABLE(Category.PRE_SCRIPT, "snippet.setLocalVariable.title", "snippet.setLocalVariable.desc", "pm.variables.set('tempKey', 'tempValue');\nconsole.log('Set local variable: ' + pm.variables.get('tempKey'));"),
    RANDOM_UUID(Category.PRE_SCRIPT, "snippet.randomUUID.title", "snippet.randomUUID.desc", "pm.environment.set('uuid', pm.generateUUID());\nconsole.log('Generated random UUID: ' + pm.environment.get('uuid'));"),
    DYNAMIC_TIMESTAMP(Category.PRE_SCRIPT, "snippet.dynamicTimestamp.title", "snippet.dynamicTimestamp.desc", "pm.environment.set('timestamp', pm.getTimestamp());\nconsole.log('Generated timestamp: ' + pm.environment.get('timestamp'));"),
    SIGNATURE(Category.PRE_SCRIPT, "snippet.signature.title", "snippet.signature.desc", "// Assume signature needs to be generated\nvar timestamp = Date.now();\nvar appKey = pm.environment.get('appKey');\nvar appSecret = pm.environment.get('appSecret');\n\n// Build string to sign\nvar stringToSign = 'appKey=' + appKey + '&timestamp=' + timestamp;\n\n// Calculate signature (using SHA256)\nvar signature = SHA256(stringToSign + appSecret).toString();\n\n// Set to environment variable\npm.environment.set('timestamp', timestamp);\npm.environment.set('signature', signature);\n\nconsole.log('Generated signature: ' + signature);"),
    DYNAMIC_PARAM(Category.PRE_SCRIPT, "snippet.dynamicParam.title", "snippet.dynamicParam.desc", "// Get current date\nvar now = new Date();\n\n// Format as YYYY-MM-DD\nvar date = now.getFullYear() + '-' + \n    ('0' + (now.getMonth() + 1)).slice(-2) + '-' + \n    ('0' + now.getDate()).slice(-2);\n\n// Save to environment variable\npm.environment.set('currentDate', date);\nconsole.log('Current date: ' + date);"),
    JWT_PARSE(Category.PRE_SCRIPT, "snippet.jwtParse.title", "snippet.jwtParse.desc", "// Parse JWT Token\nvar token = pm.environment.get('jwt_token');\nif (token) {\n    // Split Token\n    var parts = token.split('.');\n    if (parts.length === 3) {\n        // Decode payload part (base64)\n        var payload = JSON.parse(atob(parts[1]));\n        console.log('Token parse result:', payload);\n        // Extract specific field\n        if (payload.exp) {\n            console.log('Token expiration:', new Date(payload.exp * 1000));\n        }\n    }\n}"),
    ENCRYPT_REQUEST_DATA(Category.PRE_SCRIPT, "snippet.encryptRequestData.title", "snippet.encryptRequestData.desc", "// Get request body\nvar requestData = JSON.parse(request.body || '{}');\n\n// Assume a field needs to be encrypted\nif (requestData.password) {\n    // Encrypt with MD5\n    requestData.password = MD5(requestData.password).toString();\n    console.log('Password encrypted');\n    \n    // Update request body\n    pm.setVariable('encryptedBody', JSON.stringify(requestData));\n    // Note: Only variable is set, actual request body won't change, use {{encryptedBody}} in request body\n}"),
    DYNAMIC_HEADER(Category.PRE_SCRIPT, "snippet.dynamicHeader.title", "snippet.dynamicHeader.desc", "// Set dynamic request headers like timestamp and signature\nvar timestamp = Date.now();\nvar nonce = Math.random().toString(36).substring(2, 15);\n\n// Set to environment variable for use in headers\npm.environment.set('req_timestamp', timestamp);\npm.environment.set('req_nonce', nonce);\n\nconsole.log('Set request timestamp: ' + timestamp);\nconsole.log('Set request nonce: ' + nonce);"),
    CONDITIONAL(Category.PRE_SCRIPT, "snippet.conditional.title", "snippet.conditional.desc", "// Use different parameters based on environment\nvar env = pm.environment.get('environment');\n\nif (env === 'production') {\n    pm.environment.set('base_url', 'https://api.example.com');\n    console.log('Switched to production environment');\n} else if (env === 'staging') {\n    pm.environment.set('base_url', 'https://staging-api.example.com');\n    console.log('Switched to staging environment');\n} else {\n    pm.environment.set('base_url', 'https://dev-api.example.com');\n    console.log('Switched to development environment');\n}"),
    DYNAMIC_MODIFY_HEADER(Category.PRE_SCRIPT, "snippet.dynamicModifyHeader.title", "snippet.dynamicModifyHeader.desc", "// Get and modify request headers\nif (pm.request && pm.request.headers) {\n    // Add custom request header\n    pm.request.headers.add({\n        key: 'X-Custom-Header',\n        value: 'Custom-Value-' + Date.now()\n    });\n    \n    // Add authentication info\n    pm.request.headers.add({\n        key: 'Authorization',\n        value: 'Bearer ' + pm.environment.get('token')\n    });\n    \n    console.log('Dynamically added request headers');\n}\n"),
    DYNAMIC_MODIFY_BODY(Category.PRE_SCRIPT, "snippet.dynamicModifyBody.title", "snippet.dynamicModifyBody.desc", "// Dynamically modify formData, urlencoded, formFiles, params\nif (pm.request.formData) {\n    pm.request.formData.add({ key: 'extraField', value: 'extraValue' });\n    console.log('Added formData field');\n}\nif (pm.request.urlencoded) {\n    pm.request.urlencoded.add({ key: 'token', value: pm.environment.get('token') });\n    console.log('Added urlencoded field');\n}\nif (pm.request.formFiles) {\n    pm.request.formFiles.add({ key: 'file', value: '/path/to/file' });\n    console.log('Added formFiles field');\n}\n\n// Note: You need to use related variables or fields in the actual request body/params for it to take effect"),
    DYNAMIC_MODIFY_PARAM(Category.PRE_SCRIPT, "snippet.dynamicModifyParam.title", "snippet.dynamicModifyParam.desc", "// Dynamically add to params\n pm.request.params.add({ key: 'timestamp', value: Date.now() });\n "),

    // Assertion category
    ASSERT_STATUS_200(Category.ASSERT, "snippet.assertStatus200.title", "snippet.assertStatus200.desc", "pm.test('Status code is 200', function () {\n    pm.response.to.have.status(200);\n});"),
    ASSERT_BODY_CONTAINS(Category.ASSERT, "snippet.assertBodyContains.title", "snippet.assertBodyContains.desc", "pm.test('Body contains string', function () {\n    pm.expect(pm.response.text()).to.include('success');\n});"),
    ASSERT_JSON_VALUE(Category.ASSERT, "snippet.assertJsonValue.title", "snippet.assertJsonValue.desc", "pm.test('JSON value check', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData.code).to.eql(0);\n});"),
    ASSERT_HEADER_PRESENT(Category.ASSERT, "snippet.assertHeaderPresent.title", "snippet.assertHeaderPresent.desc", "pm.test('Header is present', function () {\n    pm.response.to.have.header('Content-Type');\n});"),
    ASSERT_RESPONSE_TIME(Category.ASSERT, "snippet.assertResponseTime.title", "snippet.assertResponseTime.desc", "pm.test('Response time is less than 1000ms', function () {\n    pm.expect(pm.response.responseTime).to.be.below(1000);\n});"),
    ASSERT_FIELD_EXISTS(Category.ASSERT, "snippet.assertFieldExists.title", "snippet.assertFieldExists.desc", "pm.test('Field exists', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData).to.have.property('data');\n});"),
    ASSERT_ARRAY_LENGTH(Category.ASSERT, "snippet.assertArrayLength.title", "snippet.assertArrayLength.desc", "pm.test('Array length is 3', function () {\n    var arr = pm.response.json().list;\n    pm.expect(arr.length).to.eql(3);\n});"),
    ASSERT_REGEX(Category.ASSERT, "snippet.assertRegex.title", "snippet.assertRegex.desc", "pm.test('Body regex match', function () {\n    pm.expect(pm.response.text()).to.match(/success/);\n});"),

    // Extraction category
    EXTRACT_JSON_TO_ENV(Category.EXTRACT, "snippet.extractJsonToEnv.title", "snippet.extractJsonToEnv.desc", "var jsonData = pm.response.json();\npm.environment.set('token', jsonData.token);"),
    EXTRACT_HEADER_TO_ENV(Category.EXTRACT, "snippet.extractHeaderToEnv.title", "snippet.extractHeaderToEnv.desc", "var token = pm.response.headers.get('X-Token');\npm.environment.set('token', token);"),
    EXTRACT_REGEX_TO_ENV(Category.EXTRACT, "snippet.extractRegexToEnv.title", "snippet.extractRegexToEnv.desc", "var match = pm.response.text().match(/token=(\\w+)/);\nif (match) {\n    pm.environment.set('token', match[1]);\n}"),

    // Local variable management
    LOCAL_SET(Category.LOCAL_VAR, "snippet.localSet.title", "snippet.localSet.desc", "pm.variables.set('key', 'value');\nconsole.log('Set local variable: ' + pm.variables.get('key'));"),
    LOCAL_GET(Category.LOCAL_VAR, "snippet.localGet.title", "snippet.localGet.desc", "var value = pm.variables.get('key');\nconsole.log('Local variable value: ' + value);"),
    LOCAL_HAS(Category.LOCAL_VAR, "snippet.localHas.title", "snippet.localHas.desc", "if (pm.variables.has('key')) {\n    console.log('Local variable exists');\n} else {\n    console.log('Local variable does not exist');\n}"),
    LOCAL_UNSET(Category.LOCAL_VAR, "snippet.localUnset.title", "snippet.localUnset.desc", "pm.variables.unset('key');\nconsole.log('Deleted local variable: key');"),
    LOCAL_CLEAR(Category.LOCAL_VAR, "snippet.localClear.title", "snippet.localClear.desc", "pm.variables.clear();\nconsole.log('Cleared all local variables');"),
    LOCAL_BATCH_SET(Category.LOCAL_VAR, "snippet.localBatchSet.title", "snippet.localBatchSet.desc", "var data = {\n    'userId': '12345',\n    'sessionId': pm.generateUUID(),\n    'timestamp': Date.now()\n};\n\nObject.keys(data).forEach(function(key) {\n    pm.variables.set(key, data[key]);\n});\n\nconsole.log('Batch set local variables');"),
    LOCAL_FOREACH(Category.LOCAL_VAR, "snippet.localForeach.title", "snippet.localForeach.desc", "// Note: pm.variables itself does not provide traversal method\n// Here demonstrates traversal with a known variable name list\nvar knownKeys = ['userId', 'sessionId', 'timestamp'];\n\nknownKeys.forEach(function(key) {\n    if (pm.variables.has(key)) {\n        console.log(key + ': ' + pm.variables.get(key));\n    }\n});"),
    LOCAL_CONDITIONAL_SET(Category.LOCAL_VAR, "snippet.localConditionalSet.title", "snippet.localConditionalSet.desc", "// Set local variable based on condition\nvar userRole = pm.environment.get('userRole');\n\nif (userRole === 'admin') {\n    pm.variables.set('permissions', 'all');\n} else if (userRole === 'user') {\n    pm.variables.set('permissions', 'read');\n} else {\n    pm.variables.set('permissions', 'none');\n}\n\nconsole.log('Set permissions based on user role: ' + pm.variables.get('permissions'));"),
    LOCAL_DEFAULT(Category.LOCAL_VAR, "snippet.localDefault.title", "snippet.localDefault.desc", "// Get local variable, use default if not exists\nvar userId = pm.variables.get('userId') || 'defaultUser';\nvar timeout = pm.variables.get('timeout') || '5000';\n\nconsole.log('User ID: ' + userId);\nconsole.log('Timeout: ' + timeout);"),

    // Environment variable management
    ENV_SET(Category.ENV_VAR, "snippet.envSet.title", "snippet.envSet.desc", "pm.environment.set('key', 'value');"),
    ENV_GET(Category.ENV_VAR, "snippet.envGet.title", "snippet.envGet.desc", "pm.environment.get('key');"),
    ENV_UNSET(Category.ENV_VAR, "snippet.envUnset.title", "snippet.envUnset.desc", "pm.environment.unset('key');"),
    ENV_CLEAR(Category.ENV_VAR, "snippet.envClear.title", "snippet.envClear.desc", "pm.environment.clear();"),

    // Other operations
    FOREACH_ARRAY(Category.OTHER, "snippet.foreachArray.title", "snippet.foreachArray.desc", "var arr = pm.response.json().list;\narr.forEach(function(item) {\n    // Process each item\n});"),
    IF_ELSE(Category.OTHER, "snippet.ifElse.title", "snippet.ifElse.desc", "if (pm.response.code === 200) {\n    // Success logic\n} else {\n    // Failure logic\n}"),
    PRINT_LOG(Category.OTHER, "snippet.printLog.title", "snippet.printLog.desc", "console.log('Log content');"),

    // Basic encode/decode functions
    BASE64_ENCODE(Category.ENCODE, "snippet.base64Encode.title", "snippet.base64Encode.desc", "var encoded = btoa('Hello World');\nconsole.log(encoded); // SGVsbG8gV29ybGQ="),
    BASE64_DECODE(Category.ENCODE, "snippet.base64Decode.title", "snippet.base64Decode.desc", "var decoded = atob('SGVsbG8gV29ybGQ=');\nconsole.log(decoded); // Hello World"),
    URL_ENCODE(Category.ENCODE, "snippet.urlEncode.title", "snippet.urlEncode.desc", "var encoded = encodeURIComponent('Hello World!');\nconsole.log(encoded); // Hello%20World%21"),
    URL_DECODE(Category.ENCODE, "snippet.urlDecode.title", "snippet.urlDecode.desc", "var decoded = decodeURIComponent('Hello%20World%21');\nconsole.log(decoded); // Hello World!"),

    // Common string operations
    STR_SUBSTRING(Category.STRING, "snippet.strSubstring.title", "snippet.strSubstring.desc", "var str = 'Hello World';\nvar sub = str.substring(0, 5);\nconsole.log(sub); // Hello"),
    STR_REPLACE(Category.STRING, "snippet.strReplace.title", "snippet.strReplace.desc", "var str = 'Hello World';\nvar newStr = str.replace('World', 'JavaScript');\nconsole.log(newStr); // Hello JavaScript"),
    STR_SPLIT(Category.STRING, "snippet.strSplit.title", "snippet.strSplit.desc", "var str = 'a,b,c,d';\nvar arr = str.split(',');\nconsole.log(arr); // ['a', 'b', 'c', 'd']"),

    // Date/time handling
    GET_TIMESTAMP(Category.DATE, "snippet.getTimestamp.title", "snippet.getTimestamp.desc", "var timestamp = Date.now();\nconsole.log(timestamp); // Millisecond timestamp"),
    FORMAT_DATE(Category.DATE, "snippet.formatDate.title", "snippet.formatDate.desc", "var date = new Date();\nvar formatted = date.toISOString();\nconsole.log(formatted); // e.g.: 2023-01-01T12:00:00.000Z"),

    // JSON handling
    JSON_TO_OBJ(Category.JSON, "snippet.jsonToObj.title", "snippet.jsonToObj.desc", "var jsonString = '{\"name\":\"test\",\"value\":123}';\nvar obj = JSON.parse(jsonString);\nconsole.log(obj.name); // test"),
    OBJ_TO_JSON(Category.JSON, "snippet.objToJson.title", "snippet.objToJson.desc", "var obj = {name: 'test', value: 123};\nvar jsonString = JSON.stringify(obj);\nconsole.log(jsonString); // {\"name\":\"test\",\"value\":123}"),

    // Array operations
    ARRAY_FILTER(Category.ARRAY, "snippet.arrayFilter.title", "snippet.arrayFilter.desc", "var arr = [1, 2, 3, 4, 5];\nvar filtered = arr.filter(function(item) {\n    return item > 3;\n});\nconsole.log(filtered); // [4, 5]"),
    ARRAY_MAP(Category.ARRAY, "snippet.arrayMap.title", "snippet.arrayMap.desc", "var arr = [1, 2, 3];\nvar mapped = arr.map(function(item) {\n    return item * 2;\n});\nconsole.log(mapped); // [2, 4, 6]"),

    // Regular expressions
    REGEX_EXTRACT(Category.REGEX, "snippet.regexExtract.title", "snippet.regexExtract.desc", "var str = 'My email is test@example.com';\nvar regex = /[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,4}/;\nvar email = str.match(regex)[0];\nconsole.log(email); // test@example.com"),

    // Calculation and encryption
    MD5(Category.ENCRYPT, "snippet.md5.title", "snippet.md5.desc", "var hash = MD5('Message').toString();\nconsole.log(hash);"),
    SHA256(Category.ENCRYPT, "snippet.sha256.title", "snippet.sha256.desc", "var hash = SHA256('Message').toString();\nconsole.log(hash);"),

    // Cookie operations
    COOKIE_GET(Category.COOKIES, "snippet.cookieGet.title", "snippet.cookieGet.desc", "// Get cookie from response\nvar cookie = pm.cookies.get('JSESSIONID');\nif (cookie) {\n    console.log('Cookie name:', cookie.name);\n    console.log('Cookie value:', cookie.value);\n    console.log('Cookie domain:', cookie.domain);\n    console.log('Cookie path:', cookie.path);\n    // Save to environment\n    pm.environment.set('session_id', cookie.value);\n}"),
    COOKIE_GET_VALUE(Category.COOKIES, "snippet.cookieGetValue.title", "snippet.cookieGetValue.desc", "// Get cookie value directly (Postman style)\nvar sessionId = pm.cookies.get('JSESSIONID');\nif (sessionId) {\n    pm.environment.set('session_id', sessionId.value);\n    console.log('Session ID:', sessionId.value);\n} else {\n    console.log('Cookie not found');\n}"),
    COOKIE_HAS(Category.COOKIES, "snippet.cookieHas.title", "snippet.cookieHas.desc", "// Check if cookie exists\nif (pm.cookies.has('JSESSIONID')) {\n    console.log('Session cookie exists');\n    var cookie = pm.cookies.get('JSESSIONID');\n    pm.environment.set('session_id', cookie.value);\n} else {\n    console.log('Session cookie not found');\n}"),
    COOKIE_ALL(Category.COOKIES, "snippet.cookieAll.title", "snippet.cookieAll.desc", "// Get all response cookies\nvar allCookies = pm.cookies.all();\nconsole.log('Total cookies:', allCookies.length);\n\nallCookies.forEach(function(cookie) {\n    console.log('Cookie:', cookie.name + '=' + cookie.value);\n});"),
    COOKIE_POSTMAN_STYLE(Category.COOKIES, "snippet.cookiePostmanStyle.title", "snippet.cookiePostmanStyle.desc", "// Postman compatible syntax\nvar jsessionId = pm.getResponseCookie('JSESSIONID');\nif (jsessionId) {\n    pm.environment.set('session_id', jsessionId.value);\n    console.log('JSESSIONID:', jsessionId.value);\n}"),

    // Cookie Jar 操作 (设置 Cookie)
    COOKIE_JAR_SET(Category.COOKIES, "snippet.cookieJarSet.title", "snippet.cookieJarSet.desc", "// Set cookie using Cookie Jar\nvar jar = pm.cookies.jar();\n\n// Simple mode: set(url, name, value)\njar.set(pm.request.url, 'session_id', 'ABC123');\nconsole.log('Cookie set: session_id=ABC123');\n\n// Or with full options\njar.set(pm.request.url, {\n    name: 'auth_token',\n    value: pm.environment.get('token'),\n    domain: 'example.com',\n    path: '/',\n    secure: true,\n    httpOnly: true\n});\nconsole.log('Cookie set with options');"),
    COOKIE_JAR_GET(Category.COOKIES, "snippet.cookieJarGet.title", "snippet.cookieJarGet.desc", "// Get cookie using Cookie Jar\nvar jar = pm.cookies.jar();\n\njar.get(pm.request.url, 'session_id', function(error, cookie) {\n    if (error) {\n        console.error('Error getting cookie:', error);\n        return;\n    }\n    \n    if (cookie) {\n        console.log('Cookie found:', cookie.name + '=' + cookie.value);\n        pm.environment.set('session_id', cookie.value);\n    } else {\n        console.log('Cookie not found');\n    }\n});"),
    COOKIE_JAR_UNSET(Category.COOKIES, "snippet.cookieJarUnset.title", "snippet.cookieJarUnset.desc", "// Delete cookie using Cookie Jar\nvar jar = pm.cookies.jar();\n\njar.unset(pm.request.url, 'session_id', function(error) {\n    if (error) {\n        console.error('Error deleting cookie:', error);\n    } else {\n        console.log('Cookie deleted: session_id');\n    }\n});\n\n// Or without callback\njar.unset(pm.request.url, 'old_cookie');"),
    COOKIE_JAR_CLEAR(Category.COOKIES, "snippet.cookieJarClear.title", "snippet.cookieJarClear.desc", "// Clear all cookies for current URL\nvar jar = pm.cookies.jar();\n\njar.clear(pm.request.url, function(error) {\n    if (error) {\n        console.error('Error clearing cookies:', error);\n    } else {\n        console.log('All cookies cleared for this domain');\n    }\n});"),
    COOKIE_JAR_GET_ALL(Category.COOKIES, "snippet.cookieJarGetAll.title", "snippet.cookieJarGetAll.desc", "// Get all cookies for current URL\nvar jar = pm.cookies.jar();\n\njar.getAll(pm.request.url, function(error, cookies) {\n    if (error) {\n        console.error('Error getting cookies:', error);\n        return;\n    }\n    \n    console.log('Total cookies:', cookies.length);\n    cookies.forEach(function(cookie) {\n        console.log('- ' + cookie.name + '=' + cookie.value);\n    });\n});"),
    COOKIE_SET_BEFORE_REQUEST(Category.COOKIES, "snippet.cookieSetBeforeRequest.title", "snippet.cookieSetBeforeRequest.desc", "// Set cookie before sending request\nvar jar = pm.cookies.jar();\nvar baseUrl = pm.request.url;\n\n// Set session cookie\nvar sessionId = pm.environment.get('session_id') || 'default_session';\njar.set(baseUrl, 'JSESSIONID', sessionId);\nconsole.log('Set session cookie:', sessionId);\n\n// Set authentication token\nvar token = pm.environment.get('auth_token');\nif (token) {\n    jar.set(baseUrl, 'auth_token', token);\n    console.log('Set auth token cookie');\n}");

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
