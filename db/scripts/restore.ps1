# ============================================================
#  restore.ps1 — Restore a backup into the studio schema
#
#  Usage:
#    .\db\scripts\restore.ps1 -File "db\backups\studio_20260311_140000.sql"
#    .\db\scripts\restore.ps1              # picks the most recent backup
#
#  The script drops and recreates the studio schema, then loads
#  the SQL dump, so the result is an exact copy of the backup.
# ============================================================

param(
    [string]$File = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Load variables from .env ─────────────────────────────────
$envFile = Join-Path $PSScriptRoot "..\..\..env"
if (-not (Test-Path $envFile)) {
    Write-Error "Could not find .env at $envFile"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+?)\s*=\s*(.+)\s*$') {
        Set-Variable -Name $matches[1] -Value $matches[2]
    }
}

# ── Resolve the backup file ─────────────────────────────────
$backupsDir = Join-Path $PSScriptRoot "..\backups"

if ($File) {
    $backupFile = $File
} else {
    # Pick the newest .sql file in the backups folder
    $latest = Get-ChildItem -Path $backupsDir -Filter "*.sql" |
              Sort-Object LastWriteTime -Descending |
              Select-Object -First 1

    if (-not $latest) {
        Write-Error "No backup files found in $backupsDir"
        exit 1
    }
    $backupFile = $latest.FullName
}

if (-not (Test-Path $backupFile)) {
    Write-Error "Backup file not found: $backupFile"
    exit 1
}

# ── Configuration ────────────────────────────────────────────
$container = "studio-postgres"
$schema    = "studio"

# ── Confirm ──────────────────────────────────────────────────
Write-Host ""
Write-Host "  Restoring schema '$schema' ..." -ForegroundColor Yellow
Write-Host "  Container : $container"
Write-Host "  Database  : $POSTGRES_DB"
Write-Host "  Source    : $backupFile"
Write-Host ""
Write-Host "  WARNING: This will DROP and recreate the '$schema' schema." -ForegroundColor Red
Write-Host ""

$confirm = Read-Host "  Type YES to continue"
if ($confirm -ne "YES") {
    Write-Host "  Aborted." -ForegroundColor DarkGray
    exit 0
}

# ── Pipe the SQL dump into psql inside the container ─────────
Write-Host ""
Write-Host "  Restoring ..." -ForegroundColor Cyan

Get-Content $backupFile -Raw |
    docker exec -i $container psql `
        --username=$POSTGRES_USER `
        --dbname=$POSTGRES_DB `
        --single-transaction `
        --set ON_ERROR_STOP=on

if ($LASTEXITCODE -ne 0) {
    Write-Error "Restore failed (exit code $LASTEXITCODE)."
    exit 1
}

Write-Host ""
Write-Host "  Restore complete." -ForegroundColor Green
Write-Host ""

