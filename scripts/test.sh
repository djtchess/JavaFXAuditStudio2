#!/usr/bin/env bash
set -euo pipefail

TARGET="${1:-all}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

get_maven_repo_local() {
  if [[ -n "${MAVEN_REPO_LOCAL:-}" ]]; then
    printf '%s\n' "$MAVEN_REPO_LOCAL"
    return
  fi
  printf '%s\n' "$ROOT_DIR/.m2/repository"
}

test_backend() {
  local maven_repo_local
  maven_repo_local="$(get_maven_repo_local)"
  (cd "$ROOT_DIR/backend" && ./mvnw "-Dmaven.repo.local=$maven_repo_local" test)
}

test_backend_postgres_smoke() {
  local maven_repo_local
  maven_repo_local="$(get_maven_repo_local)"
  (
    cd "$ROOT_DIR"
    docker compose up -d postgres
  )
  (
    cd "$ROOT_DIR/backend"
    CI_POSTGRES_ENABLED=true \
    DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/javafx_audit}" \
    DB_USER="${DB_USER:-javafx_audit}" \
    DB_PASSWORD="${DB_PASSWORD:-changeme}" \
    ./mvnw "-Dmaven.repo.local=$maven_repo_local" -Dtest=ff.ss.javaFxAuditStudio.integration.PostgresServiceContainerIT test
  )
}

test_frontend() {
  (cd "$ROOT_DIR/frontend" && npm test)
}

case "$TARGET" in
  all)
    test_backend
    test_frontend
    ;;
  backend)
    test_backend
    ;;
  backend-postgres-smoke)
    test_backend_postgres_smoke
    ;;
  frontend)
    test_frontend
    ;;
  *)
    echo "Usage: $0 [all|backend|backend-postgres-smoke|frontend]" >&2
    exit 1
    ;;
esac
