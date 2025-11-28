# 脚本执行系统使用指南

## 📖 目录

1. [快速开始](#快速开始)
2. [核心概念](#核心概念)
3. [API 参考](#api-参考)
4. [使用示例](#使用示例)
5. [迁移指南](#迁移指南)
6. [常见问题](#常见问题)

---

## 🚀 快速开始

### 最简单的使用方式

```java
// 1. 创建脚本执行流水线
ScriptExecutionPipeline pipeline = ScriptExecutionService.createPipeline(
    preparedRequest,
    item.getPrescript(),
    item.getPostscript()
);

// 2. 执行前置脚本
ScriptExecutionResult preResult = pipeline.executePreScript();
if (!preResult.isSuccess()) {
    handleFailure(preResult);
    return;
}

// 3. 执行 HTTP 请求
HttpResponse response = HttpSingleRequestExecutor.executeHttp(preparedRequest);

// 4. 执行后置脚本
ScriptExecutionResult postResult = pipeline.executePostScript(response);

// 5. 处理测试结果
if (postResult.hasTestResults()) {
    processTestResults(postResult.getTestResults());
}
```

---

## 🎓 核心概念

### 1. ScriptExecutionPipeline（脚本执行流水线）

流水线是脚本执行的**中心协调者**，负责：

- ✅ 管理脚本执行的完整生命周期
- ✅ 自动准备和管理变量绑定
- ✅ 自动清空和收集测试结果
- ✅ 提供统一的错误处理

**核心特性**：
- **状态管理**：自动维护 bindings 生命周期
- **自动化**：减少手动操作，避免遗漏步骤
- **类型安全**：返回结构化的结果对象

### 2. ScriptExecutionResult（脚本执行结果）

结果对象包含脚本执行的**所有信息**：

```java
public class ScriptExecutionResult {
    boolean success;              // 是否成功
    List<TestResult> testResults; // 测试结果（后置脚本）
    String errorMessage;          // 错误信息
    Exception exception;          // 异常对象
}
```

**优势**：
- 统一的返回类型
- 包含完整的执行信息
- 支持链式调用

### 3. ScriptExecutionService（脚本执行服务）

服务层提供：

- ✅ 便捷的工厂方法创建 Pipeline
- ✅ 底层的脚本执行能力
- ✅ 变量绑定的准备和管理

---

## 📚 API 参考

### ScriptExecutionService

#### 创建流水线

```java
// 基础创建
ScriptExecutionPipeline createPipeline(
    PreparedRequest request,
    String preScript,
    String postScript
)

// 带自定义输出回调
ScriptExecutionPipeline createPipeline(
    PreparedRequest request,
    String preScript,
    String postScript,
    JsScriptExecutor.OutputCallback outputCallback
)
```

#### 准备变量绑定

```java
// 准备前置脚本的绑定（包含空响应对象）
Map<String, Object> preparePreRequestBindings(PreparedRequest req)

// 添加响应相关的绑定
void addResponseBindings(Map<String, Object> bindings, HttpResponse response)
```

#### 执行脚本（低级 API）

```java
// 执行前置脚本
boolean executePreScript(String prescript, Map<String, Object> bindings)

// 执行后置脚本
void executePostScript(String postscript, Map<String, Object> bindings)

// 执行后置脚本（带响应）
void executePostScriptWithResponse(
    String postscript,
    Map<String, Object> bindings,
    HttpResponse response
)

// 执行后置脚本（带响应和自定义回调）
void executePostScriptWithResponse(
    String postscript,
    Map<String, Object> bindings,
    HttpResponse response,
    JsScriptExecutor.OutputCallback outputCallback
)

// 通用执行方法
void executeScript(ScriptExecutionContext context)
```

### ScriptExecutionPipeline

#### 执行方法

```java
// 执行前置脚本
ScriptExecutionResult executePreScript()

// 执行后置脚本
ScriptExecutionResult executePostScript(HttpResponse response)

// 执行完整流程（前置 + 后置）
ScriptExecutionResult executeFullPipeline(HttpResponse response)
```

#### 辅助方法

```java
// 添加 CSV 数据变量绑定
void addCsvDataBindings(Map<String, String> csvData)

// 获取 PostmanApiContext
PostmanApiContext getPostmanContext()

// 获取当前的 bindings
Map<String, Object> getBindings()
```

### ScriptExecutionResult

#### 判断方法

```java
// 是否执行成功
boolean isSuccess()

// 是否有测试结果
boolean hasTestResults()

// 所有测试是否通过
boolean allTestsPassed()
```

#### 获取方法

```java
// 获取测试结果
List<TestResult> getTestResults()

// 获取错误信息
String getErrorMessage()

// 获取异常对象
Exception getException()
```

#### 工厂方法

```java
// 创建成功结果
static ScriptExecutionResult success()
static ScriptExecutionResult success(List<TestResult> testResults)

// 创建失败结果
static ScriptExecutionResult failure(String errorMessage, Exception exception)
static ScriptExecutionResult failure(String errorMessage, Exception exception, List<TestResult> testResults)
```

---

## 💡 使用示例

### 示例 1: 基础用法

```java
public void executeRequest(HttpRequestItem item) {
    // 构建请求
    PreparedRequest req = PreparedRequestBuilder.build(item);
    
    // 创建流水线
    ScriptExecutionPipeline pipeline = ScriptExecutionService.createPipeline(
        req,
        item.getPrescript(),
        item.getPostscript()
    );
    
    // 执行前置脚本
    ScriptExecutionResult preResult = pipeline.executePreScript();
    if (!preResult.isSuccess()) {
        showError("前置脚本执行失败: " + preResult.getErrorMessage());
        return;
    }
    
    // 执行请求
    HttpResponse response = HttpSingleRequestExecutor.executeHttp(req);
    
    // 执行后置脚本
    ScriptExecutionResult postResult = pipeline.executePostScript(response);
    
    // 显示测试结果
    if (postResult.hasTestResults()) {
        displayTestResults(postResult.getTestResults());
    }
}
```

### 示例 2: 带 CSV 数据

```java
public void executeBatchWithCsv(HttpRequestItem item, List<Map<String, String>> csvRows) {
    PreparedRequest req = PreparedRequestBuilder.build(item);
    
    for (Map<String, String> csvRow : csvRows) {
        // 创建流水线
        ScriptExecutionPipeline pipeline = ScriptExecutionService.createPipeline(
            req,
            item.getPrescript(),
            item.getPostscript()
        );
        
        // 注入 CSV 数据
        pipeline.addCsvDataBindings(csvRow);
        
        // 执行前置脚本
        ScriptExecutionResult preResult = pipeline.executePreScript();
        if (!preResult.isSuccess()) {
            continue; // 跳过失败的行
        }
        
        // 执行请求
        HttpResponse response = HttpSingleRequestExecutor.executeHttp(req);
        
        // 执行后置脚本并收集结果
        ScriptExecutionResult postResult = pipeline.executePostScript(response);
        
        // 记录结果
        recordResult(csvRow, postResult);
    }
}
```

### 示例 3: 自定义输出处理

```java
public void executeWithCustomOutput(HttpRequestItem item) {
    PreparedRequest req = PreparedRequestBuilder.build(item);
    
    // 创建带自定义输出的流水线
    ScriptExecutionPipeline pipeline = ScriptExecutionService.createPipeline(
        req,
        item.getPrescript(),
        item.getPostscript(),
        output -> {
            // 自定义输出处理
            logToFile(output);
            updateUI(output);
            notifyObservers(output);
        }
    );
    
    // 执行流程
    ScriptExecutionResult preResult = pipeline.executePreScript();
    if (preResult.isSuccess()) {
        HttpResponse response = executeRequest(req);
        ScriptExecutionResult postResult = pipeline.executePostScript(response);
        handleResult(postResult);
    }
}
```

### 示例 4: WebSocket/SSE 消息处理

```java
public class WebSocketHandler {
    private ScriptExecutionPipeline pipeline;
    
    public void onConnect(HttpRequestItem item, PreparedRequest req) {
        // 创建流水线
        this.pipeline = ScriptExecutionService.createPipeline(
            req,
            item.getPrescript(),
            item.getPostscript()
        );
        
        // 执行前置脚本
        ScriptExecutionResult preResult = pipeline.executePreScript();
        if (!preResult.isSuccess()) {
            close("前置脚本失败");
        }
    }
    
    public void onMessage(String message) {
        // 构造响应对象
        HttpResponse response = new HttpResponse();
        response.body = message;
        response.bodySize = message.length();
        
        // 执行后置脚本
        ScriptExecutionResult result = pipeline.executePostScript(response);
        
        // 处理测试结果
        if (result.hasTestResults()) {
            updateTestResults(result.getTestResults());
        }
    }
}
```

### 示例 5: 性能测试

```java
public void runPerformanceTest(HttpRequestItem item, int iterations) {
    PreparedRequest req = PreparedRequestBuilder.build(item);
    List<TestResult> allTests = new ArrayList<>();
    
    for (int i = 0; i < iterations; i++) {
        // 每次迭代创建新的流水线
        ScriptExecutionPipeline pipeline = ScriptExecutionService.createPipeline(
            req,
            item.getPrescript(),
            item.getPostscript()
        );
        
        // 执行前置脚本
        ScriptExecutionResult preResult = pipeline.executePreScript();
        if (!preResult.isSuccess()) {
            recordFailure(i, preResult);
            continue;
        }
        
        // 执行请求
        long startTime = System.currentTimeMillis();
        HttpResponse response = HttpSingleRequestExecutor.executeHttp(req);
        long duration = System.currentTimeMillis() - startTime;
        
        // 执行后置脚本
        ScriptExecutionResult postResult = pipeline.executePostScript(response);
        
        // 记录性能数据
        recordPerformance(i, duration, postResult);
        
        // 收集测试结果
        if (postResult.hasTestResults()) {
            allTests.addAll(postResult.getTestResults());
        }
    }
    
    // 生成报告
    generateReport(allTests);
}
```

### 示例 6: 条件判断简化

```java
// ❌ 之前的写法
PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
if (pm != null && pm.testResults != null && !pm.testResults.isEmpty()) {
    boolean allPassed = pm.testResults.stream().allMatch(t -> t.passed);
    if (allPassed) {
        showSuccess();
    } else {
        showFailure();
    }
} else {
    showNoTests();
}

// ✅ 使用 Pipeline 后的写法
ScriptExecutionResult result = pipeline.executePostScript(response);
if (!result.hasTestResults()) {
    showNoTests();
} else if (result.allTestsPassed()) {
    showSuccess();
} else {
    showFailure();
}
```

---

## 🔄 迁移指南

### 第1步：识别可迁移代码

查找以下代码模式：

**模式 1: 准备 bindings**
```java
Map<String, Object> bindings = HttpUtil.prepareBindings(req);
// 或
Map<String, Object> bindings = ScriptExecutionService.preparePreRequestBindings(req);
```

**模式 2: 执行前置脚本**
```java
ScriptExecutionService.executePreScript(prescript, bindings);
```

**模式 3: 清空测试结果**
```java
PostmanApiContext pm = (PostmanApiContext) bindings.get("pm");
if (pm != null) {
    pm.testResults.clear();
}
```

**模式 4: 执行后置脚本**
```java
HttpUtil.postBindings(bindings, response);
ScriptExecutionService.executePostScript(postscript, bindings);
```

**模式 5: 收集测试结果**
```java
if (pm != null && pm.testResults != null) {
    row.testResults = new ArrayList<>(pm.testResults);
}
```

### 第2步：使用 Pipeline 替换

```java
// 创建流水线（替换模式 1）
ScriptExecutionPipeline pipeline = ScriptExecutionService.createPipeline(
    req,
    item.getPrescript(),
    item.getPostscript()
);

// 执行前置脚本（替换模式 2 和 3）
ScriptExecutionResult preResult = pipeline.executePreScript();

// 执行后置脚本（替换模式 4 和 5）
ScriptExecutionResult postResult = pipeline.executePostScript(response);
List<TestResult> testResults = postResult.getTestResults();
```

### 第3步：简化条件判断

```java
// 之前
if (pm.testResults == null || pm.testResults.isEmpty()) {
    return AssertionResult.NO_TESTS;
} else {
    boolean allPassed = pm.testResults.stream().allMatch(test -> test.passed);
    return allPassed ? AssertionResult.PASS : AssertionResult.FAIL;
}

// 之后
if (!postResult.hasTestResults()) {
    return AssertionResult.NO_TESTS;
} else if (postResult.allTestsPassed()) {
    return AssertionResult.PASS;
} else {
    return AssertionResult.FAIL;
}
```

---

## ❓ 常见问题

### Q1: Pipeline 可以复用吗？

**A**: 不建议复用。每次请求应该创建新的 Pipeline，因为它维护了状态（bindings）。

```java
// ❌ 不推荐
ScriptExecutionPipeline pipeline = createPipeline(...);
for (int i = 0; i < 10; i++) {
    pipeline.executePreScript();  // 状态会累积
}

// ✅ 推荐
for (int i = 0; i < 10; i++) {
    ScriptExecutionPipeline pipeline = createPipeline(...);
    pipeline.executePreScript();
}
```

### Q2: 如何获取脚本执行的详细错误信息？

**A**: 使用 `ScriptExecutionResult` 的 `getErrorMessage()` 和 `getException()`：

```java
ScriptExecutionResult result = pipeline.executePreScript();
if (!result.isSuccess()) {
    String message = result.getErrorMessage();
    Exception ex = result.getException();
    log.error("Script failed: {}", message, ex);
}
```

### Q3: Pipeline 和直接调用 ScriptExecutionService 有什么区别？

**A**: 

| 特性 | Pipeline | ScriptExecutionService |
|------|----------|------------------------|
| 状态管理 | ✅ 自动管理 | ❌ 手动管理 |
| 测试结果清空 | ✅ 自动 | ❌ 手动 |
| 结果收集 | ✅ 自动 | ❌ 手动 |
| CSV 数据注入 | ✅ 内置支持 | ❌ 需要手动 |
| 适用场景 | 完整流程 | 单独执行脚本 |

**推荐**：大多数情况使用 Pipeline，只有特殊场景才直接调用 Service。

### Q4: 如何处理前置脚本失败的情况？

**A**: 检查返回的 `ScriptExecutionResult`：

```java
ScriptExecutionResult preResult = pipeline.executePreScript();
if (!preResult.isSuccess()) {
    // 方式1: 显示错误
    showErrorDialog(preResult.getErrorMessage());
    
    // 方式2: 记录并继续
    log.error("Pre-script failed, skipping request");
    
    // 方式3: 抛出异常
    throw new RuntimeException("Pre-script failed", preResult.getException());
    
    return; // 不执行后续请求
}
```

### Q5: 如何在脚本执行过程中修改变量？

**A**: 通过 `PostmanApiContext` 修改：

```java
ScriptExecutionPipeline pipeline = createPipeline(...);

// 执行前置脚本
pipeline.executePreScript();

// 获取 PostmanApiContext 并修改变量
PostmanApiContext pm = pipeline.getPostmanContext();
if (pm != null) {
    pm.variables.set("customVar", "customValue");
    pm.environment.set("envVar", "envValue");
}

// 继续执行
HttpResponse response = executeRequest(...);
pipeline.executePostScript(response);
```

### Q6: 如何集成到现有代码中？

**A**: 渐进式迁移，可以先在新功能中使用：

```java
public void executeRequest(HttpRequestItem item) {
    PreparedRequest req = PreparedRequestBuilder.build(item);
    
    // 方式1: 使用新的 Pipeline（推荐）
    if (USE_NEW_PIPELINE) {
        executeWithPipeline(item, req);
    } 
    // 方式2: 保留旧的实现
    else {
        executeWithLegacyCode(item, req);
    }
}
```

---

## 📖 相关文档

- [高级重构总结](./advanced-refactoring-summary.md)
- [API 文档](./API_REFERENCE.md)
- [最佳实践](./BEST_PRACTICES.md)

---

## 📝 更新日志

### v1.0.0 (2025-11-28)
- 🎉 首次发布
- ✨ 新增 `ScriptExecutionPipeline`
- ✨ 新增 `ScriptExecutionResult`
- ✨ 增强 `ScriptExecutionService`
- 📚 完整的文档和示例

