# Show connected devices and installed AasPas app version.
$ErrorActionPreference = "Stop"

function Get-AdbExe {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $adb = Join-Path $sdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) { throw "adb not found at $adb" }
    return $adb
}

function Get-DeviceLabel {
    param([string]$Info)
    $lower = $Info.ToLowerInvariant()
    if ($lower -match 'poco|beryllium') { return "POCO F1" }
    if ($lower -match 'samsung|sm-|beyond|dm3|s23|s24|s25') { return "Samsung S23" }
    if ($Info -match 'model:(\S+)') { return $matches[1] }
    return "Android device"
}

function Get-InstalledPackageInfo {
    param([string]$Adb, [string]$Serial)
    $dump = & $adb -s $Serial shell dumpsys package com.aaspas.customer 2>$null
    if (-not $dump) {
        return [pscustomobject]@{
            Installed = $false
            Label = "-"
            VersionName = "-"
            VersionCode = "-"
            Environment = "-"
        }
    }
    $versionName = "-"
    $versionCode = "-"
    foreach ($line in $dump) {
        if ($line -match 'versionName=([^\s]+)') { $versionName = $matches[1] }
        if ($line -match 'versionCode=(\d+)') { $versionCode = $matches[1] }
    }
    $label = "AasPas"
    foreach ($line in $dump) {
        if ($line -match 'applicationInfo=ApplicationInfo\{.* labelRes=.*\}') { break }
    }
    $envDump = & $adb -s $Serial shell dumpsys package com.aaspas.customer 2>$null | Out-String
    $environment = if ($envDump -match 'AasPas DEV') { "DEV" } else { "DEV/PROD unknown" }
    return [pscustomobject]@{
        Installed = $true
        Label = "AasPas DEV"
        VersionName = $versionName
        VersionCode = $versionCode
        Environment = $environment
    }
}

$adb = Get-AdbExe
$lines = & $adb devices -l 2>&1
$devices = @()
foreach ($line in $lines) {
    if ($line -match '^(\S+)\s+device\s+(.*)$') {
        $devices += [pscustomobject]@{
            Serial = $matches[1]
            Info = $matches[2]
        }
    }
}

Write-Host ""
Write-Host "AasPas Device Versions"
Write-Host "----------------------"
if ($devices.Count -eq 0) {
    Write-Host "No devices connected."
    exit 0
}

foreach ($device in $devices) {
    $label = Get-DeviceLabel -Info $device.Info
    $pkg = Get-InstalledPackageInfo -Adb $adb -Serial $device.Serial
    Write-Host ""
    Write-Host $label
    Write-Host $device.Serial
    if (-not $pkg.Installed) {
        Write-Host "AasPas not installed"
        continue
    }
    Write-Host $pkg.Label
    Write-Host "v$($pkg.VersionName)"
    Write-Host "build $($pkg.VersionCode)"
    Write-Host $pkg.Environment
}

Write-Host ""
