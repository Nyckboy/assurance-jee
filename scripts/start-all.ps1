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

# The config server starts first: the other applications read their configuration from it at startup.
$configServer = @{ Name = 'config-server'; Directory = 'config-server'; HealthUrl = 'http://localhost:8888/actuator/health' }

$applications = @(
    @{ Name = 'api-gateway'; Directory = 'api-gateway'; HealthUrl = 'http://localhost:8080/actuator/health' },
    @{ Name = 'service-polices'; Directory = 'service-polices'; HealthUrl = 'http://localhost:8081/actuator/health' },
    @{ Name = 'service-sinistres'; Directory = 'service-sinistres'; HealthUrl = 'http://localhost:8082/actuator/health' },
    @{ Name = 'service-indemnites'; Directory = 'service-indemnites'; HealthUrl = 'http://localhost:8083/actuator/health' }
)

function Start-Application($application) {
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

    Write-Host "Started $($application.Name) (launcher PID $($process.Id))."
    return [PSCustomObject]@{
        Name = $application.Name
        ProcessId = $process.Id
        HealthUrl = $application.HealthUrl
    }
}

function Wait-ForHealth($application, [int]$attempts = 30) {
    for ($attempt = 1; $attempt -le $attempts; $attempt++) {
        try {
            $response = Invoke-RestMethod -Uri $application.HealthUrl -TimeoutSec 2
            if ($response.status -eq 'UP') {
                Write-Host "$($application.Name) is UP: $($application.HealthUrl)"
                return $true
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }
    Write-Warning "$($application.Name) did not become healthy. Check .run/logs/$($application.Name).err.log"
    return $false
}

$startedProcesses = @()
try {
    $startedProcesses += Start-Application $configServer
    if (-not (Wait-ForHealth $startedProcesses[0] 60)) {
        throw 'The config server must be UP before the other applications start.'
    }

    foreach ($application in $applications) {
        $startedProcesses += Start-Application $application
    }

    $startedProcesses | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding utf8
}
catch {
    foreach ($process in $startedProcesses) {
        & taskkill.exe /PID $process.ProcessId /T /F 2>$null | Out-Null
    }
    throw
}

if ($WaitForHealth) {
    foreach ($application in $startedProcesses | Select-Object -Skip 1) {
        Wait-ForHealth $application | Out-Null
    }
}

Write-Host 'All launchers have started. Use .\scripts\stop-all.ps1 when you are done.'
