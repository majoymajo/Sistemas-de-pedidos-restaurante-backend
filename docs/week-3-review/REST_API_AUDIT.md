# REST API Endpoint Audit Report

**Generated:** February 24, 2026  
**System:** Restaurant Ordering System  
**Services Analyzed:** order-service, report-service

---

## Executive Summary

### Is the API REST?

**Yes, the API follows REST conventions**, but with some gaps and areas for improvement.

**RESTful traits present:**
- ✅ Resource-based URIs (`/orders`, `/orders/{id}`, `/menu`, `/reports`)
- ✅ Proper HTTP verbs (`GET`, `POST`, `PATCH`, `DELETE`)
- ✅ Stateless request handling
- ✅ JSON as the media type
- ✅ Consistent error response structure (`ErrorResponse` DTO)
- ✅ OpenAPI/Swagger documentation on `order-service` endpoints

**RESTful traits missing or weak:**
- ❌ No HATEOAS (no hypermedia links in responses)
- ❌ No API versioning in the URI or headers (e.g., `/api/v1/orders`)
- ❌ No `Content-Type` validation at the controller level (relies on Spring defaults)
- ❌ `report-service` has no OpenAPI annotations at all
- ❌ No pagination on list endpoints (`GET /orders`, `GET /menu`)

---

## Endpoint-by-Endpoint Analysis

### 1. POST /orders — Create Order

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/OrderController.java:65` |
| **Success Code** | `201 Created` ✅ |
| **Error Codes** | `400`, `404`, `503` |

#### HTTP Codes Assessment

| Code | Status | Comment |
|---|---|---|
| `201 Created` | ✅ **CORRECT** | Resource creation should return 201 |
| `400 Bad Request` | ✅ **CORRECT** | For invalid `tableId`, empty items, validation failures |
| `404 Not Found` | ✅ **CORRECT** | For inactive/missing products |
| `503 Service Unavailable` | ✅ **CORRECT** | For DB or broker failures |

#### Missing HTTP Codes

| Code | Why Missing | Impact |
|---|---|---|
| No `Location` header | `201` response should include `Location: /orders/{id}` | **HIGH** - Violates REST best practices |
| `409 Conflict` | Not handled for duplicate submissions | **MEDIUM** - Edge case not covered |
| `422 Unprocessable Entity` | Could be more semantic than `404` for inactive products | **LOW** - Acceptable but not optimal |

#### Improvement Suggestions

1. **Add `Location` header to `201` response:**
   ```java
   return ResponseEntity
       .created(URI.create("/orders/" + orderResponse.getId()))
       .body(orderResponse);
   ```

2. **Consider `422 Unprocessable Entity` for inactive products** — currently returns `404`, but the product exists, it's just not processable. `404` conflates "not found" with "not active."

3. **Missing `tableId` max validation** — Business rules specify `tableId` must be 1-12, but validation only checks `>= 1`:
   - **File:** `order-service/src/main/java/com/restaurant/orderservice/service/OrderValidator.java:46-49`
   - **File:** `order-service/src/main/java/com/restaurant/orderservice/dto/CreateOrderRequest.java:22-24`
   - **Fix:** Add `@Max(12)` annotation and validator logic

---

### 2. GET /orders/{id} — Get Order by ID

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/OrderController.java:222` |
| **Success Code** | `200 OK` ✅ |
| **Error Codes** | `400`, `404` |

#### HTTP Codes Assessment

| Code | Status | Comment |
|---|---|---|
| `200 OK` | ✅ **CORRECT** | Standard success response |
| `404 Not Found` | ✅ **CORRECT** | Thrown by `OrderNotFoundException` |
| `400 Bad Request` | ⚠️ **DOCUMENTED BUT NOT IMPLEMENTED** | Swagger says invalid UUID returns `400`, but actually returns `500` |

#### Critical Issues

**Contract Violation:** The OpenAPI documentation promises `400` for invalid UUID format, but the actual implementation returns `500 Internal Server Error`.

