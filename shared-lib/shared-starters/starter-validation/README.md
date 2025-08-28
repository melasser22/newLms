# starter-validation

Extends Bean Validation with common custom constraints.

## Available Constraints
- `@Trimmed` – ensures strings are trimmed and non-empty.
- `@PhoneE164` – validates phone numbers in E.164 format.
- `@CurrencyCode` – validates ISO-4217 currency codes.

## Usage
Add the dependency:
```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-validation</artifactId>
</dependency>
```
