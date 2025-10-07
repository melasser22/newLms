#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${1:-http://localhost:8000}
SERVICES=(
  setup-service
  tenant-service
  catalog-service
  subscription-service
  billing-service
  analytics-service
  security-service
)

function log_header() {
  printf '\n\033[1;34m==> %s\033[0m\n' "$1"
}

function log_step() {
  printf '\033[1;32m  - %s\033[0m\n' "$1"
}

log_header "Gateway health"
log_step "GET ${BASE_URL}/actuator/health"
curl --fail --show-error --silent "${BASE_URL}/actuator/health" | jq '.' 2>/dev/null || curl --fail --show-error --silent "${BASE_URL}/actuator/health"

log_header "Registered routes"
log_step "GET ${BASE_URL}/actuator/gateway/routes"
ROUTES=$(curl --fail --show-error --silent "${BASE_URL}/actuator/gateway/routes")
if command -v jq >/dev/null 2>&1; then
  printf '%s\n' "$ROUTES" | jq '.'
else
  printf '%s\n' "$ROUTES"
fi

log_header "Service DNS resolution"
for service in "${SERVICES[@]}"; do
  if getent hosts "$service" >/dev/null 2>&1; then
    ip=$(getent hosts "$service" | awk '{print $1; exit}')
    log_step "${service} -> ${ip}"
  else
    printf '\033[1;31m  - %s not resolvable (check Docker network)\033[0m\n' "$service"
  fi
done

log_header "Sample route probes"
TENANT_STATUS=$(curl --write-out '%{http_code}' --silent --output /dev/null "${BASE_URL}/api/v1/tenants/actuator/health")
log_step "Tenants actuator status: ${TENANT_STATUS}"
CATALOG_STATUS=$(curl --write-out '%{http_code}' --silent --output /dev/null "${BASE_URL}/api/v1/catalog/actuator/health")
log_step "Catalog actuator status: ${CATALOG_STATUS}"

printf '\n\033[1;34mDiagnostics complete.\033[0m\n'
