# Publish a new AasPas Beta APK: build, sync version metadata, upload to Neon S3.
param(
    [string[]]$ReleaseNotes = @()
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host "Step 1/4: Build Beta APK (tests + lint + assemble)..."
& (Join-Path $root "Build-AasPas-BETA-APK.bat")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Step 2/4: Sync deploy/beta/app_version.json..."
& (Join-Path $PSScriptRoot "sync_beta_app_version.ps1") -ReleaseNotes $ReleaseNotes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Step 3/4: Upload APK to Neon public bucket..."
$envFile = Join-Path $root ".env.beta"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            $name = $matches[1]
            $value = $matches[2].Trim().Trim('"').Trim("'")
            if ($value) { Set-Item -Path "env:$name" -Value $value }
        }
    }
}
python (Join-Path $PSScriptRoot "upload_beta_apk.py") --apk (Join-Path $root "releases\current\latest.apk")
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "WARN: APK upload failed. Local release is ready; configure S3 in .env.beta and re-run upload."
    Write-Host "  python scripts/upload_beta_apk.py"
    exit $LASTEXITCODE
}

Write-Host "Step 4/4: Deploy backend on Render so app_version.json is live, then verify:"
Write-Host "  GET https://aaspas-beta.onrender.com/api/v1/app/version"
Write-Host ""
Write-Host "Done."
