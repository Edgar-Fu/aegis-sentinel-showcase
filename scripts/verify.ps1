$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
& (Join-Path $PSScriptRoot "build.ps1")
$TestClasses = Join-Path $Root "build\test-classes"
if (Test-Path $TestClasses) { Remove-Item $TestClasses -Recurse -Force }
New-Item -ItemType Directory -Path $TestClasses -Force | Out-Null
$TestSources = Get-ChildItem (Join-Path $Root "src\test\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 `
    -cp (Join-Path $Root "build\classes") -d $TestClasses $TestSources
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed with exit code $LASTEXITCODE" }
$Classpath = "$(Join-Path $Root 'build\classes');$TestClasses"
Push-Location $Root
try {
    & java --add-modules jdk.httpserver "-Daegis.root=$Root" -cp $Classpath com.aegis.showcase.ShowcaseSelfTest
    if ($LASTEXITCODE -ne 0) { throw "Verification failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
