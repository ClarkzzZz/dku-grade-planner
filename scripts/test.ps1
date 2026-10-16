$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
& (Join-Path $PSScriptRoot 'build.ps1')
New-Item -ItemType Directory -Force 'build/test-classes' | Out-Null
Get-ChildItem -Recurse 'build/test-classes' -Filter '*.class' | Remove-Item -Force
$sources = @(Get-ChildItem -Recurse 'src/test/java' -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -cp build/classes -d build/test-classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -Djava.awt.headless=true -cp "build/classes;build/test-classes" edu.dku.gradeplanner.GradePlannerTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