**Root cause:** `MethodArgumentTypeMismatchException` (thrown when UUID parsing fails) is not handled by `GlobalExceptionHandler` and falls through to the generic `500` catch-all.

**File:** `order-service/src/main/java/com/restaurant/orderservice/exception/GlobalExceptionHandler.java:208`

#### Improvement Suggestions

1. **Add dedicated exception handler:**
   ```java
   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
   public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
       ErrorResponse error = ErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(HttpStatus.BAD_REQUEST.value())
               .error("Bad Request")
               .message("Invalid parameter format: " + ex.getName())
               .build();
       return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
   }
   ```

---

### 3. GET /orders — List Orders (with optional status filter)

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/OrderController.java:314` |
| **Success Code** | `200 OK` ✅ |
| **Error Codes** | `400` |

#### HTTP Codes Assessment

| Code | Status | Comment |
|---|---|---|
| `200 OK` | ✅ **CORRECT** | Even for empty results (returns `[]`) |
| `400 Bad Request` | ⚠️ **DOCUMENTED BUT NOT IMPLEMENTED** | Invalid status value hits `500`, not `400` |

#### Critical Issues

**Same issue as GET /orders/{id}:** Invalid enum values in `@RequestParam` trigger `MethodArgumentTypeMismatchException`, which returns `500` instead of the documented `400`.

#### Missing Features

| Feature | Impact | Priority |
|---|---|---|
| **No pagination** | Performance risk with large datasets | **HIGH** |
| `204 No Content` for empty list | Optional (200 with [] is also valid) | **LOW** |

#### Improvement Suggestions

1. **Add `MethodArgumentTypeMismatchException` handler** (same as GET /orders/{id})

2. **Add pagination support:**
   ```java
   @GetMapping
   public ResponseEntity<Page<OrderResponse>> getOrders(
           @RequestParam(required = false) List<OrderStatus> status,
           Pageable pageable) {
       Page<OrderResponse> orders = orderService.getOrders(status, pageable);
       return ResponseEntity.ok(orders);
   }
   ```

3. **Optional:** Return `204 No Content` when list is empty (though `200` with `[]` is acceptable in REST).

---

### 4. PATCH /orders/{id}/status — Update Order Status

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/OrderController.java:407` |
| **Success Code** | `200 OK` ✅ |
| **Error Codes** | `400`, `404` |

#### HTTP Codes Assessment

| Code | Status | Comment |
|---|---|---|
| `200 OK` | ✅ **CORRECT** | Partial update returning updated resource |
| `400 Bad Request` | ⚠️ **SEMANTICALLY WEAK** | Used for invalid transitions, but `409` would be better |
| `404 Not Found` | ✅ **CORRECT** | For missing orders |

#### Semantic Issues

**Invalid state transitions currently return `400 Bad Request`:**
- **Current behavior:** `InvalidStatusTransitionException` → `400 Bad Request`
- **File:** `order-service/src/main/java/com/restaurant/orderservice/exception/GlobalExceptionHandler.java:104-113`
- **Better approach:** `409 Conflict` more clearly signals a state conflict

**Distinction:**
- `400 Bad Request` = malformed request (syntax error)
- `409 Conflict` = request is valid but conflicts with current resource state

#### Missing Features

| Feature | Impact | Priority |
|---|---|---|
| **Optimistic locking** | Race conditions on concurrent updates | **MEDIUM** |
| `ETag` / `If-Match` headers | Concurrency control | **MEDIUM** |

#### Improvement Suggestions

1. **Return `409 Conflict` for `InvalidStatusTransitionException`:**
   ```java
   @ExceptionHandler(InvalidStatusTransitionException.class)
   public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
       log.warn("Invalid status transition attempted: {}", ex.getMessage());
       ErrorResponse error = ErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(HttpStatus.CONFLICT.value())  // Changed from BAD_REQUEST
               .error("Conflict")                     // Changed error type
               .message(ex.getMessage())
               .build();
       return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
   }
   ```

