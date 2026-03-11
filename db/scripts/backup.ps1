# ============================================================
#  backup.ps1 — Dump the studio schema to a timestamped file
#
#  Usage:
#    .\db\scripts\backup.ps1              # default backup
#    .\db\scripts\backup.ps1 -Tag "pre-migration"  # custom label
#
#  Output: db/backups/studio_<timestamp>[_<tag>].sql
# ============================================================

param(
    [string]$Tag = ""
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

# ── Resolve paths ────────────────────────────────────────────
$backupsDir = Join-Path $PSScriptRoot "..\backups"
if (-not (Test-Path $backupsDir)) {
    New-Item -ItemType Directory -Path $backupsDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$fileName  = if ($Tag) { "studio_${timestamp}_${Tag}.sql" } else { "studio_${timestamp}.sql" }
$backupFile = Join-Path $backupsDir $fileName

# ── Configuration ────────────────────────────────────────────
$container = "studio-postgres"
$schema    = "studio"

# ── Run pg_dump inside the container ─────────────────────────
Write-Host ""
Write-Host "  Backing up schema '$schema' ..." -ForegroundColor Cyan
Write-Host "  Container : $container"
Write-Host "  Database  : $POSTGRES_DB"
Write-Host "  Output    : $backupFile"
Write-Host ""

docker exec $container pg_dump `
    --username=$POSTGRES_USER `
    --dbname=$POSTGRES_DB `
    --schema=$schema `
    --clean `
    --if-exists `
    --no-owner `
    --no-privileges `
    --format=plain `
    > $backupFile

if ($LASTEXITCODE -ne 0) {
    Write-Error "Backup failed (exit code $LASTEXITCODE)."
    exit 1
}

$size = (Get-Item $backupFile).Length
$sizeKB = [math]::Round($size / 1KB, 1)
Write-Host "  Done — $sizeKB KB written." -ForegroundColor Green
Write-Host ""

