$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$stateFile = Join-Path $projectRoot '.run/processes.json'

if (-not (Test-Path -LiteralPath $stateFile)) {
    Write-Host 'No launcher state was found. Nothing to stop.'
    exit 0
}

$processes = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
foreach ($process in @($processes)) {
    & taskkill.exe /PID $process.ProcessId /T /F 2>$null | Out-Null
    Write-Host "Stopped $($process.Name) (launcher PID $($process.ProcessId))."
}

Remove-Item -LiteralPath $stateFile -Force