2. **Add optimistic locking:**
   - Add `@Version` field to `Order` entity
   - Include version in `UpdateStatusRequest`
   - Return `409 Conflict` on version mismatch

---

### 5. DELETE /orders/{id} — Delete Single Order (Soft Delete)

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/OrderController.java:509` |
| **Success Code** | `204 No Content` ✅ |
| **Error Codes** | `404` |

#### HTTP Codes Assessment

| Code | Status | Comment |
|---|---|---|
| `204 No Content` | ✅ **CORRECT** | For successful delete with no response body |
| `404 Not Found` | ✅ **CORRECT** | For missing orders |

#### Missing Documentation

| Code | Why Missing | Priority |
|---|---|---|
| `401 Unauthorized` | Kitchen auth failure not documented | **HIGH** |
| `403 Forbidden` | Insufficient permissions not documented | **HIGH** |

**Note:** The handler exists (`KitchenAccessDeniedException` → `401`) but is **not documented in Swagger** for this endpoint.

#### Semantic Issues

**`401` vs `403` conflation:**
- **Current:** Always returns `401` for `KitchenAccessDeniedException`
- **File:** `order-service/src/main/java/com/restaurant/orderservice/exception/GlobalExceptionHandler.java:170-181`
- **Issue:** `401` means "no credentials"; `403` means "credentials insufficient"
- **Fix:** Distinguish between missing token (`401`) and invalid token (`403`)

#### Improvement Suggestions

1. **Document `401`/`403` in `@ApiResponses` annotations**

2. **Distinguish authentication vs authorization:**
   ```java
   @ExceptionHandler(KitchenAccessDeniedException.class)
   public ResponseEntity<ErrorResponse> handleKitchenAccessDenied(KitchenAccessDeniedException ex) {
       // Determine if token was missing or just invalid
       boolean tokenMissing = ex.getMessage().contains("missing");
       HttpStatus status = tokenMissing ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
       
       ErrorResponse error = ErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(status.value())
               .error(status.getReasonPhrase())
               .message(ex.getMessage())
               .build();
       return ResponseEntity.status(status).body(error);
   }
   ```

3. **Consider returning audit metadata instead of `204`:**
   - Per copilot-instructions Section 4 (Destructive Operations)
   - Return `200` with `{ "deletedAt": "...", "deletedBy": "..." }`
   - Provides audit trail

---

### 6. DELETE /orders — Delete All Orders (Soft Delete)

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/OrderController.java:535` |
| **Success Code** | `204 No Content` ⚠️ |
| **Error Codes** | **NONE DOCUMENTED** ❌ |

#### Critical Issues

| Issue | Severity | Details |
|---|---|---|
| **No error responses documented** | **HIGH** | Swagger shows no `@ApiResponses` at all |
| `401`/`403` missing | **HIGH** | Kitchen auth required but not documented |
| **No confirmation mechanism** | **CRITICAL** | Violates copilot-instructions Section 4 |
| Discarded return value | **MEDIUM** | Service returns `long` count, controller discards it |

#### Improvement Suggestions

1. **Return `200 OK` with deletion count instead of `204`:**
   ```java
   @DeleteMapping
   public ResponseEntity<Map<String, Object>> deleteAllOrders() {
       long count = orderService.deleteAllOrders();
       return ResponseEntity.ok(Map.of(
           "deletedCount", count,
           "deletedAt", LocalDateTime.now()
       ));
   }
   ```

2. **Add confirmation header requirement:**
   ```java
   @DeleteMapping
   public ResponseEntity<?> deleteAllOrders(
           @RequestHeader(value = "X-Confirm-Destructive", required = false) Boolean confirm) {
       if (confirm == null || !confirm) {
           return ResponseEntity.badRequest().body(
               Map.of("error", "Destructive operation requires X-Confirm-Destructive: true header")
           );
       }
       // proceed with deletion
   }
   ```

3. **Document all possible responses:**
   - `200 OK` — Deletion successful (with count)
   - `400 Bad Request` — Missing confirmation header
   - `401 Unauthorized` — Missing kitchen token
   - `403 Forbidden` — Invalid kitchen token

