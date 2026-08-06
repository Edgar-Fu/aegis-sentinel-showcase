$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Build = Join-Path $Root "build"
if (Test-Path $Build) { Remove-Item $Build -Recurse -Force }
$Evidence = Join-Path $Root "runtime\evidence"
Get-ChildItem $Evidence -Force | Where-Object Name -ne ".gitkeep" | Remove-Item -Recurse -Force
Write-Host "Removed generated build and runtime evidence." -ForegroundColor Green
