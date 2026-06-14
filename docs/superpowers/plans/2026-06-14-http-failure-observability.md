# HTTP Failure Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every non-cancelled HTTP execution failure produce a visible `HttpResponse` and preserved Network Log details.

**Architecture:** Add a UI-neutral failure response factory in `easy-postman-http-runtime`, then make Swing HTTP execution use it for both interrupted and generic failures. Keep transport APIs throwable for now; normalize at the app-facing execution boundary.

**Tech Stack:** Java 17, Maven multi-module, TestNG, Swing worker execution, OkHttp runtime observation.

---

## File Structure

- Create: `easy-postman-http-runtime/src/main/java/com/laker/postman/http/runtime/error/HttpFailureResponseFactory.java`
  - Owns conversion from `PreparedRequest + Throwable + timestamps` to displayable `HttpResponse`.
- Create: `easy-postman-http-runtime/src/test/java/com/laker/postman/http/runtime/error/HttpFailureResponseFactoryTest.java`
  - Tests runtime failure response behavior without Swing.
- Modify: `easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/HttpRequestExecutor.java`
  - Uses the runtime factory for all non-cancelled HTTP execution exceptions.
- Delete: `easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/HttpRequestFailureResponseFactory.java`
  - App should not own runtime failure response construction.
- Modify: `easy-postman-app/src/test/java/com/laker/postman/panel/collections/editor/request/HttpRequestFailureResponseFactoryTest.java`
  - Remove after equivalent runtime coverage exists.

### Task 1: Runtime Failure Factory

**Files:**
- Create: `easy-postman-http-runtime/src/test/java/com/laker/postman/http/runtime/error/HttpFailureResponseFactoryTest.java`
- Create: `easy-postman-http-runtime/src/main/java/com/laker/postman/http/runtime/error/HttpFailureResponseFactory.java`

- [ ] **Step 1: Write failing runtime tests**

```java
package com.laker.postman.http.runtime.error;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HttpFailureResponseFactoryTest {

    @Test
    public void shouldBuildDisplayableFailureResponseForPlainIOException() {
        PreparedRequest request = new PreparedRequest();
        request.method = "POST";
        request.url = "https://example.test/api";
        request.enableNetworkLog = true;
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;

        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setQueueStart(1_000L);
        eventInfo.setCallStart(1_010L);
        eventInfo.setProtocol("HTTP/1.1");
        request.exchangeEventInfo = eventInfo;

        IOException exception = new IOException("unexpected end of stream on https://example.test/...");
        HttpResponse response = HttpFailureResponseFactory.fromException(request, exception, 1_000L, 1_250L);

        assertEquals(response.code, 0);
        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
        assertEquals(response.costMs, 250L);
        assertEquals(response.endTime, 1_250L);
        assertSame(response.httpEventInfo, eventInfo);
        assertEquals(response.protocol, "HTTP/1.1");
        assertSame(eventInfo.getError(), exception);
        assertTrue(eventInfo.getErrorMessage().contains("unexpected end of stream"));
        assertEquals(eventInfo.getCallFailed(), 1_250L);
        assertTrue(events.stream().anyMatch(event -> event.stage() == NetworkLogEventStage.FAILED
                && event.message().contains("POST https://example.test/api")
                && event.message().contains("unexpected end of stream")));
    }

    @Test
    public void shouldUseRequestStartWhenEventInfoIsMissing() {
        PreparedRequest request = new PreparedRequest();
        request.method = "GET";
        request.url = "https://example.test/slow";

        InterruptedIOException exception = new InterruptedIOException("timeout");
        HttpResponse response = HttpFailureResponseFactory.fromException(request, exception, 2_000L, 2_750L);

        assertEquals(response.code, 0);
        assertEquals(response.costMs, 750L);
        assertEquals(response.httpEventInfo.getQueueStart(), 2_000L);
        assertEquals(response.httpEventInfo.getCallFailed(), 2_750L);
        assertEquals(response.httpEventInfo.getErrorMessage(), "timeout");
    }
}
```

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
mvn -q -pl easy-postman-http-runtime -am -Dtest=HttpFailureResponseFactoryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

Expected: compile failure because `HttpFailureResponseFactory` does not exist.

- [ ] **Step 3: Implement minimal runtime factory**

Create `HttpFailureResponseFactory` with:

```java
package com.laker.postman.http.runtime.error;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSupport;
import com.laker.postman.http.runtime.transport.HttpExchangeTraceSupport;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpFailureResponseFactory {

    public static HttpResponse fromException(PreparedRequest request,
                                             Throwable throwable,
                                             long requestStartMs,
                                             long endTimeMs) {
        String errorMessage = resolveErrorMessage(throwable);
        HttpResponse response = new HttpResponse();
        response.code = 0;
        response.body = "";
        response.bodySize = 0L;
        response.endTime = endTimeMs;

        HttpEventInfo eventInfo = HttpExchangeTraceSupport.resolveFromRequest(request);
        if (eventInfo == null) {
            eventInfo = new HttpEventInfo();
            eventInfo.setQueueStart(requestStartMs);
        } else if (eventInfo.getQueueStart() <= 0 && requestStartMs > 0) {
            eventInfo.setQueueStart(requestStartMs);
        }
        eventInfo.setErrorMessage(errorMessage);
        eventInfo.setError(throwable);
        if (eventInfo.getCallFailed() <= 0) {
            eventInfo.setCallFailed(endTimeMs);
        }
        if (eventInfo.getThreadName() == null || eventInfo.getThreadName().isBlank()) {
            eventInfo.setThreadName(Thread.currentThread().getName());
        }

        response.httpEventInfo = eventInfo;
        response.threadName = eventInfo.getThreadName();
        response.protocol = eventInfo.getProtocol();
        response.costMs = resolveCostMs(eventInfo, endTimeMs);

        NetworkLogSupport.append(request, NetworkLogEventStage.FAILED, buildLogMessage(request, errorMessage), response.costMs);
        return response;
    }
}
```