---

### 7. GET /menu — Get Active Menu Products

| Attribute | Value |
|---|---|
| **Service** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/controller/MenuController.java:37` |
| **Success Code** | `200 OK` ✅ |
| **Error Codes** | `503` |

#### HTTP Codes Assessment

| Code | Status | Comment |
|---|---|---|
| `200 OK` | ✅ **CORRECT** | Standard success response |
| `503 Service Unavailable` | ✅ **CORRECT** | For DB failures |

#### Missing Optimizations

| Feature | Benefit | Priority |
|---|---|---|
| **Cache headers** | Reduce load (menu changes infrequently) | **HIGH** |
| Pagination | Handle large catalogs | **MEDIUM** |
| `304 Not Modified` | Bandwidth optimization with `ETag` | **MEDIUM** |

#### Improvement Suggestions

1. **Add cache control headers:**
   ```java
   @GetMapping
   public ResponseEntity<List<ProductResponse>> getMenu() {
       List<ProductResponse> activeProducts = menuService.getActiveProducts();
       return ResponseEntity.ok()
               .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
               .body(activeProducts);
   }
   ```

2. **Add `ETag` support for conditional requests:**
   ```java
   String etag = calculateETag(activeProducts);
   return ResponseEntity.ok()
           .eTag(etag)
           .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
           .body(activeProducts);
   ```

---

### 8. GET /reports — Generate Report

| Attribute | Value |
|---|---|
| **Service** | report-service |
| **File** | `report-service/src/main/java/com/restaurant/reportservice/controller/ReportController.java:27` |
| **Success Code** | `200 OK` ✅ |
| **Error Codes** | `400` ⚠️ |

#### Critical Issues

| Issue | Severity | Impact |
|---|---|---|
| **`400` returns empty body** | **CRITICAL** | Frontend cannot distinguish error types |
| **No `@RestControllerAdvice`** | **CRITICAL** | Unhandled exceptions return HTML, not JSON |
| **No OpenAPI annotations** | **HIGH** | Endpoint completely undocumented |
| **Manual date parsing in controller** | **HIGH** | Business logic leak, inconsistent error handling |
| **No `503` handling** | **MEDIUM** | DB failures not handled |

#### Code Issues

**Current implementation:**
```java
@GetMapping
public ResponseEntity<ReportResponseDTO> getReport(
        @RequestParam("startDate") String startDateStr,
        @RequestParam("endDate") String endDateStr) {
    try {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);
        ReportResponseDTO report = reportService.generateReport(startDate, endDate);
        return ResponseEntity.ok(report);
    } catch (DateTimeParseException e) {
        return ResponseEntity.badRequest().build();  // ❌ Empty body
    } catch (InvalidDateRangeException e) {
        return ResponseEntity.badRequest().build();  // ❌ Empty body, same as parse error
    }
}
```

**Problems:**
1. Both exceptions return identical empty `400` responses — no way to distinguish
2. No structured `ErrorResponse` DTO in `report-service`
3. Any other exception returns Spring's default HTML error page

#### Improvement Suggestions

1. **Create `ErrorResponse` DTO in `report-service`:**
   ```java
   package com.restaurant.reportservice.dto;
   
   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import java.time.LocalDateTime;
   
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class ErrorResponse {
       private LocalDateTime timestamp;
       private Integer status;
       private String error;
       private String message;
   }
   ```

2. **Create `GlobalExceptionHandler` for `report-service`:**
   ```java
   package com.restaurant.reportservice.exception;
   
   import com.restaurant.reportservice.dto.ErrorResponse;
   import org.springframework.http.HttpStatus;
   import org.springframework.http.ResponseEntity;
   import org.springframework.web.bind.annotation.ExceptionHandler;
   import org.springframework.web.bind.annotation.RestControllerAdvice;
   import java.time.LocalDateTime;
   import java.time.format.DateTimeParseException;
   
   @RestControllerAdvice
   public class GlobalExceptionHandler {
       
       @ExceptionHandler(DateTimeParseException.class)
       public ResponseEntity<ErrorResponse> handleDateParseError(DateTimeParseException ex) {
           ErrorResponse error = ErrorResponse.builder()
                   .timestamp(LocalDateTime.now())
                   .status(HttpStatus.BAD_REQUEST.value())
                   .error("Bad Request")
                   .message("Invalid date format. Expected ISO-8601 (YYYY-MM-DD)")
                   .build();
           return ResponseEntity.badRequest().body(error);
       }
       
       @ExceptionHandler(InvalidDateRangeException.class)
       public ResponseEntity<ErrorResponse> handleInvalidDateRange(InvalidDateRangeException ex) {
           ErrorResponse error = ErrorResponse.builder()
                   .timestamp(LocalDateTime.now())
                   .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                   .error("Unprocessable Entity")
                   .message(ex.getMessage())
                   .build();
           return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
       }
       
       @ExceptionHandler(Exception.class)
       public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
           ErrorResponse error = ErrorResponse.builder()
                   .timestamp(LocalDateTime.now())
                   .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                   .error("Internal Server Error")
                   .message("An unexpected error occurred")
                   .build();
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
       }
   }
   ```

3. **Let Spring handle date parsing:**
   ```java
   @GetMapping
   public ResponseEntity<ReportResponseDTO> getReport(
           @RequestParam("startDate") 
           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
           @RequestParam("endDate") 
           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
       ReportResponseDTO report = reportService.generateReport(startDate, endDate);
       return ResponseEntity.ok(report);
   }
   ```

4. **Add OpenAPI annotations:**
   ```java
   @Operation(summary = "Generate sales report by date range")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "Report generated successfully"),
       @ApiResponse(responseCode = "400", description = "Invalid date format"),
       @ApiResponse(responseCode = "422", description = "Invalid date range (start > end)")
   })
   @GetMapping
   public ResponseEntity<ReportResponseDTO> getReport(...) { ... }
   ```

---

## Cross-Cutting Issues

### 1. Missing `MethodArgumentTypeMismatchException` Handler

| Property | Value |
|---|---|
| **Severity** | **HIGH** |
| **Services Affected** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/exception/GlobalExceptionHandler.java:208` |

