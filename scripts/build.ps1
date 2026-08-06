$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Build = Join-Path $Root "build"
$Classes = Join-Path $Build "classes"
if (Test-Path $Classes) { Remove-Item $Classes -Recurse -Force }
New-Item -ItemType Directory -Path $Classes -Force | Out-Null
$Sources = Get-ChildItem (Join-Path $Root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $Sources) { throw "No Java sources found." }
& javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d $Classes $Sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }
$Jar = Join-Path $Build "aegis-sentinel-public-showcase.jar"
& jar --create --file $Jar --main-class com.aegis.showcase.AegisShowcaseApplication -C $Classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }
Write-Host "Built $Jar" -ForegroundColor Green
