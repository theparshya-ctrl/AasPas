# AasPas development backend launcher (CURSOR-024A).
# Start / stop / restart uvicorn without Cursor. Does not change app logic.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("start", "stop", "restart")]
    [string]$Action
)

$ErrorActionPreference = "Stop"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-RuntimePaths {
    $root = Get-ProjectRoot
    $logs = Join-Path $root "logs"
    if (-not (Test-Path $logs)) {
        New-Item -ItemType Directory -Path $logs | Out-Null
    }
    return [pscustomobject]@{
        Root     = $root
        Python   = Join-Path $root ".venv\Scripts\python.exe"
        PidFile  = Join-Path $logs "aaspas-backend.pid"
        Logs     = $logs
        SqliteDb = Join-Path $root "dev.db"
    }
}

function Write-Banner {
    Write-Host ""
    Write-Host "========================================"
    Write-Host " AasPas Backend Launcher"
    Write-Host "========================================"
    Write-Host ""
}

function Get-TailscaleExe {
    $candidates = @(
        (Join-Path $env:ProgramFiles "Tailscale\tailscale.exe"),
        (Join-Path ${env:ProgramFiles(x86)} "Tailscale\tailscale.exe")
    )
    foreach ($path in $candidates) {
        if ($path -and (Test-Path $path)) { return $path }
    }
    $cmd = Get-Command tailscale -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Get-TailscaleStatus {
    $exe = Get-TailscaleExe
    if (-not $exe) {
        return [pscustomobject]@{ Available = $false; Connected = $false; IPv4 = $null; Message = "Tailscale CLI not found." }
    }
    try {
        $ip = (& $exe ip -4 2>$null | Select-Object -First 1)
        $ip = if ($ip) { $ip.ToString().Trim() } else { "" }
        $connected = $ip -match '^100\.'
        return [pscustomobject]@{
            Available = $true
            Connected = $connected
            IPv4      = $(if ($connected) { $ip } else { $null })
            Message   = $(if ($connected) { "Connected" } else { "Tailscale CLI found but IPv4 is not available." })
        }
    } catch {
        return [pscustomobject]@{ Available = $true; Connected = $false; IPv4 = $null; Message = $_.Exception.Message }
    }
}

function Show-Tailscale {
    $ts = Get-TailscaleStatus
    Write-Host "Tailscale:"
    if ($ts.Connected) {
        Write-Host "Connected"
        Write-Host "IP: $($ts.IPv4)"
        Write-Host ""
        Write-Host "Phone API:"
        Write-Host "http://$($ts.IPv4):8000/"
        Write-Host ""
        Write-Host "Health:"
        Write-Host "http://$($ts.IPv4):8000/health"
    } else {
        Write-Host "WARNING:"
        Write-Host "Tailscale is not available."
        Write-Host "Local backend will still start, but remote phone access may not work."
        if ($ts.Message) { Write-Host $ts.Message }
    }
    Write-Host ""
    return $ts
}

function Show-FirewallStatus {
    Write-Host "Firewall:"
    try {
        $rule = Get-NetFirewallRule -DisplayName "AasPas FastAPI Tailscale TCP 8000" -ErrorAction Stop
        $enabled = @($rule | Where-Object { $_.Enabled -eq "True" -or $_.Enabled -eq $true })
        if ($enabled.Count -gt 0) {
            Write-Host "CURSOR-024 rule is present and enabled (Private / Tailscale)."
        } else {
            Write-Host "CURSOR-024 rule was found but is not enabled."
        }
    } catch {
        Write-Host "Could not query the CURSOR-024 rule (skipped)."
    }
    Write-Host "Do not port-forward TCP 8000 on the router. Tailscale is the private path."
    Write-Host ""
}

function Get-ListenerPidOn8000 {
    $conns = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
    if ($conns) { return [int]($conns | Select-Object -First 1).OwningProcess }
    return $null
}

function Get-AasPasUvicornProcesses {
    $paths = Get-RuntimePaths
    $venvPython = [System.IO.Path]::GetFullPath($paths.Python)
    Get-CimInstance Win32_Process -Filter "Name='python.exe' OR Name='pythonw.exe'" -ErrorAction SilentlyContinue | Where-Object {
        $cmd = $_.CommandLine
        if (-not $cmd) { return $false }
        if ($cmd -notmatch 'uvicorn\s+aaspas\.main:app') { return $false }
        $exe = $_.ExecutablePath
        if ($exe) {
            try {
                return ([System.IO.Path]::GetFullPath($exe)) -ieq $venvPython
            } catch {
                return $false
            }
        }
        return $cmd -like "*$($paths.Python)*"
    }
}

function Read-PidFile {
    $paths = Get-RuntimePaths
    if (-not (Test-Path $paths.PidFile)) { return $null }
    $raw = (Get-Content $paths.PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ($raw -match '^\d+$') { return [int]$raw }
    return $null
}

function Test-ProcessId {
    param([int]$ProcessId)
    try {
        $p = Get-Process -Id $ProcessId -ErrorAction Stop
        return $null -ne $p
    } catch {
        return $false
    }
}

function Invoke-HealthCheck {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:8000/health" -UseBasicParsing -TimeoutSec 5
        $content = [string]$response.Content
        $ok = ($response.StatusCode -eq 200) -and $content.Contains('ok') -and $content.Contains('success')
        return [pscustomobject]@{
            Ok      = [bool]$ok
            Status  = [int]$response.StatusCode
            Body    = $response.Content
            Message = $null
        }
    } catch {
        return [pscustomobject]@{
            Ok      = $false
            Status  = 0
            Body    = $null
            Message = $_.Exception.Message
        }
    }
}

function Invoke-DatabaseMigrations {
    param($Paths)
    Write-Host "Migrations: upgrading database schema..."
    $prevPyPath = $env:PYTHONPATH
    $env:PYTHONPATH = "src"
    try {
        $output = & $Paths.Python -m alembic upgrade head 2>&1
        $exitCode = $LASTEXITCODE
        foreach ($line in $output) { Write-Host $line }
        if ($exitCode -ne 0) {
            Write-Host "WARNING: alembic upgrade head failed (exit $exitCode)."
            return $false
        }
        Write-Host "Migrations: up to date."
        return $true
    } catch {
        Write-Host "WARNING: could not run migrations: $($_.Exception.Message)"
        return $false
    } finally {
        if ($null -eq $prevPyPath) {
            Remove-Item Env:PYTHONPATH -ErrorAction SilentlyContinue
        } else {
            $env:PYTHONPATH = $prevPyPath
        }
    }
}

function Set-DevDatabaseEnv {
    param($Paths)
    if ($env:DATABASE_URL) {
        Write-Host "Database: using DATABASE_URL from the environment"
        return
    }
    $pg = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object { $_.Status -eq "Running" }
    if ($pg) {
        Write-Host "Database: PostgreSQL service detected (using app defaults / .env)"
        return
    }
    $dbPath = ($Paths.SqliteDb -replace '\\', '/')
    $env:DATABASE_URL = "sqlite:///$dbPath"
    if (-not $env:APP_ENV) { $env:APP_ENV = "development" }
    Write-Host "Database: sqlite"
    Write-Host $Paths.SqliteDb
    Write-Host "PostgreSQL not detected. Later this launcher can also start PostgreSQL."
}

function Stop-PidTree {
    param([int]$ProcessId, [string]$Reason)
    if (-not (Test-ProcessId $ProcessId)) { return $false }
    Write-Host "Stopping PID $ProcessId ($Reason)"
    & taskkill.exe /PID $ProcessId /T /F 2>$null | Out-Null
    Start-Sleep -Milliseconds 400
    return $true
}

function Stop-AasPasBackend {
    $paths = Get-RuntimePaths
    $stopped = @()

    $filePid = Read-PidFile
    if ($filePid) {
        if (Stop-PidTree -ProcessId $filePid -Reason "PID file") {
            $stopped += $filePid
        } else {
            Write-Host "PID file pointed to $filePid, but that process is not running."
        }
    }

    $remaining = @(Get-AasPasUvicornProcesses)
    foreach ($proc in $remaining) {
        if ($stopped -contains [int]$proc.ProcessId) { continue }
        if (Stop-PidTree -ProcessId ([int]$proc.ProcessId) -Reason "venv uvicorn aaspas.main:app") {
            $stopped += [int]$proc.ProcessId
        }
    }

    # Leftover AasPas uvicorn on :8000 (for example started from Cursor, not this launcher).
    $listenPid = Get-ListenerPidOn8000
    if ($listenPid -and ($stopped -notcontains $listenPid)) {
        $listenProc = Get-CimInstance Win32_Process -Filter "ProcessId=$listenPid" -ErrorAction SilentlyContinue
        if ($listenProc -and $listenProc.CommandLine -match 'uvicorn\s+aaspas\.main:app') {
            if (Stop-PidTree -ProcessId $listenPid -Reason "port 8000 uvicorn aaspas.main:app") {
                $stopped += $listenPid
            }
        }
    }

    if (Test-Path $paths.PidFile) {
        Remove-Item $paths.PidFile -Force -ErrorAction SilentlyContinue
    }

    if ($stopped.Count -eq 0) {
        Write-Host "No AasPas uvicorn process was running."
        return 0
    }

    $still = @(Get-AasPasUvicornProcesses)
    if ($still.Count -gt 0) {
        Write-Host "ERROR: These AasPas uvicorn PIDs are still running: $($still.ProcessId -join ', ')"
        return 1
    }

    Write-Host "AasPas Backend: STOPPED"
    Write-Host "Stopped PID(s): $($stopped -join ', ')"
    return 0
}

function Wait-ForHealth {
    param([int]$TimeoutSeconds = 40)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = $null
    while ((Get-Date) -lt $deadline) {
        $last = Invoke-HealthCheck
        if ($last.Ok) { return $last }
        Start-Sleep -Seconds 1
    }
    return $last
}

function Start-AasPasBackend {
    Write-Banner
    $paths = Get-RuntimePaths
    Set-Location $paths.Root

    Write-Host "Project: $($paths.Root)"
    if (-not (Test-Path $paths.Python)) {
        Write-Host "ERROR: Python virtual environment not found:"
        Write-Host $paths.Python
        Write-Host "Create it with: python -m venv .venv"
        return 1
    }
    Write-Host "Python: $($paths.Python)"
    Write-Host ""

    $ts = Show-Tailscale
    if ($ts.Connected) {
        $env:MEDIA_PUBLIC_BASE_URL = "http://$($ts.IPv4):8000"
        Write-Host "Media public base: $env:MEDIA_PUBLIC_BASE_URL"
    }
    Show-FirewallStatus
    Set-DevDatabaseEnv -Paths $paths
    Invoke-DatabaseMigrations -Paths $paths | Out-Null
    Write-Host ""

    $existingHealth = Invoke-HealthCheck
    $existingPid = Get-ListenerPidOn8000
    $trackedPid = Read-PidFile

    if ($existingHealth.Ok) {
        Write-Host "AasPas Backend: RUNNING"
        Write-Host "Already listening on port 8000; health check passed."
        if ($trackedPid) { Write-Host "Tracked PID: $trackedPid" }
        elseif ($existingPid) { Write-Host "Listener PID: $existingPid" }
        Write-Host ""
        Write-Host "Use Stop-AasPas-Backend.bat to stop it."
        Write-Host ""
        if ($Host.Name -eq "ConsoleHost") {
            Read-Host "Press Enter to close this window"
        }
        return 0
    }

    if ($existingPid) {
        Write-Host "AasPas Backend: FAILED"
        Write-Host "Port 8000 is in use by PID $existingPid, but /health did not succeed."
        if ($existingHealth.Message) { Write-Host $existingHealth.Message }
        Write-Host "Stop that process, or run Stop-AasPas-Backend.bat if it is an old AasPas server."
        return 1
    }

    $env:PYTHONPATH = "src"
    $argList = @(
        "-m", "uvicorn", "aaspas.main:app",
        "--reload",
        "--app-dir", "src",
        "--host", "0.0.0.0",
        "--port", "8000"
    )

    Write-Host "Starting FastAPI:"
    Write-Host "$($paths.Python) -m uvicorn aaspas.main:app --reload --app-dir src --host 0.0.0.0 --port 8000"
    Write-Host ""

    try {
        $proc = Start-Process -FilePath $paths.Python -ArgumentList $argList -WorkingDirectory $paths.Root -PassThru -NoNewWindow
    } catch {
        Write-Host "AasPas Backend: FAILED"
        Write-Host $_.Exception.Message
        return 1
    }

    Set-Content -Path $paths.PidFile -Value $proc.Id -Encoding ascii
    Write-Host "Started PID $($proc.Id) (PID file: $($paths.PidFile))"
    Write-Host "Waiting for http://127.0.0.1:8000/health ..."
    Write-Host ""

    $health = Wait-ForHealth -TimeoutSeconds 40
    if (-not $health.Ok) {
        Write-Host "AasPas Backend: FAILED"
        Write-Host "Process started, but the health endpoint did not succeed."
        if ($health.Status) { Write-Host "HTTP status: $($health.Status)" }
        if ($health.Body) { Write-Host $health.Body }
        if ($health.Message) { Write-Host $health.Message }
        Stop-PidTree -ProcessId $proc.Id -Reason "health check failed" | Out-Null
        Remove-Item $paths.PidFile -Force -ErrorAction SilentlyContinue
        return 1
    }

    Write-Host ""
    Write-Host "AasPas Backend: RUNNING"
    Write-Host "Local health: http://127.0.0.1:8000/health"
    Write-Host "API docs:     http://127.0.0.1:8000/docs"
    Write-Host "PID:          $($proc.Id)"
    Write-Host ""
    if ($ts.Connected) {
        Write-Host "Phone API:"
        Write-Host "http://$($ts.IPv4):8000/"
        Write-Host ""
        Write-Host "Health:"
        Write-Host "http://$($ts.IPv4):8000/health"
        Write-Host ""
    }
    Write-Host "Logs stay in this window. PID file: logs/aaspas-backend.pid"
    Write-Host "Delete a stale PID file if the process was killed outside this launcher."
    Write-Host "Press Ctrl+C in this window, or run Stop-AasPas-Backend.bat, to stop."
    Write-Host ""

    try {
        Wait-Process -Id $proc.Id
        $code = $proc.ExitCode
        Write-Host ""
        Write-Host "AasPas backend process exited (code $code)."
    } finally {
        if ((Read-PidFile) -eq $proc.Id) {
            Remove-Item $paths.PidFile -Force -ErrorAction SilentlyContinue
        }
    }
    return 0
}

switch ($Action) {
    "stop" { exit (Stop-AasPasBackend) }
    "restart" {
        Write-Banner
        Write-Host "Restarting AasPas backend..."
        Write-Host ""
        $stopCode = Stop-AasPasBackend
        if ($stopCode -ne 0) { exit $stopCode }
        Start-Sleep -Seconds 2
        exit (Start-AasPasBackend)
    }
    default { exit (Start-AasPasBackend) }
}
