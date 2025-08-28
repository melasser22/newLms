# starter-money-time

A pragmatic starter that makes **money** and **time** first‑class:
- JSR-354 (`javax.money`) via **Moneta** with JSON serializers/deserializers.
- Spring **formatters** for `MonetaryAmount`, `Instant`, `Duration`.
- **Clock** and `TimeService` with zone from properties.
- JPA **helpers**: `MoneyEmbeddable` and converters.
- Validation annotations: `@CurrencyAllowed`, `@MoneyMin`, `@MoneyMax`.
- MapStruct helpers.

## Quick use

```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-money-time</artifactId>
  <version>1.0.0</version>
</dependency>
```

```yaml
shared:
  money:
    default-currency: SAR   # or USD, EUR, ...
    locale: en-US
    rounding-mode: HALF_EVEN
    format: AMOUNT_CURRENCY # or CURRENCY_AMOUNT
  time:
    zone: Asia/Riyadh
    business-days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,SUNDAY
    work-start: 09:00
    work-end: 18:00
```

JSON shape for money:
```json
{ "amount": "123.45", "currency": "USD" }
```
