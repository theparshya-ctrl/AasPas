# Deploy AasPas External Beta to Fly.io (CURSOR-041B).
#
# STOP: Fly.io requires a credit card on file (pay-as-you-go). This script does NOT
# charge you, but will refuse to deploy until you confirm billing is acceptable.
#
# Prerequisites:
#   - flyctl installed: https://fly.io/docs/hands-on/install-flyctl/
#   - fly auth login
#   - Copy .env.beta.example to .env.beta and set SECRET_KEY
#
# Usage:
#   powershell -File scripts/deploy_beta_fly.ps1 -CheckOnly
#   powershell -File scripts/deploy_beta_fly.ps1 -Deploy

param(
    [switch]$CheckOnly,
    [switch]$Deploy
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host ""
Write-Host "AasPas Beta — Fly.io deployment helper"
Write-Host "======================================"
Write-Host ""

Write-Host "Cost check: verify current Fly.io / Oracle / Neon pricing before provisioning."
Write-Host "- Fly.io: credit card REQUIRED."
Write-Host "- Alternative: Oracle Always Free VM or Neon PostgreSQL — confirm current free-tier limits on provider sites."
Write-Host ""

$fly = Get-Command fly -ErrorAction SilentlyContinue
if (-not $fly) {
    Write-Host "STOP: flyctl not installed. Install from https://fly.io/docs/hands-on/install-flyctl/"
    Write-Host "Deployment package is ready at deploy/beta/fly.toml"
    exit 2
}

if (-not $Deploy) {
    Write-Host "Dry run / check only (pass -Deploy to create Fly resources)."
    Write-Host ""
    Write-Host "Before deploying:"
    Write-Host "1. Confirm credit card on Fly.io account is acceptable."
    Write-Host "2. Create .env.beta with a unique SECRET_KEY."
    Write-Host "3. fly apps create aaspas-beta --org personal  (if not exists)"
    Write-Host "4. fly volumes create aaspas_beta_data --region bom --size 1"
    Write-Host "5. fly secrets set SECRET_KEY=... DATABASE_URL=postgresql://... MEDIA_PUBLIC_BASE_URL=https://aaspas-beta.fly.dev BETA_ADMIN_EMAIL=... BETA_ADMIN_PASSWORD=..."
    Write-Host "6. fly deploy --config deploy/beta/fly.toml"
    Write-Host "7. fly ssh console -a aaspas-beta -C `"PYTHONPATH=/app/src python /app/scripts/seed_beta_admin.py`""
    exit 0
}

Write-Host "STOP: Automated deploy requires explicit billing approval."
Write-Host "Run the manual steps above after adding your payment method to Fly.io."
Write-Host "This protects against unexpected charges."
exit 3
