## OkHttpClient 连接池和并发参数说明

在 Easy Postman 的 OkHttpClientManager 中，使用如下方式为每个 baseUri 配置连接池和 Dispatcher：

```java
// 连接池配置
.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))

// Dispatcher 并发配置
Dispatcher dispatcher = new Dispatcher();
dispatcher.setMaxRequests(maxRequests);
dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
.dispatcher(dispatcher)
```

### 参数解释

#### 连接池参数
- **maxIdleConnections**：最大空闲连接数。表示连接池中允许保持空闲状态的最大连接数量。
- **keepAliveDuration**：空闲连接的最大存活时间（单位由第三个参数指定）。超过该时间未被使用的空闲连接将被关闭。
- **TimeUnit.SECONDS**：指定 keepAliveDuration 的时间单位为秒。

#### Dispatcher 并发参数（⚠️ 压测关键参数）
- **maxRequests**：最大并发请求数（所有主机总和）。默认64，压测时建议设置为并发用户数的2-4倍。
- **maxRequestsPerHost**：单个主机最大并发请求数。默认5，**这是导致压测排队的主要原因**！压测时建议设置为并发用户数。

### ⚠️ 压测性能问题排查

如果你在压测时发现 **Stalled** 时间异常长（例如几十秒），说明请求在队列中等待，原因是 **maxRequestsPerHost** 限制了并发数。

#### 问题示例
```
DnsStart 14:21:35.808 
DnsEnd 14:21:36.424 (DNS查询正常，616ms)
ConnectStart 14:22:51.429 ⚠️ 75秒延迟！

Stalled: 75621 ms ← 这就是请求排队时间
```

**原因**：OkHttp 默认 `maxRequestsPerHost=5`，如果并发数超过5，多余的请求会在队列中等待。

**解决方案**：在压测设置中增大以下参数：
- **最大并发请求数**：建议设置为并发用户数的2-4倍（如100并发用户 → 设置为 200-400）
- **单主机最大并发数**：建议设置为并发用户数（如100并发用户 → 设置为 100-200）

在 Easy Postman 中，进入 **设置 → 压力测试**，调整：
- 最大并发请求数 (Max Concurrent Requests)
- 单主机最大并发数 (Max Concurrent Requests Per Host)

### maxIdleConnections 详解
maxIdleConnections 控制同一主机（baseUri）下，连接池中最多可以有多少个空闲连接被复用。

#### 举例说明
假设 maxIdleConnections 设置为 6：
- 如果你对同一个服务端（如 https://api.example.com）并发发起 10 个请求，连接池最多只会保留 6 个空闲连接。
- 其余的连接在空闲后会被关闭，避免资源浪费。
- 这样可以提升高并发下的性能，同时防止过多无用连接占用系统资源。

#### 适用场景
- **接口调试/开发环境**：通常 6 足够。
- **性能压测/高并发场景**：可通过 setConnectionPoolConfig 动态调大 maxIdleConnections，例如设置为 50 或 100，以适应更多并发连接需求。

### maxIdleConnections 详解（更详细流程说明）

假设 maxIdleConnections 设置为 6，keepAliveDuration 设置为 90 秒，场景如下：

#### 并发发起 10 个请求的全过程

1. **请求发起阶段**  
   客户端（如 Easy Postman）同时向同一服务端（如 https://api.example.com）并发发起 10 个 HTTP 请求。
   OkHttp 会为每个请求分配一个连接（如果连接池中有可用空闲连接，则复用，否则新建连接）。

2. **请求处理与连接归还**  
   10 个请求处理完毕后，这 10 个连接会被归还到连接池。
   连接池会检查当前空闲连接数是否超过 maxIdleConnections（本例为 6）。

3. **空闲连接管理**  
   如果归还后空闲连接数超过 6，连接池会立即关闭多余的空闲连接，只保留 6 个。
   哪些连接被保留、哪些被关闭由 OkHttp 内部策略决定（通常是最近最活跃的连接被保留）。

4. **keepAliveDuration 的作用**  
   被保留的 6 个空闲连接会在连接池中等待复用。
   如果在 keepAliveDuration（如 90 秒）内没有新的请求复用这些连接，这些空闲连接会被自动关闭，释放资源。
   也就是说，**maxIdleConnections** 决定了“最多能保留多少个空闲连接”，**keepAliveDuration** 决定了“空闲连接最多能保留多久”。

#### 关键参数的作用

- **maxIdleConnections**：决定连接池中最多能保留多少个空闲连接。超过这个数，多余的空闲连接会被立即关闭。
- **keepAliveDuration**：决定空闲连接最多能保留多长时间。超过这个时间未被复用的空闲连接会被自动关闭。

#### 过程示意

1. 并发 10 个请求，建立 10 个连接。
2. 请求结束，10 个连接归还到连接池。
3. 连接池只保留 6 个空闲连接，立即关闭多余的 4 个。
4. 这 6 个空闲连接在 90 秒内如果没有被新请求复用，也会被自动关闭。

#### 这样做的好处

- 避免了大量无用连接长期占用系统资源。
- 保证高并发下的性能和资源利用率。
- 连接池参数可根据实际并发量灵活调整。

### 参考
- OkHttp 官方文档：[ConnectionPool](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-connection-pool/)
- Chrome 浏览器默认每 host 6 个连接，Easy Postman 默认采用相同策略。
