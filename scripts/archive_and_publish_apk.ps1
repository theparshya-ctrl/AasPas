# Archive previous current APK and publish a freshly built APK to releases/current.
param(
    [ValidateSet("Debug", "Beta", "Release")]
    [string]$BuildType = "Debug",
    [string]$SourceApk = ""
)

$ErrorActionPreference = "Stop"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-GradleVersionInfo {
    param([string]$Root)
    $gradleFile = Join-Path $Root "android\app\build.gradle.kts"
    if (-not (Test-Path $gradleFile)) {
        throw "Gradle file not found: $gradleFile"
    }
    $content = Get-Content $gradleFile -Raw
    $versionCode = $null
    $versionName = $null
    if ($content -match 'versionCode\s*=\s*(\d+)') { $versionCode = [int]$matches[1] }
    if ($content -match 'versionName\s*=\s*"([^"]+)"') { $versionName = $matches[1] }
    if ($null -eq $versionCode -or [string]::IsNullOrWhiteSpace($versionName)) {
        throw "Could not read versionCode/versionName from $gradleFile"
    }
    return [pscustomobject]@{
        VersionCode = $versionCode
        VersionName = $versionName
    }
}

function Get-DefaultSourceApk {
    param([string]$Root, [string]$Type)
    switch ($Type) {
        "Release" { return Join-Path $Root "android\app\build\outputs\apk\release\app-release.apk" }
        "Beta" { return Join-Path $Root "android\app\build\outputs\apk\beta\app-beta.apk" }
        default { return Join-Path $Root "android\app\build\outputs\apk\debug\app-debug.apk" }
    }
}

$root = Get-ProjectRoot
$version = Get-GradleVersionInfo -Root $root
$environment = switch ($BuildType) {
    "Release" { "PROD" }
    "Beta" { "BETA" }
    default { "DEV" }
}
$prefix = switch ($BuildType) {
    "Release" { "AasPas-PROD" }
    "Beta" { "AasPas-BETA" }
    default { "AasPas-DEV" }
}
$fileName = "$prefix-v$($version.VersionName)-build-$($version.VersionCode).apk"

if ([string]::IsNullOrWhiteSpace($SourceApk)) {
    $SourceApk = Get-DefaultSourceApk -Root $root -Type $BuildType
}
$SourceApk = (Resolve-Path -LiteralPath $SourceApk).Path

$releasesRoot = Join-Path $root "releases"
$currentDir = Join-Path $releasesRoot "current"
$archiveDir = Join-Path $releasesRoot "archive"
New-Item -ItemType Directory -Path $currentDir -Force | Out-Null
New-Item -ItemType Directory -Path $archiveDir -Force | Out-Null

$targetPath = Join-Path $currentDir $fileName
$latestPath = Join-Path $currentDir "latest.apk"
$archivedAny = @()

foreach ($existing in @(Get-ChildItem -Path $currentDir -Filter "*.apk" -File -ErrorAction SilentlyContinue)) {
    if ($existing.Name -eq "latest.apk") { continue }
    if ($existing.Name -eq $fileName) { continue }
    $archiveTarget = Join-Path $archiveDir $existing.Name
    if (-not (Test-Path $archiveTarget)) {
        Move-Item -LiteralPath $existing.FullName -Destination $archiveTarget
        $archivedAny += $existing.Name
    } else {
        Remove-Item -LiteralPath $existing.FullName -Force
    }
}

$previousCurrent = if (Test-Path $targetPath) { Get-Item $targetPath } else { $null }
Copy-Item -LiteralPath $SourceApk -Destination $targetPath -Force
Copy-Item -LiteralPath $SourceApk -Destination $latestPath -Force

Write-Host ""
Write-Host "AasPas APK Release"
Write-Host "------------------"
Write-Host "Version: $($version.VersionName)"
Write-Host "Build: $($version.VersionCode)"
Write-Host "Environment: $environment"
Write-Host ""
if ($archivedAny.Count -gt 0) {
    Write-Host "Previous:"
    foreach ($name in $archivedAny) {
        Write-Host "$name"
        Write-Host "-> archived"
    }
    Write-Host ""
} elseif ($previousCurrent) {
    Write-Host "Previous:"
    Write-Host "$fileName"
    Write-Host "-> replaced in current (same version republished)"
    Write-Host ""
}
Write-Host "Current:"
Write-Host $fileName
Write-Host ""
Write-Host "Path:"
Write-Host $targetPath
Write-Host ""
Write-Host "Latest alias:"
Write-Host $latestPath
Write-Host ""
