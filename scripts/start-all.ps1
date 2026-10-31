param(
    [switch]$WaitForHealth
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$stateDirectory = Join-Path $projectRoot '.run'
$logDirectory = Join-Path $stateDirectory 'logs'
$stateFile = Join-Path $stateDirectory 'processes.json'

if (Test-Path -LiteralPath $stateFile) {
    throw "A previous launcher state exists at $stateFile. Run .\scripts\stop-all.ps1 first."
}

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

$applications = @(
    @{ Name = 'api-gateway'; Directory = 'api-gateway'; HealthUrl = 'http://localhost:8080/actuator/health' },
    @{ Name = 'service-polices'; Directory = 'service-polices'; HealthUrl = 'http://localhost:8081/actuator/health' },
    @{ Name = 'service-sinistres'; Directory = 'service-sinistres'; HealthUrl = 'http://localhost:8082/actuator/health' },
    @{ Name = 'service-indemnites'; Directory = 'service-indemnites'; HealthUrl = 'http://localhost:8083/actuator/health' }
)

$startedProcesses = @()
try {
    foreach ($application in $applications) {
        $applicationDirectory = Join-Path $projectRoot $application.Directory
        $stdout = Join-Path $logDirectory "$($application.Name).out.log"
        $stderr = Join-Path $logDirectory "$($application.Name).err.log"

        $process = Start-Process -FilePath (Join-Path $applicationDirectory 'mvnw.cmd') `
            -ArgumentList 'spring-boot:run' `
            -WorkingDirectory $applicationDirectory `
            -WindowStyle Hidden `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr `
            -PassThru

        $startedProcesses += [PSCustomObject]@{
            Name = $application.Name
            ProcessId = $process.Id
            HealthUrl = $application.HealthUrl
        }
        Write-Host "Started $($application.Name) (launcher PID $($process.Id))."
    }

    $startedProcesses | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding utf8
}
catch {
    foreach ($process in $startedProcesses) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
    throw
}

if ($WaitForHealth) {
    foreach ($application in $startedProcesses) {
        $healthy = $false
        for ($attempt = 1; $attempt -le 30; $attempt++) {
            try {
                $response = Invoke-RestMethod -Uri $application.HealthUrl -TimeoutSec 2
                if ($response.status -eq 'UP') {
                    Write-Host "$($application.Name) is UP: $($application.HealthUrl)"
                    $healthy = $true
                    break
                }
            }
            catch {
                Start-Sleep -Seconds 2
            }
        }
        if (-not $healthy) {
            Write-Warning "$($application.Name) did not become healthy. Check .run/logs/$($application.Name).err.log"
        }
    }
}

Write-Host 'All launchers have started. Use .\scripts\stop-all.ps1 when you are done.'
