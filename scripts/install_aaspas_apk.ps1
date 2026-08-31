# Install releases/current APK onto a specific physical device profile.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("POCO", "Samsung")]
    [string]$DeviceProfile,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-AdbExe {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk) {
        $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
    $adb = Join-Path $sdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) {
        throw "adb not found at $adb"
    }
    return $adb
}

function Get-ConnectedDevices {
    param([string]$Adb)
    $lines = & $Adb devices -l 2>&1
    $devices = @()
    foreach ($line in $lines) {
        if ($line -match '^(\S+)\s+device\s+(.*)$') {
            $devices += [pscustomobject]@{
                Serial = $matches[1]
                Info   = $matches[2]
            }
        }
    }
    return $devices
}

function Test-DeviceProfile {
    param([string]$Info, [string]$Profile)
    $lower = $Info.ToLowerInvariant()
    switch ($Profile) {
        "POCO" {
            return ($lower -match 'poco|beryllium')
        }
        "Samsung" {
            return ($lower -match 'samsung|sm-|beyond|dm3|dm1|dm2|r8q|r9q|r11q|a52|a53|a54|a55|s23|s24|s25') -and
                ($lower -notmatch 'poco|beryllium')
        }
    }
    return $false
}

function Get-ReleaseMetadata {
    param([string]$ApkPath)
    $aapt = Get-ChildItem -Path (Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools") -Recurse -Filter "aapt.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($aapt) {
        $badging = & $aapt.FullName dump badging $ApkPath 2>$null
        $versionName = ($badging | Select-String "versionName='([^']+)'").Matches.Groups[1].Value
        $versionCode = ($badging | Select-String "versionCode='([^']+)'").Matches.Groups[1].Value
        $appId = ($badging | Select-String "package: name='([^']+)'").Matches.Groups[1].Value
        return [pscustomobject]@{
            VersionName = $versionName
            VersionCode = $versionCode
            ApplicationId = $appId
        }
    }
    return $null
}

$root = Get-ProjectRoot
$currentDir = Join-Path $root "releases\current"
$versioned = @(Get-ChildItem -Path $currentDir -Filter "AasPas-*.apk" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -ne "latest.apk" })
if ($versioned.Count -eq 0) {
    throw "No current APK found in $currentDir. Run Build-AasPas-APK.bat first."
}
$apk = $versioned | Sort-Object {
    if ($_.Name -match '-build-(\d+)\.apk$') { [int]$matches[1] } else { 0 }
} -Descending | Select-Object -First 1
$latest = Join-Path $currentDir "latest.apk"
if (-not (Test-Path $latest)) {
    Copy-Item -LiteralPath $apk.FullName -Destination $latest -Force
}

$adb = Get-AdbExe
$allDevices = Get-ConnectedDevices -Adb $adb
if ($allDevices.Count -eq 0) {
    throw "No adb devices connected."
}

$matches = @($allDevices | Where-Object { Test-DeviceProfile -Info $_.Info -Profile $DeviceProfile })
if ($matches.Count -eq 0) {
    throw "No connected device matched profile '$DeviceProfile'. Connected: $($allDevices.Count)"
}

$target = $null
if ($matches.Count -eq 1) {
    $target = $matches[0]
} else {
    Write-Host "Multiple devices matched $DeviceProfile profile:"
    for ($i = 0; $i -lt $matches.Count; $i++) {
        Write-Host "[$i] $($matches[$i].Serial) $($matches[$i].Info)"
    }
    $choice = Read-Host "Enter device index"
    if ($choice -notmatch '^\d+$' -or [int]$choice -ge $matches.Count) {
        throw "Invalid device selection."
    }
    $target = $matches[[int]$choice]
}

$meta = Get-ReleaseMetadata -ApkPath $apk.FullName
$displayName = if ($DeviceProfile -eq "POCO") { "POCO F1" } else { "Samsung S23" }

Write-Host ""
Write-Host "AasPas APK Install"
Write-Host "------------------"
Write-Host "Device:"
Write-Host "$displayName ($($target.Serial))"
Write-Host ""
Write-Host "APK:"
Write-Host $apk.Name
Write-Host ""
if ($meta) {
    Write-Host "Version:"
    Write-Host "v$($meta.VersionName)"
    Write-Host ""
    Write-Host "Build:"
    Write-Host $meta.VersionCode
    Write-Host ""
    Write-Host "Application ID:"
    Write-Host $meta.ApplicationId
    Write-Host ""
}

if (-not $Force) {
    $confirm = Read-Host "Install on this device? (Y/N)"
    if ($confirm -notin @("Y", "y", "Yes", "yes")) {
        Write-Host "Install cancelled."
        exit 0
    }
}

Write-Host "Installing (app data preserved)..."
& $adb -s $target.Serial install -r $latest
if ($LASTEXITCODE -ne 0) {
    throw "adb install failed with exit code $LASTEXITCODE"
}
Write-Host "Install succeeded."
