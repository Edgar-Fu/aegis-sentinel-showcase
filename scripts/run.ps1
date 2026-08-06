$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
& (Join-Path $PSScriptRoot "build.ps1")
$PortValue = if ($env:PORT) { $env:PORT } else { "8080" }
Write-Host "Open http://localhost:$PortValue" -ForegroundColor Cyan
Push-Location $Root
try {
    & java --add-modules jdk.httpserver "-Daegis.root=$Root" -jar (Join-Path $Root "build\aegis-sentinel-public-showcase.jar")
    if ($LASTEXITCODE -ne 0) { throw "Application exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
}
