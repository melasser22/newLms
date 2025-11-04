# 🚨 Issue: Audit DB insert fails due to hyphenated schema name (`security-service`)

## Summary

`DatabaseSink` يفشل في إدراج سجل التدقيق في الجدول `security-service.audit_logs` بسبب **Syntax Error near "-"** في PostgreSQL عند بناء جملة الـ INSERT بدون اقتباس اسم الـ schema. يتم تنفيذ عملية تسجيل الدخول نفسها بنجاح، وتصل رسالة Kafka `audit_logs`، ويتم تفعيل مسار الـ fallback الذي يحفظ السجل في `audit_outbox`.

## Resolution (Implemented)

- تم تحديث `DatabaseSink` ليقتبس أسماء الـ schema والجدول تلقائيًا مع دعم أسماء تحتوي على شرطات أو أحرف خاصة أخرى.
- تم توحيد تسلسل JSON في مسارات DB و Outbox باستخدام redaction افتراضي للحقول الحساسة (`password`, `accessToken`, `authorization`, `phoneNumber`, `otp`, `token`).
- أُضيف اختبار تكاملي يعتمد على Testcontainers PostgreSQL لتغطية حالة schema باسم يحتوي على شرطة وللتحقق من نجاح الإدراج والـ redaction.
- تم توثيق طريقة تهيئة الـ schema والجدول في README الخاص بـ starter-audit.

## Impact

- فقدان السجل المباشر في جدول التدقيق الأساسي.
- Rollback للمعاملة الخاصة بكتابة الـ audit مع استمرار نجاح الإدراج في outbox.
- وجود مخاطر امتثال ومتابعة لحظية حتى يتم إصلاح المشكلة.

## Steps to Reproduce

1. تنفيذ طلب تسجيل دخول ناجح `POST /api/v1/auth/admin/login` ببيانات صحيحة.
2. مراقبة سجل `DatabaseSink` أو قاعدة البيانات مباشرة.
3. ملاحظة فشل الإدراج:

   ```
   ERROR: syntax error at or near "-"  (SQLSTATE 42601)
   INSERT INTO security-service.audit_logs (...)
   ```
4. التحقق من أن إدراج outbox ينجح:

   ```
   INSERT INTO audit_outbox (...) VALUES (..., 'NEW');
   ```

## Expected Behavior

- إدراج ناجح في `<schema>.audit_logs` بدون أخطاء.
- عدم الحاجة للاعتماد على outbox كبديل في هذا المسار.

## Actual Behavior

- فشل الإدراج المباشر بسبب وجود الشرطة `-` في اسم الـ schema.
- Rollback للمعاملة ثم نجاح الإدراج في `audit_outbox`.

## Key Logs (redacted)

```
ERROR: syntax error at or near "-" Position: 21 (SQLSTATE 42601)
SQL: INSERT INTO security-service.audit_logs (id, ts_utc, ..., payload) VALUES (...)
```

> **ملاحظة:** تم إخفاء الحقول الحساسة مثل كلمات المرور والرموز.

## Root Cause (Hypothesis)

PostgreSQL لا يقبل المعرفات التي تحتوي على `-` بدون اقتباس مزدوج. يتم تكوين جملة SQL حاليًا على النحو التالي:

```sql
INSERT INTO security-service.audit_logs (...)
```

بينما الصيغة الصحيحة يجب أن تكون:

```sql
INSERT INTO "security-service".audit_logs (...)
```

## Proposed Fixes

### Option A (Recommended): Rename schema to snake_case

- تغيير اسم الـ schema إلى `security_service` أو استخدام schema موجود مسبقًا مثل `security`.
- تحديث تعريفات DDL والتكوينات المرافقة.

**Migration (example)**

```sql
-- Create new schema (idempotent)
CREATE SCHEMA IF NOT EXISTS security_service;

-- Move table if it exists under the old schema name
ALTER TABLE IF EXISTS "security-service".audit_logs SET SCHEMA security_service;

-- أو أنشئ الجدول إذا لم يكن موجودًا
CREATE TABLE IF NOT EXISTS security_service.audit_logs (
  id UUID PRIMARY KEY,
  ts_utc TIMESTAMPTZ NOT NULL,
  x_tenant_id VARCHAR NULL,
  actor_id BIGINT NULL,
  actor_username VARCHAR NULL,
  action VARCHAR NOT NULL,
  entity_type VARCHAR NULL,
  entity_id VARCHAR NULL,
  outcome VARCHAR NOT NULL,
  data_class VARCHAR NOT NULL,
  sensitivity VARCHAR NOT NULL,
  resource_path VARCHAR NOT NULL,
  resource_method VARCHAR NOT NULL,
  correlation_id VARCHAR NULL,
  span_id VARCHAR NULL,
  message TEXT NULL,
  payload JSONB NOT NULL
);
```

**Config (example)**

```yaml
audit:
  db:
    schema: security_service
    table: audit_logs
```

### Option B: Keep current name and quote the schema everywhere

- إحاطة الاسم باقتباس مزدوج في SQL المُنشأ:

```java
String rawSchema = props.getSchema(); // e.g. security-service
String qSchema = "\"" + rawSchema.replace("\"","\"\"") + "\""; // safe quoting
String sql = "INSERT INTO " + qSchema + ".audit_logs (id, ts_utc, ..., payload) " +
             "VALUES (?, ?, ..., cast(? as jsonb))";
jdbcTemplate.update(sql, params...);
```

- أو ضبط `search_path` للمستخدم ليشير إلى الـ schema الحالي:

```sql
ALTER USER <app_user> SET search_path TO "security-service", public;
```

> في هذه الحالة يمكن ترك جملة INSERT بدون prefix للـ schema.

## Security/Privacy Hardening (nice-to-have)

- تنفيذ **redaction** للقيم الحساسة قبل النشر أو التخزين مثل `password`, `accessToken`, `authorization`, `phoneNumber`, وغيرها.

```java
private static final Set<String> SENSITIVE_KEYS = Set.of(
  "password","accessToken","authorization","phoneNumber","otp","token"
);
```

- استبدال القيم بـ `"***"` داخل الـ payload أو في السجلات.

## Acceptance Criteria

- [ ] عدم ظهور الخطأ `syntax error at or near "-"` في أي مسار إدراج.
- [ ] إدراج ناجح في الجدول المستهدف (schema/table) مع التحقق عبر اختبار تكاملي.
- [ ] الإبقاء على مسار fallback إلى `audit_outbox` ولكن دون الحاجة له في الحالة العادية.
- [ ] تغطية الوحدة/التكامل للاختبارات التالية:
  - تكوين اسم schema يحتوي على أحرف خاصة.
  - تنقيح (redaction) البيانات الحساسة داخل الـ payload.
- [ ] توثيق التغييرات الخاصة بالترحيل والتكوين في README أو دليل التشغيل.

## Tasks

- [ ] تطبيق خيار **Option A** (إعادة التسمية) أو **Option B** (الاقتباس) في الخدمة المسؤولة عن الإدراج.
- [ ] إضافة اختبار تكاملي يعمل على PostgreSQL حقيقي (Testcontainers).
- [ ] تحديث الإعدادات (`application.yaml`) ونشر الترحيل (migration).
- [ ] مراجعة كود الـ redaction وتفعيله في مسارات الطلب/الاستجابة قبل الإرسال.

## Rollback Plan

- استرجاع الـ schema القديم عند الحاجة والاعتماد مؤقتًا على معالج outbox لحين إعادة المحاولة.
- لا توجد تغييرات غير قابلة للعكس على البيانات.
