# lms-Setup
Spring Boot microservice for reference lookups (multi-tenant). Uses Postgres + Redis, integrated with shared-lib starters.

## Database configuration

Provide connection details via `spring.datasource.url` in your `application-*.yaml` or through the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables. If these values are absent or empty, the service now defaults to an in-memory H2 database, enabling the application to start without an external Postgres instance.

## API Testing

Import `LMS_Setup.postman_collection.json` into Postman to explore and test the available endpoints. The collection uses `{{baseUrl}}` (default `http://localhost:8080`) and `{{token}}` variables for configuring requests.
