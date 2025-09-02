# starter-data

JPA and data-layer helpers with multi-tenancy support.

## Use cases
- Base entity classes (`BaseEntity`, `SoftDeleteEntity`).
- Tenant filtering via Hibernate and annotations.
- Pagination helpers for API responses.
- Auto configuration for clock and tenant properties.

## Usage
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-data</artifactId>
</dependency>
```

Entities can extend `BaseEntity` and use `@TenantScoped` to apply tenant filters.
