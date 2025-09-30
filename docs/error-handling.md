# REST Error Handling Strategy

To ensure consistent REST error payloads across microservices the shared libraries now expose
utility components that standardise error codes and the HTTP status returned for each case.

## Error codes

`shared-lib/shared-common` keeps the canonical error codes inside
[`ErrorCodes`](../shared-lib/shared-common/src/main/java/com/ejada/common/constants/ErrorCodes.java).
New authentication-specific codes were introduced for common platform scenarios (invalid
credentials, password history availability issues, data access failures). Services should
re-use these constants instead of hard-coding string literals.

## Status mapping

[`ApiStatusMapper`](../shared-lib/shared-common/src/main/java/com/ejada/common/http/ApiStatusMapper.java)
centralises the mapping between business codes and HTTP statuses. A new
`fromErrorCode` helper resolves the status directly from an error code so that controllers
and exception handlers do not need to duplicate switch statements.

## Building responses

[`RestErrorResponseFactory`](../shared-lib/shared-common/src/main/java/com/ejada/common/http/RestErrorResponseFactory.java)
wraps the mapping logic and returns `ResponseEntity<ErrorResponse>` instances using the
shared mapping. Services can build error responses with a single line whilst guaranteeing
consistent codes/statuses and allowing optional detail lists.

### Example usage

```java
return RestErrorResponseFactory.build(
    ErrorCodes.AUTH_INVALID_CREDENTIALS,
    "Invalid credentials"
);
```

This will automatically render a `401` JSON response with the `ERR-AUTH-INVALID_CREDENTIALS`
code. When the mapping does not contain a code a custom fallback `HttpStatus` can be
provided, otherwise `500` is used.

These utilities are now used by `sec-service` and are available to all other
microservices via the shared libraries.
