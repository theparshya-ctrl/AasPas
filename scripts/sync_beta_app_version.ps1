# Sync deploy/beta/app_version.json from android/app/build.gradle.kts version fields.
param(
    [string[]]$ReleaseNotes = @()
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$gradleFile = Join-Path $root "android\app\build.gradle.kts"
$versionFile = Join-Path $root "deploy\beta\app_version.json"

$content = Get-Content $gradleFile -Raw
if ($content -notmatch 'versionCode\s*=\s*(\d+)') { throw "versionCode not found in build.gradle.kts" }
$versionCode = [int]$matches[1]
if ($content -notmatch 'versionName\s*=\s*"([^"]+)"') { throw "versionName not found in build.gradle.kts" }
$versionName = $matches[1]

$existingNotes = @()
if (Test-Path $versionFile) {
    $existing = Get-Content $versionFile -Raw | ConvertFrom-Json
    if ($existing.release_notes) {
        $existingNotes = @($existing.release_notes)
    }
}

$notes = if ($ReleaseNotes.Count -gt 0) { $ReleaseNotes } else { $existingNotes }
if ($notes.Count -eq 0) {
    $notes = @("Beta release $versionName (build $versionCode)")
}

$payload = [ordered]@{
    latest_version_name = $versionName
    latest_version_code = $versionCode
    mandatory = $false
    release_notes = $notes
}

$json = $payload | ConvertTo-Json -Depth 4
Set-Content -Path $versionFile -Value $json -Encoding UTF8
Write-Host "Updated $versionFile -> $versionName (build $versionCode)"
