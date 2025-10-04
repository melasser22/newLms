# Shared Data Module - Tenant-Aware Repository Pattern

## Overview

This module provides shared infrastructure for enforcing tenant isolation at the data access layer.

## Key Components

### TenantAwareRepository

Base interface that all tenant-scoped repositories should extend. It offers `*Secure()` variants of
common Spring Data methods that automatically scope queries to the current tenant.

### TenantValidationAspect

Aspect that validates tenant context before any secure repository method executes and logs the use
of unsafe bypass methods.

### DataAutoConfiguration

Auto-configuration that registers the aspect and supporting components when the module is present on
the classpath.

## Usage

1. Extend `TenantAwareRepository` from your repository interfaces.
2. Use `findByIdSecure`, `findAllSecure`, `deleteByIdSecure`, etc., in service layers.
3. Reserve `findAllUnsafe` and `findByIdUnsafe` for platform-wide administration guarded by
   EJADA_OFFICER privileges.

## Security Guarantees

* Requires tenant context via `TenantContextResolver.requireTenantId()`.
* Prevents cross-tenant reads, writes, and deletes by forcing tenant predicates into queries.
* Logs and highlights any usage of unsafe APIs for auditing.

## Testing

Repository tests should set the tenant context via `ContextManager.Tenant.set(tenantId)` before
invoking secure methods and clear the context afterwards.
