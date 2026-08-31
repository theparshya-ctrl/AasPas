#!/bin/sh
set -e

cd /app

export DATABASE_URL="${DATABASE_URL:?DATABASE_URL is required for Beta (PostgreSQL)}"
export MEDIA_ROOT="${MEDIA_ROOT:-/data/uploads}"
export APP_ENV="${APP_ENV:-staging}"

mkdir -p "${MEDIA_ROOT}"

echo "AasPas Beta: running migrations (PostgreSQL)..."
alembic upgrade head

echo "AasPas Beta: starting uvicorn on ${HOST:-0.0.0.0}:${PORT:-8000} (APP_ENV=${APP_ENV})"
exec uvicorn aaspas.main:app --host "${HOST:-0.0.0.0}" --port "${PORT:-8000}"
