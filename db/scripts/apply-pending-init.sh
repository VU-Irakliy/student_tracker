#!/bin/sh
# Apply db/init SQL scripts once per checksum using studio.schema_migration_history.

set -eu

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"

export PGPASSWORD="$POSTGRES_PASSWORD"

max_attempts=60
attempt=0
while [ "$attempt" -lt "$max_attempts" ]; do
  if pg_isready -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 1
done

if [ "$attempt" -ge "$max_attempts" ]; then
  echo "PostgreSQL at $POSTGRES_HOST is not ready after ${max_attempts}s." >&2
  exit 1
fi

psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 <<'SQL'
CREATE SCHEMA IF NOT EXISTS studio;
CREATE TABLE IF NOT EXISTS studio.schema_migration_history (
    script_name     VARCHAR(255) PRIMARY KEY,
    script_checksum VARCHAR(64)  NOT NULL,
    applied_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
SQL

sql_files=$(find /docker-entrypoint-initdb.d -maxdepth 1 -type f -name '*.sql' | LC_ALL=C sort)
if [ -z "$sql_files" ]; then
  echo "No SQL scripts found in /docker-entrypoint-initdb.d"
  exit 0
fi

echo "Applying pending scripts from db/init ..."
for file in $sql_files; do
  script_name=$(basename "$file")
  escaped_name=$(printf "%s" "$script_name" | sed "s/'/''/g")
  checksum=$(sha256sum "$file" | awk '{print $1}')

  existing_checksum=$(psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tA -v ON_ERROR_STOP=1 \
    -c "SELECT script_checksum FROM studio.schema_migration_history WHERE script_name = '$escaped_name';")

  if [ -n "$existing_checksum" ]; then
    if [ "$existing_checksum" = "$checksum" ]; then
      echo "  [SKIP] $script_name"
      continue
    fi
    echo "  [SKIP] $script_name (already applied; checksum differs from current file)"
    continue
  fi

  echo "  [RUN ] $script_name"
  psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -f "$file"

  psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
    -c "INSERT INTO studio.schema_migration_history (script_name, script_checksum) VALUES ('$escaped_name', '$checksum');"
done

echo "Done. Pending scripts are applied."

