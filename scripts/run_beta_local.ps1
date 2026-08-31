# AasPas Beta backend launcher (local validation only - isolated from DEV).
# Starts uvicorn on port 8001 with beta_data/uploads and PostgreSQL from .env.beta.
# Requires Beta Postgres (docker compose -f docker-compose.beta.yml up postgres).
# Does NOT touch dev.db or uploads/.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("start", "stop", "health", "seedadmin")]
    [string]$Action
)

$ErrorActionPreference = "Stop"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-BetaPaths {
    $root = Get-ProjectRoot
    $betaData = Join-Path $root "beta_data"
    $uploads = Join-Path $betaData "uploads"
    $dbFile = Join-Path $betaData "beta.db"
    $envFile = Join-Path $root ".env.beta"
    $pidFile = Join-Path (Join-Path $root "logs") "aaspas-beta.pid"
    return [pscustomobject]@{
        Root     = $root
        Python   = Join-Path $root ".venv\Scripts\python.exe"
        BetaData = $betaData
        Uploads  = $uploads
        DbFile   = $dbFile
        EnvFile  = $envFile
        PidFile  = $pidFile
        Port     = 8001
    }
}

function Ensure-BetaLayout {
    param($Paths)
    foreach ($dir in @((Split-Path $Paths.PidFile), $Paths.BetaData, $Paths.Uploads)) {
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir | Out-Null
        }
    }
    if (-not (Test-Path $Paths.EnvFile)) {
        $example = Join-Path $Paths.Root ".env.beta.example"
        if (-not (Test-Path $example)) {
            throw "Missing .env.beta.example"
        }
        Copy-Item $example $Paths.EnvFile
        Write-Host "Created .env.beta from .env.beta.example - edit SECRET_KEY before cloud deploy."
    }
}

function Import-BetaEnv {
    param($Paths)
    Ensure-BetaLayout -Paths $Paths
    Get-Content $Paths.EnvFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $pair = $_ -split '=', 2
        if ($pair.Count -eq 2) {
            Set-Item -Path "Env:$($pair[0].Trim())" -Value $pair[1].Trim()
        }
    }
    $dbPath = ($Paths.DbFile -replace '\\', '/')
    if ($env:DATABASE_URL) {
        # Respect DATABASE_URL from .env.beta (PostgreSQL for Beta)
    } elseif (Test-Path $Paths.EnvFile) {
        # already loaded above
    } else {
        $env:DATABASE_URL = "postgresql://aaspas_beta:aaspas_beta@localhost:5433/aaspas_beta"
    }
    $env:MEDIA_ROOT = $Paths.Uploads
    $env:APP_ENV = "staging"
    $env:PORT = "$($Paths.Port)"
    if (-not $env:MEDIA_PUBLIC_BASE_URL -or $env:MEDIA_PUBLIC_BASE_URL -like "*REPLACE_AFTER_DEPLOY*") {
        $env:MEDIA_PUBLIC_BASE_URL = "http://127.0.0.1:$($Paths.Port)"
    }
}

function Get-BetaListenerPid {
    param([int]$Port)
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($conns) { return [int]($conns | Select-Object -First 1).OwningProcess }
    return $null
}

function Stop-BetaBackend {
    $paths = Get-BetaPaths
    $stopped = $false
    if (Test-Path $paths.PidFile) {
        $filePid = [int](Get-Content $paths.PidFile | Select-Object -First 1)
        if (Get-Process -Id $filePid -ErrorAction SilentlyContinue) {
            taskkill.exe /PID $filePid /T /F 2>$null | Out-Null
            $stopped = $true
        }
        Remove-Item $paths.PidFile -Force -ErrorAction SilentlyContinue
    }
    $listenPid = Get-BetaListenerPid -Port $paths.Port
    if ($listenPid) {
        taskkill.exe /PID $listenPid /T /F 2>$null | Out-Null
        $stopped = $true
    }
    if ($stopped) { Write-Host "AasPas Beta backend stopped (port $($paths.Port))." }
    else { Write-Host "No Beta backend was running on port $($paths.Port)." }
}

function Invoke-BetaHealth {
    param([int]$Port)
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/health" -UseBasicParsing -TimeoutSec 5
        return [pscustomobject]@{ Ok = ($response.StatusCode -eq 200); Body = $response.Content }
    } catch {
        return [pscustomobject]@{ Ok = $false; Body = $_.Exception.Message }
    }
}

function Start-BetaBackend {
    $paths = Get-BetaPaths
    Set-Location $paths.Root
    if (-not (Test-Path $paths.Python)) {
        throw "Python venv not found: $($paths.Python)"
    }
    Import-BetaEnv -Paths $paths

    $health = Invoke-BetaHealth -Port $paths.Port
    if ($health.Ok) {
        Write-Host "AasPas Beta already running on http://127.0.0.1:$($paths.Port)/health"
        return
    }

    Write-Host "Running alembic migrations for Beta DB..."
    $env:PYTHONPATH = "src"
    & $paths.Python -m alembic upgrade head
    if ($LASTEXITCODE -ne 0) { throw "alembic upgrade head failed" }

    $argList = @(
        "-m", "uvicorn", "aaspas.main:app",
        "--app-dir", "src",
        "--host", "0.0.0.0",
        "--port", "$($paths.Port)"
    )
    Write-Host "Starting Beta backend on port $($paths.Port) (isolated from DEV port 8000)..."
    $proc = Start-Process -FilePath $paths.Python -ArgumentList $argList -WorkingDirectory $paths.Root -PassThru -NoNewWindow
    Set-Content -Path $paths.PidFile -Value $proc.Id -Encoding ascii

    $deadline = (Get-Date).AddSeconds(30)
    do {
        Start-Sleep -Seconds 1
        $health = Invoke-BetaHealth -Port $paths.Port
        if ($health.Ok) {
            Write-Host "AasPas Beta: RUNNING"
            Write-Host "Health: http://127.0.0.1:$($paths.Port)/health"
            Write-Host "DB: $($paths.DbFile)"
            Write-Host "Media: $($paths.Uploads)"
            return
        }
    } while ((Get-Date) -lt $deadline)
    throw "Beta health check failed: $($health.Body)"
}

function Seed-BetaAdmin {
    $paths = Get-BetaPaths
    Import-BetaEnv -Paths $paths
    $env:PYTHONPATH = "src"
    & $paths.Python (Join-Path $paths.Root "scripts\seed_beta_admin.py")
    if ($LASTEXITCODE -ne 0) { throw "seed_beta_admin failed" }
}

switch ($Action) {
    "stop" { Stop-BetaBackend }
    "health" {
        $paths = Get-BetaPaths
        $health = Invoke-BetaHealth -Port $paths.Port
        if ($health.Ok) { Write-Host $health.Body } else { Write-Host $health.Body; exit 1 }
    }
    "seedadmin" { Seed-BetaAdmin }
    default { Start-BetaBackend }
}
