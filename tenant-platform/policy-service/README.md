# Policy Service

Service responsible for evaluating access policies across tenant modules.

## Local Development

Configuration for running the service locally is available in `src/main/resources/application-dev.yaml`.
The service leverages shared starter modules for logging, security and actuator support.

## Build

```bash
mvn clean verify
```
