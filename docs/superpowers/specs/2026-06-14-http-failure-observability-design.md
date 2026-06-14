# HTTP Failure Observability Design

## Context

EasyPostman currently has repeated regressions where request failures are visible only as a toast or console line, while the response panel, timing view, and Network Log are empty. Issue #122 exposed one concrete case: OkHttp throws a plain `java.io.IOException` for `unexpected end of stream` before response headers arrive. The current Swing worker only converts `InterruptedIOException` into a displayable `HttpResponse`; ordinary exceptions leave `resp == null`, and `RequestExecutionUiUpdater.updateUIForResponse(null)` clears the response and Network Log panels.

The project already has the right module direction: HTTP runtime models, transport, event info, and network observation ports live in `easy-postman-http-runtime`, while Swing panels and notifications live in `easy-postman-app`. The weakness is that failure normalization is still owned by app UI code instead of the runtime boundary.

## Goals

- Every non-cancelled HTTP execution failure produces a displayable `HttpResponse`.
- Failed requests keep timing, request snapshot, error details, and Network Log events visible.
- Swing request execution no longer decides which exception classes deserve a response.
- Runtime remains UI-neutral and communicates through `HttpResponse`, `HttpEventInfo`, and existing observation sinks.
- Existing successful response, redirect, incomplete response body, and download cancellation behavior remains intact.

## Non-Goals

- Redesigning the full Network Log UI.
- Reworking WebSocket execution.
- Replacing OkHttp or changing low-level retry/connection behavior.
- Treating user-cancelled downloads as red error responses.

## Proposed Architecture

Move failure response construction into `easy-postman-http-runtime`.

Add a runtime utility such as `HttpFailureResponseFactory` under `com.laker.postman.http.runtime.transport` or `com.laker.postman.http.runtime.error`. It converts a `PreparedRequest`, `Throwable`, request start time, and end time into a normalized `HttpResponse`.

The normalized failure response should set:

- `code = 0`
- `body = ""`
- `bodySize = 0`
- `endTime`
- `costMs`
- `threadName`
- `protocol`, if available from `HttpEventInfo`
- `httpEventInfo.errorMessage`
- `httpEventInfo.error`
- `httpEventInfo.callFailed`
- `httpEventInfo.queueStart`, if missing

It should reuse `HttpExchangeTraceSupport.resolveFromRequest(request)` so failures retain any OkHttp `EventListener` data collected before the exception.

It should publish a final `NetworkLogEventStage.FAILED` event through `NetworkLogSupport.append(...)`. OkHttp-specific `CALL_FAILED`, `REQUEST_FAILED`, and `RESPONSE_FAILED` events should remain owned by `OkHttpExchangeEventListener`; the new factory provides the stable final summary event.

## Execution Flow

Keep `DefaultHttpTransport.execute(...)` and `HttpExchangeExecutor.executeHttp(...)` as the low-level transport API that may throw. Add the failure normalization at the app-facing HTTP execution boundary.

For the current Swing HTTP flow, this means:

1. `HttpRequestExecutor` records `requestStartMs`.
2. The normal path calls `HttpRedirectExecutor.executeWithRedirects(...)`.
3. `DownloadCancelledException` keeps the current cancellation behavior.
4. Any other exception is converted with runtime `HttpFailureResponseFactory`.
5. `done()` always receives a non-null `resp` for non-cancelled HTTP failures.
6. `requestExecutionUiUpdater.updateUIForResponse(resp)` renders the failure response instead of clearing all panels.
7. `RequestResponseHandler.handleResponse(...)` can record history and request/response details for failure responses in the same way it handles timeout responses today.

This design intentionally does not require `HttpRedirectExecutor` to swallow exceptions globally yet. Keeping the catch in the app worker limits the first change while moving the actual normalization logic into the runtime module. If later CLI or performance paths need displayable failure responses, they can call the same runtime factory or introduce a small `safeExecute` wrapper without duplicating logic.

## UI Behavior

For a failed HTTP request:

- Status shows `0`.
- Response body is empty unless a future UX change adds a formatted error body.
- Timing remains visible, including any phases captured before failure.
- Network Log keeps previous per-stage events and appends a final failure summary.
- Request details remain visible.
- Response details include the error-bearing `HttpResponse`.
- A toast and console error can still be shown, but they are not the only record of the failure.

`updateUIForResponse(null)` remains a defensive fallback for cases where no request result exists, but normal HTTP failures must not use that path.

## Module Boundaries

- Runtime failure factory belongs in `easy-postman-http-runtime`.
- It must not import Swing, `ConsolePanel`, `NotificationUtil`, app panels, app settings, or IOC.
- Swing-specific user notification remains in `easy-postman-app`.
- Existing app `HttpRequestFailureResponseFactory` should be removed or reduced to a thin delegating wrapper during transition.

## Error Classification

Use `NetworkErrorMessageResolver.toUserFriendlyMessage(Throwable)` as the single source for user-facing error text.

Known failure classes that must produce a response:

- `InterruptedIOException` timeout/interruption
- plain `IOException`, including `unexpected end of stream`
- DNS/connect/proxy/TLS failures
- unexpected runtime exceptions escaping request construction or response handling after the request starts

Known non-error path:

- `DownloadCancelledException` from user cancellation

## Tests

Add focused coverage for:

- Plain `IOException("unexpected end of stream ...")` creates a failure response with `code = 0`, cost, error message, and a `FAILED` network log event.
- Existing `InterruptedIOException` behavior still creates a failure response and user-friendly timeout message.
- `HttpRequestExecutor` catch-all exception path sets `resp` instead of leaving it null.
- Existing tests for body-read disconnect, Brotli decompression, redirects, and failure response factory continue to pass.

Prefer runtime unit tests for the factory and minimal app tests for worker/UI control flow. Do not add Swing-only seams unless the seam also improves production design.

## Rollout

Implement in small steps:

1. Add runtime failure factory and tests.
2. Replace app failure factory usage for `InterruptedIOException`.
3. Convert generic HTTP exceptions through the same runtime factory.
4. Remove or delegate the old app factory.
5. Run focused HTTP/app tests and a quick app compile.
