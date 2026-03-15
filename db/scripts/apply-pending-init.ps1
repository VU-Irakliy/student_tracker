# ============================================================
#  apply-pending-init.ps1 — Apply db/init SQL scripts once
#
#  Usage:
#    .\db\scripts\apply-pending-init.ps1
#
#  Behavior:
#  - Creates studio.schema_migration_history if missing
#  - Runs db/init/*.sql in filename order
#  - Skips scripts already recorded in migration history
# ============================================================

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$container = "studio-postgres"

# Load .env values
$envFile = Join-Path $PSScriptRoot "..\..\.env"
if (-not (Test-Path $envFile)) {
    Write-Error "Could not find .env at $envFile"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+?)\s*=\s*(.+)\s*$') {
        Set-Variable -Name $matches[1] -Value $matches[2]
    }
}

$initDir = Join-Path $PSScriptRoot "..\init"
if (-not (Test-Path $initDir)) {
    Write-Error "Could not find init directory at $initDir"
    exit 1
}

# Ensure container is running
$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne "true") {
    Write-Error "Container '$container' is not running. Start it with: docker compose up -d postgres"
    exit 1
}

# Wait for PostgreSQL to accept connections
$maxAttempts = 30
$attempt = 0
while ($attempt -lt $maxAttempts) {
    docker exec $container pg_isready -U $POSTGRES_USER -d $POSTGRES_DB | Out-Null
    if ($LASTEXITCODE -eq 0) {
        break
    }

    Start-Sleep -Seconds 1
    $attempt++
}

if ($attempt -ge $maxAttempts) {
    Write-Error "PostgreSQL in '$container' is not ready after $maxAttempts seconds."
    exit 1
}

function Invoke-PsqlCommand([string]$sql) {
    docker exec $container psql `
        --username=$POSTGRES_USER `
        --dbname=$POSTGRES_DB `
        --set ON_ERROR_STOP=on `
        --command $sql | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "psql command failed"
    }
}

# Bootstrap migration metadata table
Invoke-PsqlCommand "CREATE SCHEMA IF NOT EXISTS studio;"
Invoke-PsqlCommand @"
CREATE TABLE IF NOT EXISTS studio.schema_migration_history (
    script_name     VARCHAR(255) PRIMARY KEY,
    script_checksum VARCHAR(64)  NOT NULL,
    applied_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
"@

$sqlFiles = Get-ChildItem -Path $initDir -Filter "*.sql" | Sort-Object Name
if (-not $sqlFiles) {
    Write-Host "No SQL scripts found in $initDir" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Applying pending scripts from db/init ..." -ForegroundColor Cyan

foreach ($file in $sqlFiles) {
    $scriptName = $file.Name
    $escapedName = $scriptName.Replace("'", "''")
    $checksum = (Get-FileHash -Algorithm SHA256 -Path $file.FullName).Hash.ToLowerInvariant()

    $alreadyApplied = docker exec $container psql `
        --username=$POSTGRES_USER `
        --dbname=$POSTGRES_DB `
        --tuples-only `
        --no-align `
        --set ON_ERROR_STOP=on `
        --command "SELECT 1 FROM studio.schema_migration_history WHERE script_name = '$escapedName';"

    if ($LASTEXITCODE -ne 0) {
        throw "Failed checking migration history for $scriptName"
    }

    $alreadyAppliedText = if ($null -eq $alreadyApplied) { "" } else { $alreadyApplied.ToString().Trim() }
    if ($alreadyAppliedText -eq "1") {
        Write-Host "  [SKIP] $scriptName" -ForegroundColor DarkGray
        continue
    }

    Write-Host "  [RUN ] $scriptName" -ForegroundColor Green

    docker exec $container psql `
        --username=$POSTGRES_USER `
        --dbname=$POSTGRES_DB `
        --set ON_ERROR_STOP=on `
        --file "/docker-entrypoint-initdb.d/$scriptName" | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Failed applying $scriptName"
    }

    Invoke-PsqlCommand "INSERT INTO studio.schema_migration_history (script_name, script_checksum) VALUES ('$escapedName', '$checksum');"
}

Write-Host ""
Write-Host "Done. Pending scripts are applied." -ForegroundColor Green
Write-Host ""

