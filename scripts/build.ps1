$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force 'build/classes' | Out-Null
$sources = @(Get-ChildItem -Recurse 'src/main/java' -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -d build/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& jar --create --file build/dku-grade-planner.jar --main-class edu.dku.gradeplanner.App -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed' }
Write-Output 'Build complete: build/dku-grade-planner.jar (Java 21 compatible)'
