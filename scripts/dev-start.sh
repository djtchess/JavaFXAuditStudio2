#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

POSTGRES_DB_VALUE="${POSTGRES_DB:-javafx_audit}"
POSTGRES_USER_VALUE="${POSTGRES_USER:-javafx_audit}"
DB_PASSWORD_VALUE="${DB_PASSWORD:-changeme}"
DB_URL_VALUE="${DB_URL:-jdbc:postgresql://localhost:5432/${POSTGRES_DB_VALUE}}"

cleanup() {
  if [[ -n "${BACKEND_PID:-}" ]]; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

docker compose up -d postgres
(cd backend && DB_URL="$DB_URL_VALUE" DB_USER="$POSTGRES_USER_VALUE" DB_PASSWORD="$DB_PASSWORD_VALUE" ./mvnw spring-boot:run) &
BACKEND_PID=$!
(cd frontend && npm install && npm start)
