#!/bin/sh
set -e

cd /app

export DATABASE_URL="${DATABASE_URL:?DATABASE_URL is required for Beta (PostgreSQL)}"
export APP_ENV="${APP_ENV:-staging}"
export PORT="${PORT:-8000}"

echo "AasPas Beta (Render): running migrations..."
alembic upgrade head

echo "AasPas Beta (Render): starting uvicorn on 0.0.0.0:${PORT} (APP_ENV=${APP_ENV})"
exec uvicorn aaspas.main:app --host 0.0.0.0 --port "${PORT}"
