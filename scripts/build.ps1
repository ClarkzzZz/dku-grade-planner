$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force 'build/classes' | Out-Null
Get-ChildItem -Recurse 'build/classes' -Filter '*.class' | Remove-Item -Force
$sources = @(Get-ChildItem -Recurse 'src/main/java' -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -d build/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$version = (Get-Content -Raw 'VERSION').Trim()
"Implementation-Title: DKU Grade Planner`nImplementation-Version: $version`n" | Set-Content -Encoding ascii 'build/manifest.mf'
& jar --create --file build/dku-grade-planner.jar --manifest build/manifest.mf --main-class edu.dku.gradeplanner.App -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed' }
Write-Output 'Build complete: build/dku-grade-planner.jar (Java 21 compatible)'
