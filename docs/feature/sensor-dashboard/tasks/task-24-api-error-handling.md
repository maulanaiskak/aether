# Task 24: API Error Handling

**Status:** pending
**HLD Reference:** §API Design — REST Endpoints (400/404/503 cases)

## Description

Implement a global WebFlux `WebExceptionHandler` (or `@ControllerAdvice` equivalent for functional routes) that converts domain exceptions and validation errors into consistent JSON error responses. This ensures all endpoints return structured errors instead of Spring's default HTML error pages.

## Acceptance Criteria

- [ ] `GlobalErrorHandler` implements `WebExceptionHandler`; handles: `ResponseStatusException`, `MethodArgumentNotValidException`, generic `Exception`
- [ ] Error response shape: `{ "status": 400, "error": "Bad Request", "message": "...", "path": "/api/v1/..." }`
- [ ] `400` for missing/invalid request parameters
- [ ] `404` for unknown locations or missing resources
- [ ] `503` for upstream failures (sidecar timeout, DB unavailable) — distinguish from 500
- [ ] `500` for unexpected exceptions — message sanitized (no stack trace in response body)
- [ ] CORS configured: allow `localhost:*` origins for `GET`, `POST`; expose `Content-Type` header
- [ ] `GlobalErrorHandlerTest` (WebTestClient): verify each error case returns correct status + JSON shape

## Dependencies

- **Depends on:** Task 21, 22, 23 (all REST endpoints in place)
- **Blocks:** Task 25 (FE depends on consistent error shapes)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/api/error/GlobalErrorHandler.java` | Create | WebExceptionHandler |
| `src/main/java/io/aether/api/error/ErrorResponse.java` | Create | Error response record |
| `src/main/java/io/aether/api/config/CorsConfig.java` | Create | WebFlux CORS configuration |
| `src/test/java/io/aether/api/error/GlobalErrorHandlerTest.java` | Create | WebTestClient error tests |

## Implementation Hints

- **WebFlux global error handler (functional routes):**
  ```java
  @Component
  @Order(-2)  // Higher priority than DefaultErrorWebExceptionHandler
  public class GlobalErrorHandler implements WebExceptionHandler {
      @Override
      public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
          var status = resolveStatus(ex);
          var body = new ErrorResponse(status.value(), status.getReasonPhrase(),
              sanitize(ex.getMessage()), exchange.getRequest().getPath().value());
          exchange.getResponse().setStatusCode(status);
          exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
          var bytes = objectMapper.writeValueAsBytes(body);
          return exchange.getResponse().writeWith(
              Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
      }
  }
  ```
- **CORS:**
  ```java
  @Bean
  public WebFilter corsFilter() {
      return (exchange, chain) -> {
          exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
          // ... or use CorsWebFilter with CorsConfiguration
          return chain.filter(exchange);
      };
  }
  ```
  Prefer `CorsWebFilter` with explicit origin patterns over wildcard `*` for local dev.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