Include private helper methods `resolveErrorMessage`, `resolveCostMs`, and `buildLogMessage` matching the existing app factory behavior.

- [ ] **Step 4: Run tests to verify green**

Run:

```bash
mvn -q -pl easy-postman-http-runtime -am -Dtest=HttpFailureResponseFactoryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

Expected: pass.

### Task 2: Swing HTTP Executor Uses Runtime Factory

**Files:**
- Modify: `easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/HttpRequestExecutor.java`
- Delete: `easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/HttpRequestFailureResponseFactory.java`
- Delete: `easy-postman-app/src/test/java/com/laker/postman/panel/collections/editor/request/HttpRequestFailureResponseFactoryTest.java`

- [ ] **Step 1: Update imports and exception handling**

In `HttpRequestExecutor`, import:

```java
import com.laker.postman.http.runtime.error.HttpFailureResponseFactory;
```

Change both non-cancel exception branches so they set `resp`:

```java
} catch (InterruptedIOException ex) {
    log.warn("Request interrupted: {} {} - {}", req.method, req.url, ex.getMessage());
    if (!isCancelled()) {
        resp = HttpFailureResponseFactory.fromException(req, ex, requestStartMs, System.currentTimeMillis());
        String userMessage = toInterruptedRequestUserMessage(ex, resp);
        ConsolePanel.appendLog("[Error] " + userMessage, ConsolePanel.LogType.ERROR);
        if (!executionState.isDisposed()) {
            NotificationUtil.showError(userMessage);
        }
    }
} catch (Exception ex) {
    log.error("Error executing HTTP request: {} {} - {}", req.method, req.url, ex.getMessage(), ex);
    resp = HttpFailureResponseFactory.fromException(req, ex, requestStartMs, System.currentTimeMillis());
    String userFriendlyMessage = resolveResponseErrorMessage(resp, ex);
    ConsolePanel.appendLog("[Error] " + userFriendlyMessage, ConsolePanel.LogType.ERROR);
    if (!executionState.isDisposed()) {
        NotificationUtil.showError(userFriendlyMessage);
    }
}
```

Add helper:

```java
private String resolveResponseErrorMessage(HttpResponse response, Exception ex) {
    if (response != null && response.httpEventInfo != null
            && response.httpEventInfo.getErrorMessage() != null
            && !response.httpEventInfo.getErrorMessage().isBlank()) {
        return response.httpEventInfo.getErrorMessage();
    }
    return NetworkErrorMessageResolver.toUserFriendlyMessage(ex);
}
```

- [ ] **Step 2: Remove app-local factory and duplicate tests**

Delete the app factory and its app test after runtime factory tests are green.

- [ ] **Step 3: Run app HTTP tests**

Run:

```bash
mvn -q -pl easy-postman-app -am -Dtest=DefaultHttpTransportIntegrationTest#shouldThrowWhenServerClosesConnectionBeforeSendingResponseHeaders+DefaultHttpTransportIntegrationTest#shouldDecompressBrotliResponseBody -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

Expected: pass.

### Task 3: Verification

**Files:**
- No additional production files.

- [ ] **Step 1: Run focused runtime/app tests**

Run:

```bash
mvn -q -pl easy-postman-http-runtime -am -Dtest=HttpFailureResponseFactoryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
mvn -q -pl easy-postman-app -am -Dtest=DefaultHttpTransportIntegrationTest#shouldThrowWhenServerClosesConnectionBeforeSendingResponseHeaders+DefaultHttpTransportIntegrationTest#shouldDecompressBrotliResponseBody -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

Expected: both pass.

- [ ] **Step 2: Run boundary and compile checks**

Run:

```bash
mvn -q -pl easy-postman-app -am -Dtest=ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
mvn -q -pl easy-postman-app -am -DskipTests compile
```

Expected: both pass.

- [ ] **Step 3: Review diff**

Run:

```bash
git diff -- easy-postman-http-runtime/src/main/java/com/laker/postman/http/runtime/error/HttpFailureResponseFactory.java \
  easy-postman-http-runtime/src/test/java/com/laker/postman/http/runtime/error/HttpFailureResponseFactoryTest.java \
  easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/HttpRequestExecutor.java \
  easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/HttpRequestFailureResponseFactory.java \
  easy-postman-app/src/test/java/com/laker/postman/panel/collections/editor/request/HttpRequestFailureResponseFactoryTest.java
```

Expected: only planned files changed for this task.
