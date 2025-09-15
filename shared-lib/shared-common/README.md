# shared-common

Reusable utilities, DTOs, constants and exception types used across services.

## Use cases
- Standard API response wrappers (`BaseResponse`, `ErrorResponse`).
- Context propagation helpers and request metadata.
- Utility classes for JSON, dates, strings, sorting and masking.
- Common exception hierarchy for consistent error handling.

## Usage
Add the dependency:

```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>shared-common</artifactId>
</dependency>
```

Example:

```java
BaseResponse<CustomerDto> resp = BaseResponse.success(dto);
String json = JsonUtils.toJson(resp);
```
