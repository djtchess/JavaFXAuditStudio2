#!/usr/bin/env bash
set -euo pipefail

TARGET="${1:-all}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

test_backend() {
  (cd "$ROOT_DIR/backend" && ./mvnw test)
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
  frontend)
    test_frontend
    ;;
  *)
    echo "Usage: $0 [all|backend|frontend]" >&2
    exit 1
    ;;
esac