**Impact:** Invalid UUID format or bad enum values hit the `500` catch-all instead of returning `400`.

**Affected Endpoints:**
- `GET /orders/{id}` — Invalid UUID format
- `GET /orders?status=INVALID` — Invalid enum value
- `PATCH /orders/{id}/status` — Invalid UUID format

**Fix:** Add dedicated handler (see endpoint-specific sections above).

---

### 2. `401` vs `403` Conflation

| Property | Value |
|---|---|
| **Severity** | **MEDIUM** |
| **Services Affected** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/exception/GlobalExceptionHandler.java:170-181` |

**Current behavior:** `KitchenAccessDeniedException` always returns `401 Unauthorized`.

**Issue:** Should return `403 Forbidden` when credentials are present but insufficient.

**HTTP Semantics:**
- `401 Unauthorized` = "Who are you?" (authentication failed)
- `403 Forbidden` = "I know who you are, but you can't do that" (authorization failed)

**Fix:** See DELETE /orders/{id} section above.

---

### 3. Generic `500` Handler Leaks Internal Details

| Property | Value |
|---|---|
| **Severity** | **HIGH** (Security) |
| **Services Affected** | order-service |
| **File** | `order-service/src/main/java/com/restaurant/orderservice/exception/GlobalExceptionHandler.java:215-221` |

**Current code:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
    log.error("Unhandled exception", ex);
    ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")  // ❌ Exposes internals
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

**Risk:** `ex.getMessage()` can expose:
- Stack traces
- SQL queries
- Internal class names
- File paths
- Database connection details

**Fix:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
    log.error("Unhandled exception", ex);  // Log full details server-side
    ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please contact support.")  // ✅ Generic message
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

---

### 4. No API Versioning

| Property | Value |
|---|---|
| **Severity** | **LOW** (but important for future) |
| **Services Affected** | All |

**Current:** No versioning strategy exists.

**Options:**
1. **URI versioning:** `/api/v1/orders`
2. **Header versioning:** `Accept: application/vnd.restaurant.v1+json`
3. **Query parameter:** `/orders?version=1`

**Recommendation:** URI versioning for simplicity and clarity.

**Migration path:**
1. Current endpoints → `/api/v1/orders`
2. Keep unversioned endpoints as aliases pointing to v1 (backwards compatibility)
3. Future breaking changes → `/api/v2/orders`

---

### 5. `tableId` Max Bound Not Enforced

| Property | Value |
|---|---|
| **Severity** | **MEDIUM** |
| **Services Affected** | order-service |
| **Files** | `CreateOrderRequest.java:22-24`, `OrderValidator.java:46-49` |

**Business Rule:** `tableId` must be between 1 and 12 (inclusive).

**Current Implementation:** Only validates `>= 1`.

**Files needing changes:**

1. **DTO:** `order-service/src/main/java/com/restaurant/orderservice/dto/CreateOrderRequest.java`
   ```java
   @NotNull(message = "Table ID is required")
   @Min(value = 1, message = "Table ID must be positive")
   @Max(value = 12, message = "Table ID must not exceed 12")  // ← ADD THIS
   private Integer tableId;
   ```

2. **Validator:** `order-service/src/main/java/com/restaurant/orderservice/service/OrderValidator.java`
   ```java
   private void validateTableId(Integer tableId) {
       if (tableId == null || tableId <= 0) {
           throw new InvalidOrderException("Table ID must be a positive integer");
       }
       if (tableId > 12) {  // ← ADD THIS
           throw new InvalidOrderException("Table ID must not exceed 12");
       }
   }
   ```

---

### 6. `report-service` Has No Error Response Structure

| Property | Value |
|---|---|
| **Severity** | **HIGH** |
| **Services Affected** | report-service |
| **File** | `report-service/src/main/java/com/restaurant/reportservice/controller/ReportController.java` |

**Issues:**
- Returns empty body on `400` errors
- No `ErrorResponse` DTO
- No `@RestControllerAdvice` / global exception handler
- Unhandled exceptions return HTML instead of JSON

**Fix:** See GET /reports section above for complete implementation.

---

### 7. Frontend/Backend Contract Mismatch

| Property | Value |
|---|---|
| **Severity** | **LOW** (harmless but indicates drift) |
| **Files** | `src/api/orders.ts:73-74`, `UpdateStatusRequest.java` |

**Frontend sends:**
```typescript
{
  method: 'PATCH',
  json: { newStatus, status: newStatus },  // ← Sends both
  kitchenToken,
}
```

**Backend expects:**
```java
@Data
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;  // ← Only this field
}
```

**Impact:** The `newStatus` field is silently ignored. Not harmful but indicates contract drift.

**Fix:** Update frontend to send only `status`:
```typescript
json: { status: newStatus },
```

---

## Priority Matrix

### Critical (Fix Immediately)

| Issue | Endpoint(s) | File |
|---|---|---|
| `report-service` returns empty `400` bodies | `GET /reports` | `ReportController.java` |
| `report-service` has no global exception handler | All | (missing) |
| No confirmation mechanism on `DELETE /orders` | `DELETE /orders` | `OrderController.java:535` |
| Generic `500` handler leaks exception messages | All | `GlobalExceptionHandler.java:215` |

### High (Fix Soon)

| Issue | Endpoint(s) | File |
|---|---|---|
| `MethodArgumentTypeMismatchException` not handled | `GET /orders/{id}`, `GET /orders` | `GlobalExceptionHandler.java` |
| No `Location` header on `201 Created` | `POST /orders` | `OrderController.java:65` |
| `401`/`403` not documented | `DELETE /orders/{id}`, `DELETE /orders` | `OrderController.java` |
| `tableId` max validation missing | `POST /orders` | `OrderValidator.java`, `CreateOrderRequest.java` |
| No OpenAPI docs in `report-service` | `GET /reports` | `ReportController.java` |

### Medium (Plan for Next Sprint)

| Issue | Endpoint(s) | File |
|---|---|---|
| Invalid state transitions return `400` instead of `409` | `PATCH /orders/{id}/status` | `GlobalExceptionHandler.java:104` |
| `401`/`403` conflation | All auth-protected | `GlobalExceptionHandler.java:170` |
| No pagination on list endpoints | `GET /orders`, `GET /menu` | Controllers |
| `DELETE` endpoints discard audit metadata | `DELETE /orders/*` | `OrderController.java` |
| Manual date parsing in controller | `GET /reports` | `ReportController.java` |

### Low (Backlog)

| Issue | Endpoint(s) | File |
|---|---|---|
| No API versioning | All | All controllers |
| No cache headers on `GET /menu` | `GET /menu` | `MenuController.java` |
| `422` vs `404` semantic confusion | `POST /orders` | `GlobalExceptionHandler.java` |
| Frontend/backend contract drift | `PATCH /orders/{id}/status` | `orders.ts:73` |

---

## Testing Recommendations

### REST Contract Tests

Validate HTTP status codes with integration tests:

```java
@Test
void createOrder_withInvalidUUID_returns400NotSuchMethod() {
    // Currently fails - returns 500
    mockMvc.perform(get("/orders/not-a-uuid"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.status").value(400));
}

@Test
void createOrder_withInactiveProduct_returns422() {
    // Consider 422 instead of 404
    CreateOrderRequest request = new CreateOrderRequest(1, 
        List.of(new OrderItemRequest(999L, 1, null)));
    
    mockMvc.perform(post("/orders")
           .contentType(MediaType.APPLICATION_JSON)
           .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isUnprocessableEntity());
}

@Test
void createOrder_success_includesLocationHeader() {
    // Currently missing
    CreateOrderRequest request = validRequest();
    
    mockMvc.perform(post("/orders")
           .contentType(MediaType.APPLICATION_JSON)
           .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isCreated())
           .andExpect(header().exists("Location"))
           .andExpect(header().string("Location", matchesPattern("/orders/[a-f0-9-]+")));
}
```

---

## Appendix: HTTP Status Code Reference

### Success Codes

| Code | Meaning | Use When |
|---|---|---|
| `200 OK` | Success | GET, PATCH (returning body) |
| `201 Created` | Resource created | POST |
| `204 No Content` | Success, no body | DELETE (when not returning metadata) |

### Client Error Codes

| Code | Meaning | Use When |
|---|---|---|
| `400 Bad Request` | Malformed request | Invalid JSON, failed validation |
| `401 Unauthorized` | Authentication failed | Missing or invalid credentials |
| `403 Forbidden` | Authorization failed | Valid credentials, insufficient permissions |
| `404 Not Found` | Resource doesn't exist | GET/PATCH/DELETE on non-existent ID |
| `409 Conflict` | State conflict | Invalid state transition, optimistic lock failure |
| `422 Unprocessable Entity` | Semantic error | Valid syntax but business rule violation |

### Server Error Codes

| Code | Meaning | Use When |
|---|---|---|
| `500 Internal Server Error` | Unexpected error | Unhandled exceptions |
| `503 Service Unavailable` | Dependency failure | Database down, message broker unavailable |

---

## Conclusion

The API is **fundamentally RESTful** but has **implementation gaps** that violate documented contracts and REST best practices.

**Top 3 actions:**
1. Add `MethodArgumentTypeMismatchException` handler to `order-service`
2. Create global exception handler for `report-service`
3. Add `Location` header to `POST /orders` responses

**Documentation generated from:**
- `order-service/src/main/java/com/restaurant/orderservice/controller/`
- `report-service/src/main/java/com/restaurant/reportservice/controller/`
- `src/api/` (TypeScript frontend contracts)

**Validation status: REQUIRES FIXES** ⚠️
