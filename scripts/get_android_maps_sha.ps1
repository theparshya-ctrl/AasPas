# Prints SHA-1 and SHA-256 for the Android debug keystore.
# Add these in Google Cloud Console when restricting your Maps API key.
#
# Package name: com.aaspas.customer

$debugKeystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
if (-not (Test-Path $debugKeystore)) {
    Write-Error "Debug keystore not found at $debugKeystore. Build the app once in Android Studio."
    exit 1
}

$keytoolCandidates = @(
    "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
    "$env:JAVA_HOME\bin\keytool.exe"
)

$keytool = $keytoolCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $keytool) {
    Write-Error "keytool not found. Install Android Studio or set JAVA_HOME."
    exit 1
}

Write-Host "AasPas Android debug certificate fingerprints"
Write-Host "Package name: com.aaspas.customer"
Write-Host ""

& $keytool -list -v `
    -keystore $debugKeystore `
    -alias androiddebugkey `
    -storepass android `
    -keypass android |
    Select-String -Pattern "SHA1:|SHA256:"

Write-Host ""
Write-Host "Google Cloud Console:"
Write-Host "  1. Enable 'Maps SDK for Android'"
Write-Host "  2. Create API key -> Restrict -> Android apps"
Write-Host "  3. Add package com.aaspas.customer + SHA-1 above"
Write-Host "  4. Set MAPS_API_KEY in android/local.properties"
Write-Host "  5. Rebuild: cd android; .\gradlew.bat installDebug"
