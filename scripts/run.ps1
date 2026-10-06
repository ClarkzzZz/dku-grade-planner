$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
& java -jar (Join-Path $PSScriptRoot '../build/dku-grade-planner.jar')
