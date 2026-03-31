#!/usr/bin/env bash
set -euo pipefail

TARGET="${1:-all}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

build_backend() {
  (cd "$ROOT_DIR/backend" && ./mvnw -q -DskipTests package)
}

build_frontend() {
  (cd "$ROOT_DIR/frontend" && npm run build)
}

case "$TARGET" in
  all)
    build_backend
    build_frontend
    ;;
  backend)
    build_backend
    ;;
  frontend)
    build_frontend
    ;;
  *)
    echo "Usage: $0 [all|backend|frontend]" >&2
    exit 1
    ;;
esac
