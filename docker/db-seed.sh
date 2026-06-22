#!/bin/sh
set -e

echo "Waiting for Flyway migrations to create the schema..."
timeout_secs=120
elapsed=0

until psql -h postgres -U ktor_user -d ktor_sample -tAc "SELECT CASE WHEN to_regclass('public.users') IS NOT NULL AND to_regclass('public.follows') IS NOT NULL AND to_regclass('public.tags') IS NOT NULL AND to_regclass('public.articles') IS NOT NULL AND to_regclass('public.article_tags') IS NOT NULL AND to_regclass('public.favorites') IS NOT NULL AND to_regclass('public.comments') IS NOT NULL THEN 1 ELSE 0 END" | grep -qx '1'; do
  if [ "$elapsed" -ge "$timeout_secs" ]; then
    echo "Schema not found after ${timeout_secs}s. Start the backend so Flyway migrations can create the tables, then rerun this service."
    exit 1
  fi
  sleep 2
  elapsed=$((elapsed + 2))
done

echo "Loading manual test data..."
psql -h postgres -U ktor_user -d ktor_sample -v ON_ERROR_STOP=1 -f /seed/manual-test-data.sql
echo "Manual test data loaded."
